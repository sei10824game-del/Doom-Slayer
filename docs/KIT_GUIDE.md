# Writing a SlayerKit (one protagonist of one game)

Each kit = one file `src/main/java/com/doomslayer/kit/kits/<Name>.java` extending `SlayerKit`. It is already registered in `kit/Kits.java`.
Characters are PLAYER-ALIGNED mobs (`SlayerEntity`): they follow the owner, hunt `Enemy` mobs, never hurt players/slayers/pets/villagers (all Combat.* helpers enforce this).

## Pieces
| Concern | API |
|---|---|
| stats | fields on SlayerKit (maxHealth, speed, armor, scale, preferredRange, dmgMult, egg colours, idleWeaponR/L) |
| attacks | `defineAttacks(List<AttackDef>)`: `AttackDef.of("id").range(min,max).cd(ticks).dur(ticks).weapon("name").tick((e,target,t)->{...}).anim((pose,t,age)->{...})` |
| server effects | `fx/Combat` (hitscan, shoot projectile, cone, blast, lunge, damage, knock), `fx/Fx` (arc/slash trail, ring, beam, sparks, explosion), `ProjStyle` presets/custom |
| model | `buildModel(Rig)`: boxes via `rig.box(part, Mat, x,y,z,w,h,d)`; weapons via `rig.weapon(rig.rHand,"name")` (GUN SPACE: origin=fist, barrel along -z, y down) |
| texture | procedural, per `Mat` (colour, noise, bevel, glow(), decal lambda). No PNG files. Atlas is 256x256 total; each box uses 2(w+d) x (d+h) px so keep boxes few/small. |
| animation | `Pose` (radians) + `Keys` helpers (`track`, `smooth`, `kick`, `win`...). `t` = 0..1 over the attack's `dur`. Server `tick` `t` is integer tick index (0..dur-1). Keep the hit moment of `tick` aligned with the strike frame of `anim` (frame = hitTick/dur). |

## Quality bar (user requirement)
* EVERY attack the character has in that game (all weapons, melee, special moves, glory kills/executions, abilities, grenades, dashes...) must exist as its own AttackDef.
* Each attack has a bespoke animation: anticipation -> strike -> follow-through -> recovery, using torso twist, legs (stance, step, lunge), head, both arms, weapon hand tilt, root lean/hop. NOT a single arm swing. Use recoil (`Keys.kick`), wind-up, weight shifts.
* Distinct look per game: armor colours/shape, helmet, visor (glow), shoulder pads, backpack, cape, belts, weapon models with several boxes each.
* Weapons are separate `rig.weapon` mounts; show with `.weapon("x")` / `.weapons(r,l)`; idle weapon via `idleWeaponR`.
* Effects: particles, sounds (vanilla `SoundEvents` only), projectiles via `Combat.shoot` + `ProjStyle` (create custom ProjStyle as `static final` fields in the kit class).
* Use `AttackDef.when/lowHealthTarget` for conditional moves (glory kill when target < 25% health, ability only if health low, etc.).
* Don't edit files outside your kit file unless you must fix a framework bug (report it in your final message).

## Verify
`./gradlew compileJava` (needs JAVA_HOME=/opt/jdk17/jdk-17.0.21+9). Fix all compile errors in your file. Do not run the game client.

## Known framework notes (read before coding)
* Torso pivot bug is fixed (hip = 12 above ground). `Pose.fx` / `Rig.pivot` parts are NOT animated by SlayerModel -> fake moving parts by swapping weapon models (e.g. two barrel models alternating) or whole-hand rotation. Weapon mounts: hand is pre-rotated 90deg, so rHandY rolls gun around barrel, rHandZ swings sideways.
* AttackDef.tick may receive a NULL target (target died mid-attack): always null-check.
* `ProjStyle.color()` overwrites trail: call `.trail(..)` AFTER `.color(..)`. Custom ProjStyle must be `static final` fields in the kit class.
* `Combat.dirTo(SlayerEntity,..)` / `Combat.muzzle` already aim from the right-hand side. Kits cannot intercept incoming damage: emulate shields/guards with MobEffects (Resistance) or by removing projectiles in `tick`.
* `AttackDef.range()` disables line-of-sight when max range <= 4.
* Shields/regen/abilities of the character belong in `passive(e)` or as attacks.
* Not allowed: copyrighted audio/textures. Use only vanilla SoundEvents and the procedural Mat system.
* Register nothing yourself: the stub is already in kit/Kits.java.
