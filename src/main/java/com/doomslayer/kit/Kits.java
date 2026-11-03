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
            new DoomRpgMarine()
    );
}
