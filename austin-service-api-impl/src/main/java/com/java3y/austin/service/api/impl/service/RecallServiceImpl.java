package com.java3y.austin.service.api.impl.service;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.service.api.domain.SendRequest;
import com.java3y.austin.service.api.domain.SendResponse;
import com.java3y.austin.service.api.impl.domain.SendTaskModel;
import com.java3y.austin.service.api.service.RecallService;
import com.java3y.austin.support.pipeline.ProcessContext;
import com.java3y.austin.support.pipeline.ProcessController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 撤回接口
 *
 * @author 3y
 */
@Service
public class RecallServiceImpl implements RecallService {

    @Autowired
    private ProcessController processController;

    @Override
    public SendResponse recall(SendRequest sendRequest) {
        //这里还是熟悉的责任链的味道
        SendTaskModel sendTaskModel = SendTaskModel.builder()
                .messageTemplateId(sendRequest.getMessageTemplateId())
                .build();
        ProcessContext context = ProcessContext.builder()
                .code(sendRequest.getCode())
                .processModel(sendTaskModel)
                .needBreak(false)
                .response(BasicResultVO.success()).build();
        ProcessContext process = processController.process(context);
        return new SendResponse(process.getResponse().getStatus(), process.getResponse().getMsg());
    }
}
