package net.simplysmith.forge;

import net.minecraft.world.item.CreativeModeTabs;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.simplysmith.SimplySmith;
import net.simplysmith.forge.client.SimplySmithForgeClient;
import net.simplysmith.platform.PlatformBridge;
import net.simplysmith.smith.SmithItems;

// Forge 主入口，由 FML 扫描 @Mod 注解实例化
@Mod(SimplySmith.MOD_ID)
public final class SimplySmithForge {

    public SimplySmithForge() {
        // 先注入平台能力，再执行通用层初始化，顺序不可颠倒
        // FMLPaths.CONFIGDIR 与 Fabric 的 getConfigDir() 指向同一个 .minecraft/config
        PlatformBridge.init(FMLPaths.CONFIGDIR.get());

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeItemRegistrar registrar = new ForgeItemRegistrar();
        SmithItems.register(registrar);
        // 登记完成后再挂到事件总线，DeferredRegister 会在注册事件里统一提交
        registrar.attach(modEventBus);
        modEventBus.addListener(SimplySmithForge::addToCreativeTab);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> SimplySmithForgeClient::registerConfigScreen);

        SimplySmith.init();
    }

    // 两块石头挂在原版的「材料」页
    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.INGREDIENTS) {
            return;
        }
        SmithItems.all().forEach(item -> event.accept(item.get()));
    }
}
