package net.simplysmith.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.simplysmith.network.AffixSyncPacket;
import net.simplysmith.smith.affix.Affixes;

import java.util.Map;

/*
Fabric 侧的词条表下发

两个时机：玩家进服，以及 /reload 之后——重载会换掉整张表，不重发的话
在线玩家手上还是旧的。
*/
public final class FabricAffixNetwork {

    private FabricAffixNetwork() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer()));

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player);
            }
        });

        /*
        数据包词条属于服务端，服务端一停就得清掉

        单人游戏退出世界走的是这条：客户端的断连回调会因为「与服务端同 JVM」而跳过重置，
        不在这里清的话，上一个世界的词条会留到主菜单，甚至带进下一个世界。
        */
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Affixes.replaceDataDriven(Map.of()));
    }

    private static void send(ServerPlayer player) {
        // 对端没登记这个通道就别发，否则网络层会为每个玩家刷一条警告
        if (!ServerPlayNetworking.canSend(player, AffixSyncPacket.CHANNEL)) {
            return;
        }

        FriendlyByteBuf buf = PacketByteBufs.create();
        AffixSyncPacket.encode(AffixSyncPacket.snapshot(), buf);
        ServerPlayNetworking.send(player, AffixSyncPacket.CHANNEL, buf);
    }
}
