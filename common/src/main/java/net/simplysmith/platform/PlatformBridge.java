package net.simplysmith.platform;

import java.nio.file.Path;

// 注入两端不同的平台能力
public final class PlatformBridge {

    // .minecraft/config
    private static Path configDir;

    private PlatformBridge() {
    }

    // 初始化平台配置目录
    public static void init(Path gameConfigDir) {
        configDir = gameConfigDir;
    }

    public static Path configDir() {
        if (configDir == null) {
            throw new IllegalStateException("PlatformBridge 尚未初始化，平台入口必须先调用 init()");
        }
        return configDir;
    }
}
