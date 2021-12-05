package cn.morethink.netty.server.session;

public abstract class BackendSessionFactory {
    private static BackendSession session = new BackendSessionMemoryImpl();
    public static BackendSession getSession() {
        return session;
    }
}
