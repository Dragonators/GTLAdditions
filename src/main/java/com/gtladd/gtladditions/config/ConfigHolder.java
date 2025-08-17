package com.gtladd.gtladditions.config;

import com.gtladd.gtladditions.GTLAdditions;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = GTLAdditions.MOD_ID)
public class ConfigHolder {

    public static ConfigHolder INSTANCE;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.yaml()).getConfigInstance();
            }
        }
    }

    @Configurable
    @Configurable.Comment("ME超级样板总成配置")
    public SuperPatternBufferConfig superPatternBuffer = new SuperPatternBufferConfig();

    public static class SuperPatternBufferConfig {

        @Configurable
        @Configurable.Comment("每行样板数量")
        @Configurable.Range(min = 1, max = 18)
        public int patternsPerRow = 9;

        @Configurable
        @Configurable.Comment("每页行数")
        @Configurable.Range(min = 1, max = 10)
        public int rowsPerPage = 6;

        @Configurable
        @Configurable.Comment("最大页数")
        @Configurable.Range(min = 1, max = 10)
        public int maxPages = 3;
    }

    @Configurable
    @Configurable.Comment("超INT性能配置")
    public PerformanceConfig performance = new PerformanceConfig();

    public static class PerformanceConfig {

        @Configurable
        @Configurable.Comment("样板供应器额外发配次数")
        @Configurable.Range(min = 1, max = 500000)
        public int externalStorageMaxTimes = 250000;

        @Configurable
        @Configurable.Comment("GT配方超INT物品额外发配次数")
        @Configurable.Range(min = 1, max = 1000000)
        public int recipeContentMaxTimes = 100000;
    }
}
