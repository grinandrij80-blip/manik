package com.grinandrij80blip.manik;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ManikItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ManikMod.MOD_ID);

    public static final RegistryObject<Item> MARK_GLOVE = ITEMS.register("mark_glove",
            () -> new Item(new Item.Properties().setId(ITEMS.key("mark_glove")).stacksTo(1)));

    public static final RegistryObject<Item> CANISTER = ITEMS.register("canister",
            () -> new CanisterItem(new Item.Properties()
                    .setId(ITEMS.key("canister"))
                    .durability(CanisterItem.CAPACITY)));

    public static final RegistryObject<Item> MEDKIT = ITEMS.register("medkit",
            () -> new MedkitItem(new Item.Properties().setId(ITEMS.key("medkit")).stacksTo(1)));

    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
            () -> new CrowbarItem(new Item.Properties()
                    .setId(ITEMS.key("crowbar"))
                    .durability(3)
                    .stacksTo(1)));

    public static final RegistryObject<Item> STUN_GUN = ITEMS.register("stun_gun",
            () -> new Item(new Item.Properties()
                    .setId(ITEMS.key("stun_gun"))
                    .durability(2)
                    .stacksTo(1)));

    public static final RegistryObject<Item> TRIPWIRE = ITEMS.register("tripwire",
            () -> new TripwireItem(new Item.Properties()
                    .setId(ITEMS.key("tripwire"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> MACADAMIA = ITEMS.register("macadamia",
            () -> new MacadamiaItem(new Item.Properties()
                    .setId(ITEMS.key("macadamia"))
                    .stacksTo(16)));

    public static final RegistryObject<Item> CLOWN_HAMMER = ITEMS.register("clown_hammer",
            () -> new Item(new Item.Properties().setId(ITEMS.key("clown_hammer")).stacksTo(1)));

    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
            () -> new Item(new Item.Properties()
                    .setId(ITEMS.key("wrench"))
                    .durability(100)
                    .stacksTo(1)));

    // Test-only maniac items (not in the creative tab, not in loot).
    public static final RegistryObject<Item> SMOKER = ITEMS.register("smoker",
            () -> new Item(new Item.Properties().setId(ITEMS.key("smoker")).stacksTo(1)));
    public static final RegistryObject<Item> FEATHER_BOW = ITEMS.register("feather_bow",
            () -> new ExtraItems.FeatherBow(new Item.Properties().setId(ITEMS.key("feather_bow")).stacksTo(1)));
    public static final RegistryObject<Item> DAGGER = ITEMS.register("dagger",
            () -> new Item(new Item.Properties().setId(ITEMS.key("dagger")).stacksTo(1)));
    public static final RegistryObject<Item> CANDLE_ITEM = ITEMS.register("candle_item",
            () -> new ExtraItems.CandleItem(new Item.Properties().setId(ITEMS.key("candle_item")).stacksTo(1)));
    public static final RegistryObject<Item> MINI_HIVE = ITEMS.register("mini_hive",
            () -> new ExtraItems.MiniHive(new Item.Properties().setId(ITEMS.key("mini_hive")).stacksTo(1)));
    public static final RegistryObject<Item> HONEY_BOTTLE = ITEMS.register("honey_bottle",
            () -> new ExtraItems.Honey(new Item.Properties().setId(ITEMS.key("honey_bottle")).stacksTo(16)));

    private ManikItems() {}
}
