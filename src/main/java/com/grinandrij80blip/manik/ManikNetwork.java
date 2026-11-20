package com.grinandrij80blip.manik;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Carries the real client-side held state of Shift + right mouse button to
 * the logical server. Only state changes are sent, not one packet per tick.
 */
public final class ManikNetwork {
    private record UseInputPacket(boolean held) {
    }

    private static final SimpleChannel CHANNEL =
            ChannelBuilder.named("manik:main").networkProtocolVersion(1).simpleChannel();

    private static final StreamCodec<RegistryFriendlyByteBuf, UseInputPacket> USE_INPUT_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeBoolean(packet.held()),
                    buffer -> new UseInputPacket(buffer.readBoolean())
            );

    public static void register() {
        CHANNEL.play()
                .flow(PacketFlow.SERVERBOUND)
                .addMain(UseInputPacket.class, USE_INPUT_CODEC, (packet, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        GeneratorManager.setUseInputHeld(sender, packet.held());
                    }
                })
                .build();
    }

    public static void sendUseInput(boolean held) {
        CHANNEL.send(new UseInputPacket(held), PacketDistributor.SERVER.noArg());
    }

    private ManikNetwork() {
    }
}
