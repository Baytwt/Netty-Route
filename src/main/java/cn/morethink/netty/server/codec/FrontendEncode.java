package cn.morethink.netty.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 
 * 后端编码器.
 */
@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class FrontendEncode extends MessageToByteEncoder<byte[]> {

	private static final Logger LOGGER = LoggerFactory.getLogger(FrontendEncode.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
//		LOGGER.info(String.format("发送出的报文:[%s]", ByteBufUtil.hexDump((byte[]) msg)));
		log.debug("向{}发送出报文", ctx.channel().remoteAddress().toString());
		out.writeBytes(msg);
	}
}
