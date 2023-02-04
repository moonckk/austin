package com.java3y.austin.handler.discard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.domain.AnchorInfo;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.enums.AnchorState;
import com.java3y.austin.support.service.ConfigService;
import com.java3y.austin.support.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 丢弃模板消息
 *
 * @author 3y.
 */
@Service
public class DiscardMessageService {
    private static final String DISCARD_MESSAGE_KEY = "discardMsgIds";

    @Autowired
    private ConfigService config;

    @Autowired
    private LogUtils logUtils;


    /**
     * 丢弃消息，配置在apollo
     *
     * @param taskInfo
     * @return
     */
    public boolean isDiscard(TaskInfo taskInfo) {
        // 配置示例:	["1","2"]
        // 数组中存的是要丢弃的模板id
        JSONArray array = JSON.parseArray(config.getProperty(DISCARD_MESSAGE_KEY, CommonConstant.EMPTY_VALUE_JSON_ARRAY));

        //根据配置丢弃指定模版id的消息
        if (array.contains(String.valueOf(taskInfo.getMessageTemplateId()))) {
            //丢弃消息时要打点,构建打点对象(业务id,任务接收者,打点状态:消息被丢弃（从Kafka消费后，被丢弃）)
            logUtils.print(AnchorInfo.builder().businessId(taskInfo.getBusinessId()).ids(taskInfo.getReceiver()).state(AnchorState.DISCARD.getCode()).build());
            return true;
        }
        return false;
    }

}
