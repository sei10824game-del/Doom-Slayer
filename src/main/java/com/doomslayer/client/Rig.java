package com.doomslayer.client;

import net.minecraft.client.model.geom.builders.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Model scaffold handed to SlayerKit#buildModel. Coordinates are vanilla model space: +y DOWN, ground at y=24 in 'all',
 * so inside body/head/arms use negative y to go up. Standard humanoid proportions: torso 8x12x4 (hip at y=0 of 'torso'),
 * head 8x8x8 on the neck, arms 4x12x4 hanging from the shoulder pivot, legs 4x12x4.
 * Parts you can attach boxes to:
 *   torso  - pivot at hip (everything upper-body is a child -> torso bend carries head+arms)
 *   head   - pivot at neck (local (0,0,0) = neck, head cube is y -8..0)
 *   rArm/lArm - pivot at shoulder (hand end is y=+10 in local), rLeg/lLeg - pivot at hip joint
 *   all    - the whole model, pivot on the ground (use for capes/tails/floating things, y = -height up)
 * Weapons: rig.weapon(rHand,"shotgun") returns a part in GUN SPACE: origin = fist, barrel along -z, x right, y down.
 * (With the arm raised forward gun space equals body space.) Show them from AttackDef.weapon(..)/Pose.weaponR.
 */
public class Rig {
    public final Atlas atlas = new Atlas();
    public final PartDefinition root, all, torso, head, rArm, lArm, rLeg, lLeg, rHand, lHand;
    public final Set<String> weapons = new LinkedHashSet<>();
    private int n;

    public Rig(MeshDefinition mesh) {
        root = mesh.getRoot();
        all = root.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        torso = all.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0, 12, 0));
        head = torso.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0, -12, 0));
        rArm = torso.addOrReplaceChild("rArm", CubeListBuilder.create(), PartPose.offset(-6, -10, 0));
        lArm = torso.addOrReplaceChild("lArm", CubeListBuilder.create(), PartPose.offset(6, -10, 0));
        rLeg = all.addOrReplaceChild("rLeg", CubeListBuilder.create(), PartPose.offset(-2, -12, 0));
        lLeg = all.addOrReplaceChild("lLeg", CubeListBuilder.create(), PartPose.offset(2, -12, 0));
        rHand = rArm.addOrReplaceChild("rHand", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 10, 0, 1.5708f, 0, 0));
        lHand = lArm.addOrReplaceChild("lHand", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 10, 0, 1.5708f, 0, 0));
    }

    /** Add a w*h*d box at (x,y,z) (min corner, local to parent) painted with m. Returns the created child part so you can animate it. */
    public PartDefinition box(PartDefinition parent, Mat m, float x, float y, float z, int w, int h, int d) {
        return box(parent, m, x, y, z, w, h, d, 0f);
    }
    public PartDefinition box(PartDefinition parent, Mat m, float x, float y, float z, int w, int h, int d, float inflate) {
        int[] uv = atlas.alloc(m, w, h, d);
        return parent.addOrReplaceChild("b" + (n++), CubeListBuilder.create().texOffs(uv[0], uv[1])
                .addBox(x, y, z, w, h, d, new CubeDeformation(inflate)), PartPose.ZERO);
    }
    /** Box inside a pivot group: creates group at pivot (px,py,pz) with rotation, box coords relative to pivot. Animatable via Pose.fx by kit code. */
    public PartDefinition pivot(PartDefinition parent, String name, float px, float py, float pz, float rx, float ry, float rz) {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }
    /** Standard humanoid: legs, torso, head, arms in the given materials. Add details on top with box(). */
    public void humanoid(Mat skin, Mat suit, Mat helmet) {
        box(torso, suit, -4, -12, -2, 8, 12, 4);
        box(head, helmet, -4, -8, -4, 8, 8, 8);
        box(rArm, suit, -2, -2, -2, 4, 12, 4);
        box(lArm, suit, -2, -2, -2, 4, 12, 4);
        box(rLeg, suit, -2, 0, -2, 4, 12, 4);
        box(lLeg, suit, -2, 0, -2, 4, 12, 4);
    }
    /** Create a weapon mount (GUN SPACE) attached to the right or left hand. Register once per name per hand. */
    public PartDefinition weapon(PartDefinition hand, String name) {
        weapons.add(name);
        return hand.addOrReplaceChild("w_" + name, CubeListBuilder.create(), PartPose.ZERO);
    }
}
