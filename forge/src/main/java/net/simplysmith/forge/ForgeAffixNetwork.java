package net.simplysmith.forge;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import net.simplysmith.forge.client.ForgeAffixSyncHandler;
import net.simplysmith.network.AffixSyncPacket;

import java.util.List;
import java.util.function.Supplier;

/*
Forge 侧的词条表下发

OnDatapackSyncEvent 同时覆盖玩家进服与 /reload 两个时机，不用像 Fabric 那样挂两个事件。
*/
public final class ForgeAffixNetwork {

    /*
    协议版本随载荷结构变动而变

    两端版本对不上时 Forge 会直接拒绝连接，好过让旧客户端按错误的结构解包。
    */
    private static final String VERSION = "2";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(AffixSyncPacket.CHANNEL)
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private ForgeAffixNetwork() {
    }

    public record Message(List<AffixSyncPacket.Entry> entries) {
    }

    public static void register() {
        CHANNEL.registerMessage(0, Message.class,
                (message, buf) -> AffixSyncPacket.encode(message.entries(), buf),
                buf -> new Message(AffixSyncPacket.decode(buf)),
                ForgeAffixNetwork::handle);
    }

    public static void sendTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Message(AffixSyncPacket.snapshot()));
    }

    /*
    收包只发生在客户端

    内层 lambda 才引用客户端类，专用服务端不会执行外层 Supplier，那个类因此不会被加载。
    */
    private static void handle(Message message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ForgeAffixSyncHandler.apply(message.entries())));
        ctx.setPacketHandled(true);
    }
}
