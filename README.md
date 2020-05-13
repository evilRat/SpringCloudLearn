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

1. 在需要熔断处理的方法加上@HystrixCommand，并注明熔断后处理的方法。
2. 在主启动类启用熔断器@EnableCircuitBreaker


```java
    @HystrixCommand(fallbackMethod = "calErrorHystrixHandler")
    public String calErrorHystrix() {
        int i = 1/0; //报错触发服务降级
        return "success";
    }

    public String calErrorHystrixHandler() {
        return "hystrix";
    }
```

默认fallbackMethod，在类上加注解：@DefaultProperties(defaultFallback = "fallback-method-name")

这样的话，如果没有加fallbackMethod的@HystrixCommand，就会使用这个默认的降级方法来处理。

当然这里也是就近原则， 如果在方法上标注了， 还是按方法上的来。

另外一个同意配置fallbackMethod的方法是：开启feign自带的hystrix，在配置中加入：

```YML
feign:
  hystrix:
    enabled: true
```

然后写一个类比如叫XXXFallbackService来实现我们的FeignClient-XXXService，在实现方法里写对应的降级操作。然后将这个降级service加到IOC容器，在feignClient配置fallback，指向这个类，那么对应的方法就作为feign调用失败的fallback。

```java
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

```

```java
package com.evil.cloud.service;

import com.evil.cloud.config.FeignConfig;
import com.evil.cloud.entities.CommonResult;
import com.evil.cloud.entities.Payment;
import com.evil.cloud.hystrix.PaymentHystrixService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(value = "PAYMENT-HYSTRIX-SERVICE", fallback = PaymentHystrixService.class)
public interface PaymentFeignService {

    @GetMapping(value = "/payment/get/{id}")
    CommonResult getPaymentById(@PathVariable("id") Long id);

    @GetMapping(value = "/payment/getTimeout/{id}")
    CommonResult getPaymentByIdTimeout(@PathVariable("id") Long id);
}

```

熔断器：配置熔断器，有默认值的

```
@HystrixCommand(fallbackMethod = "paymentCircuitBreakerFallback", commandProperties = {
            @HystrixProperty(name = "circuitBreaker.enabled", value = "true"), //是否开启熔断器
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"), //请求次数
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"), //时间窗口期
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60")    //失败率到达多少后跳闸
            //上面的配置就是----如果10次失败率达到60%就开启熔断，10s后开始尝试让下一个请求通过（半开），如果失败了，就保持开启熔断器，等过10s再试试，如果成功了，就关闭熔断器
    })
```

抽象类`HystrixCommandProperties`里设置了所有properties的默认值：
```java
public abstract class HystrixCommandProperties {
    private static final Logger logger = LoggerFactory.getLogger(HystrixCommandProperties.class);
    static final Integer default_metricsRollingStatisticalWindow = 10000;
    private static final Integer default_metricsRollingStatisticalWindowBuckets = 10;
    private static final Integer default_circuitBreakerRequestVolumeThreshold = 20;
    private static final Integer default_circuitBreakerSleepWindowInMilliseconds = 5000;
    private static final Integer default_circuitBreakerErrorThresholdPercentage = 50;
    private static final Boolean default_circuitBreakerForceOpen = false;
    static final Boolean default_circuitBreakerForceClosed = false;
    private static final Integer default_executionTimeoutInMilliseconds = 1000;
    private static final Boolean default_executionTimeoutEnabled = true;
    private static final HystrixCommandProperties.ExecutionIsolationStrategy default_executionIsolationStrategy;
    private static final Boolean default_executionIsolationThreadInterruptOnTimeout;
    private static final Boolean default_executionIsolationThreadInterruptOnFutureCancel;
    private static final Boolean default_metricsRollingPercentileEnabled;
    private static final Boolean default_requestCacheEnabled;
    private static final Integer default_fallbackIsolationSemaphoreMaxConcurrentRequests;
    private static final Boolean default_fallbackEnabled;
    private static final Integer default_executionIsolationSemaphoreMaxConcurrentRequests;
    private static final Boolean default_requestLogEnabled;
    private static final Boolean default_circuitBreakerEnabled;
    private static final Integer default_metricsRollingPercentileWindow;
    private static final Integer default_metricsRollingPercentileWindowBuckets;
    private static final Integer default_metricsRollingPercentileBucketSize;
    private static final Integer default_metricsHealthSnapshotIntervalInMilliseconds;
    private final HystrixCommandKey key;
    private final HystrixProperty<Integer> circuitBreakerRequestVolumeThreshold;
    private final HystrixProperty<Integer> circuitBreakerSleepWindowInMilliseconds;
    private final HystrixProperty<Boolean> circuitBreakerEnabled;
    private final HystrixProperty<Integer> circuitBreakerErrorThresholdPercentage;
    private final HystrixProperty<Boolean> circuitBreakerForceOpen;
    private final HystrixProperty<Boolean> circuitBreakerForceClosed;
    private final HystrixProperty<HystrixCommandProperties.ExecutionIsolationStrategy> executionIsolationStrategy;
    private final HystrixProperty<Integer> executionTimeoutInMilliseconds;
    private final HystrixProperty<Boolean> executionTimeoutEnabled;
    private final HystrixProperty<String> executionIsolationThreadPoolKeyOverride;
    private final HystrixProperty<Integer> executionIsolationSemaphoreMaxConcurrentRequests;
    private final HystrixProperty<Integer> fallbackIsolationSemaphoreMaxConcurrentRequests;
    private final HystrixProperty<Boolean> fallbackEnabled;
    private final HystrixProperty<Boolean> executionIsolationThreadInterruptOnTimeout;
    private final HystrixProperty<Boolean> executionIsolationThreadInterruptOnFutureCancel;
    private final HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds;
    private final HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets;
    private final HystrixProperty<Boolean> metricsRollingPercentileEnabled;
    private final HystrixProperty<Integer> metricsRollingPercentileWindowInMilliseconds;
    private final HystrixProperty<Integer> metricsRollingPercentileWindowBuckets;
    private final HystrixProperty<Integer> metricsRollingPercentileBucketSize;
    private final HystrixProperty<Integer> metricsHealthSnapshotIntervalInMilliseconds;
    private final HystrixProperty<Boolean> requestLogEnabled;
    private final HystrixProperty<Boolean> requestCacheEnabled;

    static {
        default_executionIsolationStrategy = HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
        default_executionIsolationThreadInterruptOnTimeout = true;
        default_executionIsolationThreadInterruptOnFutureCancel = false;
        default_metricsRollingPercentileEnabled = true;
        default_requestCacheEnabled = true;
        default_fallbackIsolationSemaphoreMaxConcurrentRequests = 10;
        default_fallbackEnabled = true;
        default_executionIsolationSemaphoreMaxConcurrentRequests = 10;
        default_requestLogEnabled = true;
        default_circuitBreakerEnabled = true;
        default_metricsRollingPercentileWindow = 60000;
        default_metricsRollingPercentileWindowBuckets = 6;
        default_metricsRollingPercentileBucketSize = 100;
        default_metricsHealthSnapshotIntervalInMilliseconds = 500;
    }
}
```

`com.evil.cloud.service.impl.PaymentServiceImpl#paymentCircuitBreaker`为暴露的服务接口，`com.evil.cloud.service.impl.PaymentServiceImpl#paymentCircuitBreakerFallback`为fallbackMethod。我们请求id为正数时可以正常返回，如果为负数，就会抛出异常，这时候就会走fallbackMethod方法，如果我们狂刷负数，超过6次，那么就会触发熔断器打开，这时候我们在10s内修改为正数来调用，得到的还时fallbackMethod方法的返回，当时一旦超过窗口期（10s），就会触发熔断器半开，让下一个请求通过，得到正常返回，之后就会触发熔断器关闭，也就是确认服务恢复了。

**这里要注意的是sleepWindowInMilliseconds，它设置的是触发短路的时间值，当该值设为5000时，则当触发circuit break后的5000毫秒内都会拒绝request，也就是5000毫秒后才会关闭circuit。默认5000。也就是说窗口期的意思是从熔断器打开到半开的时间间隔**

#### Hystrix-Dashboard

新增一个module
```java
@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashboardMain9001 {
    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboardMain9001.class, args);
    }
}
```
application.yml
```yaml
server:
  port: 9001
```

被监控的模块：
```java
package com.evil.cloud;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableEurekaClient
@EnableCircuitBreaker
public class PaymentHystirxMain8001 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentHystirxMain8001.class, args);
    }

    /**
     * 此配置是为了服务监控，与服务容错本身无关，springcloud升级后的bug
     * ServletRegistrationBean因为springboot的默认路径不是“/hystrix.stream”，所以要加下面的配置
     * @return
     */
    @Bean
    public ServletRegistrationBean getServlet() {
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
        registrationBean.setLoadOnStartup(1);
        registrationBean.addUrlMappings("/hystrix.stream");
        registrationBean.setName("HystrixMetricsStreamServlet");
        return registrationBean;
    }

}
```
此外，被监控的模块还需要有web和actuator的依赖。

启动后打开`http://localhost:9001/hystrix` ，填入监控流`http://localhost:8001/hystrix.stream` ，也就是我们上面bean里配置的Mapping，点击`Monitor Stream`就可以监控我们的接口了，可以看到成功、失败数目和熔断器状态等。