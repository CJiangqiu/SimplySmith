package net.simplysmith.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.simplysmith.client.config.SimplySmithConfigScreen;

/*
Forge 客户端的可选 Cloth Config 入口
未安装 Cloth Config 时不注册配置页面，也不会加载共用页面类。

context 由 Mod 主入口的构造器一路传进来，不走静态入口取——
FMLJavaModLoadingContext.get() 与 ModLoadingContext.get() 都已标记为待删除。
*/
public final class SimplySmithForgeClient {

    private SimplySmithForgeClient() {
    }

    public static void registerConfigScreen(FMLJavaModLoadingContext context) {
        if (!ModList.get().isLoaded("cloth_config")) {
            return;
        }
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> SimplySmithConfigScreen.create(parent)));
    }
}
