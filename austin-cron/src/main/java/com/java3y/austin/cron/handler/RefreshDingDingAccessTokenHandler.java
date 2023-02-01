package com.java3y.austin.cron.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.google.common.base.Throwables;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.constant.SendAccountConstant;
import com.java3y.austin.common.dto.account.DingDingWorkNoticeAccount;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.support.config.SupportThreadPoolConfig;
import com.java3y.austin.support.dao.ChannelAccountDao;
import com.java3y.austin.support.domain.ChannelAccount;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 刷新钉钉的access_token
 * <p>
 * https://open.dingtalk.com/document/orgapp-server/obtain-orgapp-token
 *
 * @author 3y
 */
@Service
@Slf4j
public class RefreshDingDingAccessTokenHandler {


    private static final String URL = "https://oapi.dingtalk.com/gettoken";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ChannelAccountDao channelAccountDao;


    /**
     * 每小时请求一次接口刷新（以防失效)
     */
    @XxlJob("refreshAccessTokenJob")
    public void execute() {
        log.info("refreshAccessTokenJob#execute!");
        SupportThreadPoolConfig.getPendingSingleThreadPool().execute(() -> {    //流程和个推差不多
            List<ChannelAccount> accountList = channelAccountDao.findAllByIsDeletedEqualsAndSendChannelEquals(CommonConstant.FALSE, ChannelType.DING_DING_WORK_NOTICE.getCode());
            for (ChannelAccount channelAccount : accountList) {
                //账号配置中反序列化钉钉的渠道账户, 钉钉使用了3个标志: agentId,appKey,appSecret, 开放平台都有这几个标志
                DingDingWorkNoticeAccount account = JSON.parseObject(channelAccount.getAccountConfig(), DingDingWorkNoticeAccount.class);
                String accessToken = getAccessToken(account);
                if (StrUtil.isNotBlank(accessToken)) {
                    redisTemplate.opsForValue().set(SendAccountConstant.DING_DING_ACCESS_TOKEN_PREFIX + channelAccount.getId(), accessToken);
                }
            }
        });
    }

    /**
     * 获取 access_token
     *
     * @param account
     * @return
     */
    private String getAccessToken(DingDingWorkNoticeAccount account) {
        String accessToken = "";
        try {
            DingTalkClient client = new DefaultDingTalkClient(URL);     //钉钉通过自己的客户端发送请求,他们封装了自己的客户端请求对象和响应对象,和aws的一样
            OapiGettokenRequest req = new OapiGettokenRequest();    //钉钉自己的获取token的请求对象
            req.setAppkey(account.getAppKey());
            req.setAppsecret(account.getAppSecret());
            req.setHttpMethod(CommonConstant.REQUEST_METHOD_GET);
            OapiGettokenResponse rsp = client.execute(req);
            accessToken = rsp.getAccessToken();
        } catch (Exception e) {
            log.error("RefreshDingDingAccessTokenHandler#getAccessToken fail:{}", Throwables.getStackTraceAsString(e));
        }
        return accessToken;
    }
}
