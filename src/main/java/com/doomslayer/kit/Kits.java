package com.doomslayer.kit;

import com.doomslayer.kit.kits.*;
import java.util.List;

/** Roster. One entry per (character, game). Add new kits here. */
public final class Kits {
    private Kits() {}
    public static final List<SlayerKit> ALL = List.of(
            new Doom1Marine(),
            new Doom2Marine(),
            new Doom64Marine(),
            new Doom3Marine(),
            new Doom3RoeMarine(),
            new Doom2016Slayer(),
            new DoomEternalSlayer(),
            new DarkAgesSlayer(),
            new Doom2005Grimm(),
            new BrutalDoomMarine(),
            new DoomRpgMarine(),
            new EntropyZeroBadCop(),
            new EntropyZero2BadCop(),
            new EntropyZero2CloneCop(),
            new HalfLifeAdrian(),
            new HalfLifeGordon(),
            new HalfLife2Gordon(),
            new HaloCeChief(),
            new Halo2Chief(),
            new Halo2Arbiter(),
            new Halo3Chief(),
            new Halo3Arbiter(),
            new Halo3OdstRookie(),
            new Halo3OdstBuck(),
            new HaloReachNobleSix(),
            new Halo4Chief(),
            new Halo5Chief(),
            new Halo5Locke(),
            new HaloInfiniteChief(),
            new HaloWarsForge(),
            new HaloSpartanAssaultPalmer(),
            new Omori(),
            new Chara(),
            new DeadSpaceKits.Ds1Isaac(),
            new DeadSpaceKits.Ds2Isaac(),
            new DeadSpaceKits.Ds3Isaac(),
            new DeadSpaceKits.Ds3Carver(),
            new DeadSpaceKits.DsExtractionMcNeill(),
            new DeadSpaceKits.DsExtractionLexine(),
            new DeadSpaceKits.DsExtractionGabe(),
            new ZeroKatana(),
            new SekiroWolf(),
            new HollowKnight(),
            new Hornet(),
            new Siffrin(),
            new BlasphemousKits.Blasphemous1(),
            new BlasphemousKits.Blasphemous2()
    );
}
