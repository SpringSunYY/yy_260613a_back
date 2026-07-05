package com.lz.framework.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Milvus 配置类。
 *
 * <p>存放 mlivus 相关的可调整参数，在 yml 的 {@code litchi.vector.milvus} 节点下。
 */
@Data
@ConfigurationProperties(prefix = "litchi.vector.milvus")
public class MilvusProperties {

    /** Milvus 服务地址 */
    private String host = "localhost";

    /** Milvus 服务端口 */
    private int port = 19530;

    /**
     * 登录用户名（可选）。
     * <p>对应 Milvus 内置鉴权 {@code root} 用户，或 Zilliz Cloud 的 {@code dbname} 用户。
     * 为空时表示当前 Milvus 未启用鉴权（默认）。
     */
    private String username;

    /**
     * 登录密码（可选）。
     * <p>密码留空时按"不启用鉴权"处理（即不使用 token）。
     */
    private String password;

    /** 集合名前缀，dynamic-collection=true 时自动拼接当前模块名 */
    private String collectionName = "image_search";

    /** 是否启用动态集合名（自动拼接模块名后缀） */
    private boolean dynamicCollection = true;

    /**
     * 将 ID 类型从 Int64 改成 VarChar 时，旧 collection 的 schema 已不兼容。
     * 设为 true 则启动时强制 drop 旧 collection 并用新 schema 重建（数据会丢失）。
     */
    private boolean recreateOnSchemaChange = true;

    /** 单次 Milvus insert RPC 的批处理最大行数 */
    private int insertBatchSize = 64;

    /**
     * 多租户隔离配置。
     *
     * <p>支持两种隔离策略，由 {@link Isolation#getStrategy()} 决定：
     * <ul>
     *   <li><b>partition</b>（默认）：单个 collection 内按 {@code tenantId} 建 partition，
     *       写读都通过 {@code partition_names=["t_42"]} 限定范围。
     *       适合"单租户数据量适中、租户数量中等到多"的场景。</li>
     *   <li><b>database</b>：每租户一个 Milvus database，完全物理隔离。
     *       适合"租户数据量大、需要强隔离、超大规模租户清理"的场景。</li>
     * </ul>
     *
     * <p>未启用租户 / {@code TenantContextHolder.getTenantId() == null} 时：
     * <ul>
     *   <li>partition 模式：操作 {@code _default} 分区（或不限 partition）</li>
     *   <li>database 模式：使用 Milvus 内置 {@code default} database</li>
     * </ul>
     * 即"没有开启就不需要隔离"，老的无租户代码无需改造。
     */
    @NestedConfigurationProperty
    private Isolation isolation = new Isolation();

    /**
     * 多租户隔离子配置。
     */
    @Data
    public static class Isolation {

        /** 隔离策略枚举 */
        public enum Strategy {
            /** 单 collection 内按 tenantId 分区（默认） */
            PARTITION,
            /** 每租户独立 Milvus database */
            DATABASE
        }

        /**
         * 隔离策略：partition / database，默认 partition。
         */
        private Strategy strategy = Strategy.PARTITION;

        /**
         * partition 模式：分区名前缀，例 {@code t_42}。
         */
        private String partitionPrefix = "t_";

        /**
         * database 模式：database 名前缀，例 {@code tnt_42}。
         */
        private String databasePrefix = "tnt_";

        /**
         * tenantId 为 null / 已 ignore 时使用的兜底后缀。
         * <ul>
         *   <li>partition 模式：最终 partition 名 = {@code partitionPrefix + defaultSuffix}（或单独使用 defaultSuffix）</li>
         *   <li>database 模式：最终 database 名 = {@code databasePrefix + defaultSuffix}（或单独使用 defaultSuffix）</li>
         * </ul>
         */
        private String defaultSuffix = "_default";

        /**
         * 首次访问某租户时是否自动创建 partition / database。
         */
        private boolean autoCreate = true;
    }
}