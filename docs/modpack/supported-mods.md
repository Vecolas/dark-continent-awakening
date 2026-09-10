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

**Nenhum mod de terceiro esta no pack.** O M0 entrega o perfil dev-minimal:
NeoForge + Nen Foundation e nada mais.

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
| spark | — | profiler; **ferramenta obrigatoria de QA** | *a confirmar* | M2 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/spark) |
| ModernFix | — | correcoes e otimizacoes | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modernfix) |
| FerriteCore | — | uso de memoria | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ferritecore) |
| Embeddium | — | renderizacao client-side | *a confirmar* | M7 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/embeddium) |
| GeckoLib | 6 | animacao de entidade e item | *a confirmar* | pos-MVP | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/geckolib) |
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
