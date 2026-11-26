package com.grinandrij80blip.manik;

import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only registration of the maniac model and its renderer. */
public final class ManiacClient {
    public static void register() {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(ManiacClient::onLayers);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ManiacClient::onRenderers);
    }

    private static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ManiacBodyModel.LAYER, ManiacBodyModel::createBodyLayer);
        event.registerLayerDefinition(ClownBodyModel.LAYER, ClownBodyModel::createBodyLayer);
        event.registerLayerDefinition(BeekeeperBodyModel.LAYER, BeekeeperBodyModel::createBodyLayer);
        event.registerLayerDefinition(AngelBodyModel.LAYER, AngelBodyModel::createBodyLayer);
        event.registerLayerDefinition(HelicopterModel.LAYER, HelicopterModel::createBodyLayer);
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ManikEntities.MANIAC_BODY.get(), ManiacBodyRenderer::new);
        event.registerEntityRenderer(ManikEntities.CLOWN_BODY.get(), ClownBodyRenderer::new);
        event.registerEntityRenderer(ManikEntities.BEEKEEPER_BODY.get(), ctx -> new ExtraBodyRenderer(ctx,
                new BeekeeperBodyModel(ctx.bakeLayer(BeekeeperBodyModel.LAYER)), "beekeeper_body", 1.0F));
        event.registerEntityRenderer(ManikEntities.ANGEL_BODY.get(), ctx -> new ExtraBodyRenderer(ctx,
                new AngelBodyModel(ctx.bakeLayer(AngelBodyModel.LAYER)), "angel_body", 0.4F));
        event.registerEntityRenderer(ManikEntities.HELICOPTER.get(), HelicopterRenderer::new);
    }

    private ManiacClient() {}
}
