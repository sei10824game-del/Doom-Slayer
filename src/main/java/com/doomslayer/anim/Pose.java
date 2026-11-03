package com.doomslayer.anim;

/** Per-frame pose (radians / model units). Locomotion fills it first, then an attack Anim overwrites/adds. */
public class Pose {
    // torso group (pivot at hip; head + both arms are children, so bending the torso carries them)
    public float bodyX, bodyY, bodyZ, bodyOffX, bodyOffY, bodyOffZ;
    public float headX, headY, headZ;
    public float rArmX, rArmY, rArmZ, lArmX, lArmY, lArmZ;
    /** weapon mount rotation added on top of arm rotation (gun space, see Rig#weapon) */
    public float rHandX, rHandY, rHandZ, lHandX, lHandY, lHandZ;
    public float rLegX, rLegY, rLegZ, lLegX, lLegY, lLegZ;
    /** whole-model offsets/rotation (hop, lunge, spin, lean). Y positive = up. */
    public float rootX, rootY, rootZ, rootYaw, rootPitch, rootRoll;
    /** weapon part names to show (null = none). Names are the ones passed to Rig#weapon. */
    public String weaponR, weaponL;
    /** extra named scalar channels for kit specific parts (capes, flames, blade glow...). */
    public final float[] fx = new float[8];

    /** Two handed aim: both arms raised toward -z with slight inward yaw. recoil pulls arms back. */
    public Pose aim2(float pitch, float recoil) {
        rArmX = -1.5708f + pitch - recoil * 0.25f; lArmX = -1.5708f + pitch - recoil * 0.2f;
        rArmY = -0.15f; lArmY = 0.45f; rArmZ = 0; lArmZ = 0;
        bodyX -= recoil * 0.12f; headX += recoil * 0.05f;
        return this;
    }
}
