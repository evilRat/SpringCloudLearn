package com.evil.cloud.controller;

import com.evil.cloud.entities.CommonResult;
import com.evil.cloud.entities.Payment;
import com.evil.cloud.service.PaymentService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Value("${server.port}")
    private String serverPort;

    @GetMapping(value = "payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        log.info("查询数据库结果：" + payment);

        if (payment != null) {
            return new CommonResult(200, "查询成功，serverPort：" + serverPort, payment);
        } else {
            return new CommonResult(444, "查询失败，查询ID=" + id, null);
        }
    }

    @GetMapping(value = "payment/getTimeout/{id}")
    @HystrixCommand(fallbackMethod = "getPaymentByIdTimeoutHandler")
    public CommonResult getPaymentByIdTimeout(@PathVariable("id") Long id) {
        Payment payment = paymentService.getPaymentByIdTimeout(id);
        log.info("查询数据库结果：" + payment);

        if (payment != null) {
            return new CommonResult(200, "查询成功getTimeout，serverPort：" + serverPort, payment);
        } else {
            return new CommonResult(444, "查询失败，查询ID=" + id, null);
        }
    }

    public CommonResult getPaymentByIdTimeoutHandler(Long id) {
        return new CommonResult(555, "HystrixMethod，查询ID=" + id, null);
    }

    @GetMapping("payment/calErrorHystrix")
    public CommonResult calErrorHystrix() {
        return new CommonResult(777, paymentService.calErrorHystrix(), null);
    }

}
