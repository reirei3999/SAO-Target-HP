# SAO Target HP

Minecraftに、SAO風のターゲットHP表示を追加するクライアント専用MODです。SAOのゲーム用素材・フォント・音声は含まず、UIはすべてコードで描画しています。

## 動作環境

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.19.3以上**
- Fabric API **0.116.15+1.21.1以上**
- Java **21**

## 主な機能

- 照準を合わせた敵対Mobの名前とHPバーを表示
- 敵HPが40%以下で黄色、20%以下で赤色に変化
- 最大128ブロックの長距離ターゲット取得（壁の向こうは対象外）
- HP変化の追従、被弾時の残像・フラッシュ、ターゲット切替アニメーション
- ターゲット上部の赤いダイヤ型ピンと、その左下に追従する斜め端のHPゲージ
- バニラのボスバーを斜め端の緑ゲージとして表示
- ゲージの色・色が変わるHP割合・表示位置をゲーム内GUIまたは設定ファイルで変更可能
- モブゲージ／ボスバー／MOD全体のオン・オフと、F1（HUD非表示）中の表示を切り替え可能

## 設定

`config/saotargethp.json` に設定が保存されます（初回起動時に自動生成）。ゲーム内では **`K` キー**で設定画面を開き、スライダーと色コードで調整できます。設定画面の「色設定...」から全色を編集でき、「配置をドラッグ」から開く画面では**バーをマウスで直接つかんで移動**できます。

| 項目 | 内容 |
|---|---|
| `display.enabled` | MOD全体のオン・オフ（既定 `true`） |
| `display.mobBar` / `display.bossBar` | モブゲージ・ボスバー個別のオン・オフ（既定 `true`） |
| `display.showWhenHudHidden` | F1でHUDを隠したときも表示するか（既定 `false`） |
| `mob.normalColor` / `cautionColor` / `dangerColor` | モブゲージの塗り色（グリーン・イエロー・レッド） |
| `mob.normalHighlightColor` など | 各状態の上面ハイライト色 |
| `mob.trailColor` / `backgroundColor` / `outlineColor` | ダメージ残像・背景・外枠の色 |
| `mob.cautionThresholdPercent` | 黄色に変わるHP割合（既定 40） |
| `mob.dangerThresholdPercent` | 赤に変わるHP割合（既定 20） |
| `mob.offsetX` / `mob.offsetY` | モブゲージの位置（ブロック単位、既定 0） |
| `boss.fillColor` / `highlightColor` / `backgroundColor` / `rimColor` / `outlineColor` / `nameColor` | ボスバーの各色 |
| `boss.offsetX` / `boss.offsetY` | ボスバーの位置（画面ピクセル、既定 0 / 24） |
| `boss.spacing` | ボスバー同士の間隔（既定 28） |

色は `#RRGGBB` または `#AARRGGBB`（先頭2桁が透明度）で指定します。JSONを直接編集した場合はゲームを再起動すると反映されます。

### 表示設定

「表示設定...」画面で、MOD全体・モブゲージ・ボスバーを個別にオン・オフできます。`F1中も表示` をONにすると、バニラのHUDをF1で隠してもこのMODのゲージは表示されたままになります。

OFFにした機能は描画だけでなく、ターゲット検索（128ブロックのレイキャストとエンティティ走査）も停止します。

### 負荷とタイムアウトについて

このMODはクライアント専用で、サーバーと通信するパケットを追加しません。負荷がかかり得るのは毎ティックのターゲット検索だけなので、次の対策を入れています。

- 検索は2ティックに1回（毎秒10回）に間引き、結果をキャッシュ
- HUDがOFF／F1で非表示／GUIを開いている間は検索も描画も停止
- 1回の検索が20ms以上かかった場合はログ（`logs/latest.log`）に警告を出す

`Timed out` で切断される場合は、この警告が出ているか確認してください。出ていない場合は、このMODではなくサーバー側のラグやネットワークが原因です。

## 導入方法

1. [Releases](../../releases) に公開版がある場合は、`sao-target-hp-1.3.1.jar` をダウンロードします。公開版がない場合は、[Actions](../../actions) の最新ビルド成果物から同名のJARを取得します。
2. Minecraft 1.21.1用のFabric LoaderとFabric APIを導入します。
3. ダウンロードしたJARをMinecraftの `mods` フォルダへ入れます。
4. Minecraftを起動します。

> このMODはクライアント専用です。サーバー側への導入は必要ありません。

## ビルド方法

Java 21を用意し、プロジェクトのルートで次を実行します。

```powershell
.\\gradlew.bat build
```

完成したMODは `build/libs/sao-target-hp-1.3.1.jar` に出力されます。`-dev.jar` は開発用の中間成果物なので、導入には使用しないでください。

## ライセンス

[MIT License](LICENSE)
