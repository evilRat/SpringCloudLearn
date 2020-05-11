package com.evil.cloud.hystrix;

import com.evil.cloud.entities.CommonResult;
import com.evil.cloud.service.PaymentFeignService;
import org.springframework.stereotype.Component;

@Component
public class PaymentHystrixService implements PaymentFeignService {
    @Override
    public CommonResult getPaymentById(Long id) {
        return new CommonResult(888, "global hystrix，查询ID=" + id, null);
    }

    @Override
    public CommonResult getPaymentByIdTimeout(Long id) {
        return new CommonResult(888, "global hystrix，查询ID=" + id, null);
    }
}
