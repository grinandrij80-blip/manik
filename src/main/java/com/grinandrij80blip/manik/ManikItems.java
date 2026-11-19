package com.grinandrij80blip.manik;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ManikItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ManikMod.MOD_ID);

    // A harmless marking tool. It is not a combat item.
    public static final RegistryObject<Item> MARK_GLOVE = ITEMS.register("mark_glove",
            () -> new Item(new Item.Properties()
                    .setId(ITEMS.key("mark_glove"))
                    .stacksTo(1)));

    private ManikItems() {
    }
}
