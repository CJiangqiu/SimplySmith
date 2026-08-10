package net.simplysmith.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import net.simplysmith.network.AffixSyncPacket;

import java.util.List;

/*
客户端收包入口，两端的网络层都调这里

解码与应用分成两步：缓冲区必须在网络线程上当场读完，而改词条表要回到主线程做，
中间隔着一次调度。
*/
public final class ClientAffixSync {

    private ClientAffixSync() {
    }

    public static List<AffixSyncPacket.Entry> decode(FriendlyByteBuf buf) {
        return AffixSyncPacket.decode(buf);
    }

    /*
    单人游戏与局域网主机直接忽略

    两端同一个 JVM、同一份静态词条表，本来就没有不一致可言。真去覆盖反而有害：
    下发值会盖住游戏内配置页刚改完的数，玩家会看到改了没反应。
    */
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
