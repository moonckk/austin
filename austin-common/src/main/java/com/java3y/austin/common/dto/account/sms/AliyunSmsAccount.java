package com.java3y.austin.common.dto.account.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阿里云短信参数
 *
 * 账号参数实例:
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AliyunSmsAccount extends SmsAccount {
    /**
     * api
     */
    private String region;
    private String url;

    /**
     * 账号
     */
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;

}
