package com.evil.cloud.service.impl;

import com.evil.cloud.dao.PaymentDao;
import com.evil.cloud.entities.Payment;
import com.evil.cloud.service.PaymentService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentDao paymentDao;

    public int create(Payment payment) {
        return paymentDao.create(payment);
    }

    public Payment getPaymentById(Long id) {
        return paymentDao.getPaymentById(id);
    }

    public Payment getPaymentByIdTimeout(Long id) {
        try {
            TimeUnit.SECONDS.sleep(5); //consumer-feign-hystrix-order80设置的ribbon超时时间是3s所以会超时，进而触发服务降级
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return paymentDao.getPaymentById(id);
    }

    @HystrixCommand(fallbackMethod = "calErrorHystrixHandler")
    public String calErrorHystrix() {
        int i = 1/0; //报错触发服务降级
        return "success";
    }

    public String calErrorHystrixHandler() {
        return "hystrix";
    }

}
