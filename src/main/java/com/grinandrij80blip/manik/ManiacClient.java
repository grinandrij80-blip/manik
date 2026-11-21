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
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ManikEntities.MANIAC_BODY.get(), ManiacBodyRenderer::new);
    }

    private ManiacClient() {}
}
