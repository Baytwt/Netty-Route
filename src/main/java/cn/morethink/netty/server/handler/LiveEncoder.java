package cn.morethink.netty.server.handler;

import cn.morethink.netty.server.message.LiveMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by RoyDeng on 17/11/22.
 */
@Slf4j
public class LiveEncoder extends MessageToByteEncoder<LiveMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LiveMessage liveMessage, ByteBuf byteBuf) throws Exception {
        byteBuf.writeByte(liveMessage.getType());
        byteBuf.writeInt(liveMessage.getContent().getBytes().length);
        if (!StringUtil.isNullOrEmpty(liveMessage.getContent())) {
            byteBuf.writeBytes(liveMessage.getContent().getBytes());
        }
    }
}
