package cn.morethink.netty.demo.controller;

import cn.morethink.netty.router.RequestMapping;
import cn.morethink.netty.router.util.GeneralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

/**
 * Netty代理服务器的路由表
 */
@Slf4j
@Controller
public class HelloController extends RouteController {

    @RequestMapping(uri = "/hello", method = "GET")
    public GeneralResponse hello() {
        return new GeneralResponse("Hello Netty");
    }

}
