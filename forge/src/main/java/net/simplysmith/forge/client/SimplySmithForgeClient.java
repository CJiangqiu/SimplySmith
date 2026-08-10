package net.simplysmith.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

import net.simplysmith.client.config.SimplySmithConfigScreen;

/*
Forge 客户端的可选 Cloth Config 入口
未安装 Cloth Config 时不注册配置页面，也不会加载共用页面类。
*/
public final class SimplySmithForgeClient {

    private SimplySmithForgeClient() {
    }

    public static void registerConfigScreen() {
        if (!ModList.get().isLoaded("cloth_config")) {
            return;
        }
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> SimplySmithConfigScreen.create(parent)));
    }
}
