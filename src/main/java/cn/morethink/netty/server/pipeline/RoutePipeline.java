package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.router.HttpRouter;
import cn.morethink.netty.router.handler.RouterHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class RoutePipeline extends ChannelInitializer<Channel> {

    private final HttpRouter httpRouter;

    public RoutePipeline(HttpRouter httpRouter) {
        this.httpRouter = httpRouter;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        // HTTP 服务的解码器
        p.addLast(new HttpServerCodec());
        // HTTP 消息的合并处理
        p.addLast(new HttpObjectAggregator(10 * 1024));
        // 路由处理
        p.addLast(new RouterHandler(httpRouter));

    }

}
