package cn.morethink.netty.server.handler;


import cn.morethink.netty.demo.dao.BackendServerInfo;
import cn.morethink.netty.demo.dao.BackendServerRepository;
import cn.morethink.netty.server.config.AppConfig;
import cn.morethink.netty.server.pipeline.BackendPipeline;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProxyFrontendHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger log = LoggerFactory.getLogger(ProxyFrontendHandler.class);
    private static final EventLoopGroup proxyGroup = new NioEventLoopGroup();
    /**
     * 代理服务器和目标服务器之间的通道（从代理服务器出去所以是outbound过境）
     */
    private ChannelGroup allChannels = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private BackendServerRepository backendServerRepository;

    private volatile boolean frontendConnectStatus = false;


    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 当客户端和代理服务器建立通道连接时，调用此方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        frontendConnectStatus = true;

        SocketAddress clientAddress = ctx.channel().remoteAddress();
        log.info("客户端地址：" + clientAddress);

        // 获取全部后端服务器信息
//        List<BackendServerInfo> backendServerInfoList = backendServerRepository.selectList(null);
        // mybatis未配置好，先用手写的数据
        List<BackendServerInfo> backendServerInfoList = new ArrayList<>();
        backendServerInfoList.add(new BackendServerInfo("1", "127.0.0.1", 8081));
        backendServerInfoList.add(new BackendServerInfo("2", "127.0.0.1", 8082));
        backendServerInfoList.add(new BackendServerInfo("3", "127.0.0.1", 8083));

        // 新建通道，作为客户端和代理服务器的连接通道 入境的通道
        Channel inboundChannel = ctx.channel();

        // 为每个后端服务器创建一个bootstrap
        for (BackendServerInfo backendServerInfo : backendServerInfoList) {
            createBootstrap(inboundChannel, backendServerInfo.getIp(), backendServerInfo.getPort());
        }
    }

    /**
     * 在这里接收客户端的消息 在客户端和代理服务器建立连接时，也获得了代理服务器和目标服务器的通道outbound，
     * 通过outbound写入消息到目标服务器
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, byte[] msg) throws Exception {

        log.debug("收到消息来自{}", ctx.channel().remoteAddress());
        allChannels.writeAndFlush(msg).addListener(new ChannelGroupFutureListener() {
            @Override
            public void operationComplete(ChannelGroupFuture future) throws Exception {
                //防止出现发送不成功造成的永久不读取消息的错误.
                ctx.channel().read();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        log.debug("代理服务器和客户端{}断开连接", ctx.name());
        frontendConnectStatus = false;
//        allChannels.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("发生异常：", cause);
        ctx.channel().close();
    }

    public void createBootstrap(final Channel inboundChannel, final String host, final int port) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.SO_KEEPALIVE,true)
                    .group(proxyGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new BackendPipeline(inboundChannel, ProxyFrontendHandler.this, host, port));

            // 与后端服务器进行连接，并异步监听结果
            ChannelFuture f = bootstrap.connect(host, port);
            log.debug("代理服务器准备连接到{}:{}", host, port);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        log.info("连接成功");
                        allChannels.add(future.channel());

                    } else {
                        log.error("连接{}失败", future.channel().remoteAddress().toString());
                        if (inboundChannel.isActive()) {
                            log.info("重连中…");
                            // 定时重新执行本方法以尝试重连
                            final EventLoop loop = future.channel().eventLoop();
                            loop.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    ProxyFrontendHandler.this.createBootstrap(inboundChannel, host, port);
                                }
                            }, appConfig.getInterval(), TimeUnit.MILLISECONDS);
                        } else {
                            log.info("服务器{}似乎挂了", future.channel().remoteAddress().toString());
                        }
                    }
                    inboundChannel.read();
                }
            });

        } catch (Exception e) {
            log.error("连接后台服务失败", e);
        }
    }

    public boolean isConnect() {
        return frontendConnectStatus;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                log.info("空闲时间到，关闭连接.");
                frontendConnectStatus = false;
               allChannels.close();
//                ctx.channel().close();
                closeOnFlush(ctx.channel());
            }
        }
    }

}