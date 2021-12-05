package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.server.handler.BackendServerHandler;
import cn.morethink.netty.server.handler.LiveDecoder;
import cn.morethink.netty.server.handler.LiveEncoder;
import cn.morethink.netty.server.handler.LoggedIdleStateHandler;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class HeartBeatClientPipeline extends ChannelInitializer<SocketChannel> {
    private SocketChannel ch;
    private String myHost;
    private int myPort;

    protected void initChannel(SocketChannel ch, String myHost, int myPort) throws Exception {
        this.ch = ch;
        this.myHost = myHost;
        this.myPort = myPort;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LoggedIdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast("LiveDecoder", new LiveDecoder());
        pipeline.addLast("LiveEncoder", new LiveEncoder());
        pipeline.addLast(new BackendServerHandler(ch, myHost, myPort));
    }
}
