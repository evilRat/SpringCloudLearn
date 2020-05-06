package com.evil.cloud.controller;

import com.evil.cloud.entities.CommonResult;
import com.evil.cloud.entities.Payment;
import com.evil.cloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Slf4j
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Value("${server.port}")
    private String serverPort;

    @PostMapping(value = "payment/create")
    public CommonResult create(@RequestBody Payment payment) {
        int result = paymentService.create(payment);
        log.info("插入数据库结果：" + result);

        if (result > 0) {
            return new CommonResult(200, "插入成功，serverPort：" + serverPort, result);
        } else {
            return new CommonResult(444, "插入失败", null);
        }
    }


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

    @GetMapping(value = "payment/feginTimeout")
    public CommonResult feginTimeoutTest() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new CommonResult(222, "timeout", null);
    }

}
