package cn.jiale.dynamic.thread.pool.sdk.registry;

import cn.jiale.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;

public interface IRegistry {
    //报告线程池
    void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities);
    //线程池配置参数
    void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity);
}
