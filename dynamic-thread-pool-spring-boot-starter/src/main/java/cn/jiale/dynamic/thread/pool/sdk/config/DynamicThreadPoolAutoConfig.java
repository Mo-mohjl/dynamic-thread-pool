package cn.jiale.dynamic.thread.pool.sdk.config;

import cn.jiale.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DynamicThreadPoolAutoConfig {
    private static Logger logger= LoggerFactory.getLogger(DynamicThreadPoolAutoConfig.class);
    @Bean("dynamicThreadPoolService")
    public DynamicThreadPoolService dynamicThreadPoolService(ApplicationContext applicationContext, Map<String, ThreadPoolExecutor> threadPoolExecutorMap){
        String applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (StringUtils.isBlank(applicationName)){
            applicationName="缺省的";
            logger.info("未设置容器名称");
        }
        logger.info("线程池配置信息：{}", JSON.toJSONString(threadPoolExecutorMap.keySet()));
        return new DynamicThreadPoolService(applicationName,threadPoolExecutorMap);
    }
}
