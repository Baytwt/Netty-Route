package cn.morethink.netty.server.handler;

import cn.morethink.netty.server.message.LiveMessage;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ChannelHandler.Sharable
public class BackendServerHandler extends SimpleChannelInboundHandler<LiveMessage> {

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

    public BackendServerHandler(Channel inboundChannel, String myHost, int myPort){
        this.inboundChannel = inboundChannel;
        this.myHost = myHost;
        this.myPort = myPort;
    }

    /** 触发事件时执行
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
                msg.setContent("heart beat|" + myHost+":"+myPort +  "|" + heartBeat.toString());
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
                                        BackendServerHandler.this.userEventTriggered(ctx, evt);
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
                String returnedHeartBeat =content.split("\\|")[2];
                if (!returnedHeartBeat.equals(heartBeat.toString())) {
                    // 心跳验证值错误
                    log.error("心跳验证值错误");
                } else {
                    log.debug("心跳验证值正确");
                    heartBeat.incrementAndGet();
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
