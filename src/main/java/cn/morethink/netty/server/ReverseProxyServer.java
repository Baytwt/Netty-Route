package cn.morethink.netty.server;

import cn.morethink.netty.demo.dao.FrontendPortInfo;
import cn.morethink.netty.demo.dao.FrontendPortRepository;
import cn.morethink.netty.server.config.AppConfig;
import cn.morethink.netty.server.handler.ProxyBackendHandler;
import cn.morethink.netty.server.pipeline.FrontendPipeline;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;

@Component
@Slf4j
public class ReverseProxyServer {

    @Autowired
    FrontendPipeline frontendPipeline;

    @Autowired
    AppConfig appConfig;

    @Autowired
    private FrontendPortRepository frontendPortRepository;

    public static void main(String[] args) {
        ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
        reverseProxyServer.start(8090);

    }

    public void start(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 添加流水线
                    // handler在初始化时就执行，而childHandler在客户端成功connect后才执行
                    .childHandler(new FrontendPipeline())
                    // 每次读操作完毕后需要用户手动调用channel.read()
                    .childOption(ChannelOption.AUTO_READ, false);


            // 从数据库获取要暴露给客户端的前端端口
//            Page<FrontendPortInfo> page = new Page<>(1, 1);
//            //这里设置不进行count查询
//            page.setTotal(1);
//            FrontendPortInfo frontendPortInfo = frontendPortRepository.selectPage(page, null).getRecords().get(0);
//            ChannelFuture f = bootstrap.bind(frontendPortInfo.getPort()).sync();
            ChannelFuture f = bootstrap.bind(port).sync();
            log.info("http://127.0.0.1:{}/ 反向代理服务已启动", ((InetSocketAddress) f.channel().localAddress()).getPort());
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.info("代理服务关闭!");
        } catch (Exception e) {
            log.info("代理服务启动失败!", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }
}
