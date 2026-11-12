package com.doomslayer.client;

import com.doomslayer.anim.Keys;
import com.doomslayer.anim.Pose;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public class SlayerModel extends EntityModel<SlayerEntity> {
    public final ModelPart root, all, torso, head, rArm, lArm, rLeg, lLeg, rHand, lHand;
    private final SlayerKit kit;
    private final KitClient.Baked baked;
    private float partial;

    public SlayerModel(ModelPart root, SlayerKit kit) {
        this.root = root; this.kit = kit; this.baked = KitClient.get(kit);
        all = root.getChild("all"); torso = all.getChild("torso"); head = torso.getChild("head");
        rArm = torso.getChild("rArm"); lArm = torso.getChild("lArm");
        rLeg = all.getChild("rLeg"); lLeg = all.getChild("lLeg");
        rHand = rArm.getChild("rHand"); lHand = lArm.getChild("lHand");
    }

    @Override public void prepareMobModel(SlayerEntity e, float limbSwing, float limbAmt, float partial) { this.partial = partial; }

    /** the pose actually applied last frame (read by kits' particle code on client if needed) */
    public final Pose pose = new Pose();

    @Override
    public void setupAnim(SlayerEntity e, float limbSwing, float limbAmt, float age, float yawHead, float pitch) {
        Pose p = new Pose();
        // ---- locomotion: stride, counter-swinging arms, torso twist, hip sway, forward lean that grows into a run ----
        float sw = limbSwing * 0.6662f, a = Math.min(1f, limbAmt * 1.3f), run = Keys.clamp01((limbAmt - 0.55f) / 0.45f), still = 1 - a;
        float c = (float) Math.cos(sw), sn = (float) Math.sin(sw), stride = 1.1f + 0.5f * run;
        p.rLegX = c * stride * a; p.lLegX = -c * stride * a;
        p.rLegZ = 0.03f * a; p.lLegZ = -0.03f * a;
        p.rArmX = -c * (0.75f + 0.55f * run) * a; p.lArmX = c * (0.75f + 0.55f * run) * a;
        p.rHandX = -(0.25f + 0.5f * run) * a; p.lHandX = -(0.25f + 0.5f * run) * a; // bent elbows while moving
        p.bodyY = -c * 0.13f * a * (1 + run); p.bodyZ = -sn * 0.03f * a;
        p.bodyX = (0.08f + 0.24f * run) * a;
        p.rootY = Math.abs(sn) * (0.45f + 0.4f * run) * a; p.rootRoll = sn * 0.035f * a;
        // ---- idle: breathing, slow weight shift from foot to foot, the head drifts and occasionally glances aside ----
        float ph = e.getId() * 1.7f, breath = (float) Math.sin(age * 0.09f + ph), sway = (float) Math.sin(age * 0.025f + ph);
        float glance = Keys.bump(Keys.win((age + ph * 40) % 220f, 150f, 200f)) * (((int) ((age + ph * 40) / 220f) & 1) == 0 ? 1 : -1);
        p.bodyX += breath * 0.02f + 0.02f * still; p.rootY += breath * 0.07f * still;
        p.rArmZ = 0.06f + breath * 0.035f; p.lArmZ = -0.06f - breath * 0.035f;
        p.rootRoll += sway * 0.03f * still; p.bodyZ -= sway * 0.025f * still; p.rLegZ += (0.05f + sway * 0.03f) * still; p.lLegZ -= (0.05f - sway * 0.03f) * still;
        // ---- airborne: tuck on the way up, spread and brace on the way down ----
        float vy = (float) (e.getY() - e.yo), air = e.onGround() ? 0 : Keys.clamp01(Math.abs(vy) * 5f);
        if (air > 0 && e.currentAttack() == null) {
            boolean up = vy > 0;
            p.lLegX = Keys.lerp(p.lLegX, up ? -0.7f : -0.25f, air); p.rLegX = Keys.lerp(p.rLegX, up ? 0.45f : 0.3f, air);
            p.rArmX = Keys.lerp(p.rArmX, up ? -0.5f : -0.9f, air); p.lArmX = Keys.lerp(p.lArmX, up ? -0.4f : -0.9f, air);
            p.rArmZ += (up ? 0.35f : 0.8f) * air; p.lArmZ -= (up ? 0.35f : 0.8f) * air; p.bodyX += (up ? 0.12f : -0.1f) * air;
        }
        p.headY = yawHead * Keys.DEG + (c * 0.1f * a * (1 + run)) + glance * 0.3f * still; p.headX = pitch * Keys.DEG - 0.1f * run * a + breath * 0.012f;
        p.weaponR = kit.idleWeaponR; p.weaponL = kit.idleWeaponL;
        kit.idle(p, e, age, a);
        p.rArmX += breath * 0.025f * still; p.lArmX += breath * 0.02f * still; p.rArmZ += breath * 0.02f * still; p.lArmZ -= breath * 0.02f * still; // keep breathing through kit stances
        AttackDef d = e.currentAttack();
        if (d != null && e.deathTime == 0) {
            float t = Math.min(1f, (e.animTick() + partial) / Math.max(1, d.duration));
            if (d.weaponR != null) p.weaponR = d.weaponR;
            if (d.weaponL != null) p.weaponL = d.weaponL;
            d.anim.apply(p, t, age);
        }
        if (e.hurtTime > 0 && e.deathTime == 0) { // flinch: snap back and sideways, arms jerk, knees give, stagger a half-step
            float h = Math.max(0, (e.hurtTime - partial) / (float) Math.max(1, e.hurtDuration)), k = h * h * (3 - 2 * h), sd = ((e.getId() + e.tickCount / 10) & 1) == 0 ? 1 : -1;
            p.bodyX -= 0.45f * k; p.bodyY += 0.35f * sd * k; p.bodyZ += 0.12f * sd * k; p.headX -= 0.35f * k; p.headZ += 0.2f * sd * k;
            p.rArmX -= 0.45f * k; p.rArmZ += 0.5f * k; p.lArmX -= 0.3f * k; p.lArmZ -= 0.5f * k;
            p.lLegX += 0.3f * k; p.rLegX -= 0.25f * k; p.rootY -= 0.9f * k; p.rootZ += 1.6f * k; p.rootRoll += 0.08f * sd * k;
        }
        if (e.emoteAnim > 0 && e.deathTime == 0 && d == null) emote(p, e, age);
        if (e.reviveAnim > 0 && e.deathTime == 0) { // revival: from one knee, head bowed -> rise, arms spreading, lifted slightly off the ground
            float t = 1 - Math.max(0, e.reviveAnim - partial) / 24f, k = 1 - Keys.smooth(Keys.win(t, .2f, .8f)), fl = Keys.bump(Keys.win(t, .3f, 1f));
            p.rootY += -4.5f * k + 1.3f * fl; p.lLegX -= 0.9f * k; p.rLegX += 0.7f * k; p.bodyX += 0.5f * k - 0.15f * fl; p.headX += 0.5f * k - 0.4f * fl;
            p.rArmZ += 1.1f * fl; p.lArmZ -= 1.1f * fl; p.rArmX -= 0.3f * fl; p.lArmX -= 0.3f * fl;
        }
        if (e.deathTime > 0) { // death: buckle to the knees, drop the weapons, pitch forward onto the ground, small bounce, lie still
            float t = Math.min(1f, (e.deathTime + partial) / 20f), k1 = Keys.smooth(Keys.win(t, 0, .35f)), k2 = Keys.inQuad(Keys.win(t, .35f, .8f)), bounce = Keys.bump(Keys.win(t, .8f, 1f));
            Pose d0 = p; float keep = 1 - k1;
            p.rArmX = d0.rArmX * keep + 0.25f * k1 - 1.2f * k2; p.lArmX = d0.lArmX * keep + 0.15f * k1 - 1.0f * k2; p.rArmZ = d0.rArmZ * keep + 0.35f * k1 + 0.5f * k2; p.lArmZ = d0.lArmZ * keep - 0.35f * k1 - 0.5f * k2;
            p.rArmY *= keep; p.lArmY *= keep; p.rHandX *= keep; p.lHandX *= keep;
            p.bodyX = d0.bodyX * keep + 0.55f * k1 - 0.35f * k2; p.bodyY *= keep; p.bodyZ = 0.15f * k2; p.headX = 0.6f * k1 - 0.2f * k2; p.headY *= keep; p.headZ = 0.35f * k2;
            p.lLegX = d0.lLegX * keep - 0.9f * k1 * (1 - k2) + 0.15f * k2; p.rLegX = d0.rLegX * keep + 0.7f * k1 * (1 - k2) - 0.1f * k2; p.lLegZ = -0.12f * k2; p.rLegZ = 0.18f * k2;
            p.rootY = d0.rootY * keep - 4.5f * k1 * (1 - k2) - 0.6f * k2 + 0.6f * bounce; p.rootZ = d0.rootZ * keep - 1.5f * k2; p.rootYaw *= keep; p.rootRoll = 0.1f * k2;
            p.rootPitch = 1.45f * k2 - 0.06f * bounce;
            if (t > 0.3f) { p.weaponR = null; p.weaponL = null; }
        }
        apply(p);
    }

    /** victory poses and taunts, one body language per character family (see SlayerEntity#emoteStyle) */
    private void emote(Pose p, SlayerEntity e, float age) {
        float t = 1 - Math.max(0, e.emoteAnim - partial) / Math.max(1, e.emoteDur), env = Keys.smooth(Keys.win(t, 0, .18f)) * (1 - Keys.smooth(Keys.win(t, .82f, 1f)));
        float pulse = Math.abs((float) Math.sin(t * Keys.PI * 4)), wave = (float) Math.sin(t * Keys.PI * 4);
        int st = SlayerEntity.emoteStyle(kit);
        if (e.emoteKind == 0) switch (st) { // ---- victory ----
            case 0 -> { // both fists pumped overhead, leaning back in a roar
                p.rArmX = Keys.lerp(p.rArmX, -2.6f - 0.3f * pulse, env); p.rArmZ = Keys.lerp(p.rArmZ, 0.55f, env); p.lArmX = Keys.lerp(p.lArmX, -2.6f - 0.3f * pulse, env); p.lArmZ = Keys.lerp(p.lArmZ, -0.55f, env);
                p.rHandX = 0; p.lHandX = 0; p.bodyX -= 0.28f * env; p.headX -= 0.45f * env; p.rootY += 0.6f * pulse * env; p.lLegZ -= 0.15f * env; p.rLegZ += 0.15f * env;
            }
            case 1 -> { // weapon thrust overhead one-handed, fist on the hip, a slow nod
                p.rArmX = Keys.lerp(p.rArmX, -2.95f, env); p.rArmZ = Keys.lerp(p.rArmZ, 0.2f, env); p.rHandX = Keys.lerp(p.rHandX, -0.3f, env);
                p.lArmX = Keys.lerp(p.lArmX, -0.45f, env); p.lArmZ = Keys.lerp(p.lArmZ, -0.4f, env); p.lArmY = Keys.lerp(p.lArmY, -0.5f, env);
                p.headX += 0.22f * (float) Math.sin(t * Keys.PI * 3) * env; p.bodyX -= 0.08f * env; p.lLegZ -= 0.12f * env; p.rLegZ += 0.12f * env;
            }
            case 3 -> { // flick the blade clean, sheathe at the hip, short bow
                float flick = Keys.bump(Keys.win(t, .05f, .35f)), sheath = Keys.smooth(Keys.win(t, .35f, .6f)) * (1 - Keys.smooth(Keys.win(t, .85f, 1f))), bow = Keys.bump(Keys.win(t, .55f, 1f));
                p.rArmX = Keys.lerp(p.rArmX, 0.25f, flick); p.rArmZ += 0.9f * flick; p.rHandX += 0.6f * flick;
                p.rArmX = Keys.lerp(p.rArmX, -0.9f, sheath); p.rArmY = Keys.lerp(p.rArmY, 0.85f, sheath); p.lArmX = Keys.lerp(p.lArmX, -0.6f, sheath); p.lArmY = Keys.lerp(p.lArmY, -0.5f, sheath);
                p.bodyX += 0.38f * bow; p.headX += 0.3f * bow;
            }
            case 4 -> { // head tilts, the knife twirls idly in the fingers
                p.headZ += 0.36f * env; p.rArmX = Keys.lerp(p.rArmX, -1.0f, env); p.rArmZ = Keys.lerp(p.rArmZ, 0.3f, env); p.rHandY += Keys.PI * 4 * Keys.win(t, .2f, .8f);
                p.bodyZ += 0.08f * wave * env; p.lArmX = Keys.lerp(p.lArmX, 0.2f, env);
            }
            default -> { // sword raised to the sky, held, then a flourish back down
                float hold = Keys.smooth(Keys.win(t, 0, .2f)) * (1 - Keys.smooth(Keys.win(t, .62f, .8f))), fl = Keys.win(t, .62f, .95f);
                p.rArmX = Keys.lerp(p.rArmX, -3.05f, hold); p.rArmZ = Keys.lerp(p.rArmZ, 0.05f, hold); p.rHandX = Keys.lerp(p.rHandX, -0.25f, hold); p.rHandY += Keys.PI * 2 * fl * (1 - Keys.win(t, .95f, 1f));
                p.lArmX = Keys.lerp(p.lArmX, -0.35f, env); p.lArmZ = Keys.lerp(p.lArmZ, -0.5f, env); p.headX -= 0.32f * hold; p.bodyX -= 0.1f * hold; p.rootY += 0.45f * hold;
            }
        } else switch (st) { // ---- taunt ----
            case 0 -> { // chest pound, alternating fists
                float r = Math.max(0, wave), l = Math.max(0, -wave);
                p.rArmX = Keys.lerp(p.rArmX, -1.2f, env); p.rArmY = Keys.lerp(p.rArmY, 0.9f * r - 0.3f, env); p.rArmZ = Keys.lerp(p.rArmZ, 0.7f - 0.7f * r, env);
                p.lArmX = Keys.lerp(p.lArmX, -1.2f, env); p.lArmY = Keys.lerp(p.lArmY, -0.9f * l + 0.3f, env); p.lArmZ = Keys.lerp(p.lArmZ, -0.7f + 0.7f * l, env);
                p.bodyX -= 0.18f * env; p.headX -= 0.25f * env; p.rootY += 0.35f * pulse * env;
            }
            case 1 -> { // off hand out, two-finger "come on" flick, weapon held low and ready
                p.lArmX = Keys.lerp(p.lArmX, -1.5f, env); p.lArmZ = Keys.lerp(p.lArmZ, -0.1f, env); p.lHandX = -0.9f * pulse * env;
                p.rArmX = Keys.lerp(p.rArmX, -0.7f, env); p.headZ += 0.12f * env; p.bodyY += 0.2f * env;
            }
            case 3 -> { // drop into a low stance, blade trailing behind, open palm beckoning
                p.rootY -= 1.5f * env; p.lLegX -= 0.5f * env; p.rLegX += 0.45f * env; p.bodyX += 0.2f * env;
                p.lArmX = Keys.lerp(p.lArmX, -1.3f, env); p.lHandX = -0.7f * pulse * env; p.rArmX = Keys.lerp(p.rArmX, 0.4f, env); p.rArmZ = Keys.lerp(p.rArmZ, 0.6f, env);
            }
            case 4 -> { // slow head tilt side to side, knife lifted beside the face
                p.headZ += 0.4f * (float) Math.sin(t * Keys.PI * 2) * env; p.rArmX = Keys.lerp(p.rArmX, -1.9f, env); p.rArmY = Keys.lerp(p.rArmY, 0.5f, env); p.rHandX = Keys.lerp(p.rHandX, -0.4f, env);
            }
            default -> { // level the sword at the enemy and trace a small circle with the tip
                p.rArmX = Keys.lerp(p.rArmX, -1.55f + 0.08f * wave, env); p.rArmY = Keys.lerp(p.rArmY, 0.1f * (float) Math.cos(t * Keys.PI * 4), env); p.rArmZ = Keys.lerp(p.rArmZ, 0.05f, env); p.rHandX = Keys.lerp(p.rHandX, 0.05f, env);
                p.bodyY += 0.3f * env; p.headY -= 0.3f * env; p.lLegX -= 0.3f * env; p.rLegX += 0.25f * env; p.rootY -= 0.4f * env;
            }
        }
    }

    private static void set(ModelPart m, float dx, float dy, float dz, float rx, float ry, float rz) {
        PartPose i = m.getInitialPose();
        m.x = i.x + dx; m.y = i.y + dy; m.z = i.z + dz;
        m.xRot = i.xRot + rx; m.yRot = i.yRot + ry; m.zRot = i.zRot + rz;
    }

    private void apply(Pose p) {
        set(all, p.rootX, -p.rootY, p.rootZ, p.rootPitch, p.rootYaw, p.rootRoll);
        set(torso, p.bodyOffX, p.bodyOffY, p.bodyOffZ, p.bodyX, p.bodyY, p.bodyZ);
        set(head, 0, 0, 0, p.headX - p.bodyX * 0.5f, p.headY, p.headZ);
        set(rArm, 0, 0, 0, p.rArmX, p.rArmY, p.rArmZ); set(lArm, 0, 0, 0, p.lArmX, p.lArmY, p.lArmZ);
        set(rLeg, 0, 0, 0, p.rLegX, p.rLegY, p.rLegZ); set(lLeg, 0, 0, 0, p.lLegX, p.lLegY, p.lLegZ);
        set(rHand, 0, 0, 0, p.rHandX, p.rHandY, p.rHandZ); set(lHand, 0, 0, 0, p.lHandX, p.lHandY, p.lHandZ);
        for (String w : baked.rig.weapons) {
            toggle(rHand, w, w.equals(p.weaponR)); toggle(lHand, w, w.equals(p.weaponL));
        }
        pose.rArmX = p.rArmX; // minimal echo; kits needing more can read Pose through their own anim
    }
    private static void toggle(ModelPart hand, String w, boolean on) {
        if (hand.hasChild("w_" + w)) hand.getChild("w_" + w).visible = on;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
