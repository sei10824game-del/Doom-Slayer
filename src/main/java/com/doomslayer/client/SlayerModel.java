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
        float sw = limbSwing * 0.6662f, a = Math.min(1f, limbAmt * 1.3f);
        p.rLegX = Keys.lerp(0, (float) Math.cos(sw) * 1.4f, a); p.lLegX = Keys.lerp(0, (float) Math.cos(sw + Keys.PI) * 1.4f, a);
        p.rArmX = (float) Math.cos(sw + Keys.PI) * 0.9f * a; p.lArmX = (float) Math.cos(sw) * 0.9f * a;
        p.rArmZ = 0.05f + (float) Math.sin(age * 0.067f) * 0.04f; p.lArmZ = -0.05f - (float) Math.sin(age * 0.067f) * 0.04f;
        p.bodyX = 0.06f * a + (float) Math.sin(age * 0.09f) * 0.015f;
        p.rootY = Math.abs((float) Math.cos(sw)) * 0.35f * a;
        p.headY = yawHead * Keys.DEG; p.headX = pitch * Keys.DEG;
        p.weaponR = kit.idleWeaponR; p.weaponL = kit.idleWeaponL;
        kit.idle(p, e, age, a);
        AttackDef d = e.currentAttack();
        if (d != null && e.deathTime == 0) {
            float t = Math.min(1f, (e.animTick() + partial) / Math.max(1, d.duration));
            if (d.weaponR != null) p.weaponR = d.weaponR;
            if (d.weaponL != null) p.weaponL = d.weaponL;
            d.anim.apply(p, t, age);
        }
        if (e.deathTime > 0) { p.bodyX += 0.3f; p.headX += 0.4f; p.rArmZ += 1.0f; p.lArmZ -= 1.0f; }
        if (e.hurtTime > 0) { float h = e.hurtTime / (float) Math.max(1, e.hurtDuration); p.bodyX -= 0.25f * h; p.headX -= 0.15f * h; }
        apply(p);
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
