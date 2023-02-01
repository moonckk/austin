package com.java3y.austin.web.utils;

import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.constant.OfficialAccountParamConstant;
import com.java3y.austin.web.config.WeChatLoginConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author 3y
 * @date 2022/12/22
 * 微信服务号登录的Utils
 */
@Component
@Slf4j
public class LoginUtils {

    @Autowired
    private ApplicationContext applicationContext;      //注入spring上下文对象

    @Value("${spring.profiles.active}")
    private String env;     //环境

    /**
     * 测试环境 使用
     * 获取 WeChatLoginConfig 对象
     *
     * @return
     */
    public WeChatLoginConfig getLoginConfig() {
        try {
            //从spring容器中获取WeChatLoginConfig
            //bean的名字是WE_CHAT_LOGIN_CONFIG = "weChatLoginConfig"
            return applicationContext.getBean(OfficialAccountParamConstant.WE_CHAT_LOGIN_CONFIG, WeChatLoginConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 测试环境使用
     * 判断是否需要登录
     *
     * @return
     */
    public boolean needLogin() {
        try {
            WeChatLoginConfig bean = applicationContext.getBean(OfficialAccountParamConstant.WE_CHAT_LOGIN_CONFIG, WeChatLoginConfig.class);
            if (CommonConstant.ENV_TEST.equals(env) && Objects.nonNull(bean)) {     //测试环境且微信登陆配置存在时才需要登陆
                return true;
            }
        } catch (Exception e) {
        }
        return false;   //否则不需要登陆
    }
}
