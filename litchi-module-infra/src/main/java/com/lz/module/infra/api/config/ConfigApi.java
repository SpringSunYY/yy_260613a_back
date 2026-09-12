package com.lz.module.infra.api.config;

/**
 * 参数配置 API 接口
 *
 * @author 荔枝源码
 */
public interface ConfigApi {

    /**
     * 根据参数键查询参数值
     *
     * @param key 参数键
     * @return 参数值
     */
    String getConfigValueByKey(String key);

    /**
     * 根据传递过来的参数转换为对应的数据类型
     */
    <T> T getConfigValueByKey(String key, Class<T> type);
}
