package com.java3y.austin.handler.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Throwables;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.dto.model.SmsContentModel;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.handler.domain.sms.MessageTypeSmsConfig;
import com.java3y.austin.handler.domain.sms.SmsParam;
import com.java3y.austin.handler.handler.BaseHandler;
import com.java3y.austin.handler.handler.Handler;
import com.java3y.austin.handler.script.SmsScript;
import com.java3y.austin.support.dao.SmsRecordDao;
import com.java3y.austin.support.domain.MessageTemplate;
import com.java3y.austin.support.domain.SmsRecord;
import com.java3y.austin.support.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 短信发送处理
 *
 * @author 3y
 */
@Component
@Slf4j
public class SmsHandler extends BaseHandler implements Handler {

    public SmsHandler() {
        channelCode = ChannelType.SMS.getCode();
    }

    @Autowired
    private SmsRecordDao smsRecordDao;  //查询短信记录dao

    @Autowired
    private ConfigService config;       //配置读取服务,apollo,nacos,local

    @Autowired
    private Map<String, SmsScript> smsScripts;      //不同渠道发送短信的脚本

    private static final String FLOW_KEY = "msgTypeSmsConfig";      //流控配置key
    private static final String FLOW_KEY_PREFIX = "message_type_";      //流控key前缀

    @Override
    public boolean handler(TaskInfo taskInfo) {     //发送短信处理器
        //构建smsParam,发送短信
        SmsParam smsParam = SmsParam.builder()
                .phones(taskInfo.getReceiver())     //接收者set
                .content(getSmsContent(taskInfo))       //获取短信内容
                .messageTemplateId(taskInfo.getMessageTemplateId())     //消息模版id
                .build();
        try {
            /**
             * 1、动态配置做流量负载
             * 2、发送短信
             */
            List<MessageTypeSmsConfig> smsConfigs = getMessageTypeSmsConfig(taskInfo.getMsgType());     //获取指定类型短信的所有配置
            if (CollUtil.isEmpty(smsConfigs)) {
                log.info("SmsHandler#handler smsConfigs is empty. smsConfigs:{}", smsConfigs);
            }
            //对这些类型的配置做负载均衡,流量负载 根据配置的权重优先走某个账号，并取出一个备份的
            MessageTypeSmsConfig[] messageTypeSmsConfigs = loadBalance(smsConfigs);
            for (MessageTypeSmsConfig messageTypeSmsConfig : messageTypeSmsConfigs) {
                smsParam.setScriptName(messageTypeSmsConfig.getScriptName());   //设置要发送短信的服务商
                //调用服务商脚本发送短信,获得发送响应额列表
                //根据脚本名获取对应的脚本对象,脚本对象发送消息
                List<SmsRecord> recordList = smsScripts.get(messageTypeSmsConfig.getScriptName()).send(smsParam);
                if (CollUtil.isNotEmpty(recordList)) {
                    smsRecordDao.saveAll(recordList);   //保存所有短信记录
                    return true;
                }
                //如果发送不成功,则用另一个渠道发
                //todo 如果两个服务商都挂了,那岂不是不能发送短信了?
            }
        } catch (Exception e) {
            log.error("SmsHandler#handler fail:{},params:{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(smsParam));
        }
        return false;
    }

    /**
     * 流量负载
     * 根据配置的权重优先走某个账号，并取出一个备份的
     *
     * @param messageTypeSmsConfigs
     */
    private MessageTypeSmsConfig[] loadBalance(List<MessageTypeSmsConfig> messageTypeSmsConfigs) {

        int total = 0;      //权重总和
        for (MessageTypeSmsConfig channelConfig : messageTypeSmsConfigs) {
            total += channelConfig.getWeights();
        }

        // 生成一个随机数[1,total]，看落到哪个区间
        Random random = new Random();
        int index = random.nextInt(total) + 1;      //index随机[1,total]

        MessageTypeSmsConfig supplier = null;       //主要的
        MessageTypeSmsConfig supplierBack = null;   //备份的
        for (int i = 0; i < messageTypeSmsConfigs.size(); ++i) {
            if (index <= messageTypeSmsConfigs.get(i).getWeights()) {   //index小于当前配置的权重,则处理
                supplier = messageTypeSmsConfigs.get(i);

                // 取下一个供应商
                int j = (i + 1) % messageTypeSmsConfigs.size();
                if (i == j) {
                    return new MessageTypeSmsConfig[]{supplier};    //只配置了一个
                }
                supplierBack = messageTypeSmsConfigs.get(j);    //找到另一个备用的
                return new MessageTypeSmsConfig[]{supplier, supplierBack};  //返回两个
            }
            index -= messageTypeSmsConfigs.get(i).getWeights();
        }
        return null;
    }

    /**
     * 每种类型都会有其下发渠道账号的配置(流量占比也会配置里面)
     * <p>
     * 样例：
     * key：msgTypeSmsConfig
     * value：[{"message_type_10":[{"weights":80,"scriptName":"TencentSmsScript"},{"weights":20,"scriptName":"YunPianSmsScript"}]},{"message_type_20":[{"weights":20,"scriptName":"YunPianSmsScript"}]},{"message_type_30":[{"weights":20,"scriptName":"TencentSmsScript"}]},{"message_type_40":[{"weights":20,"scriptName":"TencentSmsScript"}]}]
     * 通知类短信有两个发送渠道 TencentSmsScript 占80%流量，YunPianSmsScript占20%流量
     * 营销类短信只有一个发送渠道 YunPianSmsScript
     * 验证码短信只有一个发送渠道 TencentSmsScript
     *
     * @param msgType
     * @return
     */
    private List<MessageTypeSmsConfig> getMessageTypeSmsConfig(Integer msgType) {       //获取指定类型的配置
        String property = config.getProperty(FLOW_KEY, CommonConstant.EMPTY_VALUE_JSON_ARRAY);
        JSONArray jsonArray = JSON.parseArray(property);
        for (int i = 0; i < jsonArray.size(); i++) {
            //message_type_10,message_type_20,message_type_30,message_type_40
            JSONArray array = jsonArray.getJSONObject(i).getJSONArray(FLOW_KEY_PREFIX + msgType);
            if (CollUtil.isNotEmpty(array)) {       //获取对应sms类型的MessageTypeSmsConfig(weights,scriptName)集合
                List<MessageTypeSmsConfig> result = JSON.parseArray(JSON.toJSONString(array), MessageTypeSmsConfig.class);
                return result;
            }
        }
        return null;
    }

    /**
     * 如果有输入链接，则把链接拼在文案后
     * <p>
     * PS: 这里可以考虑将链接 转 短链
     * PS: 如果是营销类的短信，需考虑拼接 回TD退订 之类的文案
     */
    private String getSmsContent(TaskInfo taskInfo) {
        SmsContentModel smsContentModel = (SmsContentModel) taskInfo.getContentModel(); //ContentModel抽象父类,所有渠道的内容模型都需要继承此类
        if (StrUtil.isNotBlank(smsContentModel.getUrl())) {
            return smsContentModel.getContent() + StrUtil.SPACE + smsContentModel.getUrl();
        } else {
            return smsContentModel.getContent();
        }
    }

    @Override
    public void recall(MessageTemplate messageTemplate) {

    }
}
