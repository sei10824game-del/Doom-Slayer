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
    private static ResourceLocation GLOW, STAR, CAPSULE, DART, MISSILE, GRENADE, DISC, RING, CRYSTAL, HEART_T;
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
        CRYSTAL = tex("crystal", 64, 16, (x, y) -> { // double-pointed shard
            float dx = (x + .5f - 32) / 32f, dy = Math.abs(y + .5f - 8) / 8f; float edge = 1 - Math.abs(dx) * 0.95f;
            if (dy > edge) return 0; int v = 255 - (int) (dy / Math.max(edge, 0.05f) * 120); return gray(255, v);
        });
        HEART_T = tex("heart", 32, 32, (x, y) -> {
            float dx = (x - 15.5f) / 11f, dy = -(y - 17f) / 11f; float v = (dx * dx + dy * dy - 1); boolean in = v * v * v - dx * dx * dy * dy * dy <= 0;
            return in ? gray(255, 255 - (int) ((y - 6) * 4)) : 0;
        });
        RING = tex("ring", 64, 64, (x, y) -> {
            float d = (float) Math.hypot(x - 31.5f, y - 31.5f) / 32f; float a = (float) Math.exp(-Math.pow((d - 0.82f) * 11f, 2)) * (1 - sstep(0.9f, 1f, d));
            return argb((int) (255 * a), 0xffffff);
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
        int col = p.tint() != 0 ? p.tint() : s.rgb;
        float baseSize = p.sizeOv() > 0 ? p.sizeOv() : s.size;
        float t = p.tickCount + pt, sz = baseSize * (1 + 0.07f * (float) Math.sin(t * 1.7f));
        int hot = mix(col, 0xffffff, 0.7f), full = LightTexture.FULL_BRIGHT;
        ps.pushPose();
        if (s.visual && effect(p, s, ps, buf, ePos, toCam, col, baseSize, pt)) { ps.popPose(); return; }
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
            case PLASMA -> { // fat glowing bolt: halo, teardrop body, white-hot core, crackling orbiting sparks
                VertexConsumer cap = buf.getBuffer(RenderType.eyes(CAPSULE));
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 4.0, sz * 2.2, -sz * 0.6, col, 0.55f, full);
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 3.0, sz * 1.3, 0, mix(col, 0xffffff, 0.35f), 0.95f, full);
                oriented(cap, ps, Vec3.ZERO, vel, toCam, sz * 1.8, sz * 0.55, sz * 0.5, 0xffffff, 1f, full);
                billboard(ps, add, sz * 2.4f, col, 0.45f, 0, full);
                Vec3 d = vel.normalize(), a1 = d.cross(new Vec3(0, 1, 0)); if (a1.lengthSqr() < 1e-4) a1 = new Vec3(1, 0, 0); a1 = a1.normalize(); Vec3 a2 = d.cross(a1);
                for (int i = 0; i < 3; i++) { double ang = t * 0.9 + i * 2.094, rad = sz * 1.3; Vec3 o = a1.scale(Math.cos(ang) * rad).add(a2.scale(Math.sin(ang) * rad)).add(d.scale(-sz * (i - 1) * 1.1)); ps.pushPose(); ps.translate(o.x, o.y, o.z); billboard(ps, add, sz * 0.5f, hot, 0.9f, 0, full); ps.popPose(); }
            }
            case NEEDLE -> { // glowing crystal shard
                oriented(buf.getBuffer(RenderType.eyes(CAPSULE)), ps, Vec3.ZERO, vel, toCam, sz * 4.0, sz * 1.6, -sz * 0.5, col, 0.5f, full);
                oriented(buf.getBuffer(RenderType.eyes(CRYSTAL)), ps, Vec3.ZERO, vel, toCam, sz * 3.4, sz * 1.1, 0, mix(col, 0xffffff, 0.5f), 1f, full);
                billboard(ps, add, sz * 1.5f, col, 0.5f, 0, full);
            }
            case BFG -> { // huge green orb with orbiting lights and lightning tendrils
                billboard(ps, add, sz * 2.4f, col, 0.3f, 0, full);
                billboard(ps, add, sz * 1.5f, col, 0.8f, 0, full);
                billboard(ps, add, sz * 0.8f, 0xffffff, 1f, 0, full);
                spin(ps, buf.getBuffer(RenderType.eyes(STAR)), sz * 3.2f, mix(col, 0xffffff, 0.4f), t * 0.12f, full);
                spin(ps, buf.getBuffer(RenderType.eyes(STAR)), sz * 2.4f, hot, -t * 0.2f, full);
                java.util.Random rnd = new java.util.Random(p.getId() * 31L + (long) (t / 1.5f));
                VertexConsumer g = buf.getBuffer(RenderType.eyes(CAPSULE));
                for (int i = 0; i < 5; i++) {
                    Vec3 dir = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize(); Vec3 prev = dir.scale(sz * 0.4);
                    for (int j = 1; j <= 3; j++) { Vec3 q = dir.scale(sz * (0.4 + 0.5 * j)).add(new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).scale(sz * 0.25)); ribbon(g, ps, prev, q, toCam, sz * 0.09f, sz * 0.07f, hot, 0.9f, full); prev = q; }
                }
            }
            case FLAME -> { // flickering teardrop of fire dragging an ember tail
                java.util.Random rnd = new java.util.Random(p.getId() * 13L + (long) (t * 2));
                VertexConsumer cap = buf.getBuffer(RenderType.eyes(CAPSULE));
                for (int i = 0; i < 3; i++) {
                    double wob = 0.25 * sz * rnd.nextGaussian();
                    oriented(cap, ps, new Vec3(wob, wob * 0.5, -wob), vel, toCam, sz * (2.6 - i * 0.5), sz * (1.5 - i * 0.35), -sz * 1.0, mix(col, 0xffe060, i / 2.5f), 0.7f, full);
                }
                billboard(ps, add, sz * 1.6f, 0xffb040, 0.6f, 0, full); billboard(ps, add, sz * 0.8f, 0xfff0a0, 1f, 0, full);
            }
            case HEART -> {
                float pulse = 1 + 0.15f * (float) Math.sin(t * 0.7f);
                billboard(ps, add, sz * 1.9f, col, 0.4f, 0, full);
                spin(ps, buf.getBuffer(RenderType.entityCutoutNoCull(HEART_T)), sz * 1.2f * pulse, mix(0xffffff, col, 0.7f), 0, full);
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

    /** purely visual carriers: beam segment, flash, fireball blast, ground shock ring. returns true when handled */
    private boolean effect(SlayerProjectile p, ProjStyle s, PoseStack ps, MultiBufferSource buf, Vec3 ePos, Vec3 toCam, int col, float size, float pt) {
        ProjStyle.Shape sh = s.resolved();
        int full = LightTexture.FULL_BRIGHT, life = Math.max(1, p.life());
        float t = Mth.clamp((p.tickCount + pt) / life, 0, 1), fade = 1 - t * t;
        int hot = mix(col, 0xffffff, 0.75f);
        switch (sh) {
            case BEAM -> {
                Vec3 end = p.end().subtract(ePos);
                VertexConsumer g = buf.getBuffer(RenderType.eyes(CAPSULE));
                ribbon(g, ps, Vec3.ZERO, end, toCam, size * 1.0f, size * 1.0f, col, 0.8f * fade, full);
                ribbon(g, ps, Vec3.ZERO, end, toCam, size * 0.42f, size * 0.42f, hot, fade, full);
                billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), size * 2.2f, col, 0.6f * fade, 0, full);
                ps.pushPose(); ps.translate(end.x, end.y, end.z); billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), size * 2.8f, hot, 0.8f * fade, 0, full); ps.popPose();
                return true;
            }
            case FLASH -> {
                float k = size * (1.3f - 0.6f * t);
                spin(ps, buf.getBuffer(RenderType.eyes(STAR)), k * 2.2f, mix(col, 0xffffff, 0.5f), p.getId() * 1.7f + t * 2f, full);
                billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), k * 1.3f, col, 0.9f * fade, 0, full);
                billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), k * 0.55f, 0xffffff, fade, 0, full);
                return true;
            }
            case BLAST -> { // cauliflower fireball: hot core, orange puffs bursting outward, grey smoke lingering
                float grow = com.doomslayer.anim.Keys.outCubic(t), r = size * (0.45f + 0.9f * grow);
                java.util.Random rnd = new java.util.Random(p.getId() * 7919L);
                VertexConsumer add = buf.getBuffer(RenderType.eyes(GLOW)), smoke = buf.getBuffer(RenderType.entityTranslucent(GLOW));
                for (int i = 0; i < 7; i++) {
                    Vec3 o = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.6 + 0.2, rnd.nextGaussian()).scale(r * 0.55 * grow);
                    ps.pushPose(); ps.translate(o.x, o.y, o.z);
                    billboard(ps, add, r * (0.55f + 0.3f * rnd.nextFloat()), mix(col, 0xffd060, rnd.nextFloat() * 0.6f), 0.85f * fade, 0, full);
                    if (t > 0.35f) billboard(ps, smoke, r * (0.7f + 0.4f * rnd.nextFloat()), 0x444444, 0.5f * (1 - t) * (t - 0.35f) * 3f, 0, full);
                    ps.popPose();
                }
                billboard(ps, add, r * 0.8f * (1 - t * t), 0xffffff, 1f - t, 0, full);
                spin(ps, buf.getBuffer(RenderType.eyes(STAR)), r * 2.4f * (0.5f + t), mix(col, 0xffffff, 0.4f), t * 1.5f, full);
                return true;
            }
            case PUFF -> { // N pseudo-random sprites per entity: glow puffs / rising smoke / spark streaks, deterministic per entity id
                Vec3 sv = p.end(); int cnt = (int) sv.y; float spread = p.radius(), spd = (float) sv.x, tt = p.tickCount + pt;
                java.util.Random rnd = new java.util.Random(p.getId() * 131L + 7);
                boolean smoke = s == ProjStyle.VIS_PUFF_SMOKE, spark = s == ProjStyle.VIS_PUFF_SPARK;
                VertexConsumer add = buf.getBuffer(RenderType.eyes(GLOW)), sm = buf.getBuffer(RenderType.entityTranslucent(GLOW)), cap = buf.getBuffer(RenderType.eyes(CAPSULE));
                for (int i = 0; i < cnt; i++) {
                    Vec3 dir = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * (smoke ? 0.5 : 1), rnd.nextGaussian()).normalize(); float rr = rnd.nextFloat(), k = 0.6f + rnd.nextFloat() * 0.8f;
                    double drift = Math.max(spd, 0.015) * tt * (spark ? 7 : 4) * k;
                    Vec3 pos = dir.scale(spread * rr).add(dir.scale(drift)); if (smoke) pos = pos.add(0, 0.025 * tt, 0); if (spark) pos = pos.add(0, -0.004 * tt * tt, 0);
                    ps.pushPose(); ps.translate(pos.x, pos.y, pos.z);
                    if (spark) { Vec3 d = dir.add(0, -0.016 * tt, 0); oriented(cap, ps, Vec3.ZERO, d, toCam, size * 2.8 * (1 - t * 0.6), size * 0.9, 0, col, fade, full); }
                    else if (smoke) billboard(ps, sm, size * (0.6f + 1.2f * t) * k, col, 0.5f * (1 - t) * Math.min(1, t * 6), 0, full);
                    else { billboard(ps, add, size * (1f - 0.7f * t) * k, col, fade, 0, full); billboard(ps, add, size * 0.4f * (1f - t) * k, mix(col, 0xffffff, 0.7f), fade, 0, full); }
                    ps.popPose();
                }
                return true;
            }
            case SLASH -> { // crescent sweep: the bright head races along the arc, the tail thins out and fades
                Vec3 se = p.end(); double yaw = Math.toRadians(se.x), tilt = Math.toRadians(p.getDeltaMovement().x); float rad = p.radius();
                float prog = Mth.clamp((p.tickCount + pt) / (life * 0.55f), 0, 1), from = (float) se.y, to = (float) se.z, head =com.doomslayer.anim.Keys.lerp(from, to, com.doomslayer.anim.Keys.outCubic(prog));
                int N = 16; Vec3 prev = null; VertexConsumer g = buf.getBuffer(RenderType.eyes(CAPSULE));
                for (int i = 0; i <= N; i++) {
                    float ang =com.doomslayer.anim.Keys.lerp(from, head, i / (float) N); double a = Math.toRadians(ang), x = -Math.sin(a) * rad, fwd = Math.cos(a) * rad, y = x * Math.sin(tilt); x *= Math.cos(tilt);
                    Vec3 q = new Vec3(x * Math.cos(yaw) - fwd * Math.sin(yaw), y, x * Math.sin(yaw) + fwd * Math.cos(yaw));
                    if (prev != null) {
                        float w0 = size * 0.9f * ((i - 1f) / N), w1 = size * 0.9f * (i / (float) N), al = (i / (float) N) * (1 - t) * (1 - t);
                        ribbon(g, ps, prev, q, toCam, w0, w1, col, 0.85f * al + 0.05f, full); ribbon(g, ps, prev, q, toCam, w0 * 0.4f, w1 * 0.4f, hot, al, full);
                    }
                    prev = q;
                }
                return true;
            }
            case RING -> {
                float rr = size * (0.25f + 0.75f * com.doomslayer.anim.Keys.outCubic(t));
                Vec3 u = new Vec3(rr, 0, 0), v = new Vec3(0, 0, rr);
                sprite(buf.getBuffer(RenderType.eyes(RING)), ps, new Vec3(0, 0.12, 0), u, v, toCam, col, 0.9f * fade, full);
                sprite(buf.getBuffer(RenderType.eyes(RING)), ps, new Vec3(0, 0.12, 0), u.scale(0.92), v.scale(0.92), toCam, hot, 0.5f * fade, full);
                return true;
            }
            default -> { return false; }
        }
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
