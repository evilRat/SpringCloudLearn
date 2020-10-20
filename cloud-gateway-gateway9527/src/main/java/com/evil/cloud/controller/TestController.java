package com.evil.cloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试controller，访问controller不会进入过滤器，gateway的过滤器过滤的是gateway的路由
 */

@RestController
public class TestController {

    @GetMapping("test")
    public String test() {
        return "test";
    }

}
