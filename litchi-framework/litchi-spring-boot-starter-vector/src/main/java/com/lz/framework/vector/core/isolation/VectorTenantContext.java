package com.lz.framework.vector.core.isolation;

/**
 * 向量模块的多租户上下文桥接接口。
 *
 * <p>设计目的：与 {@code litchi-spring-boot-starter-redis / job / mq} 保持完全相同的模式。
 * 上游 starter 只声明"我需要一个能被外面实现的接口"，由 {@code litchi-spring-boot-starter-biz-tenant}
 * 主动反向依赖本模块并提供基于 {@code TenantContextHolder} 的实现，
 * vector 模块自身 <b>不依赖</b> biz-tenant，pom 干净。
 *
 * <p>对应现有 starter 的同类接口：
 * <ul>
 *   <li>{@code litchi-spring-boot-starter-mq}：
 *       {@code RedisMessageInterceptor} 接口住 mq 模块，
 *       {@code TenantRedisMessageInterceptor} 实现住 biz-tenant 模块。</li>
 *   <li>{@code litchi-spring-boot-starter-vector}（本接口）：
 *       接口住 vector 模块，
 *       由 biz-tenant 模块反向依赖 vector 后提供 {@code TenantVectorTenantContext} 实现。</li>
 * </ul>
 *
 * <p><b>返回值语义</b>：
 * <ul>
 *   <li>{@code null}：当前线程未设置租户 → MilvusService 走 {@code _default} partition / database</li>
 *   <li>{@code -1L}：{@code @TenantIgnore} 显式忽略 → 同样走 {@code _default}</li>
 *   <li>其它正数：真实租户 ID → MilvusService 走 {@code tnt_{tenantId}} 隔离</li>
 * </ul>
 *
 * @author litchi
 */
public interface VectorTenantContext {

    /**
     * 解析当前线程所属租户编号。
     *
     * @return 租户编号；未启用租户返回 {@code null}；{@code @TenantIgnore} 场景返回 {@code -1L}
     */
    Long currentTenantId();
}
