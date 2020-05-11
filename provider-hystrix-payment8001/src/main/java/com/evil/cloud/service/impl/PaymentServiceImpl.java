package com.evil.cloud.service.impl;

import cn.hutool.core.util.IdUtil;
import com.evil.cloud.dao.PaymentDao;
import com.evil.cloud.entities.Payment;
import com.evil.cloud.service.PaymentService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

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


    @HystrixCommand(fallbackMethod = "paymentCircuitBreakerFallback", commandProperties = {
            @HystrixProperty(name = "circuitBreaker.enabled", value = "true"), //是否开启熔断器
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"), //请求次数
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"), //时间窗口期
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60")    //失败率到达多少后跳闸
            //上面的配置就是----如果10次失败率达到60%就开启熔断，10s后开始尝试让下一个请求通过（半开），如果失败了，就保持开启熔断器，等过10s再试试，如果成功了，说明服务可能恢复了，就关闭熔断器
    })
    public String paymentCircuitBreaker(@PathVariable("id") Integer id) {
        if (id < 0) {
            throw new RuntimeException("id 不能为负数");
        }

        String serialNumber = IdUtil.simpleUUID();
        return Thread.currentThread().getName() + "\t" + "调用成功，流水号：" + serialNumber;
    }

    public String paymentCircuitBreakerFallback(@PathVariable("id") Integer id) {
        return "id 不能为负数，请稍后再试。。。。。。。。。。。。。。 id：" + id;
    }




}
