package com.doomslayer.client;

import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.kit.SlayerKit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class SlayerRenderer extends MobRenderer<SlayerEntity, SlayerModel> {
    private final SlayerKit kit;

    public SlayerRenderer(EntityRendererProvider.Context ctx, SlayerKit kit) {
        super(ctx, new SlayerModel(ctx.bakeLayer(KitClient.layerOf(kit)), kit), 0.5f * kit.scale);
        this.kit = kit;
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack ps, MultiBufferSource buf, int light, SlayerEntity e, float ls, float la, float pt, float age, float yaw, float pitch) {
                if (e.isInvisible()) return;
                KitClient.ensureTextures(kit);
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(KitClient.get(kit).glowTex));
                getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            }
        });
    }

    @Override protected void scale(SlayerEntity e, PoseStack ps, float pt) { ps.scale(kit.scale, kit.scale, kit.scale); }

    @Override
    public ResourceLocation getTextureLocation(SlayerEntity e) {
        KitClient.ensureTextures(kit);
        return KitClient.get(kit).tex;
    }
}
