package cn.morethink.netty.demo.dao;

import io.netty.handler.codec.http.HttpMethod;
import lombok.AllArgsConstructor;

import java.net.SocketAddress;
import java.util.*;


//@Repository
//@TableName(value = "backend_server_info")
@AllArgsConstructor
public class BackendServerInfo {
//    @TableId(value = "id",type = IdType.AUTO)
//    private String id;
//    @TableField(value = "ip")
    private String ip;
//    @TableField(value = "port")
    private int port;

    // 各http方法接收的路由列表 TODO:route之间的不重复性
    private Map<HttpMethod, Set<String>> route;

//    public String getId() {
//        return id;
//    }

//    public void setId(String id) {
//        this.id = id;
//    }
//    public BackendServerInfo(SocketAddress address) {
//        this.ip = address.
//    }

    public BackendServerInfo(String address){
        this.ip = address.split(":")[0].replaceAll("/","");
        this.port = Integer.parseInt(address.split(":")[1]);
        // 添加默认路由 (如8081端口的默认接收路由是"/hello8081")
        Map<HttpMethod, Set<String>> route = new HashMap<>();
        Set<String> defaultHello = new HashSet<>();
        defaultHello.add("/hello"+port);
        route.put(HttpMethod.GET, defaultHello);
        this.route = route;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setRoute(HttpMethod method, List<String> routeList) {
        if(route.containsKey(method)){
            Set<String> routeSet = route.get(method);
            routeSet.addAll(routeList);
            route.put(method, routeSet);
        } else {
            Set<String> routeSet = new HashSet(routeList);
            route.put(method, routeSet);
        }
    }

    public Set<String> getRoute(HttpMethod method) {
        if(route.containsKey(method)) {
            return route.get(method);
        } else {
            return new HashSet<>();
        }

    }

    public String hashcode() {
        return ip + ":" + port;
    }

    public boolean equals(BackendServerInfo info) {
        return ip.equals(info.getIp()) && port == info.getPort();
    }
}
