package cn.morethink.netty.server.session;

import cn.morethink.netty.demo.dao.BackendServerInfo;
import cn.morethink.netty.router.HttpLabel;
import io.netty.channel.Channel;

import java.util.List;

public interface BackendSession {

    Channel getChannel(BackendServerInfo info);
    BackendServerInfo getBackendServerInfo(Channel channel);

    void add(Channel channel, String address);

    void delete(Channel channel);
    void delete(BackendServerInfo info);

}
