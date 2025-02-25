package cn.morethink.netty.server.message;

/**
 *
 * Created by RoyDeng on 17/11/23.
 */
public class LiveMessage {

    public static final byte TYPE_HEART = 1;
    public static final byte TYPE_MESSAGE = 2;

    private byte type;
    private int length;
    private String content;

    public LiveMessage() {
    }

    public LiveMessage helloMessage() {
        LiveMessage msg = new LiveMessage();
        msg.setType(LiveMessage.TYPE_MESSAGE);
        msg.setContent("hello");
        msg.setLength(msg.getContent().getBytes().length);
        return msg;
    }

    public LiveMessage heartBeatMessage(String address) {
        LiveMessage msg = new LiveMessage();
        msg.setType(LiveMessage.TYPE_HEART);
        msg.setContent("heart beat|" + address);
        msg.setLength(msg.getContent().getBytes().length);
        return msg;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "LiveMessage{" +
                "type=" + type +
                ", length=" + length +
                ", content=" + content +
                '}';
    }
}
