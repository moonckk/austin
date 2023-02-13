package com.java3y.austin.handler.script.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponseBody;
import com.java3y.austin.common.dto.account.sms.AliyunSmsAccount;
import com.java3y.austin.common.enums.SmsStatus;
import com.java3y.austin.handler.domain.sms.SmsParam;
import com.java3y.austin.handler.script.SmsScript;
import com.java3y.austin.support.domain.SmsRecord;
import com.java3y.austin.support.utils.AccountUtils;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component("AliyunSmsScript")
public class AliyunSmsScript implements SmsScript {
    @Autowired
    private AccountUtils accountUtils;

    @Override
    public List<SmsRecord> send(SmsParam smsParam) {
        AliyunSmsAccount aliyunSmsAccount = null;
        SendSmsRequest sendSmsRequest = null;
        AsyncClient client = null;
        try {
            //账号工具类根据scriptName找到对应的渠道账号类
            aliyunSmsAccount = accountUtils.getSmsAccountByScriptName(smsParam.getScriptName(), AliyunSmsAccount.class);
            //初始化客户端
            client = init(aliyunSmsAccount);
            //构建发送请求对象
            sendSmsRequest = assembleSendReq(smsParam, aliyunSmsAccount);
            //使用客户端异步发送请求
            CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);
            //返回参数
            return assembleSendSmsRecord(smsParam, response.get(), aliyunSmsAccount);
        } catch (Exception err) {
            log.error("AliyunSmsScript#send,smsParam:{},aliyunSmsAccount:{},sendSmsRequest;{}", smsParam, aliyunSmsAccount, sendSmsRequest);
        } finally {
            if (!Objects.isNull(client)) {
                client.close();
            }
        }
        return null;
    }

    private List<SmsRecord> assembleSendSmsRecord(SmsParam smsParam, SendSmsResponse response, AliyunSmsAccount account) {  //组装响应参数
        if (Objects.isNull(response)) {
            return null;
        }
        List<SmsRecord> smsRecordList = CollUtil.newArrayList();
        SendSmsResponseBody body = response.getBody();
        for (String phone : smsParam.getPhones()) {
            SmsRecord smsRecord = SmsRecord.builder()       //构造短信记录
                    .sendDate(Integer.valueOf(DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN)))
                    .messageTemplateId(smsParam.getMessageTemplateId())
                    .phone(Long.valueOf(phone))
                    .supplierId(account.getSupplierId())
                    .supplierName(account.getSupplierName())
                    .msgContent(smsParam.getContent())
                    .seriesId(body.getBizId())      //标记bizId
                    .chargingNum(1)
                    .status(SmsStatus.SEND_SUCCESS.getCode())
                    .reportContent(body.getCode())
                    .created(Math.toIntExact(DateUtil.currentSeconds()))
                    .updated(Math.toIntExact(DateUtil.currentSeconds()))
                    .build();
            smsRecordList.add(smsRecord);
        }
        return smsRecordList;
    }

    private SendSmsRequest assembleSendReq(SmsParam smsParam, AliyunSmsAccount account) {       //组装发送参数
        return SendSmsRequest.builder()
                .signName(account.getSignName())
                .templateCode(account.getTemplateCode())
                .phoneNumbers(CollUtil.join(smsParam.getPhones(), ","))
                .templateParam(smsParam.getContent())
                .build();
    }

    private AsyncClient init(AliyunSmsAccount account) {    //初始化客户端
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(account.getAccessKeyId())
                .accessKeySecret(account.getAccessKeySecret())
                .build());
        return AsyncClient.builder()
                .region(account.getRegion())
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride(account.getUrl())
                )
                .build();
    }

    @Override
    public List<SmsRecord> pull(String scriptName) {
        return null;
    }
}
