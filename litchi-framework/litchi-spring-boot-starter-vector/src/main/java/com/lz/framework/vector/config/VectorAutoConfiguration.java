package com.lz.framework.vector.config;

import com.lz.framework.vector.config.MilvusProperties;
import com.lz.framework.vector.config.VectorProperties;
import com.lz.framework.vector.core.feature.ClipFeatureExtractor;
import com.lz.framework.vector.core.feature.DinoFeatureExtractor;
import com.lz.framework.vector.core.feature.FeatureExtractor;
import com.lz.framework.vector.core.feature.FeatureService;
import com.lz.framework.vector.core.feature.SiglipFeatureExtractor;
import com.lz.framework.vector.core.isolation.VectorTenantContext;
import com.lz.framework.vector.core.milvus.MilvusService;
import com.lz.framework.vector.core.vector.ImageIndexService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;

/**
 * 向量模块自动配置。
 *
 * <p>本类是整个 {@code litchi-spring-boot-starter-vector} 模块唯一的 bean 注册入口：
 * 所有 {@link FeatureExtractor}（CLIP / DINOv2 / SigLIP）、{@link MilvusService}、
 * {@link ImageIndexService}、{@link FeatureService}、{@link MilvusServiceClient}
 * 都通过本类的 {@link Bean} 方法显式注册，不再依赖 {@code @Component} / {@code @Service}。
 *
 * <p><b>两层注册策略</b>：
 * <ul>
 *   <li><b>编排层（始终注册）</b>：{@link FeatureService}、{@link MilvusService}、{@link ImageIndexService}
 *       以及三个 {@code *Properties} —— 业务方 {@code @Resource} 注入这些 bean 时永远拿得到实例，
 *       不会因为 {@code litchi.vector.enable=false} 触发 {@code NoSuchBeanDefinitionException}。
 *       关闭时这些 bean 内部的 {@code checkEnabled()} 统一抛 {@link com.lz.framework.vector.core.exception.VectorDisabledException}。</li>
 *   <li><b>重量级层（按 enable 条件注册）</b>：{@link MilvusServiceClient}（gRPC 连接）和
 *       各 {@link FeatureExtractor}（加载 ONNX 模型）—— 仅在 {@code litchi.vector.enable=true}
 *       时才创建，避免关闭时无谓的启动开销（gRPC 握手 / ONNX Runtime 加载）。</li>
 * </ul>
 *
 * <p>特性提取器按模型选择 — 通过方法级 {@code @ConditionalOnProperty} 实现：
 * <pre>
 * embedding.model=clip  → 注册 ClipFeatureExtractor
 * embedding.model=dino  → 注册 DinoFeatureExtractor
 * embedding.model=siglip → 注册 SiglipFeatureExtractor
 * </pre>
 * 未来扩展新模型只需在 {@link #clipFeatureExtractor} 下面追加一个 {@code @Bean @ConditionalOnProperty} 方法。
 *
 * <p>编排层 bean 通过 {@link ObjectProvider} 懒拿重量级 bean —— 关闭时
 * {@code ObjectProvider.getIfAvailable()} 返回 null，编排层据此判断模块是否启用。
 */
@AutoConfiguration
@EnableConfigurationProperties({EmbeddingProperties.class, MilvusProperties.class, VectorProperties.class})
@EnableCaching
@Slf4j
public class VectorAutoConfiguration {

    /**
     * 模块加载感知日志。
     */
    @PostConstruct
    public void onLoaded() {
        log.info("[VectorAutoConfiguration] ✅ 向量模块自动配置已加载。"
                + "编排层（FeatureService / MilvusService / ImageIndexService）始终注册，"
                + "重量级层（MilvusServiceClient / *FeatureExtractor）由 litchi.vector.enable 控制。");
    }

    // ==================== 特征提取器（按 embedding.model 条件装配，仅 enable=true 时注册） ====================

    /**
     * CLIP 特征提取器。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${litchi.vector.enable:false}'.equals('true') "
            + "and ('${litchi.vector.embedding.model:clip}'.equals('clip') "
            + "     or '${litchi.vector.embedding.model:clip}'.equals(''))")
    public ClipFeatureExtractor clipFeatureExtractor(EmbeddingProperties embeddingProps) {
        log.info("[VectorAutoConfiguration] 注册 ClipFeatureExtractor（默认 / 显式 model=clip）");
        return new ClipFeatureExtractor(embeddingProps);
    }

    /**
     * DINOv2 特征提取器。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${litchi.vector.enable:false}'.equals('true') "
            + "and '${litchi.vector.embedding.model:clip}'.equals('dino')")
    public DinoFeatureExtractor dinoFeatureExtractor(EmbeddingProperties embeddingProps) {
        log.info("[VectorAutoConfiguration] 注册 DinoFeatureExtractor（model=dino）");
        return new DinoFeatureExtractor(embeddingProps);
    }

    /**
     * SigLIP 2 特征提取器。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${litchi.vector.enable:false}'.equals('true') "
            + "and '${litchi.vector.embedding.model:clip}'.equals('siglip')")
    public SiglipFeatureExtractor siglipFeatureExtractor(EmbeddingProperties embeddingProps) {
        log.info("[VectorAutoConfiguration] 注册 SiglipFeatureExtractor（model=siglip）");
        return new SiglipFeatureExtractor(embeddingProps);
    }

    // ==================== Milvus gRPC 客户端（仅 enable=true 时注册） ====================

    /**
     * Milvus gRPC 客户端单例。Spring 关闭时自动调用 close()。
     *
     * <p>注意：{@link MilvusServiceClient} 构造时会立即建立 gRPC channel 并尝试握手，
     * 所以这里挂 {@code litchi.vector.enable=true} 条件 —— 关闭时不创建，
     * 编排层通过 {@link ObjectProvider} 懒拿，拿不到就抛 {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "litchi.vector.enable", havingValue = "true")
    public MilvusServiceClient milvusServiceClient(MilvusProperties props) {
        log.info("[VectorAutoConfiguration] 注册 MilvusServiceClient（{}:{}）", props.getHost(), props.getPort());
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(props.getHost())
                .withPort(props.getPort())
                .build();
        return new MilvusServiceClient(connectParam);
    }

    // ==================== 编排层（始终注册） ====================

    /**
     * Milvus CRUD 服务。
     *
     * <p>始终注册（即使 {@code litchi.vector.enable=false}）—— 业务方注入时不会因 bean 缺失报错。
     * 关闭时 {@code featureExtractorProvider} / {@code milvusClientProvider} 都返回 null，
     * 本 bean 内部所有调用经 {@link MilvusService} 私有访问器抛
     * {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     *
     * <p>多租户：通过 {@link ObjectProvider}<{@link VectorTenantContext}> 懒注入。
     * 与 {@code litchi-spring-boot-starter-redis / job / mq} 完全相同的反向桥接模式：
     * 本模块不引 {@code biz-tenant}，由 biz-tenant 反向依赖 vector 后提供实现。
     * 没引 biz-tenant 时本字段无实现，{@link MilvusService} 走 {@code _default}。
     */
    @Bean
    public MilvusService milvusService(ObjectProvider<MilvusServiceClient> milvusClientProvider,
                                       ObjectProvider<FeatureExtractor> featureExtractorProvider,
                                       ObjectProvider<VectorTenantContext> tenantContextProvider,
                                       MilvusProperties milvusProps) {
        log.info("[VectorAutoConfiguration] 注册 MilvusService（编排层始终注册；重量级 bean 懒加载；多租户通过 VectorTenantContext 可选注入）");
        return new MilvusService(milvusClientProvider, featureExtractorProvider, tenantContextProvider, milvusProps);
    }

    /**
     * 特征提取多模型注册中心。
     *
     * <p>接收所有 {@link FeatureExtractor} bean（关闭时为空列表），按模型名收敛注册。
     * {@link FeatureService#isEnabled()} 以 extractor 列表是否为空判断模块开关。
     */
    @Bean
    public FeatureService featureService(ObjectProvider<FeatureExtractor> availableExtractorProvider,
                                         EmbeddingProperties props) {
        ArrayList<FeatureExtractor> available = new ArrayList<>();
        availableExtractorProvider.forEach(available::add);
        log.info("[VectorAutoConfiguration] 注册 FeatureService（候选 extractor={}）", available.size());
        return new FeatureService(available, props);
    }

    /**
     * 图片索引编排服务：特征提取 + Milvus 入库 + 以图搜图。
     *
     * <p>始终注册（即使 {@code litchi.vector.enable=false}）—— 业务方注入时不会因 bean 缺失报错。
     * 关闭时 {@link FeatureService#isEnabled()} 返回 false，本类入口的
     * {@link ImageIndexService#checkEnabled(String)} 抛 {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     */
    @Bean
    public ImageIndexService imageIndexService(FeatureService featureService,
                                               MilvusService milvusService,
                                               EmbeddingProperties embeddingProps,
                                               MilvusProperties milvusProps,
                                               ObjectProvider<VectorTenantContext> tenantContextProvider) {
        log.info("[VectorAutoConfiguration] 注册 ImageIndexService（多租户通过 VectorTenantContext 可选注入）");
        return new ImageIndexService(featureService, milvusService, embeddingProps, milvusProps, tenantContextProvider);
    }
}