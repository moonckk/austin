package com.java3y.austin.handler.handler.impl;


import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.RateLimiter;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.dto.model.EmailContentModel;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.handler.enums.RateLimitStrategy;
import com.java3y.austin.handler.flowcontrol.FlowControlParam;
import com.java3y.austin.handler.handler.BaseHandler;
import com.java3y.austin.handler.handler.Handler;
import com.java3y.austin.support.domain.MessageTemplate;
import com.java3y.austin.support.utils.AccountUtils;
import com.sun.mail.util.MailSSLSocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 邮件发送处理
 *
 * @author 3y
 */
@Component
@Slf4j
public class EmailHandler extends BaseHandler implements Handler {      //邮件消息处理器

    @Autowired
    private AccountUtils accountUtils;      //账户工具

    public EmailHandler() {
        channelCode = ChannelType.EMAIL.getCode();      //40邮件

        // 按照请求限流，默认单机 3 qps （具体数值配置在apollo动态调整)
        Double rateInitValue = Double.valueOf(3);   //设置限流初始大小
        flowControlParam = FlowControlParam.builder()
                .rateInitValue(rateInitValue)       //初始限流大小
                .rateLimitStrategy(RateLimitStrategy.REQUEST_RATE_LIMIT)        //真实qps限流
                .rateLimiter(RateLimiter.create(rateInitValue)).build();        //构建限流参数对象

    }

    @Override
    public boolean handler(TaskInfo taskInfo) {
        //获取邮件内容
        EmailContentModel emailContentModel = (EmailContentModel) taskInfo.getContentModel();
        //hutool 获取邮件账户对象
        MailAccount account = getAccountConfig(taskInfo.getSendAccount());
        try {
            //hutool 发送邮件
            MailUtil.send(account, taskInfo.getReceiver(), emailContentModel.getTitle(),
                    emailContentModel.getContent(), true, null);
        } catch (Exception e) {
            log.error("EmailHandler#handler fail!{},params:{}", Throwables.getStackTraceAsString(e), taskInfo);
            return false;
        }
        return true;
    }

    /**
     * 获取账号信息合配置
     *
     * @return
     */
    private MailAccount getAccountConfig(Integer sendAccount) {     //构造hutool所需的邮件账户对象
        MailAccount account = accountUtils.getAccountById(sendAccount, MailAccount.class);      //hutool
        try {
            MailSSLSocketFactory sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            account.setAuth(account.isAuth()).setStarttlsEnable(account.isStarttlsEnable()).setSslEnable(account.isSslEnable()).setCustomProperty("mail.smtp.ssl.socketFactory", sf);
            account.setTimeout(25000).setConnectionTimeout(25000);
        } catch (Exception e) {
            log.error("EmailHandler#getAccount fail!{}", Throwables.getStackTraceAsString(e));
        }
        return account;
    }

    @Override
    public void recall(MessageTemplate messageTemplate) {       //撤回邮件

    }
}
