package com.grinandrij80blip.manik;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ManikBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ManikMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = ManikItems.ITEMS;

    public static final RegistryObject<Block> ITEM_SPAWN = BLOCKS.register("item_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("item_spawn"))
                    .mapColor(MapColor.COLOR_YELLOW).strength(0.5F).noOcclusion().noCollission()));
    public static final RegistryObject<Block> GENERATOR_SPAWN = BLOCKS.register("generator_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("generator_spawn"))
                    .mapColor(MapColor.COLOR_BLUE).strength(0.5F).noOcclusion().noCollission()));
    public static final RegistryObject<Block> GENERATOR = BLOCKS.register("generator",
            () -> new Block(BlockBehaviour.Properties.of().setId(BLOCKS.key("generator"))
                    .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).noOcclusion()));
    public static final RegistryObject<Block> SURVIVOR_SPAWN = BLOCKS.register("survivor_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("survivor_spawn"))
                    .mapColor(MapColor.COLOR_GREEN).strength(0.5F).noOcclusion().noCollission()));
    public static final RegistryObject<Block> KILLER_SPAWN = BLOCKS.register("killer_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("killer_spawn"))
                    .mapColor(MapColor.COLOR_RED).strength(0.5F).noOcclusion().noCollission()));
    public static final RegistryObject<Block> EXIT_DOOR = BLOCKS.register("exit_door",
            () -> new ManikExitDoorBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("exit_door"))
                    .mapColor(MapColor.COLOR_GRAY).strength(4.0F).noOcclusion()));
    public static final RegistryObject<ManikTrapBlock> TRAP = BLOCKS.register("trap",
            () -> new ManikTrapBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("trap"))
                    .mapColor(MapColor.COLOR_GRAY).strength(-1.0F).noOcclusion()));

    public static final RegistryObject<Item> ITEM_SPAWN_ITEM = ITEMS.register("item_spawn",
            () -> new BlockItem(ITEM_SPAWN.get(), new Item.Properties().setId(ITEMS.key("item_spawn"))));
    public static final RegistryObject<Item> GENERATOR_SPAWN_ITEM = ITEMS.register("generator_spawn",
            () -> new BlockItem(GENERATOR_SPAWN.get(), new Item.Properties().setId(ITEMS.key("generator_spawn"))));
    public static final RegistryObject<Item> GENERATOR_ITEM = ITEMS.register("generator",
            () -> new BlockItem(GENERATOR.get(), new Item.Properties().setId(ITEMS.key("generator"))));
    public static final RegistryObject<Item> SURVIVOR_SPAWN_ITEM = ITEMS.register("survivor_spawn",
            () -> new BlockItem(SURVIVOR_SPAWN.get(), new Item.Properties().setId(ITEMS.key("survivor_spawn"))));
    public static final RegistryObject<Item> KILLER_SPAWN_ITEM = ITEMS.register("killer_spawn",
            () -> new BlockItem(KILLER_SPAWN.get(), new Item.Properties().setId(ITEMS.key("killer_spawn"))));
    public static final RegistryObject<Item> EXIT_DOOR_ITEM = ITEMS.register("exit_door",
            () -> new BlockItem(EXIT_DOOR.get(), new Item.Properties().setId(ITEMS.key("exit_door"))));
    public static final RegistryObject<Item> TRAP_ITEM = ITEMS.register("trap",
            () -> new ManikTrapItem(TRAP.get(), new Item.Properties()
                    .setId(ITEMS.key("trap")).stacksTo(16)));

    private ManikBlocks() {}
}
