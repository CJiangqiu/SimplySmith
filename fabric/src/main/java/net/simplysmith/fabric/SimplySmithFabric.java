package net.simplysmith.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTabs;

import net.simplysmith.SimplySmith;
import net.simplysmith.platform.PlatformBridge;
import net.simplysmith.smith.SmithItems;

// Fabric 主入口，对应 fabric.mod.json 的 main 入口点
public final class SimplySmithFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 先注入平台能力，再执行通用层初始化，顺序不可颠倒
        PlatformBridge.init(FabricLoader.getInstance().getConfigDir());
        SmithItems.register(new FabricItemRegistrar());
        addToCreativeTab();
        registerAffixLoader();
        FabricAffixNetwork.register();
        SimplySmith.init();
    }

    // 挂在数据包一侧，随世界加载与 /reload 一起重跑
    private static void registerAffixLoader() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricAffixDataLoader());
    }

    // 两块石头挂在原版的「材料」页
    private static void addToCreativeTab() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries ->
                SmithItems.all().forEach(item -> entries.accept(item.get())));
    }
}
