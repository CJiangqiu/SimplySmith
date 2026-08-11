package net.simplysmith.forge;

import net.minecraft.world.item.CreativeModeTabs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.simplysmith.SimplySmith;
import net.simplysmith.forge.client.ForgeAffixSyncHandler;
import net.simplysmith.forge.client.SimplySmithForgeClient;
import net.simplysmith.platform.PlatformBridge;
import net.simplysmith.smith.SmithItems;
import net.simplysmith.smith.affix.AffixDataLoader;
import net.simplysmith.smith.affix.Affixes;

import java.util.Map;

// Forge 主入口，由 FML 扫描 @Mod 注解实例化
@Mod(SimplySmith.MOD_ID)
public final class SimplySmithForge {

    // 构造器参数由 FML 注入
    public SimplySmithForge(FMLJavaModLoadingContext context) {
        // 先注入平台能力，再执行通用层初始化，顺序不可颠倒
        // 使用通用配置目录
        PlatformBridge.init(FMLPaths.CONFIGDIR.get());

        IEventBus modEventBus = context.getModEventBus();
        ForgeItemRegistrar registrar = new ForgeItemRegistrar();
        SmithItems.register(registrar);
        // 登记完成后再挂到事件总线，DeferredRegister 会在注册事件里统一提交
        registrar.attach(modEventBus);
        modEventBus.addListener(SimplySmithForge::addToCreativeTab);

        // 词条加载器挂在数据包一侧，随世界加载与 /reload 一起重跑
        // 在 Forge 总线注册数据包重载监听器
        MinecraftForge.EVENT_BUS.addListener(SimplySmithForge::addReloadListener);

        // OnDatapackSyncEvent 同时覆盖玩家进服与 /reload 两个时机
        ForgeAffixNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(SimplySmithForge::syncAffixes);

        // 数据包词条属于服务端，服务端一停就得清掉
        MinecraftForge.EVENT_BUS.addListener(
                (ServerStoppedEvent event) -> Affixes.replaceDataDriven(Map.of()));

        // 延迟引用客户端类，避免专用服务端加载
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SimplySmithForgeClient.registerConfigScreen(context));

        // 断开连接时清掉服务端下发的词条表，恢复本地配置
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ForgeAffixSyncHandler::register);

        SimplySmith.init();
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AffixDataLoader());
    }

    // getPlayer() 为空表示这是一次 /reload，要给全体在线玩家重发
    private static void syncAffixes(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ForgeAffixNetwork.sendTo(event.getPlayer());
            return;
        }
        event.getPlayerList().getPlayers().forEach(ForgeAffixNetwork::sendTo);
    }

    // 两块石头挂在原版的「材料」页
    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.INGREDIENTS) {
            return;
        }
        SmithItems.all().forEach(item -> event.accept(item.get()));
    }
}
