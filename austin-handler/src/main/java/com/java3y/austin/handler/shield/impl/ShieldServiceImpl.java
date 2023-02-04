package com.java3y.austin.handler.shield.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.java3y.austin.common.domain.AnchorInfo;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.enums.AnchorState;
import com.java3y.austin.common.enums.ShieldType;
import com.java3y.austin.handler.shield.ShieldService;
import com.java3y.austin.support.utils.LogUtils;
import com.java3y.austin.support.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;

/**
 * 屏蔽服务
 *
 * @author 3y
 */
@Service
@Slf4j
public class ShieldServiceImpl implements ShieldService {

    private static final String NIGHT_SHIELD_BUT_NEXT_DAY_SEND_KEY = "night_shield_send";
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private LogUtils logUtils;

    @Override
    public void shield(TaskInfo taskInfo) {

        if (ShieldType.NIGHT_NO_SHIELD.getCode().equals(taskInfo.getShieldType())) {        //夜间不屏蔽
            return;
        }

        /**
         * example:当消息下发至austin平台时，已经是凌晨1点，业务希望此类消息在次日的早上9点推送
         * (配合 分布式任务定时任务框架搞掂)
         */
        if (isNight()) {
            if (ShieldType.NIGHT_SHIELD.getCode().equals(taskInfo.getShieldType())) {       //夜间屏蔽
                //夜间屏蔽打点
                logUtils.print(AnchorInfo.builder().state(AnchorState.NIGHT_SHIELD.getCode())
                        .businessId(taskInfo.getBusinessId()).ids(taskInfo.getReceiver()).build());
            }
            if (ShieldType.NIGHT_SHIELD_BUT_NEXT_DAY_SEND.getCode().equals(taskInfo.getShieldType())) {     //次日9点后发送
                //将消息序列化后缓存到redis的list(相当于队列),设置过期时间(1天后的时间(秒)-当前时间(秒))
                redisUtils.lPush(NIGHT_SHIELD_BUT_NEXT_DAY_SEND_KEY, JSON.toJSONString(taskInfo,
                                SerializerFeature.WriteClassName),
                        (DateUtil.offsetDay(new Date(), 1).getTime() / 1000) - DateUtil.currentSeconds());
                //夜间屏蔽,次日发送打点
                logUtils.print(AnchorInfo.builder().state(AnchorState.NIGHT_SHIELD_NEXT_SEND.getCode()).businessId(taskInfo.getBusinessId()).ids(taskInfo.getReceiver()).build());
            }
            //夜间屏蔽,清空所有接收者
            taskInfo.setReceiver(new HashSet<>());
        }
    }

    /**
     * 小时 < 8 默认就认为是凌晨(夜晚)
     *
     * @return
     */
    private boolean isNight() {
        return LocalDateTime.now().getHour() < 8;

    }

}
