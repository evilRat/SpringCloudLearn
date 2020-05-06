# SpringCloudLearn
SpringCloud学习项目

## 要点
### ribbon 服务调用 负载均衡
#### 1.ribbon的核心是接口IRule

```java
package com.evil.loadbalancerule;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRule {
    @Bean
    public IRule myselfRule() {
        return new RandomRule();
    }
}
```
ribbon官方文档指出自定义的均衡负载不能在@ComponentScan能扫描到的地方，@SpringBootApplication其实就包含了@ComponentScan，所以要在主启动类所在包之外，新增一个package

#### 2.手写LB算法
通过DiscoveryClient可以获取服务实例，自己写算法得到服务实例进行调用即可。

```java
@Configuration
public class MyRule {
    @Resource
    private DiscoveryClient discoveryClient;

    @GetMapping("/payment/discovery")
    public Object discovery() {
        List<String> services = discoveryClient.getServices();

        for (String service : services) {
            log.info("####service: " + service);
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            for (ServiceInstance instance : instances) {
                log.info(instance.getServiceId() + "\t" + instance.getHost() + "\t" + instance.getPort() + "\t" + instance.getUri());
            }
        }
        return this.discoveryClient;
    }
}
```

### Feign/OpenFeign 服务调用

一个生命式的Web服务客户端，让编写Web服务客户端变得容易，只需要创建一个接口并在接口上添加注释即可

主启动类增加@EnableFeignClients，接口增加@FeignClient，实现调用。

```java
package com.evil.cloud.service;

import com.evil.cloud.entities.CommonResult;
import com.evil.cloud.entities.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(value = "PAYMENT-SERVICE")
public interface PaymentFeignService {

    @GetMapping(value = "/payment/get/{id}")
    CommonResult<Payment> getPaymentById(@PathVariable("id") Long id);
}

```

OpenFeign自带均衡负载

面向接口开发，类似于mybatis的mapper接口

openfeign默认1s超时，超市报错

feign底层还是ribbon和restTemplate，调用超时时间配置：
```yaml
ribbon:
  ReadTimeout: 5000 #建立连接后读取的超时时间 socketTimeout
  ConnectTimeout: 5000  #建立连接超时时间 connectTimeout
```

feign日志：
1. NONE
2. BASIC
3. HEADERS
4. FULL

```java
package com.evil.cloud.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * feign日志配置类
 */
@Configuration
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
```

```yaml
logging:
  level:
    com.evil.cloud.serive.PaymentFeignService: debug  #feigin日志以什么级别监控哪个接口
```
日志如下
```text
2020-05-06 20:31:56.477 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] <--- HTTP/1.1 200 (368ms)
2020-05-06 20:31:56.477 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] connection: keep-alive
2020-05-06 20:31:56.477 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] content-type: application/json
2020-05-06 20:31:56.477 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] date: Wed, 06 May 2020 12:31:56 GMT
2020-05-06 20:31:56.478 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] keep-alive: timeout=60
2020-05-06 20:31:56.478 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] transfer-encoding: chunked
2020-05-06 20:31:56.478 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] 
2020-05-06 20:31:56.479 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] {"code":200,"message":"查询成功，serverPort：8002","data":{"id":1,"serial":"111"}}
2020-05-06 20:31:56.480 DEBUG 18256 --- [p-nio-80-exec-1] c.e.cloud.service.PaymentFeignService    : [PaymentFeignService#getPaymentById] <--- END HTTP (88-byte body)
```

### Hystrix 服务降级

服务雪崩：多个微服务之间存在多级调用关系，也就是所谓的“扇出”，如果扇出的链路上某个服务响应时间过长或者不可用，对越靠近源头的服务影响越大，占用越来越多的系统资源，进而引起系统崩溃，整个系统也会发生级联故障。

Hystrix是一个用于处理分布式系统的延迟和容错的开源组件。Hystrix能够保证在一个依赖服务出现问题的情况下，不会导致整体服务失败，避免级联故障，以提高分布式系统的弹性。

“断路器”：当某个服务单元发生故障后，通过断路器的故障监控，向调用方返回一个符合预期的、可处理的备选相应，而不是长时间的等待或者抛出调用方无法处理的异常，这样就保证了服务调用方线程不会长时间、不必要的占用，从而避免故障在分布式系统中的蔓延，乃至雪崩。

**重要概念：**

- 服务降级：返回一个友好的提示，不让客户端等待。程序运行异常、超时、服务熔断触发服务降级、线程池/信号量打满等都会导致服务降级。
- 服务熔断：访问量过大，直接拒绝访问，然后通过服务降级返回友好提示
- 服务限流：秒杀等高并发场景，防止流量全部进来


