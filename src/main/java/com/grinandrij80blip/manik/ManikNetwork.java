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
    private record StaminaSyncPacket(int stamina, int exhaustionTicks, int jumpBlockedTicks,
                                     boolean active, boolean canSprint) {}

    private record ExitSyncPacket(int phase, int ticksLeft) {}
    private record ShakePacket(int ticks) {}
    private record PowerRequestPacket() {}
    private record PowerSyncPacket(int cooldownTicks, int activeTicks, boolean enabled) {}

    private static final SimpleChannel CHANNEL =
            ChannelBuilder.named("manik:main").networkProtocolVersion(7).simpleChannel();

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
                        buffer.writeBoolean(packet.canSprint());
                    },
                    buffer -> new StaminaSyncPacket(
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readBoolean(), buffer.readBoolean())
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, PowerRequestPacket> POWER_REQUEST_CODEC =
            StreamCodec.of((buffer, packet) -> {}, buffer -> new PowerRequestPacket());

    private static final StreamCodec<RegistryFriendlyByteBuf, PowerSyncPacket> POWER_SYNC_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.cooldownTicks());
                        buffer.writeVarInt(packet.activeTicks());
                        buffer.writeBoolean(packet.enabled());
                    },
                    buffer -> new PowerSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean())
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, ExitSyncPacket> EXIT_SYNC_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.phase());
                        buffer.writeVarInt(packet.ticksLeft());
                    },
                    buffer -> new ExitSyncPacket(buffer.readVarInt(), buffer.readVarInt())
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, ShakePacket> SHAKE_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeVarInt(packet.ticks()),
                    buffer -> new ShakePacket(buffer.readVarInt())
            );

    public static void register() {
        CHANNEL.play().flow(PacketFlow.SERVERBOUND)
                .addMain(UseInputPacket.class, USE_INPUT_CODEC, (packet, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) GeneratorManager.setUseInputHeld(sender, packet.held());
                })
                .addMain(PowerRequestPacket.class, POWER_REQUEST_CODEC, (packet, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) PowerManager.activate(sender);
                });

        CHANNEL.play().flow(PacketFlow.CLIENTBOUND)
                .addMain(StaminaSyncPacket.class, STAMINA_CODEC,
                        (packet, context) -> StaminaHud.update(
                                packet.stamina(), packet.exhaustionTicks(),
                                packet.jumpBlockedTicks(), packet.active(), packet.canSprint()))
                .addMain(ExitSyncPacket.class, EXIT_SYNC_CODEC,
                        (packet, context) -> ExitHud.update(packet.phase(), packet.ticksLeft()))
                .addMain(ShakePacket.class, SHAKE_CODEC,
                        (packet, context) -> ShakeClient.start(packet.ticks()))
                .addMain(PowerSyncPacket.class, POWER_SYNC_CODEC,
                        (packet, context) -> PowerHud.update(
                                packet.cooldownTicks(), packet.activeTicks(), packet.enabled()))
                .build();
    }

    public static void sendUseInput(boolean held) {
        CHANNEL.send(new UseInputPacket(held), PacketDistributor.SERVER.noArg());
    }

    public static void sendExit(ServerPlayer player, int phase, int ticksLeft) {
        CHANNEL.send(new ExitSyncPacket(phase, ticksLeft), PacketDistributor.PLAYER.with(player));
    }

    public static void sendShake(ServerPlayer player, int ticks) {
        CHANNEL.send(new ShakePacket(ticks), PacketDistributor.PLAYER.with(player));
    }

    public static void sendPowerRequest() {
        CHANNEL.send(new PowerRequestPacket(), PacketDistributor.SERVER.noArg());
    }

    public static void sendPower(ServerPlayer player, int cooldownTicks, int activeTicks, boolean enabled) {
        CHANNEL.send(new PowerSyncPacket(cooldownTicks, activeTicks, enabled),
                PacketDistributor.PLAYER.with(player));
    }

    public static void sendStamina(ServerPlayer player, int stamina, int exhaustionTicks,
                                   int jumpBlockedTicks, boolean active, boolean canSprint) {
        CHANNEL.send(new StaminaSyncPacket(
                        stamina, exhaustionTicks, jumpBlockedTicks, active, canSprint),
                PacketDistributor.PLAYER.with(player));
    }

    private ManikNetwork() {}
}
