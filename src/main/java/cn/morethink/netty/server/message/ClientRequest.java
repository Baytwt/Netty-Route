package cn.morethink.netty.server.message;

import io.netty.handler.codec.http.HttpMethod;
import lombok.Data;

@Data
public class ClientRequest {

    private HttpMethod method;
    private String url;

    Object content;
}
