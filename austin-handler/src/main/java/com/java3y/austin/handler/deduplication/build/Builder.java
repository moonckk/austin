package com.java3y.austin.handler.deduplication.build;

import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.handler.deduplication.DeduplicationParam;

/**
 * @author luohaojie
 * @date 2022/1/18
 */
public interface Builder {      //构建去重参数

    String DEDUPLICATION_CONFIG_PRE = "deduplication_";

    /**
     * 根据配置构建去重参数
     *
     * @param deduplication   去重配置
     * @param taskInfo   发送任务信息
     * @return 去重参数
     */
    DeduplicationParam build(String deduplication, TaskInfo taskInfo);
}
