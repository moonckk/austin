package com.java3y.austin.handler.receipt.stater.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.dto.account.sms.SmsAccount;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.handler.receipt.stater.ReceiptMessageStater;
import com.java3y.austin.handler.script.SmsScript;
import com.java3y.austin.support.dao.ChannelAccountDao;
import com.java3y.austin.support.dao.SmsRecordDao;
import com.java3y.austin.support.domain.ChannelAccount;
import com.java3y.austin.support.domain.SmsRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 拉取短信回执信息
 *
 * @author 3y
 */
@Component
@Slf4j
public class SmsPullReceiptStarterImpl implements ReceiptMessageStater {

    @Autowired
    private ChannelAccountDao channelAccountDao;

    @Autowired
    private Map<String, SmsScript> scriptMap;

    @Autowired
    private SmsRecordDao smsRecordDao;

    /**
     * 拉取消息并入库
     */
    @Override
    public void start() {
        try {
            //获取类型为sms的所有可用的渠道账号
            List<ChannelAccount> channelAccountList = channelAccountDao.findAllByIsDeletedEqualsAndSendChannelEquals(CommonConstant.FALSE, ChannelType.SMS.getCode());
            for (ChannelAccount channelAccount : channelAccountList) {
                //从渠道账号的配置中获取smsAccount
                SmsAccount smsAccount = JSON.parseObject(channelAccount.getAccountConfig(), SmsAccount.class);
                //从scriptMap中根据scriptName获取SmsScript对象,然后拉取回执
                List<SmsRecord> smsRecordList = scriptMap.get(smsAccount.getScriptName()).pull(smsAccount.getScriptName());
                if (CollUtil.isNotEmpty(smsRecordList)) {
                    smsRecordDao.saveAll(smsRecordList);        //更新短信记录
                }
            }
        } catch (Exception e) {
            log.error("SmsPullReceiptStarter#start fail:{}", Throwables.getStackTraceAsString(e));

        }

    }
}
