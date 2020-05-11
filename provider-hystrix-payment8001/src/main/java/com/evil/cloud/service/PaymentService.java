package com.evil.cloud.service;

import com.evil.cloud.entities.Payment;
import org.apache.ibatis.annotations.Param;

public interface PaymentService {

    int create(Payment payment);

    Payment getPaymentById(@Param("id") Long id);

    Payment getPaymentByIdTimeout(Long id);

    String calErrorHystrix();

    String paymentCircuitBreaker(Integer id);
}
