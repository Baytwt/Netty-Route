package cn.morethink.netty.server;

import cn.morethink.netty.server.pipeline.FrontendPipeline;
import cn.morethink.netty.server.pipeline.HeartBeatServerPipeline;
import cn.morethink.netty.server.session.BackendSessionMemoryImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Component
@Slf4j
public class GatewayServer {

    @Autowired
    FrontendPipeline frontendPipeline;

    private final int port;

    public static BackendSessionMemoryImpl backendSessionMemory;

//    public static FrontendPortRepository frontendPortRepository;

//    private List<BackendServerInfo> backendServerInfoList;

    public GatewayServer(int port) {
        this.port = port;
        backendSessionMemory = new BackendSessionMemoryImpl();
    }

    public static void main(String[] args) {
        GatewayServer gatewayServer = new GatewayServer(8090);
        gatewayServer.start();

    }

    /**
     *
     */
    public void start() {
        // 初始化两个线程池，一个负责接受新的连接，一个负责处理读写
        // bossGroup负责与客户端建立连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // workerGroup负责与后端服务器建立连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 客户端请求转发服务
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    // nio类型的channel
                    .channel(NioServerSocketChannel.class)
                    .localAddress(port)
                    // handler在初始化时就执行，而childHandler在客户端成功connect后才执行
                    .childHandler(new FrontendPipeline());

            // 后端服务器注册服务
            ServerBootstrap heartBeatBootstrap = new ServerBootstrap();
            heartBeatBootstrap.group(workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HeartBeatServerPipeline())
                    // 保持连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(port).sync();
            ChannelFuture bf = heartBeatBootstrap.bind(8091).sync();
            log.info("http://127.0.0.1:{}/ 反向代理服务已启动", ((InetSocketAddress) f.channel().localAddress()).getPort());
            log.info("http://127.0.0.1:{}/ 注册服务已启动", ((InetSocketAddress) bf.channel().localAddress()).getPort());
            f.channel().closeFuture().sync();
            bf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("代理服务关闭!");
        } catch (Exception e) {
            log.error("代理服务启动失败!", e);
        } finally {
            log.info("代理服务优雅关闭!");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }
}
