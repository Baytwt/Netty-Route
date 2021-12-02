package cn.morethink.netty.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 
 * 后端解码器.
 */
@Slf4j
public class BackendDecode extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// 防止不发报文就关闭连接出现的错误
		if (!in.isReadable()) {
			return;
		}
//		LOGGER.info(String.format("[%s]收到数据:%s",  (ctx.channel().remoteAddress()).toString(), ByteBufUtil.hexDump(in)));
		log.debug("收到来自{}数据", ctx.channel().remoteAddress().toString());
		byte[] ss = new byte[in.readableBytes()];

		in.readBytes(ss);

		out.add(ss);
	}
}
