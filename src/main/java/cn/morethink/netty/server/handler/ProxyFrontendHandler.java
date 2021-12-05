package cn.morethink.netty.server.handler;


import cn.morethink.netty.demo.dao.BackendServerInfo;
import cn.morethink.netty.demo.dao.BackendServerRepository;
import cn.morethink.netty.server.GatewayServer;
import cn.morethink.netty.server.message.LiveMessage;
import cn.morethink.netty.server.pipeline.BackendPipeline;
import cn.morethink.netty.server.session.BackendSessionMemoryImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ProxyFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private volatile boolean frontendConnectStatus = false;

    private static final EventLoopGroup proxyGroup = new NioEventLoopGroup();

    /**
     * 代理服务器和目标服务器之间的通道（从代理服务器出去所以是outbound过境）
     */
    private ChannelGroup allChannels = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    /**
     * 当客户端和代理服务器建立通道连接时，调用此方法
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        frontendConnectStatus = true;

        SocketAddress clientAddress = ctx.channel().remoteAddress();
        log.info("客户端地址: " + clientAddress);
    }

    /**
     * 在这里接收客户端的消息
     * 取得代理服务器和目标服务器的通道outbound，通过outbound写入消息到目标服务器
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        // 解码失败
        if(!request.decoderResult().isSuccess()){
            sendError(ctx, BAD_REQUEST);
            return;
        }
        // 只支持GET方法
        if(request.method() != GET) {
           sendError(ctx, METHOD_NOT_ALLOWED);
           return;
        }

        log.debug("收到来自{} 的http请求：{}", ctx.channel().remoteAddress(), request.toString());

        HttpMethod method = request.method();
        String uri = request.uri();
        String address = ctx.channel().remoteAddress().toString();

        // 从信息表取得对应的后端服务器信息
        BackendSessionMemoryImpl foo = GatewayServer.backendSessionMemory;
        BackendServerInfo info = foo.getBackendServerInfo(method, uri);
        if(info != null) {
            // 根据请求url与对应的后端服务器建立连接，然后得到消息后写回
            createBootstrap(ctx.channel(), info.getIp(), info.getPort());
        } else {
            // 没有合适的后端服务器可供转发
            sendError(ctx, NOT_FOUND);
        }

//        if(outboundChannel != null) {
//            // 将消息转发到此channel中，并监听结果
//            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
//            response.headers().set(CONTENT_TYPE, "test/html;charset=UTF-8");
//            response.content().writeBytes("Hello Netty".getBytes(StandardCharsets.UTF_8));
//            ByteBuf msg = request.content();
//            outboundChannel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
//
//            Bootstrap bootstrap = new Bootstrap();
//            bootstrap.option(ChannelOption.SO_KEEPALIVE,true)
//                    .group(proxyGroup)
//                    .channel(NioSocketChannel.class)
//                    ;
//
//        } else {
//            // 没有合适的后端服务器可供转发
////            sendError();
//        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        log.debug("代理服务器和客户端{}断开连接", ctx.channel().remoteAddress());
        frontendConnectStatus = false;
//        allChannels.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("发生异常：", cause);
        ctx.channel().close();
    }


    public boolean isConnect() {
        return frontendConnectStatus;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 发生超时时间，关闭连接
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                log.info("空闲时间到，关闭连接.");
                frontendConnectStatus = false;
                Channel ch = ctx.channel();
                if (ch.isActive()) {
                    ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    /**
     *  返回错误信息
      */
    private static void sendError(ChannelHandlerContext ctx,
                                  HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status, Unpooled.copiedBuffer("Failure: " + status.toString()
                + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    public void createBootstrap(final Channel inboundChannel, final String host, final int port) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.SO_KEEPALIVE,true)
                    .group(proxyGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new BackendPipeline(inboundChannel, ProxyFrontendHandler.this, host, port));

            // 与后端服务器进行连接，并异步监听结果
            log.debug("代理服务器准备连接到{}:{}", host, port);
            ChannelFuture f = bootstrap.connect(host, port);
            f.channel().writeAndFlush(
                    new LiveMessage().helloMessage()
            ).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        log.info("连接成功");
                        allChannels.add(future.channel());

                    } else {
                        log.error("连接失败" + future.channel().toString());  //TODO
                        if (inboundChannel.isActive()) {
                            log.info("重连中…");
                            // 定时重新执行本方法以尝试重连
                            final EventLoop loop = future.channel().eventLoop();
                            loop.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    ProxyFrontendHandler.this.createBootstrap(inboundChannel, host, port);
                                }
//                            }, appConfig.getInterval(), TimeUnit.MILLISECONDS);
                            }, 5000, TimeUnit.MILLISECONDS);
                        } else {
                            log.info("服务器似乎挂了");
                        }
                    }
                    inboundChannel.read();
                }
            });

        } catch (Exception e) {
            log.error("连接后台服务失败", e);
        }
    }
}

