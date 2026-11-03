package com.doomslayer.kit.kits;

import com.doomslayer.client.Mat;
import com.doomslayer.client.Rig;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.List;

public class Doom3Marine extends SlayerKit {
    public Doom3Marine() { super("doom3_marine", "Doom3Marine", "Doom3Marine", "stub"); }
    @Override protected void defineAttacks(List<AttackDef> out) { out.add(AttackDef.of("punch").range(0, 3).dur(12).cd(15).tick((e, t, n) -> {})); }
    @Override public void buildModel(Rig rig) { Mat m = Mat.of(0x808080); rig.humanoid(m, m, m); }
}
