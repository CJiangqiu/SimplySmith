package net.simplysmith.platform;

import java.nio.file.Path;

/*
common 层需要、但两端实现不同的能力从这里取，由平台入口在 SimplySmith.init() 之前注入
不走注解处理器方案是为了不给 common 层引额外依赖
*/
public final class PlatformBridge {

    // .minecraft/config
    private static Path configDir;

    private PlatformBridge() {
    }

    // Fabric 传 FabricLoader.getConfigDir()，Forge 传 FMLPaths.CONFIGDIR，两者指向同一目录
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
