package net.simplysmith.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.simplysmith.client.config.SimplySmithConfigScreen;

// Forge 客户端的可选 Cloth Config 入口
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
