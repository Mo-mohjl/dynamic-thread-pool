package cn.jiale.dynamic.thread.pool.sdk.config;

import cn.jiale.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import cn.jiale.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import cn.jiale.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.jiale.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;
import cn.jiale.dynamic.thread.pool.sdk.registry.IRegistry;
import cn.jiale.dynamic.thread.pool.sdk.registry.redis.RedisRegistry;
import cn.jiale.dynamic.thread.pool.sdk.trigger.job.ThreadPoolDataReportJob;
import cn.jiale.dynamic.thread.pool.sdk.trigger.listener.ThreadPoolConfigAdjustListener;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@EnableScheduling
public class DynamicThreadPoolAutoConfig {
    private static Logger logger= LoggerFactory.getLogger(DynamicThreadPoolAutoConfig.class);
    String applicationName;
    @Bean("redissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties){
        Config config = new Config();
        config.setCodec(JsonJacksonCodec.INSTANCE);
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;
        RedissonClient redissonClient = Redisson.create(config);
        logger.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", properties.getHost(), properties.getPoolSize(), !redissonClient.isShutdown());
        return redissonClient;
    }
    @Bean("dynamicThreadPoolService")
    public DynamicThreadPoolService dynamicThreadPoolService(ApplicationContext applicationContext, Map<String, ThreadPoolExecutor> threadPoolExecutorMap,RedissonClient redissonClient){
        applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (StringUtils.isBlank(applicationName)){
            applicationName="缺省的";
            logger.info("未设置容器名称");
        }
        logger.info("线程池配置信息：{}", JSON.toJSONString(threadPoolExecutorMap.keySet()));
        // 获取缓存数据，设置本地线程池配置
        Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
        for (String threadPoolKey : threadPoolKeys) {
            ThreadPoolConfigEntity threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + applicationName + "_" + threadPoolKey).get();
            if (null == threadPoolConfigEntity) continue;
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
            threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
            threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
        }
        return new DynamicThreadPoolService(applicationName,threadPoolExecutorMap);
    }
    @Bean
    public IRegistry redisRegistry(RedissonClient redissonClient) {
        return new RedisRegistry(redissonClient);
    }
    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService,IRegistry registry){
        return new ThreadPoolDataReportJob(dynamicThreadPoolService,registry);
    }
    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService,IRegistry registry){
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService,registry);
    }
    @Bean(name = "dynamicThreadPoolRedisTopic")
    public RTopic threadPoolConfigAdjustListener(RedissonClient redissonClient,ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener){
        RTopic topic = redissonClient.getTopic(RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + applicationName);
        topic.addListener(ThreadPoolConfigEntity.class, threadPoolConfigAdjustListener);
        return topic;
    }

}
