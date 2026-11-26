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

    public static final RegistryObject<EntityType<ClownBodyEntity>> CLOWN_BODY =
            ENTITY_TYPES.register("clown_body", () -> EntityType.Builder
                    .<ClownBodyEntity>of(ClownBodyEntity::new, MobCategory.MISC)
                    .sized(1.4F, 3.7F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ENTITY_TYPES.key("clown_body")));

    public static final RegistryObject<EntityType<BeekeeperBodyEntity>> BEEKEEPER_BODY =
            ENTITY_TYPES.register("beekeeper_body", () -> EntityType.Builder
                    .<BeekeeperBodyEntity>of(BeekeeperBodyEntity::new, MobCategory.MISC)
                    .sized(1.4F, 2.6F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ENTITY_TYPES.key("beekeeper_body")));

    public static final RegistryObject<EntityType<AngelBodyEntity>> ANGEL_BODY =
            ENTITY_TYPES.register("angel_body", () -> EntityType.Builder
                    .<AngelBodyEntity>of(AngelBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.4F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ENTITY_TYPES.key("angel_body")));

    public static final RegistryObject<EntityType<HelicopterEntity>> HELICOPTER =
            ENTITY_TYPES.register("helicopter", () -> EntityType.Builder
                    .<HelicopterEntity>of(HelicopterEntity::new, MobCategory.MISC)
                    .sized(6.0F, 3.0F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(ENTITY_TYPES.key("helicopter")));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MANIAC_BODY.get(), LivingEntity.createLivingAttributes().build());
        event.put(CLOWN_BODY.get(), LivingEntity.createLivingAttributes().build());
        event.put(BEEKEEPER_BODY.get(), LivingEntity.createLivingAttributes().build());
        event.put(ANGEL_BODY.get(), LivingEntity.createLivingAttributes().build());
        event.put(HELICOPTER.get(), LivingEntity.createLivingAttributes().build());
    }

    private ManikEntities() {}
}
