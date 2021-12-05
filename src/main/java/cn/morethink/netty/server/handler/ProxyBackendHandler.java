package cn.morethink.netty.server.handler;

import cn.morethink.netty.server.message.PingPong;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyBackendHandler extends SimpleChannelInboundHandler<String> {

    private Channel inboundChannel;
    private ProxyFrontendHandler proxyFrontendHandler;
    private String host;
    private int port;

    public ProxyBackendHandler(Channel inboundChannel, ProxyFrontendHandler proxyFrontendHandler, String host,
                               int port) {
        this.inboundChannel = inboundChannel;
        this.proxyFrontendHandler = proxyFrontendHandler;
        this.host = host;
        this.port = port;
    }

    // 当和目标服务器的通道连接建立时
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.debug("与{}建立连接", ctx.channel().remoteAddress());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
    /**
     * msg是从目标服务器返回的消息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, String msg) throws Exception {
        log.debug("{}服务器返回了消息", ctx.channel().remoteAddress());
        if(msg.startsWith(PingPong.IAMALIVE)) {
            log.info(msg);
        }
        //接收目标服务器发送来的数据并打印 然后把数据写入代理服务器和客户端的通道里
        //通过inboundChannel向客户端写入数据
        inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    closeOnFlush(future.channel());
//                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("{}服务器关闭连接", ctx.channel().remoteAddress());
        if (proxyFrontendHandler.isConnect()) {
            proxyFrontendHandler.createBootstrap(inboundChannel, host, port);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("发生异常：", cause);
//        ctx.channel().close();
        closeOnFlush(ctx.channel());
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                if (proxyFrontendHandler.isConnect()) {
                    return;
                }
                log.debug("空闲时间到，关闭连接.");
//                ctx.channel().close();
                closeOnFlush(ctx.channel());
            }
        }
    }
}