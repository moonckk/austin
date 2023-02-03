package com.java3y.austin.handler.deduplication.service;


import com.java3y.austin.handler.deduplication.DeduplicationParam;

/**
 * @author huskey
 * @date 2022/1/18
 */
public interface DeduplicationService {

    /**
     * 根据去重参数进行去重
     *
     * @param param 去重参数 JSON
     */
    void deduplication(DeduplicationParam param);
}
