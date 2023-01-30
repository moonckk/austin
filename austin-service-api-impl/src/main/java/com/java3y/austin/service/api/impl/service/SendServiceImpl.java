package com.java3y.austin.service.api.impl.service;

import cn.monitor4all.logRecord.annotation.OperationLog;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.service.api.domain.BatchSendRequest;
import com.java3y.austin.service.api.domain.SendRequest;
import com.java3y.austin.service.api.domain.SendResponse;
import com.java3y.austin.service.api.impl.domain.SendTaskModel;
import com.java3y.austin.service.api.service.SendService;
import com.java3y.austin.support.pipeline.ProcessContext;
import com.java3y.austin.support.pipeline.ProcessController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 发送接口
 *
 * @author 3y
 */
@Service
public class SendServiceImpl implements SendService {

    @Autowired
    private ProcessController processController;        //责任链控制器,我可太熟了

    @Override
    @OperationLog(bizType = "SendService#send", bizId = "#sendRequest.messageTemplateId", msg = "#sendRequest")     //todo 不懂
    public SendResponse send(SendRequest sendRequest) {
        //要结合责任链来理解
        //发送消息对象模型,消息模版+消息参数列表, 其实也就是责任链依赖的业务对象模型
        SendTaskModel sendTaskModel = SendTaskModel.builder()
                .messageTemplateId(sendRequest.getMessageTemplateId())
                .messageParamList(Collections.singletonList(sendRequest.getMessageParam()))
                .build();

        //构建责任链上下文对象,在不同的action之间传递,这个上下文携带了上边的业务对象模型
        ProcessContext context = ProcessContext.builder()
                .code(sendRequest.getCode())        //code表示使用哪个责任链
                .processModel(sendTaskModel)        //携带的业务对象模型
                .needBreak(false)   //false表示责任链不中断,true表示中断责任链,控制责任链传递
                .response(BasicResultVO.success()).build();     //责任链的响应消息

        //将责任链上下文传给责任链控制器,执行责任链.
        //责任链执行完成或者因为异常被中断了,都会返回上下文对象,这个上下文对象存储了责任链的执行状态.
        ProcessContext process = processController.process(context);

        //从上下文中获取请求的状态信息,消息.
        //其实也可以返回请求之后的数据对象,按需索取
        return new SendResponse(process.getResponse().getStatus(), process.getResponse().getMsg());
    }

    @Override
    @OperationLog(bizType = "SendService#batchSend", bizId = "#batchSendRequest.messageTemplateId", msg = "#batchSendRequest")
    public SendResponse batchSend(BatchSendRequest batchSendRequest) {
        SendTaskModel sendTaskModel = SendTaskModel.builder()
                .messageTemplateId(batchSendRequest.getMessageTemplateId())
                .messageParamList(batchSendRequest.getMessageParamList())
                .build();

        ProcessContext context = ProcessContext.builder()
                .code(batchSendRequest.getCode())
                .processModel(sendTaskModel)
                .needBreak(false)
                .response(BasicResultVO.success()).build();

        ProcessContext process = processController.process(context);

        return new SendResponse(process.getResponse().getStatus(), process.getResponse().getMsg());
    }


}
