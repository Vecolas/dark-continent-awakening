# Compatibilidade e o gate do Epic Fight

## Regra geral

O nucleo funciona sem as integracoes do pack. GeckoLib e biblioteca de runtime
obrigatoria do pipeline de entidades; as demais integracoes continuam opcionais
([ADR-003](../adr/ADR-003-integracoes-opcionais.md),
[ADR-012](../adr/ADR-012-geckolib-obrigatorio.md)).

O primeiro teste de compatibilidade e, portanto, o mais importante e o mais
facil de esquecer:

> **Remover todos os mods opcionais e verificar que o Nen Foundation ainda
> inicia em cliente e em servidor dedicado com NeoForge + GeckoLib.**

Isso entra na matriz do M6, quando as integracoes existirem. Ate la e trivial,
porque nao ha integracao nenhuma — e e exatamente por isso que a verificacao
precisa comecar agora, enquanto e barata.

---

## Smoke test por integracao

Cada mod da lista aprovada roda estes tres, no minimo:

1. **Boot** — cliente e servidor dedicado sobem com ele instalado.
2. **Sem ele** — cliente e servidor sobem depois de remove-lo, e o Nen
   continua funcionando.
3. **Funcao especifica** — a razao de ele estar no pack de fato funciona.

| Mod | Funcao especifica a testar | Marco |
| --- | --- | --- |
| FTB Quests | quest dispara o despertar via API, sem escrever NBT | M6 |
| KubeJS | receita customizada aparece e funciona | M6 |
| LootJS | loot de mob de teste sai alterado | M6 |
| In Control | regra de spawn de zona vale | M6 |
| Patchouli | o manual de Nen abre e navega | M6 |
| JEI **ou** EMI | itens do mod aparecem; **nao instalar os dois** | M6 |
| Jade | tooltip aparece e **nao** revela Nen alheio | M6 |
| Curios | slot funciona; **nao** usar como armazenamento de perfil | pos-MVP |
| ModernFix / FerriteCore / Embeddium | perfil de spark antes e depois | M7 |
| spark | o profiler funciona e o perfil e arquivavel | M2 |

---

## O gate do Epic Fight

Epic Fight so entra no pack se passar em **todos** os itens abaixo. Ver
[ADR-006](../adr/ADR-006-epic-fight-fora-da-fundacao.md).

| Teste obrigatorio | Criterio de aprovacao |
| --- | --- |
| Melee com Ten e Ren ligados | dano aplicado **uma** vez; modificador na ordem documentada |
| Habilidade de Emission | projetil e dano independentes da animacao de arma |
| Dodge e stun | a tecnica interrompe ou continua conforme regra previsivel |
| Servidor dedicado | nenhuma classe client-only carregada no servidor |
| PvP com dois jogadores | mesmos resultados e mesmos cooldowns nos dois clientes |
| Varredura de keybind | nenhum bind critico fica inacessivel |

O primeiro item e o que costuma reprovar. Dois sistemas com hook no pipeline de
dano multiplicam duas vezes, e o numero final e plausivel demais para alguem
notar sem medir. **Meca o dano, nao olhe a barra de vida.**

### Resultado do gate

No M7 a decisao e escrita aqui, com data e com quem executou:

```
Data: <preencher>
Executado por: <preencher>
Versao do Epic Fight testada: <preencher>
Resultado: aprovado | experimental | fora do MVP
Itens reprovados: <lista, ou "nenhum">
```

**Status atual: nao executado.** O adapter nao existe.

---

## Incompatibilidades conhecidas

| Com o que | Sintoma | Estado |
| --- | --- | --- |
| — | — | nada registrado ainda |

Esta tabela nasce vazia e **precisa** crescer. Uma tabela de
incompatibilidades permanentemente vazia depois do M7 nao significa que nao ha
incompatibilidade: significa que ninguem procurou.

---

## Dois mods de Nen ao mesmo tempo: nunca

A pesquisa de viabilidade e explicita, e vale registrar aqui porque e a
decisao mais facil de reverter por engano quando alguem quiser "so testar":

> Mine X Hunter, Nen Unbound e Hunter X Craft parecem complementares e nao sao.
> Todos querem controlar aura, stamina, atributos, HUD, animacao, keybind, dano
> e progressao.

**O pack tem UMA autoridade de Nen, e ela e o Nen Foundation.** Instalar outro
mod de Nen junto nao produz erro — produz dois HUDs, duas barras de recurso e
um balanceamento que ninguem consegue explicar.
