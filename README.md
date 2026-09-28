> ## ⚠ これは改変版です（muon-rw/Medieval-Origins-Revival の fork・公式のものではありません）
>
> **This is a modified version of Medieval Origins Revival, not the official build.**
> Modified by the minecraft club (eruto). Original work: [muon-rw/Medieval-Origins-Revival](https://github.com/muon-rw/Medieval-Origins-Revival).
> Source code under **MIT**, assets and documentation under **CC BY 4.0**, same as upstream
> (see upstream [LICENSE.md](https://github.com/muon-rw/Medieval-Origins-Revival/blob/1.21.1-fabric/LICENSE.md)).
> Changes are listed below. The original author does not endorse this build.
>
> 上流: [muon-rw/Medieval-Origins-Revival](https://github.com/muon-rw/Medieval-Origins-Revival) ／ 枝 `eruto/world3-1.20.1`
> ／ 上流の枝 `1.20.1-multiloader` から分岐。**以下は上流の README です。**
> この枝は [eruto-mc/eruto-origins](https://github.com/eruto-mc/eruto-origins) の submodule として建てる（`medieval/gradlew :forge:build`）。
>
> ⚠ **不具合をここの改変版で見つけても、上流へ報告しないでください。**
>
> **当部が変えたところ**（中身は `git log --author=erutobusiness`）:
>
> | 何を | なぜ |
> | - | - |
> | ここから届かない maven を避けてソースから建てられるようにし、fabric を建てる対象から外した | 依存の置き場（`maven.greenhouse.lgbt`）に TCP が届かず、fabric の設定の段で `:forge:build` まで落ちていた |
> | ピクシーの上昇ダッシュと前方ダッシュを外した | 前方ダッシュは遊ぶ人の要望 |
> | ピクシーのゲージを回復する食べ物と、アンデッドの召喚の時間制限・同時 5 体まで・指示（待つ／ついて来る／呼び寄せる）・腐肉と骨で寿命が延びる、を足した | 上流の新しい版（6.7.x）に在る形を、この版へ移した |
> | 翼（Icarus）: 品の翼だけエリトラと同じ飛び方にし、種族の翼の前進を当部の計算に置き換えた | 種族の翼は、真上を向いて前進すると秒速 50 m で上昇できていた |
> | Curios で表示を切った枠の翼を描かないようにした | 表示を切っても品の翼が描かれていた（上流の Icarus の Issue #131） |
> | この構成に無い MOD を当てにした能力と、中身の無い power への名指しを落とした。説明文を実物に合わせた | どれも一度も読み込めていなかった |

# Medieval Origins Revival 
**Medieval Origins Revival** is an addon for Origins, adding medieval, fantasy, and mythology inspired origins. 

It was originally based off of Medieval Origins by ItsParkieLad.

You can download it here:
- https://modrinth.com/mod/medieval-origins-revival
- https://www.curseforge.com/minecraft/mc-mods/medieval-origins-revival

If you'd like to ask a question or get support, head to #muons-projects in the Lunapixel Studios discord:

https://discord.gg/lunapixel
