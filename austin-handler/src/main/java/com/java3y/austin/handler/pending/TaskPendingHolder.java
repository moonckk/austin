package com.java3y.austin.handler.pending;

import com.dtp.core.thread.DtpExecutor;
import com.java3y.austin.handler.config.HandlerThreadPoolConfig;
import com.java3y.austin.handler.utils.GroupIdMappingUtils;
import com.java3y.austin.support.utils.ThreadPoolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;


/**
 * 存储 每种消息类型 与 TaskPending 的关系
 *
 * @author 3y
 */
@Component
public class TaskPendingHolder {        //pending 挂起   TaskPending 任务挂起   TaskPendingHolder任务挂起持有者
    @Autowired
    private ThreadPoolUtils threadPoolUtils;        //线程池工具类

    private Map<String, ExecutorService> taskPendingHolder = new HashMap<>(32);     //当前有32种

    /**
     * 获取得到所有的groupId
     */
    private static List<String> groupIds = GroupIdMappingUtils.getAllGroupIds();        //所有消费者组

    /**
     * 给每个渠道，每种消息类型初始化一个线程池
     */
    @PostConstruct
    public void init() {
        /**
         * example ThreadPoolName:austin.im.notice
         *
         * 可以通过apollo配置：dynamic-tp-apollo-dtp.yml  动态修改线程池的信息
         */
        for (String groupId : groupIds) {
            //处理器线程池配置根据消费者组 获取 执行器executor
            DtpExecutor executor = HandlerThreadPoolConfig.getExecutor(groupId);
            threadPoolUtils.register(executor);  //注册线程执行器

            taskPendingHolder.put(groupId, executor);
        }
    }

    /**
     * 得到对应的线程池
     *
     * @param groupId
     * @return
     */
    public ExecutorService route(String groupId) {      //根据消费者组id获取对应的executorService
        return taskPendingHolder.get(groupId);
    }


}
