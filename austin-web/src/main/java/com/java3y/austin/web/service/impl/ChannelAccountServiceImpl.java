package com.java3y.austin.web.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.java3y.austin.common.constant.AustinConstant;
import com.java3y.austin.common.constant.CommonConstant;
import com.java3y.austin.support.dao.ChannelAccountDao;
import com.java3y.austin.support.domain.ChannelAccount;
import com.java3y.austin.support.utils.WxServiceUtils;
import com.java3y.austin.web.service.ChannelAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author 3y
 */
@Service
public class ChannelAccountServiceImpl implements ChannelAccountService {

    @Autowired
    private ChannelAccountDao channelAccountDao;
    @Autowired
    private WxServiceUtils wxServiceUtils;

    @Override
    public ChannelAccount save(ChannelAccount channelAccount) {
        if (Objects.isNull(channelAccount.getId())) {
            channelAccount.setCreated(Math.toIntExact(DateUtil.currentSeconds()));
            channelAccount.setIsDeleted(CommonConstant.FALSE);
        }
        channelAccount.setCreator(StrUtil.isBlank(channelAccount.getCreator()) ? AustinConstant.DEFAULT_CREATOR : channelAccount.getCreator());     //创建者为空时,使用默认创建者
        channelAccount.setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        ChannelAccount result = channelAccountDao.save(channelAccount);     //jpa内置save方法
        wxServiceUtils.fresh();     //渠道账号增加时,刷新Map
        return result;
    }

    @Override
    public List<ChannelAccount> queryByChannelType(Integer channelType, String creator) {
        //查询指定创建者,指定消息渠道渠道,且没有删除的渠道账号列表
        return channelAccountDao.findAllByIsDeletedEqualsAndCreatorEqualsAndSendChannelEquals(CommonConstant.FALSE, creator, channelType);
    }

    @Override
    public List<ChannelAccount> list(String creator) {
        return channelAccountDao.findAllByCreatorEquals(creator);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        channelAccountDao.deleteAllById(ids);   //jpa根据id删除
    }
}
