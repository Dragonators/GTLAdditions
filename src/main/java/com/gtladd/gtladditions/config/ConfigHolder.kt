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
    @Configurable.Comment("AE2兼容配置")
    @JvmField
    var ae2 = AE2Config()

    companion object {
        @JvmStatic
        val INSTANCE: ConfigHolder by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Configuration.registerConfig(ConfigHolder::class.java, ConfigFormats.yaml()).configInstance
        }

        fun init() {
            INSTANCE
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

        class AE2Config {

            @Configurable
            @Configurable.Comment("成型面板物品实体输出轮数。每轮最多输出一个对应物品的最大堆叠数量。")
            @Configurable.Range(min = 1, max = 1000)
            @JvmField
            var formationPlaneItemEntityOutputRounds = 1
        }
    }
}