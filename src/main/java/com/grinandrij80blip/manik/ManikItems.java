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

    private ManikItems() {}
}
