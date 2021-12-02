package cn.morethink.netty.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 后端编码器.
 */
@Slf4j
public class BackendEncode extends MessageToByteEncoder<byte[]> {

	@Override
	protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
//		log.info(String.format("[%s]发送出的报文:[%s]",(ctx.channel().remoteAddress()).toString(),ByteBufUtil.hexDump((byte[]) msg)));
		log.debug("向{}发送出报文", ctx.channel().remoteAddress().toString());
		out.writeBytes(msg);
	}
}
