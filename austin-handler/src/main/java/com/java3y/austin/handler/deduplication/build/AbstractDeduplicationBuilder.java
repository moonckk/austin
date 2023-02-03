package com.java3y.austin.handler.deduplication.build;

import com.alibaba.fastjson.JSONObject;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.handler.deduplication.DeduplicationHolder;
import com.java3y.austin.handler.deduplication.DeduplicationParam;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * 抽象发送消息去重
 *
 * @author 3y
 * @date 2022/1/19
 */
public abstract class AbstractDeduplicationBuilder implements Builder {

    protected Integer deduplicationType;    //去重类型

    @Autowired
    private DeduplicationHolder deduplicationHolder;   //去重holder,用于获取去重参数构建者和构建service

    @PostConstruct
    public void init() {
        //Holder的Key是去重类型, 去重参数建造者是当前对象this
        //当每个具体建造者创建后就加入deduplicationHolder
        deduplicationHolder.putBuilder(deduplicationType, this);
    }

    /**
     * 从去重参数中获取去重参数对象
     *
     * @param key               和DEDUPLICATION_CONFIG_PRE前缀拼接,构成标志Key,标志这次的发送消息对象
     * @param duplicationConfig 去重参数JSON字符串
     * @param taskInfo          发送任务对象
     * @return 去重参数对象
     */
    public DeduplicationParam getParamsFromConfig(Integer key, String duplicationConfig, TaskInfo taskInfo) {
        JSONObject object = JSONObject.parseObject(duplicationConfig);
        if (Objects.isNull(object)) {   //去重参数字符串为空
            return null;
        }
        //去重参数在去重字符串里,字段是DEDUPLICATION_CONFIG_PRE + key, 提取出来
        DeduplicationParam deduplicationParam =
                JSONObject.parseObject(object.getString(DEDUPLICATION_CONFIG_PRE + key),
                        DeduplicationParam.class);
        if (Objects.isNull(deduplicationParam)) {
            return null;
        }
        deduplicationParam.setTaskInfo(taskInfo);       //将发送任务添加到去重参数对象里
        return deduplicationParam;
    }

}
