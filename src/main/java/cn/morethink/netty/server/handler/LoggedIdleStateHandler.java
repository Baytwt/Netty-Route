package cn.morethink.netty.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author linjiashen
 */
@Slf4j
public class LoggedIdleStateHandler extends IdleStateHandler {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("与[{}]连接激活", ctx.channel().remoteAddress().toString());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("与[{}]连接丢失", ctx.channel().remoteAddress().toString());
        super.channelInactive(ctx);
    }

    public LoggedIdleStateHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

}