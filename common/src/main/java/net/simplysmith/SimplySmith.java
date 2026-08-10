package net.simplysmith;

import net.simplysmith.config.SimplySmithConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
通用层入口，两端的平台入口都调这里
前提：平台入口必须先调 PlatformBridge.init() 注入配置目录
*/
public final class SimplySmith {

    // 需与 fabric.mod.json / mods.toml 中的 id 一致
    public static final String MOD_ID = "simplysmith";

    // 日志一律用英文并带此前缀，便于国际化环境下排查
    public static final String LOG_PREFIX = "[SimplySmith] ";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SimplySmith() {
    }

    public static void init() {
        SimplySmithConfig.load();
        LOGGER.info(LOG_PREFIX + "Initialized");
    }
}
