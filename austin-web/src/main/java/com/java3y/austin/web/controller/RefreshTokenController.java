package com.java3y.austin.web.controller;


import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.cron.handler.RefreshDingDingAccessTokenHandler;
import com.java3y.austin.cron.handler.RefreshGeTuiAccessTokenHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @Author 3y
 */
@Api(tags = {"手动刷新token的接口"})
@RestController
public class RefreshTokenController {


    @Autowired
    private RefreshDingDingAccessTokenHandler refreshDingDingAccessTokenHandler;    //刷新钉钉访问token处理器
    @Autowired
    private RefreshGeTuiAccessTokenHandler refreshGeTuiAccessTokenHandler;  //刷新访问个推token处理器

    /**
     * 按照不同的渠道刷新对应的Token，channelType取值来源com.java3y.austin.common.enums.ChannelType
     *
     * @param channelType
     * @return
     */
    @ApiOperation(value = "手动刷新token", notes = "钉钉/个推 token刷新")
    @GetMapping("/refresh")
    public BasicResultVO refresh(Integer channelType) {     //根据id刷新对应渠道的token
        if (ChannelType.PUSH.getCode().equals(channelType)) {
            refreshGeTuiAccessTokenHandler.execute();
        }
        if (ChannelType.DING_DING_WORK_NOTICE.getCode().equals(channelType)) {
            refreshDingDingAccessTokenHandler.execute();

        }
        return BasicResultVO.success("刷新成功");
    }

}
