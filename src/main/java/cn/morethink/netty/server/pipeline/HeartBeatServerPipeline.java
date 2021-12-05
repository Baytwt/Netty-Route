package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.server.handler.HeartBeatServerHandler;
import cn.morethink.netty.server.handler.LiveDecoder;
import cn.morethink.netty.server.handler.LiveEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 心跳检测
 */
@Component("heartBeatServerPipeline")
public class HeartBeatServerPipeline extends ChannelInitializer<SocketChannel> {

    private static final int READ_IDEL_TIME_OUT = 2; // 读空闲
    private static final int WRITE_IDEL_TIME_OUT = 2;// 写空闲
    private static final int ALL_IDEL_TIME_OUT = 2; // 所有空闲
    private static final TimeUnit TIME_OUT_UNIT = TimeUnit.MINUTES; // 时间单位

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(READ_IDEL_TIME_OUT, WRITE_IDEL_TIME_OUT, ALL_IDEL_TIME_OUT, TIME_OUT_UNIT));
        pipeline.addLast("LiveDecoder", new LiveDecoder());
        pipeline.addLast("LiveEncoder", new LiveEncoder());
        pipeline.addLast("HeartBeatHostHandler", new HeartBeatServerHandler());

    }
}
