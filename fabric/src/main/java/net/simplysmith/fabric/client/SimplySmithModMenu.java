package net.simplysmith.fabric.client;

import net.fabricmc.loader.api.FabricLoader;

import net.simplysmith.client.config.SimplySmithConfigScreen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/*
Mod Menu 可选入口
Mod Menu 会读取自己的 entrypoint；未安装时 Fabric Loader 不会实例化本类。
*/
public final class SimplySmithModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            // Mod Menu 以返回 null 的工厂表示该 Mod 当前没有配置页面
            return parent -> null;
        }
        return SimplySmithConfigScreen::create;
    }
}
