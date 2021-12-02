package cn.morethink.netty.server;

import cn.morethink.netty.demo.controller.DemoController;
import cn.morethink.netty.demo.controller.HelloController;
import cn.morethink.netty.demo.controller.RouteController;
import cn.morethink.netty.router.HttpRouter;
import cn.morethink.netty.router.handler.RouterHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

/**
 * 路由服务器
 */
@Slf4j
public class HttpServer {

    public static void main(String[] args) throws InterruptedException {
        HttpServer httpServer = new HttpServer();
        httpServer.start(8081, new HelloController());
    }

    public void start(int port, RouteController routeController) throws InterruptedException {

        HttpRouter httpRouter = new HttpRouter();
        httpRouter.addRouter(routeController.getClass().getName());

        EventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        //HTTP 服务的解码器
                        p.addLast(new HttpServerCodec());
                        //HTTP 消息的合并处理
                        p.addLast(new HttpObjectAggregator(10 * 1024));
                        //自己写的路由处理
                        p.addLast(new RouterHandler(httpRouter));
                    }
                });

        Channel ch = b.bind(port).sync().channel();
        log.info("http://127.0.0.1:{}/ HttpServer已启动", port);
        ch.closeFuture().sync();
    }
}
