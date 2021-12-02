package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.server.codec.FrontendDecode;
import cn.morethink.netty.server.codec.FrontendEncode;
import cn.morethink.netty.server.handler.ProxyFrontendHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("frontendPipeline")
public class FrontendPipeline extends ChannelInitializer<SocketChannel> {

    private static final int READ_IDEL_TIME_OUT = 3; // 读超时
    private static final int WRITE_IDEL_TIME_OUT = 4;// 写超时
    private static final int ALL_IDEL_TIME_OUT = 5; // 所有超时
    private static final TimeUnit TIME_OUT_UNIT = TimeUnit.SECONDS; // 超时时间单位

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 注册handler
        pipeline.addLast("idleStateHandler", new IdleStateHandler(READ_IDEL_TIME_OUT, WRITE_IDEL_TIME_OUT, ALL_IDEL_TIME_OUT,
                TIME_OUT_UNIT)); // 设定超时时间
        pipeline.addLast("FrontendDecode", new FrontendDecode());
        pipeline.addLast("FrontendEncode", new FrontendEncode());
        pipeline.addLast("ProxyFrontendHandler", new ProxyFrontendHandler());

    }
}
