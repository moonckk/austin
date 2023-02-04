package com.java3y.austin.handler.flowcontrol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.handler.enums.RateLimitStrategy;
import com.java3y.austin.handler.flowcontrol.annotations.LocalRateLimit;
import com.java3y.austin.support.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 3y
 * @date 2022/4/18
 */
@Service
@Slf4j
public class FlowControlFactory implements ApplicationContextAware {

    private static final String FLOW_CONTROL_KEY = "flowControlRule";
    private static final String FLOW_CONTROL_PREFIX = "flow_control_";

    //限流枚举:流量控制服务
    private final Map<RateLimitStrategy, FlowControlService> flowControlServiceMap = new ConcurrentHashMap<>();

    @Autowired
    private ConfigService config;       //读配置

    private ApplicationContext applicationContext;  //上下文

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void flowControl(TaskInfo taskInfo, FlowControlParam flowControlParam) {     //任务对象,流量控制参数
        RateLimiter rateLimiter;        //guava流量控制
        Double rateInitValue = flowControlParam.getRateInitValue();     //限流器初始大小
        // 对比 初始限流值 与 配置限流值，以 配置中心的限流值为准
        Double rateLimitConfig = getRateLimitConfig(taskInfo.getSendChannel());
        if (Objects.nonNull(rateLimitConfig) && !rateInitValue.equals(rateLimitConfig)) {
            rateLimiter = RateLimiter.create(rateLimitConfig);
            flowControlParam.setRateInitValue(rateLimitConfig);     //限流参数设置限流值
            flowControlParam.setRateLimiter(rateLimiter);           //先留参数指定限流器
        }
        //限流参数中指定的限流策略,得到指定的限流服务
        FlowControlService flowControlService = flowControlServiceMap.get(flowControlParam.getRateLimitStrategy());
        if (Objects.isNull(flowControlService)) {
            log.error("没有找到对应的单机限流策略");
            return;
        }
        //限流服务,进行流量控制
        double costTime = flowControlService.flowControl(taskInfo, flowControlParam);
        if (costTime > 0) {
            log.info("consumer {} flow control time {}",
                    ChannelType.getEnumByCode(taskInfo.getSendChannel()).getDescription(), costTime);
        }
    }

    /**
     * 得到限流值的配置
     * <p>
     * apollo配置样例     key：flowControl value：{"flow_control_40":1}
     * <p>
     * 渠道枚举可看：com.java3y.austin.common.enums.ChannelType
     *
     * @param channelCode
     */
    private Double getRateLimitConfig(Integer channelCode) {
        String flowControlConfig = config.getProperty(FLOW_CONTROL_KEY, CommonConstant.EMPTY_JSON_OBJECT);
        JSONObject jsonObject = JSON.parseObject(flowControlConfig);
        if (Objects.isNull(jsonObject.getDouble(FLOW_CONTROL_PREFIX + channelCode))) {
            return null;
        }
        return jsonObject.getDouble(FLOW_CONTROL_PREFIX + channelCode);
    }

    @PostConstruct
    private void init() {
        Map<String, Object> serviceMap = this.applicationContext.getBeansWithAnnotation(LocalRateLimit.class);
        serviceMap.forEach((name, service) -> {
            if (service instanceof FlowControlService) {
                LocalRateLimit localRateLimit = AopUtils.getTargetClass(service).getAnnotation(LocalRateLimit.class);
                RateLimitStrategy rateLimitStrategy = localRateLimit.rateLimitStrategy();
                //通常情况下 实现的限流service与rateLimitStrategy一一对应
                flowControlServiceMap.put(rateLimitStrategy, (FlowControlService) service);
            }
        });
    }
}
