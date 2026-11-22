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
                    .mapColor(MapColor.COLOR_YELLOW).strength(0.5F).noOcclusion().noCollision()));
    public static final RegistryObject<Block> GENERATOR_SPAWN = BLOCKS.register("generator_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("generator_spawn"))
                    .mapColor(MapColor.COLOR_BLUE).strength(0.5F).noOcclusion().noCollision()));
    public static final RegistryObject<Block> GENERATOR = BLOCKS.register("generator",
            () -> new Block(BlockBehaviour.Properties.of().setId(BLOCKS.key("generator"))
                    .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).noOcclusion()));
    public static final RegistryObject<Block> SURVIVOR_SPAWN = BLOCKS.register("survivor_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("survivor_spawn"))
                    .mapColor(MapColor.COLOR_GREEN).strength(0.5F).noOcclusion().noCollision()));
    public static final RegistryObject<Block> KILLER_SPAWN = BLOCKS.register("killer_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("killer_spawn"))
                    .mapColor(MapColor.COLOR_RED).strength(0.5F).noOcclusion().noCollision()));
    public static final RegistryObject<Block> EXIT_SPAWN = BLOCKS.register("exit_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("exit_spawn"))
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.5F).noOcclusion().noCollision()));
    public static final RegistryObject<Block> ROUND_LEVER = BLOCKS.register("round_lever",
            () -> new ManikLeverBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("round_lever"))
                    .mapColor(MapColor.COLOR_GRAY).strength(3.0F)));
    public static final RegistryObject<Block> LOBBY_SPAWN = BLOCKS.register("lobby_spawn",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("lobby_spawn"))
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5F).noOcclusion().noCollision()));
    public static final RegistryObject<Block> EXIT_DOOR2 = BLOCKS.register("exit_door2",
            () -> new ManikExitDoor2Block(BlockBehaviour.Properties.of().setId(BLOCKS.key("exit_door2"))
                    .mapColor(MapColor.COLOR_GRAY).strength(4.0F).noOcclusion()));
    public static final RegistryObject<Block> FLARE_CRATE = BLOCKS.register("flare_crate",
            () -> new FlareCrateBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("flare_crate"))
                    .mapColor(MapColor.COLOR_RED).strength(1.0F)));
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
    public static final RegistryObject<Item> EXIT_SPAWN_ITEM = ITEMS.register("exit_spawn",
            () -> new BlockItem(EXIT_SPAWN.get(), new Item.Properties().setId(ITEMS.key("exit_spawn"))));
    public static final RegistryObject<Item> ROUND_LEVER_ITEM = ITEMS.register("round_lever",
            () -> new BlockItem(ROUND_LEVER.get(), new Item.Properties().setId(ITEMS.key("round_lever"))));
    public static final RegistryObject<Item> LOBBY_SPAWN_ITEM = ITEMS.register("lobby_spawn",
            () -> new BlockItem(LOBBY_SPAWN.get(), new Item.Properties().setId(ITEMS.key("lobby_spawn"))));
    public static final RegistryObject<Item> EXIT_DOOR2_ITEM = ITEMS.register("exit_door2",
            () -> new BlockItem(EXIT_DOOR2.get(), new Item.Properties().setId(ITEMS.key("exit_door2"))));
    public static final RegistryObject<Item> FLARE_CRATE_ITEM = ITEMS.register("flare_crate",
            () -> new FlareCrateItem(FLARE_CRATE.get(), new Item.Properties()
                    .setId(ITEMS.key("flare_crate")).stacksTo(1)));
    public static final RegistryObject<Item> EXIT_DOOR_ITEM = ITEMS.register("exit_door",
            () -> new BlockItem(EXIT_DOOR.get(), new Item.Properties().setId(ITEMS.key("exit_door"))));
    public static final RegistryObject<Item> TRAP_ITEM = ITEMS.register("trap",
            () -> new ManikTrapItem(TRAP.get(), new Item.Properties()
                    .setId(ITEMS.key("trap")).stacksTo(16)));

    private ManikBlocks() {}
}
