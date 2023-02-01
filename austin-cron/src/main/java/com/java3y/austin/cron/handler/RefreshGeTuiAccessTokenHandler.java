package com.java3y.austin.cron.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.constant.SendAccountConstant;
import com.java3y.austin.common.dto.account.GeTuiAccount;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.cron.dto.getui.GeTuiTokenResultDTO;
import com.java3y.austin.cron.dto.getui.QueryTokenParamDTO;
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
 * 刷新个推的token
 * <p>
 * https://docs.getui.com/getui/server/rest_v2/token/
 *
 * @author 3y
 */
@Service
@Slf4j
public class RefreshGeTuiAccessTokenHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;      //redis客户端

    @Autowired
    private ChannelAccountDao channelAccountDao;    //渠道账号dao


    /**
     * 每小时请求一次接口刷新（以防失效)
     */
    @XxlJob("refreshGeTuiAccessTokenJob")       //xxl调度,每小时刷新各个推访问token
    public void execute() {
        log.info("refreshGeTuiAccessTokenJob#execute!");
        //开启线程池刷新
        //execute没有返回值
        SupportThreadPoolConfig.getPendingSingleThreadPool().execute(() -> {
            //获取所有没有被删除,类型为个推的渠道账号
            List<ChannelAccount> accountList = channelAccountDao.findAllByIsDeletedEqualsAndSendChannelEquals(CommonConstant.FALSE, ChannelType.PUSH.getCode());
            for (ChannelAccount channelAccount : accountList) { //处理每个个推的渠道账号
                GeTuiAccount account = JSON.parseObject(channelAccount.getAccountConfig(), GeTuiAccount.class);     //config存的是个推对象的json字符串,这里反序列化成个推对象
                String accessToken = getAccessToken(account);   //获取当前个推账号的访问token
                if (StrUtil.isNotBlank(accessToken)) {
                    //将个推token缓存到redis,K="ge_tui_access_token_"+渠道账户id,V=token
                    //此处如果是多个开发环境公用一个redis,要做数据隔离, 设置KV时最好设置过期时间,不然会导致KV堆积
                    //存redis的方法最好封装起来,可以加上日志,获取异常
                    redisTemplate.opsForValue().set(SendAccountConstant.GE_TUI_ACCESS_TOKEN_PREFIX + channelAccount.getId(), accessToken);
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
    private String getAccessToken(GeTuiAccount account) {
        String accessToken = "";
        try {
            String url = "https://restapi.getui.com/v2/" + account.getAppId() + "/auth";    //个推认证url
            String time = String.valueOf(System.currentTimeMillis());
            String digest = SecureUtil.sha256().digestHex(account.getAppKey() + time + account.getMasterSecret());      //生成digest标志码,appId+时间+访问密钥
            QueryTokenParamDTO param = QueryTokenParamDTO.builder()     //查询token参数DTO,是官方个推服务器的请求参数对象,服务与服务之间的调用一般用DTO当作请求参数对象
                    .timestamp(time)    //时间戳
                    .appKey(account.getAppKey())    //appId
                    .sign(digest).build();  //digest标志码
            //hutool发请求, 还是用hutool构建请求头,CONTENT_TYPE("Content-Type"),JSON("application/json")  , 也就是Content-Type:application/json 这个请求头标志
            String body = HttpRequest.post(url).header(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue())
                    .body(JSON.toJSONString(param))     //请求体就是上边的dto转成json字符串
                    .timeout(20000)     //请求过期时间20秒
                    .execute().body();  //从HttpResponse中获取响应体
            GeTuiTokenResultDTO geTuiTokenResultDTO = JSON.parseObject(body, GeTuiTokenResultDTO.class);    //把响应体转成GeTuiTokenResultDTO,响应对象的DTO
            if (geTuiTokenResultDTO.getCode().equals(0)) {      //todo 此处没用枚举,是有点问题的.  响应值0表示成功
                accessToken = geTuiTokenResultDTO.getData().getToken();
            }
        } catch (Exception e) {
            log.error("RefreshGeTuiAccessTokenHandler#getAccessToken fail:{}", Throwables.getStackTraceAsString(e));
        }
        return accessToken;
    }

}
