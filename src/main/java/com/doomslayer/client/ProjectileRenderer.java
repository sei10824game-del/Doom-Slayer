package com.doomslayer.client;

import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerProjectile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Hand-built projectile visuals (no vanilla particles): layered glow orbs with rotating star rays, stretched energy bolts,
 * darts, missiles with exhaust flame, spinning grenades and saw discs, plus a tapering ribbon trail. All textures are generated procedurally.
 */
public class ProjectileRenderer extends EntityRenderer<SlayerProjectile> {
    private static ResourceLocation GLOW, STAR, CAPSULE, DART, MISSILE, GRENADE, DISC;
    public ProjectileRenderer(EntityRendererProvider.Context c) { super(c); }

    // ---------- procedural textures ----------
    interface Px { int at(int x, int y); } // ARGB
    private static ResourceLocation tex(String name, int w, int h, Px f) {
        NativeImage img = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = f.at(x, y);
            img.setPixelRGBA(x, y, (c & 0xff00ff00) | ((c >> 16) & 0xff) | ((c & 0xff) << 16)); // ARGB -> ABGR
        }
        return Minecraft.getInstance().getTextureManager().register("doomslayer_" + name, new DynamicTexture(img));
    }
    private static int argb(int a, int rgb) { return Mth.clamp(a, 0, 255) << 24 | (rgb & 0xffffff); }
    private static int gray(int a, int v) { v = Mth.clamp(v, 0, 255); return argb(a, v << 16 | v << 8 | v); }
    private static float sstep(float a, float b, float x) { float t = Mth.clamp((x - a) / (b - a), 0, 1); return t * t * (3 - 2 * t); }

    private static void init() {
        if (GLOW != null) return;
        GLOW = tex("glow", 32, 32, (x, y) -> {
            double d = Math.hypot(x - 15.5, y - 15.5) / 16.0; double a = Math.max(0, 1 - d); a = a * a * (d < 0.35 ? 1.0 : 0.9);
            return argb((int) (255 * a), 0xffffff);
        });
        STAR = tex("star", 32, 32, (x, y) -> {
            float dx = (x - 15.5f) / 16f, dy = (y - 15.5f) / 16f, ax = Math.abs(dx), ay = Math.abs(dy);
            float h = (float) Math.exp(-ay * 22) * (1 - ax), v = (float) Math.exp(-ax * 22) * (1 - ay);
            float dg = (float) Math.max(Math.exp(-Math.abs(dx - dy) * 16), Math.exp(-Math.abs(dx + dy) * 16)) * (1 - Math.max(ax, ay)) * 0.35f;
            float core = Math.max(0, 1 - (float) Math.hypot(dx, dy) * 3f);
            return argb((int) (255 * Mth.clamp(Math.max(Math.max(h, v), Math.max(dg, core)), 0, 1)), 0xffffff);
        });
        CAPSULE = tex("capsule", 64, 16, (x, y) -> {
            float dx = (x + .5f - 32) / 32f, dy = (y + .5f - 8) / 8f;
            float a = (float) Math.exp(-dy * dy * 3.2f) * (1 - sstep(0.55f, 1f, Math.abs(dx)));
            return argb((int) (255 * a), 0xffffff);
        });
        DART = tex("dart", 64, 16, (x, y) -> { // points toward +x
            int cy = 8, dy = Math.abs(y - cy);
            if (x >= 10 && x < 54 && dy <= 1) return dy == 0 ? gray(255, 235) : gray(255, 140); // shaft
            if (x >= 54 && x < 64 && dy <= (63 - x) * 3 / 10 + 0) return gray(255, 250 - dy * 20); // head
            if (x >= 2 && x < 14 && dy >= 1 && dy <= 5 && Math.abs(dy - (14 - x) * 5 / 12) <= 1) return gray(255, 190); // fletching
            return 0;
        });
        MISSILE = tex("missile", 64, 16, (x, y) -> {
            int dy = Math.abs(y - 8);
            if (x >= 50 && x < 64) return dy <= (63 - x) * 5 / 13 ? argb(255, 0xd03030) : 0; // nose cone
            if (x >= 12 && x < 50 && dy <= 3) return (x >= 40 && x < 44) ? argb(255, 0xffffff) : gray(255, 200 - dy * 25 + (x % 7 == 0 ? -30 : 0));
            if (x >= 8 && x < 12 && dy <= 2) return gray(255, 70); // nozzle
            if (x >= 12 && x < 24 && dy >= 4 && dy <= 7 && dy - 4 <= (24 - x) / 3) return argb(255, 0x909090); // fins
            return 0;
        });
        GRENADE = tex("grenade", 32, 32, (x, y) -> {
            float dx = x - 15.5f, dy = y - 17f, r = (float) Math.hypot(dx, dy);
            if (r <= 10.5f) { int v = (int) (230 - 90 * (r / 10.5f) - 25 * (dx + dy) / 10.5f); if (x % 7 == 0 || y % 7 == 3) v -= 55; return gray(255, v); }
            if (x >= 13 && x <= 18 && y >= 3 && y <= 7) return gray(255, 120); // fuse top
            if (x >= 18 && x <= 25 && y >= 4 && y <= 6) return gray(255, 170); // lever
            return 0;
        });
        DISC = tex("disc", 32, 32, (x, y) -> {
            float dx = x - 15.5f, dy = y - 15.5f, r = (float) Math.hypot(dx, dy), ang = (float) Math.atan2(dy, dx);
            float R = 10f + 5f * (float) Math.pow(Math.max(0, Math.cos(ang * 8)), 3);
            if (r < 3f || r > R) return 0;
            int v = 235 - (int) (r * 3.5f) + ((int) r % 4 == 0 ? -40 : 0);
            return gray(255, v);
        });
    }

    // ---------- drawing helpers ----------
    private static float[] rgb(int c) { return new float[] {((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f}; }
    private static int mix(int a, int b, float t) {
        float[] x = rgb(a), y = rgb(b);
        return ((int) (255 * Mth.lerp(t, x[0], y[0])) << 16) | ((int) (255 * Mth.lerp(t, x[1], y[1])) << 8) | (int) (255 * Mth.lerp(t, x[2], y[2]));
    }
    private static void quad(VertexConsumer vc, PoseStack ps, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, float a, int light, float u0, float v0, float u1, float v1) {
        Matrix4f m = ps.last().pose(); Matrix3f n = ps.last().normal(); float[] c = rgb(rgb);
        float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}}; Vec3[] p = {p0, p1, p2, p3};
        for (int i = 0; i < 4; i++)
            vc.vertex(m, (float) p[i].x, (float) p[i].y, (float) p[i].z).color(c[0], c[1], c[2], a).uv(uv[i][0], uv[i][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
    }
    /** quad centred at c spanned by half-vectors u (right) and v (up), flipped so it faces the camera */
    private static void sprite(VertexConsumer vc, PoseStack ps, Vec3 c, Vec3 u, Vec3 v, Vec3 toCam, int rgb, float a, int light) {
        if (u.cross(v).dot(toCam) < 0) v = v.scale(-1);
        quad(vc, ps, c.subtract(u).subtract(v), c.add(u).subtract(v), c.add(u).add(v), c.subtract(u).add(v), rgb, a, light, 0, 0, 1, 1);
    }
    /** sprite whose long axis follows dir (world space) and still faces the camera */
    private static void oriented(VertexConsumer vc, PoseStack ps, Vec3 c, Vec3 dir, Vec3 toCam, double halfLen, double halfWid, double shift, int rgb, float a, int light) {
        Vec3 d = dir.normalize(); Vec3 v = toCam.cross(d); if (v.lengthSqr() < 1e-6) v = new Vec3(0, 1, 0).cross(d); v = v.normalize();
        sprite(vc, ps, c.add(d.scale(shift)), d.scale(halfLen), v.scale(halfWid), toCam, rgb, a, light);
    }

    @Override
    public void render(SlayerProjectile p, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ProjStyle s = p.style();
        if (!s.visible) return;
        init();
        Vec3 ePos = new Vec3(Mth.lerp(pt, p.xo, p.getX()), Mth.lerp(pt, p.yo, p.getY()), Mth.lerp(pt, p.zo, p.getZ()));
        Vec3 cam = entityRenderDispatcher.camera.getPosition(), toCam = cam.subtract(ePos).normalize();
        Vec3 vel = p.getDeltaMovement(); if (vel.lengthSqr() < 1e-6) vel = p.getViewVector(1f);
        float t = p.tickCount + pt, sz = s.size * (1 + 0.07f * (float) Math.sin(t * 1.7f));
        int col = s.rgb, hot = mix(col, 0xffffff, 0.7f), full = LightTexture.FULL_BRIGHT;
        ps.pushPose();
        ps.translate(0, 0, 0);
        VertexConsumer add = buf.getBuffer(RenderType.eyes(GLOW));
        trail(p, s, ps, buf, ePos, toCam, sz, col, hot);
        switch (s.resolved()) {
            case BOLT -> {
                VertexConsumer cap = buf.getBuffer(RenderType.eyes(CAPSULE));
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 4.5, sz * 1.6, 0, col, 0.85f, full);
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 3.2, sz * 0.7, 0, hot, 1f, full);
                billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), sz * 1.3f, col, 0.7f, 0, full);
            }
            case DART -> {
                oriented(buf.getBuffer(RenderType.eyes(CAPSULE)), ps, Vec3.ZERO, vel, toCam, sz * 4.5, sz * 1.5, -sz, col, 0.6f, full);
                oriented(buf.getBuffer(RenderType.entityCutoutNoCull(DART)), ps, Vec3.ZERO, vel, toCam, sz * 4.2, sz * 1.05, 0, mix(0xffffff, col, 0.35f), 1f, light);
            }
            case MISSILE -> {
                Vec3 d = vel.normalize();
                float flick = 0.85f + 0.3f * (float) Math.sin(t * 2.9f);
                VertexConsumer cap = buf.getBuffer(RenderType.eyes(CAPSULE));
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 3.2 * flick, sz * 1.1, -sz * 3.4, 0xff8a20, 0.9f, full);
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 1.8 * flick, sz * 0.6, -sz * 2.5, 0xfff0a0, 1f, full);
                oriented(buf.getBuffer(RenderType.entityCutoutNoCull(MISSILE)), ps, Vec3.ZERO, vel, toCam, sz * 2.6, sz * 0.66, 0, mix(0xffffff, col, 0.25f), 1f, light);
                billboard(ps, add, sz * 1.4f, 0xff9a30, 0.5f, 0, full);
            }
            case GRENADE -> {
                spin(ps, buf.getBuffer(RenderType.entityCutoutNoCull(GRENADE)), sz * 1.5f, mix(0xffffff, col, 0.6f), t * 0.45f, light);
                if (((int) t / 4) % 2 == 0) billboard(ps, add, sz * 0.9f, 0xff2010, 0.8f, 0, full); // blinking fuse light
            }
            case DISC -> {
                spin(ps, buf.getBuffer(RenderType.entityCutoutNoCull(DISC)), sz * 1.5f, mix(0xffffff, col, 0.3f), t * 1.6f, light);
                billboard(ps, add, sz * 1.9f, col, 0.35f, 0, full);
            }
            default -> { // ORB: halo, body, rotating star rays, white-hot core
                billboard(ps, add, sz * 1.9f, col, 0.45f, 0, full);
                billboard(ps, add, sz * 1.15f, col, 0.95f, 0, full);
                spin(ps, buf.getBuffer(RenderType.eyes(STAR)), sz * 2.1f, mix(col, 0xffffff, 0.45f), t * 0.18f, full);
                spin(ps, buf.getBuffer(RenderType.eyes(STAR)), sz * 1.5f, hot, -t * 0.31f + 0.8f, full);
                billboard(ps, add, sz * 0.5f, 0xffffff, 1f, 0, full);
            }
        }
        ps.popPose();
    }

    private void billboard(PoseStack ps, VertexConsumer vc, float r, int rgb, float a, float rot, int light) { spinRaw(ps, vc, r, rgb, rot, a, light); }
    private void spin(PoseStack ps, VertexConsumer vc, float r, int rgb, float rot, int light) { spinRaw(ps, vc, r, rgb, rot, 1f, light); }
    private void spinRaw(PoseStack ps, VertexConsumer vc, float r, int rgb, float rot, float a, int light) {
        ps.pushPose();
        ps.mulPose(entityRenderDispatcher.cameraOrientation()); ps.mulPose(Axis.ZP.rotation(rot));
        quad(vc, ps, new Vec3(-r, -r, 0), new Vec3(r, -r, 0), new Vec3(r, r, 0), new Vec3(-r, r, 0), rgb, a, light, 0, 0, 1, 1);
        ps.popPose();
    }

    /** tapering ribbon from the recorded positions: glow ribbon + white-hot core (+ grey smoke for smoke-trail styles) */
    private void trail(SlayerProjectile p, ProjStyle s, PoseStack ps, MultiBufferSource buf, Vec3 ePos, Vec3 toCam, float sz, int col, int hot) {
        List<Vec3> h = p.hist; int n = h.size();
        if (n < 2) return;
        boolean smoke = s.trail == ParticleTypes.SMOKE || s.trail == ParticleTypes.LARGE_SMOKE;
        boolean fire = s.trail == ParticleTypes.FLAME || s.trail == ParticleTypes.SMALL_FLAME;
        int c1 = fire ? mix(col, 0xff8020, 0.5f) : col;
        VertexConsumer glow = buf.getBuffer(RenderType.eyes(CAPSULE)), sm = smoke ? buf.getBuffer(RenderType.entityTranslucent(CAPSULE)) : null;
        float w0 = Math.max(0.03f, sz * (smoke ? 1.3f : 0.9f));
        for (int i = 0; i < n - 1; i++) {
            Vec3 a = h.get(i).subtract(ePos), b = (i == n - 2 ? ePos : h.get(i + 1)).subtract(ePos);
            Vec3 d = b.subtract(a); if (d.lengthSqr() < 1e-8) continue;
            float ta = (i + 1f) / n, tb = (i + 2f) / n;
            ribbon(glow, ps, a, b, toCam, w0 * ta, w0 * tb, c1, 0.75f * ta, LightTexture.FULL_BRIGHT);
            ribbon(glow, ps, a, b, toCam, w0 * 0.4f * ta, w0 * 0.4f * tb, hot, 0.9f * ta, LightTexture.FULL_BRIGHT);
            if (sm != null) ribbon(sm, ps, a, b, toCam, w0 * 1.6f * ta, w0 * 1.6f * tb, 0x777777, 0.45f * ta, LightTexture.FULL_BRIGHT);
        }
    }
    private static void ribbon(VertexConsumer vc, PoseStack ps, Vec3 a, Vec3 b, Vec3 toCam, float wa, float wb, int rgb, float al, int light) {
        Vec3 d = b.subtract(a).normalize(), v = toCam.cross(d); if (v.lengthSqr() < 1e-6) return; v = v.normalize();
        quad(vc, ps, a.subtract(v.scale(wa)), b.subtract(v.scale(wb)), b.add(v.scale(wb)), a.add(v.scale(wa)), rgb, al, light, 0.5f, 0, 0.5f, 1);
    }

    @Override public ResourceLocation getTextureLocation(SlayerProjectile p) { init(); return GLOW; }
}
