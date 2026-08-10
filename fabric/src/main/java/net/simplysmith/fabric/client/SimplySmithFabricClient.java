package net.simplysmith.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.simplysmith.client.network.ClientAffixSync;
import net.simplysmith.network.AffixSyncPacket;

import java.util.List;

// Fabric 客户端入口，对应 fabric.mod.json 的 client 入口点
public final class SimplySmithFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerAffixSync();
    }

    private static void registerAffixSync() {
        ClientPlayNetworking.registerGlobalReceiver(AffixSyncPacket.CHANNEL,
                (client, handler, buf, responseSender) -> {
                    // 缓冲区在网络线程上当场读完，改词条表回主线程做
                    List<AffixSyncPacket.Entry> entries = ClientAffixSync.decode(buf);
                    client.execute(() -> ClientAffixSync.apply(entries));
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientAffixSync.reset());
    }
}
