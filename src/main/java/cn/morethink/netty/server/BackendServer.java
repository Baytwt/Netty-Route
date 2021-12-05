package cn.morethink.netty.server;

import cn.morethink.netty.demo.controller.HelloController;
import cn.morethink.netty.router.handler.RouterHandler;
import cn.morethink.netty.server.handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 后端服务器
 */
@Slf4j
public class BackendServer {

    public int gatewayPort = 8091;
    public String myHost = "127.0.0.1";
    public int myPort = 8081;

    public static AtomicInteger heartBeat;

    BackendServer(int gatewayPort, int myPort) {
        heartBeat = new AtomicInteger(0);
        this.gatewayPort = gatewayPort;
        this.myPort = myPort;
    }

    public static void main(String[] args) throws InterruptedException {
        BackendServer backendServer = new BackendServer(8091, 8081);
        backendServer.start();
    }

    public void start() throws InterruptedException {

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap sb = new ServerBootstrap();
            sb.group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // HTTP 服务的解码器
                            p.addLast(new HttpServerCodec());
                            // HTTP 消息的合并处理
                            p.addLast(new HttpObjectAggregator(10 * 1024));
                            // 路由处理
                            p.addLast(new RouterHandler(HelloController.class.getName()));
                        }
                    });
            // 接收http消息
            ChannelFuture receiveFuture = sb.bind(new InetSocketAddress("127.0.0.1", myPort)).sync();
            log.info("http://127.0.0.1:{}/ 后端HttpServer已启动", myPort);

            Bootstrap beat = new Bootstrap();
            beat.group(group)
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
                        pipeline.addLast(new BackendServerHandler(ch, myHost, myPort));
                    }
                });

            // 连接到网关服务器
            ChannelFuture connectFuture = beat.connect(
                    new InetSocketAddress("127.0.0.1", gatewayPort)
            ).sync();


            // 等待直到channel关闭
            ChannelFuture closeFuture = connectFuture.channel().closeFuture();
            closeFuture.sync();

        } catch (InterruptedException e) {
            log.error("服务关闭!");
        } catch (Exception e) {
            log.error("服务启动失败!", e);
        } finally {
            log.info("服务优雅关闭!");
            group.shutdownGracefully();
        }

    }
}

