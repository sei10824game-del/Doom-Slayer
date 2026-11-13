package com.doomslayer.kit;

import com.doomslayer.anim.Pose;
import com.doomslayer.client.Rig;
import com.doomslayer.entity.SlayerEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * One protagonist from one specific game. Subclass in kit/kits/. Server side: stats + attacks().
 * Client side: buildModel (procedural box model + procedural texture) and idle().
 */
public abstract class SlayerKit {
    public final String id, name, nameJa, game;
    public int eggPrimary = 0x553322, eggSecondary = 0xaa8844;
    public double maxHealth = 60, speed = 0.30, armor = 10, knockbackRes = 0.3, followRange = 28;
    public float scale = 1f, width = 0.6f, height = 1.95f;
    /** distance the AI tries to hold when it has ranged attacks and the target is closer */
    public float preferredRange = 7f;
    /** damage multiplier for all Combat.* damage */
    public float dmgMult = 1f;
    /** weapon shown when not attacking (mount names from Rig#weapon) */
    public String idleWeaponR, idleWeaponL;
    private List<AttackDef> attacks;

    protected SlayerKit(String id, String name, String nameJa, String game) {
        this.id = id; this.name = name; this.nameJa = nameJa; this.game = game;
    }

    /** Fill the attack list. EVERY attack of the character must be here. */
    protected abstract void defineAttacks(List<AttackDef> out);

    public final List<AttackDef> attacks() {
        if (attacks == null) {
            attacks = new ArrayList<>();
            defineAttacks(attacks);
            com.doomslayer.kit.kits.MeleeFix.apply(this, attacks);
            com.doomslayer.kit.kits.RangedFix.apply(this, attacks);
            for (int i = 0; i < attacks.size(); i++) attacks.get(i).index = i;
        }
        return attacks;
    }

    /** CLIENT: build all geometry with the Rig (also paints the texture atlas). */
    public abstract void buildModel(Rig rig);

    /** CLIENT: called after locomotion, before an attack Anim. Use for idle stance, capes, breathing... */
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {}

    /** SERVER: extra per-tick passive behaviour (regen, auras, ...). */
    public void passive(SlayerEntity e) {}

    /** SERVER: called when health hit 0. Return true to cancel death (kit must restore health itself). */
    public boolean preventDeath(SlayerEntity e) { return false; }

    /** SERVER: adjust incoming damage before it applies (return <= 0 to negate completely). Used for parry/guard. */
    public float modifyDamage(SlayerEntity e, net.minecraft.world.damagesource.DamageSource src, float amt) { return amt; }

    /** SERVER: this slayer just killed 'victim' (progression: beads, level-ups, upgrades...). */
    public void onKill(SlayerEntity e, net.minecraft.world.entity.LivingEntity victim) { com.doomslayer.kit.kits.Prog.familyKill(this, e, victim); }
}
