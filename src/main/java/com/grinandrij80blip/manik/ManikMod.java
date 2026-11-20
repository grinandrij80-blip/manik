package com.grinandrij80blip.manik;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
                        output.accept(ManikItems.MARK_GLOVE.get());
                    }).build());

    public ManikMod(FMLJavaModLoadingContext context) {
        RegisterCommandsEvent.BUS.addListener(GameCommands::onRegisterCommands);
        BlockEvent.EntityPlaceEvent.BUS.addListener(SpawnPointManager::onBlockPlace);
        BlockEvent.BreakEvent.BUS.addListener(SpawnPointManager::onBlockBreak);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(GeneratorManager::onRightClickBlock);
        TickEvent.PlayerTickEvent.BUS.addListener(GeneratorManager::onPlayerTick);
        var modBusGroup = context.getModBusGroup();
        ManikBlocks.BLOCKS.register(modBusGroup);
        ManikItems.ITEMS.register(modBusGroup);
        CREATIVE_TABS.register(modBusGroup);
    }
}