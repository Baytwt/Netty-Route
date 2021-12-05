package cn.morethink.netty.server.pipeline;

import cn.morethink.netty.server.codec.BackendDecode;
import cn.morethink.netty.server.codec.BackendEncode;
import cn.morethink.netty.server.handler.LoggedIdleStateHandler;
import cn.morethink.netty.server.handler.ProxyBackendHandler;
import cn.morethink.netty.server.handler.ProxyFrontendHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 代理服务器 → 后端服务器 流水线
 */
@Slf4j
public class BackendPipeline extends ChannelInitializer<SocketChannel> {

	private static final int READ_IDEL_TIME_OUT = 0; // 读超时
	private static final int WRITE_IDEL_TIME_OUT = 0;// 写超时
	private static final int ALL_IDEL_TIME_OUT = 2; // 所有超时
	private static final TimeUnit TIME_OUT_UNIT = TimeUnit.SECONDS; // 超时时间单位

	private Channel inboundChannel;
	private ProxyFrontendHandler proxyFrontendHandler;
	private String host;
	private int port;

	public BackendPipeline(Channel inboundChannel, ProxyFrontendHandler proxyFrontendHandler, String host, int port) {
		this.inboundChannel = inboundChannel;
		this.proxyFrontendHandler=proxyFrontendHandler;
		this.host=host;
		this.port=port;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {

		ChannelPipeline pipeline = ch.pipeline();
		// 注册handler
		pipeline.addLast("idleStateHandler", new LoggedIdleStateHandler(READ_IDEL_TIME_OUT, WRITE_IDEL_TIME_OUT, ALL_IDEL_TIME_OUT, TIME_OUT_UNIT));
		pipeline.addLast("BackendDecode", new BackendDecode());
		pipeline.addLast("BackendEncode", new BackendEncode());
		pipeline.addLast("ProxyBackendHandler", new ProxyBackendHandler(inboundChannel,proxyFrontendHandler,host,port));

	}


}
