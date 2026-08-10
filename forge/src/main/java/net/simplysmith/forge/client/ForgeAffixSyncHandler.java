package net.simplysmith.forge.client;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

import net.simplysmith.client.network.ClientAffixSync;
import net.simplysmith.network.AffixSyncPacket;

import java.util.List;

/*
Forge 客户端侧的词条表接收

单独一个类是为了让专用服务端永远不加载它——它引用了只有客户端才有的类。
*/
public final class ForgeAffixSyncHandler {

    private ForgeAffixSyncHandler() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> ClientAffixSync.reset());
    }

    public static void apply(List<AffixSyncPacket.Entry> entries) {
        ClientAffixSync.apply(entries);
    }
}
