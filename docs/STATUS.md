# STATUS (replaces the stub notes in HANDOFF.md / HANDOFF2.md)

- 76 kits registered (`kit/Kits.java`), every one implemented; no stubs left. ~1,213 attacks.
- Shared systems (all in `kit/kits/`): `KU` (helpers, strike/gun/throw poses), `MeleeFix` / `RangedFix` (animation overrides by attack id),
  `ResourceFix` (magic, ammo, shields, charges...), `Prog` (progression: beads, hearts, levels, power-ups), `ZeldaKits`, `DeadSpaceKits`, `BlasphemousKits`.
- Effects are custom-rendered (`client/ProjectileRenderer`): tracers, beams, flashes, blasts, rings, slashes, auras, shields, particle puffs.
- Config / commands: `doomslayer-common.toml` (`rivals`, `friendlyFire`), `/doomslayer rivals|friendlyfire|selftest`.
- Dev QA:
  - `DOOMSLAYER_SELFTEST=1 ./gradlew runServer` runs every attack + animation headless, prints `SELFTEST FAIL ...` lines.
  - `DOOMSLAYER_MODELTEST=<dir> xvfb-run ./gradlew runClient` builds all models, checks atlas use, dumps atlases.
  - `DOOMSLAYER_SHOTS=<dir> [DOOMSLAYER_ONLY=id,id] [DOOMSLAYER_NOATK=1] [DOOMSLAYER_FXDEMO=1] xvfb-run ./gradlew runClient -PquickWorld=<save>` screenshots kits / effects.
- New kit checklist: register in `Kits.java`, add `assets/doomslayer/models/item/<id>_spawn_egg.json`, run the self-test.
- Known gaps (need UI/vehicles, deliberately skipped): vehicles, stamina climbing/gliding, cooking, equipment screens, squad commands.
