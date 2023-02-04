package com.java3y.austin.handler.deduplication;

import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.enums.DeduplicationType;
import com.java3y.austin.support.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author 3y.
 * @date 2021/12/12
 * 去重服务
 */
@Service
public class DeduplicationRuleService {

    public static final String DEDUPLICATION_RULE_KEY = "deduplicationRule";

    @Autowired
    private ConfigService config;       //本地(local.properties),远程(轻量airflow,重量apollo)

    @Autowired
    private DeduplicationHolder deduplicationHolder;

    //设置发送任务的去重方式
    public void duplication(TaskInfo taskInfo) {
        // 配置样例：{"deduplication_10":{"num":1,"time":300},"deduplication_20":{"num":5}}
        //读取去重配置,Key=deduplicationRule,如果没有配置则是{}
        //然后根据这个配置构建去重参数对象deduplicationParam
        //读取配置
        //     * 1、当启动使用了apollo或者nacos，优先读取远程配置
        //     * 2、当没有启动远程配置，读取本地 local.properties 配置文件的内容
        String deduplicationConfig = config.getProperty(DEDUPLICATION_RULE_KEY, CommonConstant.EMPTY_JSON_OBJECT);

        // 去重
        List<Integer> deduplicationList = DeduplicationType.getDeduplicationList();     //获取去重类型集合,目前就2个
        for (Integer deduplicationType : deduplicationList) {
            //holder找到对应去重类型的建造者Builder(目前有2个),用builder创建去重参数对象
            DeduplicationParam deduplicationParam = deduplicationHolder.selectBuilder(deduplicationType).build(deduplicationConfig, taskInfo);
            if (Objects.nonNull(deduplicationParam)) {
                //从holder中根据去重类型获得对应的service,然后传入去重参数进行去重
                //就是通过不同的方式得到要去重的接收者集合,然后在taskInfo对象中去除这些就接收者
                deduplicationHolder.selectService(deduplicationType).deduplication(deduplicationParam);
            }
        }
    }
}
