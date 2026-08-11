package net.simplysmith.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import net.simplysmith.network.AffixSyncPacket;

import java.util.List;

// 客户端收包入口，两端的网络层都调这里
public final class ClientAffixSync {

    private ClientAffixSync() {
    }

    public static List<AffixSyncPacket.Entry> decode(FriendlyByteBuf buf) {
        return AffixSyncPacket.decode(buf);
    }

    // 单人游戏与局域网主机直接忽略
    public static void apply(List<AffixSyncPacket.Entry> entries) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }
        AffixSyncPacket.apply(entries);
    }

    // 断开连接时调用，恢复成本地配置与内置词条
    public static void reset() {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }
        AffixSyncPacket.reset();
    }
}
