package com.java3y.austin.web.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.service.api.domain.MessageParam;
import com.java3y.austin.service.api.domain.SendRequest;
import com.java3y.austin.service.api.domain.SendResponse;
import com.java3y.austin.service.api.enums.BusinessCode;
import com.java3y.austin.service.api.service.RecallService;
import com.java3y.austin.service.api.service.SendService;
import com.java3y.austin.support.domain.MessageTemplate;
import com.java3y.austin.web.service.MessageTemplateService;
import com.java3y.austin.web.utils.Convert4Amis;
import com.java3y.austin.web.utils.LoginUtils;
import com.java3y.austin.web.vo.MessageTemplateParam;
import com.java3y.austin.web.vo.MessageTemplateVo;
import com.java3y.austin.web.vo.amis.CommonAmisVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 消息模板管理Controller
 *
 * @author 3y
 */
@Slf4j
@RestController
@RequestMapping("/messageTemplate")
@Api("发送消息")
public class MessageTemplateController {

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private SendService sendService;

    @Autowired
    private RecallService recallService;

    @Autowired
    private LoginUtils loginUtils;

    @Value("${austin.business.upload.crowd.path}")
    private String dataPath;

    /**
     * 如果Id存在，则修改
     * 如果Id不存在，则保存
     */
    @PostMapping("/save")
    @ApiOperation("/保存数据")
    public BasicResultVO saveOrUpdate(@RequestBody MessageTemplate messageTemplate) {
        if (loginUtils.needLogin() && StrUtil.isBlank(messageTemplate.getCreator())) {
            return BasicResultVO.fail(RespStatusEnum.NO_LOGIN);
        }
        MessageTemplate info = messageTemplateService.saveOrUpdate(messageTemplate);
        return BasicResultVO.success(info);
    }

    /**
     * 列表数据
     */
    @GetMapping("/list")
    @ApiOperation("/列表页")
    public BasicResultVO queryList(@Validated MessageTemplateParam messageTemplateParam) {  //把筛选条件封装起来
        if (loginUtils.needLogin() && StrUtil.isBlank(messageTemplateParam.getCreator())) {
            return BasicResultVO.fail(RespStatusEnum.NO_LOGIN);
        }
        Page<MessageTemplate> messageTemplates = messageTemplateService.queryList(messageTemplateParam);
        List<Map<String, Object>> result = Convert4Amis.flatListMap(messageTemplates.toList());     //转成amis需要的格式
        MessageTemplateVo messageTemplateVo = MessageTemplateVo.builder().count(messageTemplates.getTotalElements()).rows(result).build();
        return BasicResultVO.success(messageTemplateVo);
    }

    /**
     * 根据Id查找
     */
    @GetMapping("query/{id}")
    @ApiOperation("/根据Id查找")
    public BasicResultVO queryById(@PathVariable("id") Long id) {
        Map<String, Object> result = Convert4Amis.flatSingleMap(messageTemplateService.queryById(id));
        return BasicResultVO.success(result);
    }

    /**
     * 根据Id复制
     */
    @PostMapping("copy/{id}")
    @ApiOperation("/根据Id复制")
    public BasicResultVO copyById(@PathVariable("id") Long id) {
        messageTemplateService.copy(id);
        return BasicResultVO.success();
    }


    /**
     * 根据Id删除
     * id多个用逗号分隔开
     */
    @DeleteMapping("delete/{id}")
    @ApiOperation("/根据Ids删除")
    public BasicResultVO deleteByIds(@PathVariable("id") String id) {
        if (StrUtil.isNotBlank(id)) {
            List<Long> idList = Arrays.stream(id.split(StrUtil.COMMA)).map(s -> Long.valueOf(s)).collect(Collectors.toList());
            messageTemplateService.deleteByIds(idList);
            return BasicResultVO.success();
        }
        return BasicResultVO.fail();
    }


    /**
     * 测试发送接口
     *
     * 重点分析这个核心流程
     */
    @PostMapping("test")
    @ApiOperation("/测试发送接口")
    public BasicResultVO test(@RequestBody MessageTemplateParam messageTemplateParam) {
        //前端请求参数转换成Map
        Map<String, String> variables = JSON.parseObject(messageTemplateParam.getMsgContent(), Map.class);
        //构建消息参数
        MessageParam messageParam = MessageParam.builder().receiver(messageTemplateParam.getReceiver()).variables(variables).build();
        //构建发送消息的请求体(业务责任链编号+前端请求参数+消息参数),复杂的请求要用请求体对象封装
        //BusinessCode.COMMON_SEND.getCode()  是不同业务的责任链编号
        SendRequest sendRequest = SendRequest.builder().code(BusinessCode.COMMON_SEND.getCode()).messageTemplateId(messageTemplateParam.getId()).messageParam(messageParam).build();
        //发送消息,得到响应对象
        SendResponse response = sendService.send(sendRequest);
        if (response.getCode() != RespStatusEnum.SUCCESS.getCode()) {
            return BasicResultVO.fail(response.getMsg());
        }
        return BasicResultVO.success(response);
    }

    /**
     * 获取需要测试的模板占位符，透出给Amis
     */
    @PostMapping("test/content")
    @ApiOperation("/获取需要测试的模板占位符")
    public BasicResultVO test(Long id) {
        MessageTemplate messageTemplate = messageTemplateService.queryById(id); //根据id获取消息模版
        CommonAmisVo commonAmisVo = Convert4Amis.getTestContent(messageTemplate.getMsgContent());  //获取消息模版内容(包含占位符)
        if (Objects.nonNull(commonAmisVo)) {
            return BasicResultVO.success(commonAmisVo);
        }
        return BasicResultVO.success();
    }


    /**
     * 撤回接口
     */
    @PostMapping("recall/{id}")
    @ApiOperation("/撤回消息接口")
    public BasicResultVO recall(@PathVariable("id") String id) {
        //撤回业务也是一条责任链业务,业务代码是RECALL
        SendRequest sendRequest = SendRequest.builder().code(BusinessCode.RECALL.getCode()).
                messageTemplateId(Long.valueOf(id)).build();
        //发送撤回消息的请求得到响应对象
        SendResponse response = recallService.recall(sendRequest);
        if (response.getCode() != RespStatusEnum.SUCCESS.getCode()) {
            return BasicResultVO.fail(response.getMsg());
        }
        return BasicResultVO.success(response);
    }


    /**
     * 启动模板的定时任务
     */
    @PostMapping("start/{id}")
    @ApiOperation("/启动模板的定时任务")
    public BasicResultVO start(@RequestBody @PathVariable("id") Long id) {
        return messageTemplateService.startCronTask(id);    //开启xxl定时任务
    }

    /**
     * 暂停模板的定时任务
     */
    @PostMapping("stop/{id}")
    @ApiOperation("/暂停模板的定时任务")
    public BasicResultVO stop(@RequestBody @PathVariable("id") Long id) {
        return messageTemplateService.stopCronTask(id);     //暂定xxl定时任务
    }

    /**
     * 上传人群文件
     */
    @PostMapping("upload")
    @ApiOperation("/上传人群文件")
    public BasicResultVO upload(@RequestParam("file") MultipartFile file) {
        String filePath = new StringBuilder(dataPath)
                .append(IdUtil.fastSimpleUUID())
                .append(file.getOriginalFilename())
                .toString();
        try {
            File localFile = new File(filePath);
            if (!localFile.exists()) {
                localFile.mkdirs();
            }
            file.transferTo(localFile);
        } catch (Exception e) {
            log.error("MessageTemplateController#upload fail! e:{},params{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(file));
            return BasicResultVO.fail(RespStatusEnum.SERVICE_ERROR);
        }
        return BasicResultVO.success(MapUtil.of(new String[][]{{"value", filePath}}));
    }

}

