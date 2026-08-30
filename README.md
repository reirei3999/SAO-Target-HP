# SAO Target HP

Minecraftに、SAO風のターゲットHP表示を追加するクライアント専用MODです。SAOのゲーム用素材・フォント・音声は含まず、UIはすべてコードで描画しています。

## 動作環境

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.19.3以上**
- Fabric API **0.116.15+1.21.1以上**
- Java **21**

## 主な機能

- 照準を合わせた敵対Mobの名前とHPバーを表示
- 最大128ブロックの長距離ターゲット取得（壁の向こうは対象外）
- HP変化の追従、被弾時の残像・フラッシュ、ターゲット切替アニメーション
- ターゲット上部の赤いダイヤ型ピンと、その左下に追従する斜め端のHPゲージ
- バニラのボスバーを斜め端の緑ゲージとして表示

## 導入方法

1. [Releases](../../releases) に公開版がある場合は、`sao-target-hp-1.2.8.jar` をダウンロードします。公開版がない場合は、[Actions](../../actions) の最新ビルド成果物から同名のJARを取得します。
2. Minecraft 1.21.1用のFabric LoaderとFabric APIを導入します。
3. ダウンロードしたJARをMinecraftの `mods` フォルダへ入れます。
4. Minecraftを起動します。

> このMODはクライアント専用です。サーバー側への導入は必要ありません。

## ビルド方法

Java 21を用意し、プロジェクトのルートで次を実行します。

```powershell
.\\gradlew.bat build
```

完成したMODは `build/libs/sao-target-hp-1.2.8.jar` に出力されます。`-dev.jar` は開発用の中間成果物なので、導入には使用しないでください。

## ライセンス

[MIT License](LICENSE)
