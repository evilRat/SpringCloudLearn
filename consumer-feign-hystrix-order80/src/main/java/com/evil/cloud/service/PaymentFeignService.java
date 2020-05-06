package com.evil.cloud.service;

import com.evil.cloud.config.FeignConfig;
import com.evil.cloud.entities.CommonResult;
import com.evil.cloud.entities.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(value = "PAYMENT-HYSTRIX-SERVICE")
public interface PaymentFeignService {

    @GetMapping(value = "/payment/get/{id}")
    CommonResult getPaymentById(@PathVariable("id") Long id);

    @GetMapping(value = "/payment/getTimeout/{id}")
    CommonResult getPaymentByIdTimeout(@PathVariable("id") Long id);
}
