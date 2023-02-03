package com.java3y.austin.handler.deduplication;

import com.java3y.austin.handler.deduplication.build.Builder;
import com.java3y.austin.handler.deduplication.service.DeduplicationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


/**
 * @author huskey
 * @date 2022/1/18
 */
@Service
public class DeduplicationHolder {      //Holder包装, 其实也可以用枚举,但是枚举是固定的,这个Holder可以改变

    //Holder解释:
    //Holder这个类属于JAX-WS 2.0规范中的一个类，其作用是为不可变的对象引用提供一个可变的包装
    //涉及到Java的按值传递与引用传递之争了。引用传递中在栈上copy一个引用的副本，其
    //指向同一堆对象。但不可变类（比如String）是新建一个堆对象或指向常量池，
    // 这样在传递的时候如果需要两者可像引用传递那样改变就需要借助Holder。

    //消息参数构建者的Map,不同的类型Key对应不同的Builder
    private final Map<Integer, Builder> builderHolder = new HashMap<>(4);

    //不同类型对应不同的去重业务
    private final Map<Integer, DeduplicationService> serviceHolder = new HashMap<>(4);

    public Builder selectBuilder(Integer key) {     //获取业务参数构建者
        return builderHolder.get(key);
    }

    public DeduplicationService selectService(Integer key) {    //获取去重业务
        return serviceHolder.get(key);
    }

    public void putBuilder(Integer key, Builder builder) {      //添加去重参数建造者
        builderHolder.put(key, builder);
    }

    public void putService(Integer key, DeduplicationService service) {     //添加去重业务
        serviceHolder.put(key, service);
    }
}
