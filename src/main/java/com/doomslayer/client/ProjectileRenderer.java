package com.doomslayer.client;

import com.doomslayer.DoomSlayerMod;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerProjectile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Camera-facing glowing orb tinted by ProjStyle#rgb (white radial gradient texture generated once). */
public class ProjectileRenderer extends EntityRenderer<SlayerProjectile> {
    private static ResourceLocation GLOW;
    public ProjectileRenderer(EntityRendererProvider.Context c) { super(c); }

    private static ResourceLocation glow() {
        if (GLOW == null) {
            NativeImage img = new NativeImage(32, 32, true);
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
                double d = Math.hypot(x - 15.5, y - 15.5) / 16.0; int a = (int) (255 * Math.max(0, Math.min(1, 1.25 - d * 1.25)) );
                a = (int) (a * a / 255.0 * (d < 0.35 ? 1.0 : 0.85));
                img.setPixelRGBA(x, y, a << 24 | 0xffffff);
            }
            GLOW = Minecraft.getInstance().getTextureManager().register("doomslayer_glow", new DynamicTexture(img));
        }
        return GLOW;
    }

    @Override
    public void render(SlayerProjectile p, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ProjStyle s = p.style();
        if (!s.visible) return;
        ps.pushPose();
        ps.mulPose(entityRenderDispatcher.cameraOrientation());
        float sz = s.size * (1 + 0.08f * (float) Math.sin((p.tickCount + pt) * 1.7f));
        ps.scale(sz, sz, sz);
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(glow()));
        Matrix4f m = ps.last().pose(); Matrix3f n = ps.last().normal();
        float r = ((s.rgb >> 16) & 255) / 255f, g = ((s.rgb >> 8) & 255) / 255f, b = (s.rgb & 255) / 255f;
        float[][] v = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] q : v) vc.vertex(m, q[0], q[1], 0).color(r, g, b, 1f).uv(q[2], q[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
        ps.popPose();
    }
    @Override public ResourceLocation getTextureLocation(SlayerProjectile p) { return glow(); }
}
