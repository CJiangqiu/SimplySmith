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

// Fabric 侧的词条表下发
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

        // 数据包词条属于服务端，服务端一停就得清掉
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
