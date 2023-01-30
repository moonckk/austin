package com.java3y.austin.support.pipeline;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.support.exception.ProcessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程控制器
 *
 * @author 3y
 */
@Slf4j
@Data
public class ProcessController {

    /**
     * 模板映射
     */
    private Map<String, ProcessTemplate> templateConfig = null;


    /**
     * 执行责任链
     *
     * @param context
     * @return 返回上下文内容
     */
    public ProcessContext process(ProcessContext context) {

        /**
         * 前置检查
         */
        try {
            preCheck(context);  //责任链前置检查,此处主要检查了责任链上下文对象是否合法
        } catch (ProcessException e) {
            return e.getProcessContext();
        }

        /**
         * 遍历流程节点
         */
        //获取执行责任链所需的处理器,这些处理都实现同一个业务接口(多态)
        //这些处理器在configuration中已经注册完成了,Map形式: K-业务编号   V-处理器集合List
        //其实这个不是经典的责任链,做出了改造,用List代替了单链表
        List<BusinessProcess> processList = templateConfig.get(context.getCode()).getProcessList();
        for (BusinessProcess businessProcess : processList) {   //按照顺序依次执行责任链的
            businessProcess.process(context);
            if (context.getNeedBreak()) {   //如果needBreak=true,说明执行过程中遇到了中断,需要结束责任链的执行
                break;
            }
        }
        return context; //责任链不论执行到哪个处理器(哪一步),都需要返回上下文对象
    }


    /**
     * 执行前检查，出错则抛出异常
     *
     * @param context 执行上下文
     * @throws ProcessException 异常信息
     */
    private void preCheck(ProcessContext context) throws ProcessException {
        // 上下文
        if (Objects.isNull(context)) {
            context = new ProcessContext();
            context.setResponse(BasicResultVO.fail(RespStatusEnum.CONTEXT_IS_NULL));
            throw new ProcessException(context);
        }

        // 业务代码
        String businessCode = context.getCode();
        if (StrUtil.isBlank(businessCode)) {
            context.setResponse(BasicResultVO.fail(RespStatusEnum.BUSINESS_CODE_IS_NULL));
            throw new ProcessException(context);
        }

        // 执行模板
        ProcessTemplate processTemplate = templateConfig.get(businessCode);
        if (Objects.isNull(processTemplate)) {
            context.setResponse(BasicResultVO.fail(RespStatusEnum.PROCESS_TEMPLATE_IS_NULL));
            throw new ProcessException(context);
        }

        // 执行模板列表
        List<BusinessProcess> processList = processTemplate.getProcessList();
        if (CollUtil.isEmpty(processList)) {
            context.setResponse(BasicResultVO.fail(RespStatusEnum.PROCESS_LIST_IS_NULL));
            throw new ProcessException(context);
        }

    }


}
