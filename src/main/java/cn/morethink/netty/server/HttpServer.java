package cn.morethink.netty.server;

import cn.morethink.netty.demo.controller.HelloController;
import cn.morethink.netty.demo.controller.RouteController;
import cn.morethink.netty.router.HttpRouter;
import cn.morethink.netty.router.handler.RouterHandler;
import cn.morethink.netty.server.handler.*;
import cn.morethink.netty.server.message.LiveMessage;
import cn.morethink.netty.server.pipeline.HeartBeatClientPipeline;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 后端服务器
 */
@Slf4j
public class HttpServer {

    public static Channel connection;
    public static AtomicInteger heartBeat;

    HttpServer() {
        heartBeat = new AtomicInteger(0);
    }

    public static void main(String[] args) throws InterruptedException {
        HttpServer httpServer = new HttpServer();
        httpServer.start(8081, new HelloController());
    }

    public void start(int port, RouteController routeController) throws InterruptedException {

        HttpRouter httpRouter = new HttpRouter();
        httpRouter.addRouter(routeController.getClass().getName());

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap sb = new Bootstrap();
        sb.option(ChannelOption.SO_BACKLOG, 1024)
            .group(group)
            .channel(NioSocketChannel.class)
            // 接收来自代理服务器的HTTP消息
            .handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    // 每隔5秒来检查一下channelRead方法被调用的情况，如果在5秒内该链上的channelRead方法都没有被触发，就会调用userEventTriggered方法
                    pipeline.addLast(new LoggedIdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                    pipeline.addLast(new LiveDecoder());
                    pipeline.addLast(new LiveEncoder());
                    pipeline.addLast(new HeartBeatClientHandler(ch));
                }
            })
            .bind(8081);

//            .childHandler(new RoutePipeline(httpRouter));
        ChannelFuture receiver = sb.connect("localhost", 8091).sync();
        log.info("http://127.0.0.1:{}/ HttpServer已启动", port);

//        EventLoopGroup beatGroup = new NioEventLoopGroup();
//        Bootstrap b = new Bootstrap();
//        b.group(beatGroup)
//                .channel(NioSocketChannel.class)
//                // 主动发送心跳
//                .handler(new HeartBeatClientPipeline())
//                .bind(port);
//        ChannelFuture beater = b.connect("localhost", 8091).sync();
//        connection = beater.channel();
//        log.info("建立连接→" + connection.remoteAddress());
    }
}
