package com.java3y.austin.cron.xxl.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.cron.xxl.constants.XxlJobConstant;
import com.java3y.austin.cron.xxl.entity.XxlJobGroup;
import com.java3y.austin.cron.xxl.entity.XxlJobInfo;
import com.java3y.austin.cron.xxl.enums.*;
import com.java3y.austin.cron.xxl.service.CronTaskService;
import com.java3y.austin.support.domain.MessageTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * xxlJob工具类
 *
 * @author 3y
 */
@Component
public class XxlJobUtils {

    @Value("${xxl.job.executor.appname}")
    private String appName;

    @Value("${xxl.job.executor.jobHandlerName}")
    private String jobHandlerName;

    @Autowired
    private CronTaskService cronTaskService;

    /**
     * 构建xxlJobInfo信息
     *
     * @param messageTemplate
     * @return
     */
    public XxlJobInfo buildXxlJobInfo(MessageTemplate messageTemplate) {

        String scheduleConf = messageTemplate.getExpectPushTime();      //实时:0,定时:cron表达式
        // 如果没有指定cron表达式，说明立即执行(给到xxl-job延迟5秒的cron表达式)
        if (messageTemplate.getExpectPushTime().equals(String.valueOf(CommonConstant.FALSE))) {
            //当前时间延迟DELAY_TIME秒,格式化成cron的时间格式
            scheduleConf = DateUtil.format(DateUtil.offsetSecond(new Date(), XxlJobConstant.DELAY_TIME), CommonConstant.CRON_FORMAT);
        }

        //构建xxl 任务信息对象
        XxlJobInfo xxlJobInfo = XxlJobInfo.builder()
                .jobGroup(queryJobGroupId()).jobDesc(messageTemplate.getName())     //执行器主键
                .author(messageTemplate.getCreator())       //消息模版创建者
                .scheduleConf(scheduleConf) //调度配置,cron表达式
                .scheduleType(ScheduleTypeEnum.CRON.name())     //调度策略,通过cron调度
                .misfireStrategy(MisfireStrategyEnum.DO_NOTHING.name()) //调度过期策略,不做处理
                .executorRouteStrategy(ExecutorRouteStrategyEnum.CONSISTENT_HASH.name())    //TODO 执行器路由策略,不懂?
                .executorHandler(XxlJobConstant.JOB_HANDLER_NAME)   //执行器,JOB_HANDLER_NAME = "austinJob"
                .executorParam(String.valueOf(messageTemplate.getId())) //执行器任务参数,消息模版id
                .executorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name())   //执行器阻塞处理策略,单机串行
                .executorTimeout(XxlJobConstant.TIME_OUT)   //执行器超时时间, 120秒
                .executorFailRetryCount(XxlJobConstant.RETRY_COUNT) //执行器失败重试次数, 0
                .glueType(GlueTypeEnum.BEAN.name())     //glue类型,Bean
                .triggerStatus(CommonConstant.FALSE)    //调度状态,0停止
                .glueRemark(StrUtil.EMPTY)  //glue备注,无备注
                .glueSource(StrUtil.EMPTY)  //glue源代码,无源代码
                .alarmEmail(StrUtil.EMPTY)  //报警邮件,无
                .childJobId(StrUtil.EMPTY).build(); //子任务ID,无子任务

        //如果消息模版已设置定时任务,则指定xxl任务信息对象的id,相当于更新了,
        //如果没有定时任务ID,则需要插入
        if (Objects.nonNull(messageTemplate.getCronTaskId())) {
            xxlJobInfo.setId(messageTemplate.getCronTaskId());
        }
        return xxlJobInfo;
    }

    /**
     * 根据就配置文件的内容获取jobGroupId，没有则创建
     *
     * @return
     */
    private Integer queryJobGroupId() {
        BasicResultVO basicResultVO = cronTaskService.getGroupId(appName, jobHandlerName);
        if (Objects.isNull(basicResultVO.getData())) {
            XxlJobGroup xxlJobGroup = XxlJobGroup.builder().appname(appName).title(jobHandlerName).addressType(CommonConstant.FALSE).build();
            if (RespStatusEnum.SUCCESS.getCode().equals(cronTaskService.createGroup(xxlJobGroup).getStatus())) {
                return (int) cronTaskService.getGroupId(appName, jobHandlerName).getData();
            }
        }
        return (Integer) basicResultVO.getData();
    }

}
