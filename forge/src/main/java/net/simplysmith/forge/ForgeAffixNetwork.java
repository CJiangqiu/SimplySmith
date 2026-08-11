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

// Forge 侧的词条表下发
public final class ForgeAffixNetwork {

    // 协议版本随载荷结构变动而变
    private static final String VERSION = "3";

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

    // 收包只发生在客户端
    private static void handle(Message message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ForgeAffixSyncHandler.apply(message.entries())));
        ctx.setPacketHandled(true);
    }
}
