package com.gtladd.gtladditions.config

import com.gtladd.gtladditions.GTLAdditions
import dev.toma.configuration.Configuration
import dev.toma.configuration.config.Config
import dev.toma.configuration.config.Configurable
import dev.toma.configuration.config.format.ConfigFormats

@Config(id = GTLAdditions.MOD_ID)
class ConfigHolder {
    @Configurable
    @Configurable.Comment("ME超级样板总成配置")
    @JvmField
    var superPatternBuffer = SuperPatternBufferConfig()

    @Configurable
    @Configurable.Comment("超INT性能配置")
    @JvmField
    var performance = PerformanceConfig()

    companion object {
        @JvmStatic
        val INSTANCE: ConfigHolder by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Configuration.registerConfig(ConfigHolder::class.java, ConfigFormats.yaml()).configInstance
        }

        fun init() {
            INSTANCE
        }

        class PerformanceConfig {

            @Configurable
            @Configurable.Comment("样板供应器额外发配次数")
            @Configurable.Range(min = 1, max = 500000)
            @JvmField
            var externalStorageMaxTimes = 250000
        }

        class SuperPatternBufferConfig {

            @Configurable
            @Configurable.Comment("每行样板数量")
            @Configurable.Range(min = 1, max = 18)
            @JvmField
            var patternsPerRow = 9

            @Configurable
            @Configurable.Comment("每页行数")
            @Configurable.Range(min = 1, max = 10)
            @JvmField
            var rowsPerPage = 6

            @Configurable
            @Configurable.Comment("最大页数")
            @Configurable.Range(min = 1, max = 10)
            @JvmField
            var maxPages = 3
        }
    }
}
