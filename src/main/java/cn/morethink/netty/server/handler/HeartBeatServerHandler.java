package cn.morethink.netty.server.handler;

import cn.morethink.netty.demo.dao.BackendServerInfo;
import cn.morethink.netty.server.GatewayServer;
import cn.morethink.netty.server.message.LiveChannelCache;
import cn.morethink.netty.server.message.LiveMessage;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 心跳检测（代理服务器响应来自后端服务器的ping）
 * https://www.cnblogs.com/demingblog/p/9957143.html
 */
@Component
@Slf4j
public class HeartBeatServerHandler extends SimpleChannelInboundHandler<LiveMessage> {

    private static Map<Integer, LiveChannelCache> channelCache = new HashMap<>();
    
    int readIdleTimes = 0;

    /**
     * 检测到某后端服务器长时间空闲
     * 发送心跳验证其存活
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent)evt;

        String eventType = null;
        switch (event.state()){
            case READER_IDLE:
                eventType = "读空闲";
                readIdleTimes ++; // 读空闲的计数加1
                break;
            case WRITER_IDLE:
                eventType = "写空闲";
                // 不处理
                break;
            case ALL_IDLE:
                eventType ="读写空闲";
                // 不处理
                break;
            default:
                break;
        }
        log.debug(ctx.channel().remoteAddress() + "超时事件：" +eventType);
        if(readIdleTimes > 3){
//            log.info("{}读空闲超过3次，关闭连接", ctx.channel().remoteAddress());
            // 空闲超过3次
            LiveMessage msg = new LiveMessage().heartBeatMessage("2");
            ChannelFuture f = ctx.channel().writeAndFlush(msg);


//            ctx.channel().close();
        }
    }

    /**
     * 处理来自后端服务器的消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LiveMessage msg) throws Exception {
        Channel channel = ctx.channel();
        final int hashCode = channel.hashCode();
        log.debug("来自[{}]消息:{}", ctx.channel().remoteAddress(), msg.toString());
//        if (!channelCache.containsKey(hashCode)) {
//            log.debug("channelCache.containsKey(hashCode), put key:" + hashCode);
//            channel.closeFuture().addListener(future -> {
//                log.debug("channel close, remove key:" + hashCode);
//                channelCache.remove(hashCode);
//            });
//            ScheduledFuture scheduledFuture = ctx.executor().schedule(
//                    () -> {
//                        log.debug("schedule runs, close channel:" + hashCode);
//                        channel.close();
//                    }, 10, TimeUnit.SECONDS);
//            channelCache.put(hashCode, new LiveChannelCache(channel, scheduledFuture));
//        }

        switch (msg.getType()) {
            case LiveMessage.TYPE_HEART: {
                // 心跳
                String content = msg.getContent();
                String address = channel.remoteAddress().toString();
                log.debug("接到心跳来自{} 内容{}", channel.remoteAddress(), content);
                // 检查通道池
                if(!GatewayServer.backendSessionMemory.containsChannel(channel)) {
                    BackendServerInfo info = new BackendServerInfo(address);
                    GatewayServer.backendSessionMemory.add(channel, address);
                }
                LiveChannelCache cache = channelCache.get(hashCode);
                // 计划5秒后关闭连接
//                ScheduledFuture scheduledFuture = ctx.executor().schedule(() -> channel.close(), 5, TimeUnit.SECONDS);
//                cache.getScheduledFuture().cancel(true);
//                cache.setScheduledFuture(scheduledFuture);
                ctx.channel().writeAndFlush(msg);
                break;
            }
            case LiveMessage.TYPE_MESSAGE: {
                channelCache.entrySet().stream().forEach(entry -> {
                    Channel otherChannel = entry.getValue().getChannel();
                    otherChannel.writeAndFlush(msg);
                });
                break;
            }
            default: {
                break;
            }
        }

//        if(PingPong.IAMALIVE.equals(s)){
//            ctx.channel().writeAndFlush(PingPong.COPYTHAT);
//        }else {
//            log.debug(" 其他信息处理 ... ");
//        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("{}状态正常", ctx.channel().remoteAddress());
    }
}
