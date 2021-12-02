package cn.morethink.netty.demo;

import cn.morethink.netty.demo.controller.DemoController;
import cn.morethink.netty.router.HttpRouter;
import cn.morethink.netty.router.handler.RouterHandler;
import cn.morethink.netty.server.HttpServer;
import cn.morethink.netty.server.ReverseProxyServer;
import cn.morethink.netty.server.handler.ProxyBackendHandler;
import com.baomidou.mybatisplus.extension.api.R;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author 李文浩
 * @date 2018/9/5
 */
@SpringBootApplication
public class Demo {
    public static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
//        HttpServer httpServer = new HttpServer();
//        httpServer.start(PORT);

//        ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
//        reverseProxyServer.start(8090);

    }
}
