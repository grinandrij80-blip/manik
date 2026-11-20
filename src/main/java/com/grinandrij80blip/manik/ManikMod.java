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
                        output.accept(ManikBlocks.TRAP_ITEM.get());
                        output.accept(ManikItems.MEDKIT.get());
                        output.accept(ManikItems.CROWBAR.get());
                        output.accept(ManikItems.MARK_GLOVE.get());
                    }).build());

    public ManikMod(FMLJavaModLoadingContext context) {
        ManikNetwork.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInputHandler.register();
            StaminaHud.register();
        }
        RegisterCommandsEvent.BUS.addListener(GameCommands::onRegisterCommands);
        BlockEvent.EntityPlaceEvent.BUS.addListener(SpawnPointManager::onBlockPlace);
        BlockEvent.EntityPlaceEvent.BUS.addListener(
                (java.util.function.Predicate<BlockEvent.EntityPlaceEvent>) TrapManager::onBlockPlace);
        BlockEvent.BreakEvent.BUS.addListener(SpawnPointManager::onBlockBreak);
        ChunkEvent.Load.BUS.addListener(SpawnPointManager::onChunkLoad);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(GeneratorManager::onRightClickBlock);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(TrapManager::onRightClickBlock);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(GeneratorManager::onPlayerTick);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(StaminaManager::onPlayerTick);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(GeneratorManager::onPlayerLogout);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(StaminaManager::onLogout);
        var modBusGroup = context.getModBusGroup();
        ManikBlocks.BLOCKS.register(modBusGroup);
        ManikItems.ITEMS.register(modBusGroup);
        CREATIVE_TABS.register(modBusGroup);
    }
}
