package cn.morethink.netty.server.session;

import cn.morethink.netty.demo.dao.BackendServerInfo;
import cn.morethink.netty.router.HttpLabel;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BackendSessionMemoryImpl implements BackendSession {

    private static final Map<BackendServerInfo, Channel> backendServerChannelMap = new ConcurrentHashMap<>();
    private static final Map<Channel, BackendServerInfo> channelBackendServerInfoMap = new ConcurrentHashMap<>();

    public Channel getChannel(HttpMethod method, String uri) {
        Set<BackendServerInfo> infoSet = backendServerChannelMap.keySet();
        for (BackendServerInfo info: infoSet) {
            if(info.getRoute(method).contains(uri)) {
                return getChannel(info);
            }
        }
        // 无匹配
        return null;
    }

    public boolean containsBackendServerInfo(BackendServerInfo info){
        return backendServerChannelMap.containsKey(info);
    }

    public boolean containsChannel(Channel channel){
        return channelBackendServerInfoMap.containsKey(channel);
    }

    public Channel getChannel(String address) {
        BackendServerInfo info = new BackendServerInfo(address);
        return backendServerChannelMap.get(info);
    }

    @Override
    public Channel getChannel(BackendServerInfo info) {
        return backendServerChannelMap.get(info);
    }

    public BackendServerInfo getBackendServerInfo(HttpMethod method, String uri) {
        Set<BackendServerInfo> infoSet = backendServerChannelMap.keySet();
        for (BackendServerInfo info: infoSet) {
            if(info.getRoute(method).contains(uri)) {
                return info;
            }
        }
        // 无匹配
        return null;
    }

    @Override
    public BackendServerInfo getBackendServerInfo(Channel channel) {
        return channelBackendServerInfoMap.get(channel);
    }

    @Override
    public void add(Channel channel, String address) {
        BackendServerInfo info = new BackendServerInfo(address);
        backendServerChannelMap.put(info, channel);
        channelBackendServerInfoMap.put(channel, info);
    }

    @Override
    public void delete(Channel channel) {
        BackendServerInfo backendServerInfo = channelBackendServerInfoMap.get(channel);
        backendServerChannelMap.remove(backendServerInfo);
        channelBackendServerInfoMap.remove(channel);
    }

    @Override
    public void delete(BackendServerInfo backendServerInfo) {
        Channel channel = backendServerChannelMap.get(backendServerInfo);
        backendServerChannelMap.remove(backendServerInfo);
        channelBackendServerInfoMap.remove(channel);
    }

    @Override
    public String toString() {
        return backendServerChannelMap.toString();
    }
}
