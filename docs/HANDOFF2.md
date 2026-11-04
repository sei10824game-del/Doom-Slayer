# HANDOFF2: 残りは HalfLifeGordon のみ

## 状態
- ブランチ `claude/happy-noether-7jtc7n`(最新 `2855a38` 以降)。20 キット中 19 キットを統合済み、`./gradlew compileJava` 成功。
- 未実装: `kit/kits/HalfLifeGordon.java`(スタブ)。担当エージェントが上限(429)で worktree 作成前に停止。
- 19 キットは各エージェントが途中停止した時点のファイル。最終報告なし・コミットなし・`runServer` 未確認。Halo2Chief の `oneHand` 呼び出し 2 箇所(k 抜け)は手で修正済み。

## 新セッションの最初の指示(貼る)
```
docs/HANDOFF2.md, docs/HANDOFF.md, docs/KIT_GUIDE.md を読んで。ブランチ claude/happy-noether-7jtc7n 上で作業。
1) HalfLifeGordon を実装(Opus サブエージェント1体 isolation=worktree、または自分で)。参考: Doom2016Slayer.java, HalfLife2Gordon.java。HANDOFF の HalfLifeGordon 行をプロンプトに含める。
2) 統合後 ./gradlew compileJava → runServer(run/eula.txt に eula=true)で Done を確認。
3) 19 キットの攻撃網羅を HANDOFF の表と突き合わせ、不足があれば補う(特に末尾が切れていないか: Halo3Chief 1241行, HaloInfiniteChief, Halo5Chief)。
4) push。
```

## HalfLifeGordon 要件(HANDOFF より)
Half-Life / Gordon Freeman (HEV Mark IV, 眼鏡+髭): バール(横・縦・溜め)、Glock、357、MP5(+ARグレ)、ショットガン(単/二連)、クロスボウ、RPG、Gauss(溜め撃ち・反動)、Egon(連続ビーム)、Hivehand(誘導ホーネット)、手榴弾、サッチェル、トリップマイン、スナーク、ロングジャンプ。

## 注意
- 上限到達後に再開したセッションでは Agent/SendMessage が使えなくなる。サブエージェントは上限前の通常セッションで起動すること。
