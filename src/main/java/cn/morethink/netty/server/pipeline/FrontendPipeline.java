package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.demo.controller.HelloController;
import cn.morethink.netty.demo.controller.RouteController;
import cn.morethink.netty.router.handler.RouterHandler;
import cn.morethink.netty.server.codec.FrontendDecode;
import cn.morethink.netty.server.codec.FrontendEncode;
import cn.morethink.netty.server.handler.LoggedIdleStateHandler;
import cn.morethink.netty.server.handler.ProxyFrontendHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 客户端 → 网关服务器 流水线
 */
@Component("frontendPipeline")
public class FrontendPipeline extends ChannelInitializer<SocketChannel> {

    private static final int READ_IDEL_TIME_OUT = 10; // 读超时
    private static final int WRITE_IDEL_TIME_OUT = 10;// 写超时
    private static final int ALL_IDEL_TIME_OUT = 10; // 所有超时
    private static final TimeUnit TIME_OUT_UNIT = TimeUnit.SECONDS; // 超时时间单位

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("idleStateHandler", new LoggedIdleStateHandler(READ_IDEL_TIME_OUT, WRITE_IDEL_TIME_OUT, ALL_IDEL_TIME_OUT,TIME_OUT_UNIT));
        // HTTP 服务的解码器
        pipeline.addLast(new HttpServerCodec());
        // HTTP 消息的合并处理
        pipeline.addLast(new HttpObjectAggregator(10 * 1024));
        // 路由
//        pipeline.addLast(new RouterHandler(HelloController.class.getName()));
        pipeline.addLast("ProxyFrontendHandler", new ProxyFrontendHandler());

    }
}
