package com.java3y.austin.handler.deduplication.service;

import cn.hutool.core.collection.CollUtil;
import com.java3y.austin.common.domain.AnchorInfo;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.handler.deduplication.DeduplicationHolder;
import com.java3y.austin.handler.deduplication.DeduplicationParam;
import com.java3y.austin.handler.deduplication.limit.LimitService;
import com.java3y.austin.support.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * @author 3y
 * @date 2021/12/9
 * 去重服务
 */
@Slf4j
public abstract class AbstractDeduplicationService implements DeduplicationService {

    protected Integer deduplicationType;        //去重类型

    protected LimitService limitService;      //去重方法,目前有2种:按照内容,按照频率

    @Autowired
    private DeduplicationHolder deduplicationHolder;    //holder根据key(去重方式)关联去重参数(param)和去重业务对象(service)

    //当去重业务对象创建好后,加入holder,key是去重类型
    @PostConstruct
    private void init() {
        deduplicationHolder.putService(deduplicationType, this);
    }

    @Autowired
    private LogUtils logUtils;      //日志工具类


    @Override
    public void deduplication(DeduplicationParam param) {      //根据去重参数进行去重
        TaskInfo taskInfo = param.getTaskInfo();    //从去重参数中获取发送消息对象

        //需要去重的接收者,这个地方命名有点恶心
        Set<String> filterReceiver = limitService.limitFilter(this, taskInfo, param);

        // 剔除符合去重条件的接收者
        if (CollUtil.isNotEmpty(filterReceiver)) {
            taskInfo.getReceiver().removeAll(filterReceiver);       //剔除符合去重条件的接收者
            //记录打点信息(日志埋点)
            //发送任务的业务id,需要去重的接收者,打点类型是去重打点(从param中获取)
            logUtils.print(AnchorInfo.builder().businessId(taskInfo.getBusinessId()).ids(filterReceiver).state(param.getAnchorState().getCode()).build());
        }
    }


    /**
     * 构建去重的Key
     *
     * @param taskInfo
     * @param receiver
     * @return
     */
    public abstract String deduplicationSingleKey(TaskInfo taskInfo, String receiver);


}
