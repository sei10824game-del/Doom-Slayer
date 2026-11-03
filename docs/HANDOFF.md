# HANDOFF: 残り 20 キットを Opus サブエージェントで並列実装する

## 状態
- ブランチ `claude/happy-noether-7jtc7n`(最新 `9381e87` 以降)。フレームワーク + Doom 11 キット完成・サーバー起動確認済み。
- 下の 20 キットは `kit/kits/<Class>.java` に**スタブ**があり `kit/Kits.java` に登録済み。実装 = スタブファイルを丸ごと置き換えるだけ。
- Windows ビルド: `.\gradlew.bat build` (JDK 17 必須) / 確認 `.\gradlew.bat runClient`。

## 新セッションの最初の指示(これをコピーして貼る)
```
docs/HANDOFF.md と docs/KIT_GUIDE.md を読んで。ブランチ claude/happy-noether-7jtc7n 上で作業。
HANDOFF の表の 20 キットを、キットごとに 1 体の Opus サブエージェント(isolation=worktree, run_in_background)で並列実装して。
各エージェントには「KIT_GUIDE.md を全部読む → 参考に kit/kits/Doom2016Slayer.java を読む → 自分のスタブを丸ごと置換 → コンパイル → コミット」と指示し、
HANDOFF の該当行の内容をプロンプトに含めること。
全員戻ったら worktree の kit ファイルを本体へコピー統合 → ./gradlew compileJava → push。
```
サブエージェントのプロンプト共通部(各自に付ける):
> Java 17 が無ければ `/opt/jdk17` に Adoptium 17 を展開し `JAVA_HOME` を設定。`./gradlew -q compileJava`(Maven 429 は 40 秒待って再試行)。フレームワークは作り直さない。自分の kit ファイル以外は、実バグ修正が必要な場合のみ触り最終報告に書く。最終報告は 12 行以内(攻撃一覧+フレームワークの問題)。

## 品質基準(ユーザー要件)
- そのキャラの**全ての攻撃法**を `AttackDef` として実装(全武器・近接・特殊能力・処刑・投擲)。
- 各攻撃は専用の凝ったアニメ(予備動作→打撃→余韻、胴のひねり・脚・頭・両腕・反動)。取って付けた腕振りは不可。
- 見た目は作品ごとに別物(鎧・ヘルメット・バイザー発光・武器モデルは複数ボックス)。
- 陣営はプレイヤー側(framework が保証)。音は vanilla のみ、テクスチャは `Mat` による手続き生成のみ(著作物アセット不可)。

## 対象 20 キット
| Class | 作品 / キャラ | 必須の攻撃・特徴 |
|---|---|---|
| EntropyZeroBadCop | Entropy Zero / Bad Cop | スタンスティック(横・縦・電撃チャージ)、シャブ、USP、SMG1(+グレネード)、SPAS-12、357、AR2(+エナジーボール)、クロスボウ、RPG、フラググレ、ZPG(引き寄せ→保持→投げ)、Xen ブースト(ダッシュ+ダメ増) |
| EntropyZero2BadCop | Entropy Zero 2 / Bad Cop | 上記+強化 3 連スタンスティック+溜め叩きつけ、Xen 強化の外観(緑/紫の発光脈)、Combine ボール受け、レジングレネード、Xen 注入爆発。EZ1 と外観・挙動を差別化 |
| EntropyZero2CloneCop | Entropy Zero 2 / Clone Cop | 上記武器全般+残像ダッシュ(ミラーストライク)、Xen 共鳴パルス(範囲)、共有ダメージ「リンク」攻撃。灰/ティール配色 |
| HalfLifeAdrian | Half-Life: Opposing Force / Adrian Shephard | ナイフ、パイプレンチ、Desert Eagle(レーザー)、M9、357、MP5(+M203)、ショットガン、M249 SAW、M40A1 スナイパー(スコープ)、RPG(レーザー誘導)、Displacer、Shock Roach、Spore Launcher、Penguin、手榴弾、サッチェル、トリップマイン、スナーク |
| HalfLifeGordon | Half-Life / Gordon Freeman (HEV Mark IV, 眼鏡+髭) | バール(横・縦・溜め)、Glock、357、MP5(+ARグレ)、ショットガン(単/二連)、クロスボウ、RPG、Gauss(溜め撃ち・反動)、Egon(連続ビーム)、Hivehand(誘導ホーネット)、手榴弾、サッチェル、トリップマイン、スナーク、ロングジャンプ |
| HalfLife2Gordon | Half-Life 2 / Gordon Freeman (HEV Mark V) | バール、グラビティガン(引き寄せ→保持→発射、スーパーチャージで分解)、USP、357、SMG1(+グレ)、SPAS-12、AR2(+ボール)、クロスボウ、RPG、フラググレ、バグベイト、スプリント/スライド |
| HaloCeChief | Halo: CE / Master Chief (MJOLNIR Mark V) | 近接(ライフル打撃・パンチ)、M6D、MA5B、ショットガン、スナイパー、ロケット、フラググレ、プラズマグレ(粘着)、Plasma Pistol(溜め撃ち)、Plasma Rifle、Needler、エネルギーソードは奪取使用、シールド回復・ヘルスパック |
| Halo2Chief | Halo 2 / Master Chief (Mark VI) | 上記+BR55、SMG、デュアルウィールド(SMG/Plasma)、Beam Rifle、Brute Shot、Spiker、Energy Sword、Carbine、Sentinel Beam、Brute Plasma Rifle |
| Halo2Arbiter | Halo 2 / The Arbiter (Thel 'Vadam) | Energy Sword コンボ・突進、Plasma Rifle(デュアル)、Carbine、Needler、Fuel Rod、Beam Rifle、Plasma グレ、Active Camouflage(透明化) |
| Halo3Chief | Halo 3 / Master Chief (Mark VI) | MA5C、BR55、SMG、ショットガン、スナイパー、Spartan Laser(長溜めビーム)、ロケット、Gravity Hammer(叩きつけ衝撃波)、Energy Sword、Mauler、Plasma、Needler、フラグ/プラズマ/スパイク/Firebomb グレ、Bubble Shield / Power Drain / Regenerator / Flare 等の装備 |
| Halo3Arbiter | Halo 3 / The Arbiter | Energy Sword(突進・連撃)、Plasma Rifle、Carbine、Needler、Fuel Rod、Beam Rifle、Brute Shot、Spiker、Gravity Hammer、Active Camo |
| Halo3OdstRookie | Halo 3: ODST / The Rookie | サイレンサー付 SMG/M6C、VISR 発光、MA5C、ショットガン、スナイパー、ロケット、Plasma 系、フラグ、ライフル打撃 |
| Halo3OdstBuck | Halo 3: ODST / Buck (Dutch) | Rookie と別外観(ODST 装甲+青いバイザー)、SMG、M6、ショットガン、Spartan 級の近接、ブルートショット |
| HaloReachNobleSix | Halo: Reach / Noble Six | MA37、DMR、M6G、ショットガン、スナイパー、ロケット、Grenade Launcher、Needle Rifle、Plasma Repeater、Focus Rifle、グレ各種、装備: Armor Lock / Sprint / Jetpack / Drop Shield / Active Camo / Hologram、Energy Sword |
| Halo4Chief | Halo 4 / Master Chief (Mark VI GEN2) | BR85、DMR、Assault Rifle、SAW、Boltshot、Light Rifle、Binary Rifle、Scattershot、Suppressor、Incineration Cannon、Sticky Detonator、Promethean Vision、Thruster Pack、ライフル打撃・ Energy Sword |
| Halo5Chief | Halo 5 / Master Chief (Mark VI GEN2 / Blue Team) | Spartan Charge、Ground Pound、Clamber/Slide、Thruster、Smart-link、BR75、DMR、AR、SMG、ショットガン、Rocket、Hydra、Scattershot、Railgun、Energy Sword |
| Halo5Locke | Halo 5 / Jameson Locke (Fireteam Osiris, Mark VI GEN2 ブラック/ゴールド) | Spartan Charge、Ground Pound、Thruster、Magnum、DMR、BR、Assault Rifle、Sniper、Rocket、Gravity Hammer、Energy Sword、Scan |
| HaloInfiniteChief | Halo Infinite / Master Chief (Mjolnir Mark VII, 老齢) | Grappleshot(フック引き寄せ+フック打撃)、Threat Sensor、Drop Wall、Thruster、Repulsor(弾反射/衝撃波)、Shroud Screen、MA40、BR75、VK78 Commando、Sidekick、Mangler、Shock Rifle、Skewer、Hydra、Ravager、Cindershot、Energy Sword、Gravity Hammer、Ravager、Disruptor |
| HaloWarsForge | Halo Wars / Sgt. John Forge | MA5B、M6 を基本に近接打撃、手榴弾、スパルタンレーザー、指揮官的特殊(ロケットバラージュ/爆撃要請) |
| HaloSpartanAssaultPalmer | Halo: Spartan Assault / Sarah Palmer | MA5、BR、SMG、ショットガン、スナイパー、Plasma 系、ロケット、Spartan Ability(Armor Lock / Overshield) |

(Halo 主人公の取り漏れがあれば追加すること。Halo は全キット共通で「シールド回復(passive)」「フラグ/プラズマグレ」「ライフル打撃」を必ず持たせる。)

## 統合後の確認
1. `./gradlew compileJava` → `./gradlew runServer`(`run/eula.txt` に `eula=true`)で `Done` が出ること。
2. 描画確認はクライアントでのみ可能: `runClient` → クリエイティブタブ「Doom Slayer」のスポーンエッグ。
3. 既知: `Pose.fx`/`Rig.pivot` は未反映(部品の個別アニメ不可・モデル差し替えで代用)、被ダメ介入不可(Resistance で代用)。
