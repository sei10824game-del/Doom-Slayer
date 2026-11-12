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
                ghosts(ps, buf, light, e, pt);
            }
        });
    }

    /** afterimages: when the slayer moves fast (dash, dodge, lunge, thruster) draw fading ghosts trailing behind */
    private void ghosts(PoseStack ps, MultiBufferSource buf, int light, SlayerEntity e, float pt) {
        double dx = e.getX() - e.xo, dy = e.getY() - e.yo, dz = e.getZ() - e.zo; double sp = Math.sqrt(dx * dx + dz * dz);
        if (sp < 0.3 || e.isInvisible()) return;
        float yaw = net.minecraft.util.Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot); double a = Math.toRadians(-(180 - yaw));
        double rx = dx * Math.cos(a) + dz * Math.sin(a), rz = -dx * Math.sin(a) + dz * Math.cos(a); // world delta -> model space (after the renderer's yaw + (-1,-1,1) flip)
        KitClient.ensureTextures(kit);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(KitClient.get(kit).tex));
        int c = kit.eggSecondary; float r = 0.5f + 0.5f * ((c >> 16) & 255) / 255f, g = 0.5f + 0.5f * ((c >> 8) & 255) / 255f, b = 0.5f + 0.5f * (c & 255) / 255f;
        for (int k = 1; k <= 3; k++) {
            float back = k * 1.6f / kit.scale;
            ps.pushPose();
            ps.translate(rx * back, dy * back, -rz * back);
            getModel().renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, 0.42f - 0.11f * k);
            ps.popPose();
        }
    }

    @Override protected float getFlipDegrees(SlayerEntity e) { return 0; } // the model does its own collapse animation

    @Override protected void scale(SlayerEntity e, PoseStack ps, float pt) { ps.scale(kit.scale, kit.scale, kit.scale); }

    @Override
    public ResourceLocation getTextureLocation(SlayerEntity e) {
        KitClient.ensureTextures(kit);
        return KitClient.get(kit).tex;
    }
}
