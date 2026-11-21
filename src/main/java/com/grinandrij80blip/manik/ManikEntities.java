package com.grinandrij80blip.manik;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ManikEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ManikMod.MOD_ID);

    /**
     * The visible "body" of a maniac. The real player stays the gameplay entity
     * (hitbox, speed, attacks) and is made invisible; this entity follows them
     * and renders the custom MANIK model.
     */
    public static final RegistryObject<EntityType<ManiacBodyEntity>> MANIAC_BODY =
            ENTITY_TYPES.register("maniac_body", () -> EntityType.Builder
                    .<ManiacBodyEntity>of(ManiacBodyEntity::new, MobCategory.MISC)
                    .sized(1.2F, 2.9F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ENTITY_TYPES.key("maniac_body")));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MANIAC_BODY.get(), LivingEntity.createLivingAttributes().build());
    }

    private ManikEntities() {}
}
