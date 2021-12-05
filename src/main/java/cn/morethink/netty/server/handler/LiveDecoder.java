package cn.morethink.netty.server.handler;

import cn.morethink.netty.server.message.LiveMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * Created by RoyDeng on 17/11/23.
 */
@Slf4j
public class LiveDecoder extends ReplayingDecoder<LiveDecoder.LiveState> {

    public enum LiveState {
        TYPE,
        LENGTH,
        CONTENT
    }

    private LiveMessage message;

    public LiveDecoder() {
        super(LiveState.TYPE);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        LiveState state = state();
//        log.debug("state:" + state + " message:" + message);
        switch (state) {
            case TYPE:
                message = new LiveMessage();
                byte type = byteBuf.readByte();
//                log.debug("type:" + type);
                message.setType(type);
                checkpoint(LiveState.LENGTH);
                break;
            case LENGTH:
                int length = byteBuf.readInt();
                message.setLength(length);
                if (length > 0) {
                    checkpoint(LiveState.CONTENT);
                } else {
                    list.add(message);
                    checkpoint(LiveState.TYPE);
                }
                break;
            case CONTENT:
                byte[] bytes = new byte[message.getLength()];
                byteBuf.readBytes(bytes);
                String content = new String(bytes);
                message.setContent(content);
                list.add(message);
                checkpoint(LiveState.TYPE);
                break;
            default:
                throw new IllegalStateException("invalid state:" + state);
        }
//        log.debug("end state:" + state + " list:" + list);
    }
}
