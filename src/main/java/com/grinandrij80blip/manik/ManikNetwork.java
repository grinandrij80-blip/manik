package com.grinandrij80blip.manik;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ManikNetwork {
    private record UseInputPacket(boolean held) {}
    private record StaminaSyncPacket(int stamina, int exhaustionTicks, int jumpBlockedTicks, boolean active) {}

    private static final SimpleChannel CHANNEL =
            ChannelBuilder.named("manik:main").networkProtocolVersion(3).simpleChannel();

    private static final StreamCodec<RegistryFriendlyByteBuf, UseInputPacket> USE_INPUT_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeBoolean(packet.held()),
                    buffer -> new UseInputPacket(buffer.readBoolean())
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, StaminaSyncPacket> STAMINA_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.stamina());
                        buffer.writeVarInt(packet.exhaustionTicks());
                        buffer.writeVarInt(packet.jumpBlockedTicks());
                        buffer.writeBoolean(packet.active());
                    },
                    buffer -> new StaminaSyncPacket(
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean())
            );

    public static void register() {
        CHANNEL.play().flow(PacketFlow.SERVERBOUND)
                .addMain(UseInputPacket.class, USE_INPUT_CODEC, (packet, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) GeneratorManager.setUseInputHeld(sender, packet.held());
                });

        CHANNEL.play().flow(PacketFlow.CLIENTBOUND)
                .addMain(StaminaSyncPacket.class, STAMINA_CODEC,
                        (packet, context) -> StaminaHud.update(
                                packet.stamina(), packet.exhaustionTicks(),
                                packet.jumpBlockedTicks(), packet.active()))
                .build();
    }

    public static void sendUseInput(boolean held) {
        CHANNEL.send(new UseInputPacket(held), PacketDistributor.SERVER.noArg());
    }

    public static void sendStamina(ServerPlayer player, int stamina, int exhaustionTicks,
                                   int jumpBlockedTicks, boolean active) {
        CHANNEL.send(new StaminaSyncPacket(stamina, exhaustionTicks, jumpBlockedTicks, active),
                PacketDistributor.PLAYER.with(player));
    }

    private ManikNetwork() {}
}
