package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.server.handler.HeartBeatClientHandler;
import cn.morethink.netty.server.handler.LiveDecoder;
import cn.morethink.netty.server.handler.LiveEncoder;
import cn.morethink.netty.server.handler.LoggedIdleStateHandler;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class HeartBeatClientPipeline extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
//        Bootstrap bootstrap = new Bootstrap();
//        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
//                .group( new NioEventLoopGroup())
//                .channel(NioSocketChannel.class)
//                .handler(new HeartBeatClientHandler(ch, serverHost, serverPort, myHost, myPort));
//
//        // 与代理服务器进行连接，并异步监听结果
//        ChannelFuture f = bootstrap.connect(serverHost, serverPort);
//        log.debug("后端服务器准备连接到{}:{}", serverHost, serverPort);
//        f.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//
//                if (future.isSuccess()) {
//                    log.info("连接成功");
//
//                } else {
//                    log.error("连接失败" + future.channel().toString());  //TODO
//                    if (ch.isActive()) {
//                        log.info("重连中…");
//                        // 定时重新执行本方法以尝试重连
//                        final EventLoop loop = future.channel().eventLoop();
//                        loop.schedule(new Runnable() {
//                            @SneakyThrows
//                            @Override
//                            public void run() {
//                                HeartBeatClientPipeline.this.initChannel(ch);
//                            }
//                        }, 5000, TimeUnit.MILLISECONDS);
//                    } else {
//                        log.info("服务器似乎挂了");
//                    }
//                }
//                ch.read();
//            }
//        });
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LoggedIdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast("LiveDecoder", new LiveDecoder());
        pipeline.addLast("LiveEncoder", new LiveEncoder());
        pipeline.addLast(new HeartBeatClientHandler(ch));
    }
}
