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
    private record CarryPacket(boolean carrying) {}
    private record CarrierPacket(int entityId) {}
    private record JumpscarePacket() {}
    public static final int CINEMATIC_START = 0;
    public static final int CINEMATIC_FADE_IN = 1;
    public static final int CINEMATIC_WIN = 2;
    public static final int CINEMATIC_CANCEL = 3;
    private record CinematicPacket(int type, int value) {}

    private record PowerRequestPacket(int slot) {}
    private record PowerSyncPacket(int cooldown1, int active1, int cooldown2, int active2, boolean enabled) {}

    private static final SimpleChannel CHANNEL =
            ChannelBuilder.named("manik:main").networkProtocolVersion(11).simpleChannel();

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

    private static final StreamCodec<RegistryFriendlyByteBuf, CinematicPacket> CINEMATIC_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.type());
                        buffer.writeVarInt(packet.value());
                    },
                    buffer -> new CinematicPacket(buffer.readVarInt(), buffer.readVarInt()));

    private static final StreamCodec<RegistryFriendlyByteBuf, PowerRequestPacket> POWER_REQUEST_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeVarInt(packet.slot()),
                    buffer -> new PowerRequestPacket(buffer.readVarInt()));

    private static final StreamCodec<RegistryFriendlyByteBuf, JumpscarePacket> JUMPSCARE_CODEC =
            StreamCodec.of((buffer, packet) -> {}, buffer -> new JumpscarePacket());

    private static final StreamCodec<RegistryFriendlyByteBuf, PowerSyncPacket> POWER_SYNC_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.cooldown1());
                        buffer.writeVarInt(packet.active1());
                        buffer.writeVarInt(packet.cooldown2());
                        buffer.writeVarInt(packet.active2());
                        buffer.writeBoolean(packet.enabled());
                    },
                    buffer -> new PowerSyncPacket(buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean())
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

    private static final StreamCodec<RegistryFriendlyByteBuf, CarryPacket> CARRY_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeBoolean(packet.carrying()),
                    buffer -> new CarryPacket(buffer.readBoolean())
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, CarrierPacket> CARRIER_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeVarInt(packet.entityId() + 1),
                    buffer -> new CarrierPacket(buffer.readVarInt() - 1)
            );

    public static void register() {
        CHANNEL.play().flow(PacketFlow.SERVERBOUND)
                .addMain(UseInputPacket.class, USE_INPUT_CODEC, (packet, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) GeneratorManager.setUseInputHeld(sender, packet.held());
                })
                .addMain(PowerRequestPacket.class, POWER_REQUEST_CODEC, (packet, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) PowerManager.activate(sender, packet.slot());
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
                                packet.cooldown1(), packet.active1(),
                                packet.cooldown2(), packet.active2(), packet.enabled()))
                .addMain(CarryPacket.class, CARRY_CODEC,
                        (packet, context) -> CarryState.set(packet.carrying()))
                .addMain(CarrierPacket.class, CARRIER_CODEC,
                        (packet, context) -> CarryState.setCarrierId(packet.entityId()))
                .addMain(CinematicPacket.class, CINEMATIC_CODEC,
                        (packet, context) -> CinematicHud.handle(packet.type(), packet.value()))
                .addMain(JumpscarePacket.class, JUMPSCARE_CODEC,
                        (packet, context) -> JumpscareHud.start())
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

    public static void sendPowerRequest(int slot) {
        CHANNEL.send(new PowerRequestPacket(slot), PacketDistributor.SERVER.noArg());
    }

    public static void sendPower(ServerPlayer player, int cooldown1, int active1,
                                 int cooldown2, int active2, boolean enabled) {
        CHANNEL.send(new PowerSyncPacket(cooldown1, active1, cooldown2, active2, enabled),
                PacketDistributor.PLAYER.with(player));
    }

    public static void sendCarry(ServerPlayer player, boolean carrying) {
        CHANNEL.send(new CarryPacket(carrying), PacketDistributor.PLAYER.with(player));
    }

    public static void sendCarrier(ServerPlayer player, int entityId) {
        CHANNEL.send(new CarrierPacket(entityId), PacketDistributor.PLAYER.with(player));
    }

    public static void sendCinematic(ServerPlayer player, int type, int value) {
        CHANNEL.send(new CinematicPacket(type, value), PacketDistributor.PLAYER.with(player));
    }

    public static void sendJumpscare(ServerPlayer player) {
        CHANNEL.send(new JumpscarePacket(), PacketDistributor.PLAYER.with(player));
    }

    public static void sendStamina(ServerPlayer player, int stamina, int exhaustionTicks,
                                   int jumpBlockedTicks, boolean active, boolean canSprint) {
        CHANNEL.send(new StaminaSyncPacket(
                        stamina, exhaustionTicks, jumpBlockedTicks, active, canSprint),
                PacketDistributor.PLAYER.with(player));
    }

    private ManikNetwork() {}
}
