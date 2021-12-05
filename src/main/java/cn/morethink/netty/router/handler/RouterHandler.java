package cn.morethink.netty.router.handler;


import cn.morethink.netty.router.Action;
import cn.morethink.netty.router.HttpLabel;
import cn.morethink.netty.router.HttpRouter;
import cn.morethink.netty.router.RequestBody;
import cn.morethink.netty.router.util.GeneralResponse;
import cn.morethink.netty.router.util.JsonUtil;
import cn.morethink.netty.router.util.ParamParser;
import cn.morethink.netty.router.util.ResponseUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author 李文浩
 * @date 2018/9/5
 */
@Slf4j
@ChannelHandler.Sharable
public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String DELIMITER = "?";

    HttpRouter httpRouter;

    public RouterHandler(String controllerClass) {
        HttpRouter httpRouter = new HttpRouter();
        httpRouter.addRouter(controllerClass);
        this.httpRouter = httpRouter;
    }

    public RouterHandler(HttpRouter httpRouter) {
        this.httpRouter = httpRouter;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("连接激活{}", ctx.channel().remoteAddress());
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String method = request.method().name();
        String content = request.content().toString(StandardCharsets.UTF_8);
        String uri = request.uri();
        HttpHeaders header = request.headers();
        List<Map.Entry<String, String>> headerList = header.entries();

        GeneralResponse generalResponse;
        if (uri.contains(DELIMITER)) {
            uri = uri.substring(0, uri.indexOf(DELIMITER));
        }
        log.debug("收到uri={}, method={}", uri, method);
        //根据不同的请求API做不同的处理(路由分发)
        Action action = httpRouter.getRoute(new HttpLabel(uri, request.method()));
        if (action != null) {
            String s = request.uri();
            if(method.equals(HttpMethod.POST.name())) {
                //TODO: 我也不知道这段是做什么的
                String contentType = header.get(HttpHeaderNames.CONTENT_TYPE.toString());
                if (contentType.equals(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
                    s = s + "&" + content;
                }
            }
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(s);
            log.debug("收到数据: {}", queryStringDecoder.toString());
            Map<String, List<String>> parameters = queryStringDecoder.parameters();
            Class[] classes = action.getMethod().getParameterTypes();
            Object[] objects = new Object[classes.length];
            for (int i = 0; i < classes.length; i++) {
                Class c = classes[i];
                //处理@RequestBody注解
                Annotation[] parameterAnnotation = action.getMethod().getParameterAnnotations()[i];
                if (parameterAnnotation.length > 0) {
                    for (int j = 0; j < parameterAnnotation.length; j++) {
                        if (parameterAnnotation[j].annotationType() == RequestBody.class &&
                                request.headers().get(HttpHeaderNames.CONTENT_TYPE.toString()).equals(HttpHeaderValues.APPLICATION_JSON.toString())) {
                            objects[i] = JsonUtil.fromJson(request, c);
                        }
                    }

                } else if (c.isArray()) {
                    //处理数组类型
                    String paramName = action.getMethod().getParameters()[i].getName();
                    List<String> paramList = parameters.get(paramName);
                    if (CollectionUtils.isNotEmpty(paramList)) {
                        objects[i] = ParamParser.INSTANCE.parseArray(c.getComponentType(), paramList);
                    }
                } else {
                    //处理基本类型和string
                    String paramName = action.getMethod().getParameters()[i].getName();
                    List<String> paramList = parameters.get(paramName);
                    if (CollectionUtils.isNotEmpty(paramList)) {
                        objects[i] = ParamParser.INSTANCE.parseValue(c, paramList.get(0));
                    } else {
                        objects[i] = ParamParser.INSTANCE.parseValue(c, null);
                    }
                }
            }
            log.debug("处理后:{}", Arrays.toString(objects));
            ResponseUtil.response(ctx, HttpUtil.isKeepAlive(request), action.call(objects));
        } else {
            //错误处理
            log.debug("没有匹配的路由。");
            generalResponse = new GeneralResponse(HttpResponseStatus.BAD_REQUEST, "请检查你的请求方法及url", null);
            ResponseUtil.response(ctx, HttpUtil.isKeepAlive(request), generalResponse);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.warn(e.getLocalizedMessage());
        ResponseUtil.response(ctx, false, new GeneralResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, String.format("Internal Error: %s", ExceptionUtils.getRootCause(e)), null));
        ctx.close();
    }
}
