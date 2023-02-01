package com.java3y.austin.web.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiMediaUploadRequest;
import com.dingtalk.api.response.OapiMediaUploadResponse;
import com.google.common.base.Throwables;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.common.constant.SendAccountConstant;
import com.java3y.austin.common.dto.account.EnterpriseWeChatRobotAccount;
import com.java3y.austin.common.enums.FileType;
import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.handler.domain.wechat.robot.EnterpriseWeChatRootResult;
import com.java3y.austin.support.utils.AccountUtils;
import com.java3y.austin.web.service.MaterialService;
import com.java3y.austin.web.utils.SpringFileUtils;
import com.java3y.austin.web.vo.UploadResponseVo;
import com.taobao.api.FileItem;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.result.WxMediaUploadResult;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author 3y
 */
@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AccountUtils accountUtils;

    private static final String DING_DING_URL = "https://oapi.dingtalk.com/media/upload";
    private static final String ENTERPRISE_WE_CHAT_ROBOT_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/upload_media?key=<KEY>&type=<TYPE>";

    @Override
    public BasicResultVO dingDingMaterialUpload(MultipartFile file, String sendAccount, String fileType) {
        OapiMediaUploadResponse rsp;
        try {
            //从redis中获取访问token
            String accessToken = redisTemplate.opsForValue().get(SendAccountConstant.DING_DING_ACCESS_TOKEN_PREFIX + sendAccount);
            DingTalkClient client = new DefaultDingTalkClient(DING_DING_URL);
            OapiMediaUploadRequest req = new OapiMediaUploadRequest();
            //FileItem是淘宝封装的, 文件名(uuid+原文件名)+文件流
            FileItem item = new FileItem(new StringBuilder().append(IdUtil.fastSimpleUUID()).append(file.getOriginalFilename()).toString(),
                    file.getInputStream());
            req.setMedia(item); //将文件加入请求对象
            req.setType(FileType.getNameByCode(fileType));  //设置文件类型
            rsp = client.execute(req, accessToken);
            if (rsp.getErrcode() == 0L) {
                //上传成功,返回上传素材的id
                return new BasicResultVO(RespStatusEnum.SUCCESS, UploadResponseVo.builder().id(rsp.getMediaId()).build());
            }
            log.error("MaterialService#dingDingMaterialUpload fail:{}", rsp.getErrmsg());
        } catch (Exception e) {
            log.error("MaterialService#dingDingMaterialUpload fail:{}", Throwables.getStackTraceAsString(e));
        }
        return BasicResultVO.fail("未知错误，联系管理员");
    }

    @Override
    public BasicResultVO enterpriseWeChatRootMaterialUpload(MultipartFile multipartFile, String sendAccount, String fileType) {
        try {
            //根据发送账号
            EnterpriseWeChatRobotAccount weChatRobotAccount = accountUtils.getAccountById(Integer.valueOf(sendAccount), EnterpriseWeChatRobotAccount.class);
            //截取webhook得到key,取=之前的
            String key = weChatRobotAccount.getWebhook().substring(weChatRobotAccount.getWebhook().indexOf(CommonConstant.EQUAL_STRING) + 1);
            //替换url中的KEY和TYPE
            String url = ENTERPRISE_WE_CHAT_ROBOT_URL.replace("<KEY>", key).replace("<TYPE>", "file");
            //hutool发送请求
            String response = HttpRequest.post(url)
                    .form(IdUtil.fastSimpleUUID(), SpringFileUtils.getFile(multipartFile))
                    .execute().body();
            //企业微信响应值
            EnterpriseWeChatRootResult result = JSON.parseObject(response, EnterpriseWeChatRootResult.class);
            if (result.getErrcode() == 0) {
                return new BasicResultVO(RespStatusEnum.SUCCESS, UploadResponseVo.builder().id(result.getMediaId()).build());
            }
            log.error("MaterialService#enterpriseWeChatRootMaterialUpload fail:{}", result.getErrmsg());
        } catch (Exception e) {
            log.error("MaterialService#enterpriseWeChatRootMaterialUpload fail:{}", Throwables.getStackTraceAsString(e));
        }
        return BasicResultVO.fail("未知错误，联系管理员");
    }

    @Override
    public BasicResultVO enterpriseWeChatMaterialUpload(MultipartFile multipartFile, String sendAccount, String fileType) {
        try {
            WxCpDefaultConfigImpl accountConfig = accountUtils.getAccountById(Integer.valueOf(sendAccount), WxCpDefaultConfigImpl.class);
            WxCpServiceImpl wxCpService = new WxCpServiceImpl();
            wxCpService.setWxCpConfigStorage(accountConfig);
            WxMediaUploadResult result = wxCpService.getMediaService().upload(FileType.getNameByCode(fileType), SpringFileUtils.getFile(multipartFile));
            if (StrUtil.isNotBlank(result.getMediaId())) {
                return new BasicResultVO(RespStatusEnum.SUCCESS, UploadResponseVo.builder().id(result.getMediaId()).build());
            }
            log.error("MaterialService#enterpriseWeChatMaterialUpload fail:{}", JSON.toJSONString(result));
        } catch (Exception e) {
            log.error("MaterialService#enterpriseWeChatMaterialUpload fail:{}", Throwables.getStackTraceAsString(e));
        }
        return BasicResultVO.fail("未知错误，联系管理员");
    }
}
