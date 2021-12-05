package cn.morethink.netty.server.handler;

import cn.morethink.netty.server.GatewayServer;
import cn.morethink.netty.server.message.LiveChannelCache;
import cn.morethink.netty.server.message.LiveMessage;
import cn.morethink.netty.server.message.PingPong;
import cn.morethink.netty.server.pipeline.HeartBeatClientPipeline;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ChannelHandler.Sharable
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<LiveMessage> {

    private Channel inboundChannel;
    private ProxyFrontendHandler proxyFrontendHandler;
    // 代理服务器的地址和端口
    private String serverHost;
    private int serverPort;
    // 本机的地址和端口
    private String myHost;
    private int myPort;

    // 心跳的时间间隔，单位为s
    private static final int HEARTBEAT_INTERVAL = 1;
    // 心跳值 用于验证
    private static AtomicInteger heartBeat = new AtomicInteger(0);

    // 缓存ChannelHandlerContext
    private ChannelHandlerContext ctx;

    public HeartBeatClientHandler(Channel inboundChannel){
        this.inboundChannel = inboundChannel;
    }

    /** 触发事件时执行
     *  空闲10秒时，发送心跳包到网关服务器
      */

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Channel ch = ctx.channel();
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                // 向网关服务器发送消息
                LiveMessage msg = new LiveMessage();
                msg.setType(LiveMessage.TYPE_HEART);
                msg.setContent(heartBeat.toString());
                log.info("我已经10秒空闲。发送心跳信息→{}: {}", ch.remoteAddress(), msg.toString());
                ChannelFuture f = ctx.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            log.info("连接成功");
                        } else {
                            log.error("连接失败" + future.channel().toString());  //TODO
                            if (ch.isActive()) {
                                log.info("重连中…");
                                // 定时重新执行本方法以尝试重连
                                final EventLoop loop = future.channel().eventLoop();
                                loop.schedule(new Runnable() {
                                    @SneakyThrows
                                    @Override
                                    public void run() {
                                        HeartBeatClientHandler.this.userEventTriggered(ctx, evt);
                                    }
                                }, 5000, TimeUnit.MILLISECONDS);
                            } else {
                                log.info("连接被关闭。网关服务器似乎挂了");
                            }
                        }
                        ch.read();
                    }
                });
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    // 添加到pipeline时自动触发
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    //在通道被激活时，开始发送心跳
//    @Override
//    public void channelActive(ChannelHandlerContext ctx)
//            throws Exception
//    {
////        ClientSession session = ClientSession.getSession(ctx);
////        UserDTO user = session.getUser();
////        HeartBeatMsgBuilder builder =
////                new HeartBeatMsgBuilder(user, session);
////
////        ProtoMsg.Message message = builder.buildMsg();
//
//
//        log.debug("与{}建立连接", ctx.channel().remoteAddress());
//        //发送心跳
//        String heartbeatMsg = "i am alive from client[" +
//                myPort+":"+myHost+"]";
//        heartBeat(this.ctx, heartbeatMsg);
//    }

    //使用定时器，发送心跳报文
    public void heartBeat(ChannelHandlerContext ctx,
                          String heartbeatMsg)
    {
        ctx.executor().schedule(() ->
        {

            if (ctx.channel().isActive())
            {
                log.info("发送心跳消息[{}] → [{}]",heartbeatMsg, ctx.channel().remoteAddress());
                ctx.writeAndFlush(heartbeatMsg);

                //递归调用，发送下一次的心跳
                heartBeat(ctx, heartbeatMsg);
            }

        }, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 接到来自网关服务器的消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LiveMessage msg) throws Exception {
        Channel ch = ctx.channel();
        String content = msg.getContent();
        switch (msg.getType()) {
            case LiveMessage.TYPE_HEART: {
                // 心跳验证
                log.debug("接到心跳回写来自{} 内容{}", ch.remoteAddress(), content);
                if (!content.equals(heartBeat.toString())) {
                    // 心跳验证值错误
                    log.error("心跳验证值错误");
                }
//                ctx.channel().writeAndFlush(msg);
                break;
            }
            case LiveMessage.TYPE_MESSAGE: {
                log.debug("接到消息来自{} 内容{}", ch.remoteAddress(), content);
                break;
            }
            default: {
                break;
            }
        }
    }
}
