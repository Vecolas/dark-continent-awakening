# Mods do pack Dark Continent Awakening

O pack e organizado em **camadas desacopladas**. A razao vem direto da pesquisa
de viabilidade: se daqui a seis meses uma camada quebrar, as outras sobrevivem.

| Camada | O que e | Se cair |
| --- | --- | --- |
| 1 — Nucleo de Nen | **Nen Foundation** (autoral) | o pack para. E por isso que ele e nosso |
| 2 — Progressao | FTB Quests, scripts | perdem-se as quests, nao o Nen |
| 3 — Balanceamento | KubeJS, LootJS, In Control | perde-se o ajuste, nao o sistema |
| 4 — Mundo | estruturas, cidades, exploracao | perde-se conteudo |
| 5 — Combate | Epic Fight, **no maximo um** | perde-se sensacao, nao regra |
| 6 — Conteudo proprio | addon nosso, quando script nao resolver | — |
| 7 — Identidade | resource pack, sons, UI, questbook | perde-se a cara do pack |

> **Decisao arquitetural mais importante do pack:** uma unica autoridade de Nen.
> Nao instalar Mine X Hunter, Nen Unbound ou Hunter X Craft junto. Ver
> [compatibility.md](../testing/compatibility.md#dois-mods-de-nen-ao-mesmo-tempo-nunca).

---

## Estado atual

**GeckoLib e a unica biblioteca de runtime obrigatoria.** O perfil dev-minimal
passa a ser NeoForge + GeckoLib + Nen Foundation; integracoes de quests,
scripts, combate e performance continuam opcionais.

Na QA da M2 foi usado **spark apenas no servidor de testes**, sem adicioná-lo
ao pack/Gradle. Versão, licença e hash em [version-lock.md](version-lock.md);
não implica aprovação dos demais mods candidatos.

A tabela abaixo e a lista **candidata**, com o marco em que cada um entra. Um
mod so passa de candidato a aprovado depois de passar no smoke test de
[compatibility.md](../testing/compatibility.md) e de ser registrado em
[version-lock.md](version-lock.md) com versao exata.

---

## Candidatos

Toda linha precisa de: pacote, versao testada, licenca, motivo e URL. Sem os
cinco, o mod nao entra — e isso vale tambem para saber o que podemos ou nao
redistribuir ([ADR-007](../adr/ADR-007-assets-autorais.md)).

| Mod | Camada | Papel | Licenca | Marco | URL |
| --- | --- | --- | --- | --- | --- |
| FTB Quests | 2 | campanha, Exame Hunter, tutorial, marcos | *a confirmar* | M6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) |
| KubeJS | 3 | receitas, ajustes, eventos leves | *a confirmar* | M6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kubejs) |
| LootJS | 3 | loot de trainers, mobs, bosses | *a confirmar* | M6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/lootjs) |
| In Control! | 3 | spawn e zonas | *a confirmar* | M6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/in-control) |
| Patchouli | 2 | manual de Nen e lore | *a confirmar* | M6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/patchouli) |
| JEI **ou** EMI | 3 | viewer de receitas — escolher **um** | *a confirmar* | M6 | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) / [EMI](https://www.curseforge.com/minecraft/mc-mods/emi) |
| Jade | 3 | informacao contextual | *a confirmar* | M6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jade) |
| spark | — | profiler; QA M2 executada | GPL-3.0-only | M2 | [Modrinth](https://modrinth.com/mod/spark/version/v5qtqRQi) |
| ModernFix | — | correcoes e otimizacoes | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modernfix) |
| FerriteCore | — | uso de memoria | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ferritecore) |
| Embeddium | — | renderizacao client-side | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/embeddium) |
| GeckoLib 4.8.3 | 6 | animacao de entidades do mesmo JAR | MIT | EN0 | [site/wiki oficial](https://wiki.geckolib.com/docs/geckolib4/) |
| Curios API | 6 | Hunter License e acessorios | *a confirmar* | pos-MVP | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/curios) |
| Epic Fight | 5 | combate e animacao — **so apos o gate** | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) |

> **A coluna de licenca esta em branco de proposito, e isso e uma divida
> nomeada.** Preenche-la exige abrir cada pagina e ler — nao citar de memoria.
> Varios mods populares sao *All Rights Reserved*, o que muda o que se pode
> fazer com o JAR. Ela precisa estar completa antes de qualquer distribuicao.

---

## Regras que valem para todo mod do pack

1. **JAR de terceiro nao entra no repositorio.** O pack se descreve por
   manifesto. Ver [ADR-007](../adr/ADR-007-assets-autorais.md).
2. **Versao pinada.** Uma dependencia por vez, com changelog e smoke test. Ver
   [ADR-005](../adr/ADR-005-versoes-pinadas.md).
3. **Um viewer de receita, nao dois.** JEI e EMI juntos produzem UI duplicada.
4. **Um mod de combate, no maximo.** E ele nao entra antes do M7.
5. **Nenhum script valida regra de Nen.** KubeJS mexe em conteudo do pack.
6. **Bug do nucleo se reproduz em dev-minimal primeiro.** Ver
   [perfis-de-execucao.md](perfis-de-execucao.md).

---

## Ferramentas de AUTORIA, que não entram no JAR

Estas não são dependências do mod: o `nenfoundation.jar` não as contém e o jogo
nunca as carrega. Elas são o que uma pessoa precisa ter instalado para
**regerar** os assets autorais a partir da fonte em `art-source/`.

| Ferramenta | Versão usada | Para quê | Sem ela |
| --- | --- | --- | --- |
| Python | 3.10 | todos os geradores de `art-source/` | os `.geo.json`, `.animation.json`, `.png` e `.ogg` continuam no git e funcionam; só não dá para **mudar** nenhum deles |
| Pillow | 9.4 | escrever as texturas `.png` | idem, para textura |
| NumPy | (vem com `soundfile`) | a síntese de áudio | idem, para som |
| `soundfile` (libsndfile) | 0.14 | escrever **OGG Vorbis** de verdade | idem, para som |

**Por que `soundfile` e não `ffmpeg`.** O Minecraft só toca OGG Vorbis, e esta
máquina não tem `ffmpeg` nem `oggenc` — foi por isso que a identidade sonora dos
mobs ficou registrada por meses como *"bloqueada por ferramenta"*. O
`libsndfile` que vem no wheel do `soundfile` escreve Vorbis, e isso foi
conferido no cabeçalho do arquivo (`OggS…vorbis`), não suposto.

**O risco que isso NÃO tem.** Um `.wav` renomeado para `.ogg` carrega mudo: o
jogo aceita o arquivo, não reclama e o mob não emite som. É o falso verde mais
barato deste domínio, e por isso `VozDeInimigoTest` confere a assinatura de cada
um dos 120 arquivos — em Java, no build, sem depender de nenhuma destas
ferramentas.

**Reprodutibilidade.** `python art-source/verificar.py` regera tudo e reprova o
que não bate byte a byte com o git. Foi ele que descobriu que o `libsndfile`
sorteava o número de série do fluxo Ogg a cada codificação; hoje o serial é
fixado e os 181 assets gerados reproduzem exatamente.
