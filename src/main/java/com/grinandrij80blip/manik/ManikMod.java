package com.grinandrij80blip.manik;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(ManikMod.MOD_ID)
public final class ManikMod {
    public static final String MOD_ID = "manik";
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final RegistryObject<CreativeModeTab> MANIK_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .title(Component.translatable("itemGroup.manik.main"))
                    .icon(() -> new ItemStack(ManikItems.MARK_GLOVE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ManikBlocks.ITEM_SPAWN_ITEM.get());
                        output.accept(ManikBlocks.GENERATOR_SPAWN_ITEM.get());
                        output.accept(ManikBlocks.GENERATOR_ITEM.get());
                        output.accept(ManikItems.CANISTER.get());
                        output.accept(ManikBlocks.SURVIVOR_SPAWN_ITEM.get());
                        output.accept(ManikBlocks.KILLER_SPAWN_ITEM.get());
                        output.accept(ManikBlocks.EXIT_DOOR_ITEM.get());
                        output.accept(ManikBlocks.EXIT_SPAWN_ITEM.get());
                        output.accept(ManikBlocks.ROUND_LEVER_ITEM.get());
                        output.accept(ManikBlocks.TRAP_ITEM.get());
                        output.accept(ManikItems.MEDKIT.get());
                        output.accept(ManikItems.MACADAMIA.get());
                        output.accept(ManikItems.CROWBAR.get());
                        output.accept(ManikItems.STUN_GUN.get());
                        output.accept(ManikItems.TRIPWIRE.get());
                        output.accept(ManikItems.MARK_GLOVE.get());
                    }).build());

    public ManikMod(FMLJavaModLoadingContext context) {
        ManikNetwork.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInputHandler.register();
            StaminaHud.register();
            ManiacClient.register();
            PowerHud.register();
            ExitHud.register();
            ShakeClient.register();
            JumpscareHud.register();
        }
        RegisterCommandsEvent.BUS.addListener(GameCommands::onRegisterCommands);
        BlockEvent.EntityPlaceEvent.BUS.addListener(SpawnPointManager::onBlockPlace);
        BlockEvent.EntityPlaceEvent.BUS.addListener(
                (java.util.function.Predicate<BlockEvent.EntityPlaceEvent>) TrapManager::onBlockPlace);
        BlockEvent.BreakEvent.BUS.addListener(
                (java.util.function.Predicate<BlockEvent.BreakEvent>) TrapManager::onBlockBreak);
        BlockEvent.BreakEvent.BUS.addListener(SpawnPointManager::onBlockBreak);
        ChunkEvent.Load.BUS.addListener(SpawnPointManager::onChunkLoad);
        net.minecraftforge.event.entity.player.EntityItemPickupEvent.BUS.addListener(
                (java.util.function.Predicate<net.minecraftforge.event.entity.player.EntityItemPickupEvent>) PickupGuard::onPickup);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(GeneratorManager::onRightClickBlock);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(TrapManager::onRightClickBlock);
        AttackEntityEvent.BUS.addListener(GloveManager::onAttackEntity);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(TrapManager::onPlayerTick);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(GeneratorManager::onPlayerTick);
        TickEvent.PlayerTickEvent.Pre.BUS.addListener(StaminaManager::enforceNoSprint);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(StaminaManager::onPlayerTick);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(ManiacBodyManager::onPlayerTick);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(PowerManager::onPlayerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(ExitManager::onServerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(TripwireManager::onServerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(ShockManager::onServerTick);
        net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent.BUS.addListener(TripwireManager::onLivingJump);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(PowerManager::onLogout);
        net.minecraftforge.event.entity.living.LivingHurtEvent.BUS.addListener(
                (java.util.function.Predicate<net.minecraftforge.event.entity.living.LivingHurtEvent>) PowerManager::onLivingHurt);
        net.minecraftforge.event.entity.EntityAttributeCreationEvent.BUS.addListener(ManikEntities::onAttributes);
        net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent.BUS.addListener(StaminaManager::onLivingJump);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(GeneratorManager::onPlayerLogout);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(StaminaManager::onLogout);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(GloveManager::onLogout);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(ManiacBodyManager::onLogout);
        var modBusGroup = context.getModBusGroup();
        ManikBlocks.BLOCKS.register(modBusGroup);
        ManikItems.ITEMS.register(modBusGroup);
        ManikEntities.ENTITY_TYPES.register(modBusGroup);
        CREATIVE_TABS.register(modBusGroup);
    }
}
