package com.doomslayer.kit.kits;

import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.List;
import java.util.Map;

/**
 * Replaces the animation of plain melee attacks (fists, bashes, clubs, knives, hammers) with {@link KU#comboAnim}: every hit gets its own
 * wind-up, accelerating snap, follow-through and recovery, with footwork and hip twist.
 * Entry: {kitClass.attackId -> {dur, kinds...; hit ticks after the -1 separator}}. kinds: 0 forehand, 1 backhand, 2 overhead, 3 thrust/butt, 4 right punch, 5 left punch.
 */
public final class MeleeFix {
    private MeleeFix() {}
    private static Object[] e(int dur, int[] kinds, int... hits) { return new Object[] {dur, kinds, hits}; }
    private static int[] k(int... k) { return k; }
    private static final Map<String, Object[]> T = Map.ofEntries(
            Map.entry("BrutalDoomMarine.fists", e(18, k(4, 5), 4, 11)), Map.entry("BrutalDoomMarine.berserk_combo", e(36, k(4, 5, 0), 14, 20, 27)),
            Map.entry("Doom1Marine.fist", e(16, k(4, 5), 5, 12)), Map.entry("Doom1Marine.berserk_punch", e(20, k(4), 11)),
            Map.entry("Doom2005Grimm.knife_combo", e(26, k(1, 0, 3), 5, 12, 19)), Map.entry("Doom2005Grimm.cqb_punches", e(20, k(4, 5, 4), 4, 9, 15)), Map.entry("Doom2005Grimm.rifle_butt", e(18, k(3), 8)),
            Map.entry("Doom2016Slayer.punch", e(16, k(4, 5), 4, 8)),
            Map.entry("Doom2Marine.berserk_punch", e(28, k(4), 12)),
            Map.entry("Doom3Marine.fists", e(14, k(4, 5), 4, 9)), Map.entry("Doom3Marine.cold_punch", e(20, k(4), 11)), Map.entry("Doom3Marine.flashlight_bash", e(16, k(0), 8)),
            Map.entry("Doom3RoeMarine.fists", e(14, k(4, 5), 5, 9)), Map.entry("Doom3RoeMarine.flashlight_bash", e(16, k(0), 8)),
            Map.entry("Doom64Marine.fist", e(14, k(4), 6)),
            Map.entry("DoomEternalSlayer.blood_punch", e(18, k(4), 9)), Map.entry("DoomEternalSlayer.crucible_overhead", e(28, k(2), 15)),
            Map.entry("DoomRpgMarine.fists", e(18, k(4, 5), 5, 12)), Map.entry("DoomRpgMarine.axe", e(36, k(2, 0), 13, 25)),
            Map.entry("EntropyZero2BadCop.stunstick_combo", e(34, k(0, 1, 2), 6, 15, 26)), Map.entry("EntropyZero2BadCop.stunstick_charged_slam", e(40, k(2), 28)),
            Map.entry("EntropyZero2CloneCop.stunstick_combo", e(20, k(0), 6)), Map.entry("EntropyZero2CloneCop.stunstick_overhead", e(26, k(2), 14)),
            Map.entry("EntropyZeroBadCop.stunstick_horizontal", e(16, k(0), 7)), Map.entry("EntropyZeroBadCop.stunstick_vertical", e(18, k(2), 8)),
            Map.entry("EntropyZeroBadCop.stunstick_charge", e(34, k(2), 22)), Map.entry("EntropyZeroBadCop.shove", e(16, k(3), 5)),
            Map.entry("HalfLife2Gordon.crowbar", e(18, k(0, 1), 5, 12)),
            Map.entry("HalfLifeAdrian.knife", e(18, k(3), 11)), Map.entry("HalfLifeAdrian.pipe_wrench", e(28, k(2), 14)),
            Map.entry("HalfLifeGordon.crowbar_side", e(16, k(0), 5)), Map.entry("HalfLifeGordon.crowbar_overhead", e(20, k(2), 8)), Map.entry("HalfLifeGordon.crowbar_charged", e(34, k(2), 23)),
            Map.entry("Halo2Chief.rifle_bash", e(14, k(3), 6)), Map.entry("Halo2Chief.punch", e(18, k(4, 5), 4, 10)),
            Map.entry("Halo2Arbiter.carbine_bash", e(16, k(3), 6)), Map.entry("Halo3Arbiter.rifle_bash", e(16, k(3), 6)), Map.entry("Halo3Arbiter.gravity_hammer", e(34, k(2), 16)),
            Map.entry("Halo3Chief.rifle_melee", e(16, k(3), 7)), Map.entry("Halo3OdstBuck.odst_haymaker", e(20, k(4), 8)), Map.entry("Halo3OdstBuck.rifle_bash", e(16, k(3), 6)),
            Map.entry("Halo3OdstRookie.rifle_bash", e(16, k(3), 6)), Map.entry("Halo4Chief.rifle_bash", e(16, k(3), 6)), Map.entry("Halo4Chief.energy_sword", e(24, k(0), 10)),
            Map.entry("Halo5Chief.rifle_bash", e(16, k(3), 6)), Map.entry("Halo5Locke.rifle_melee", e(14, k(3), 5)), Map.entry("Halo5Locke.gravity_hammer", e(30, k(2), 14)),
            Map.entry("Halo5Locke.energy_sword", e(26, k(0, 1), 10, 17)), Map.entry("HaloCeChief.rifle_butt", e(14, k(3), 6)), Map.entry("HaloCeChief.hammer_punch", e(16, k(4), 8)),
            Map.entry("HaloInfiniteChief.rifle_melee", e(16, k(3), 6)), Map.entry("HaloInfiniteChief.energy_sword", e(30, k(0), 12)), Map.entry("HaloInfiniteChief.gravity_hammer", e(32, k(2), 16)),
            Map.entry("HaloReachNobleSix.rifle_melee", e(16, k(3), 6)), Map.entry("HaloSpartanAssaultPalmer.rifle_bash", e(16, k(3), 6)), Map.entry("HaloWarsForge.rifle_butt", e(16, k(3), 6)));

    public static void apply(SlayerKit kit, List<AttackDef> list) {
        String cls = kit.getClass().getSimpleName();
        for (AttackDef a : list) {
            Object[] v = T.get(cls + "." + a.id);
            if (v != null) a.anim = KU.comboAnim((int[]) v[2], (Integer) v[0], (int[]) v[1]);
        }
    }
}
