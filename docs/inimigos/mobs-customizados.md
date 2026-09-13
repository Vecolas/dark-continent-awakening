# Mobs customizados: o que separa "funciona" de "entregue"

Decisão registrada no
[ADR-017](../adr/ADR-017-mob-vanilla-e-andaime-nao-entrega.md).

> **Mob vanilla é andaime. Ele sustenta a obra e sai antes da entrega.**

Um inimigo emprestado spawna, anda, ataca, tem loot, tem tradução e passa nos
gametests. Ele parece pronto por **todos** os sinais que o repositório sabe ler.
O único que denuncia o contrário é alguém abrir o jogo e reconhecer um hoglin — e
isso nenhum portão vê. Por isso a regra mora aqui, e não numa conversa.

---

## O que é empréstimo de identidade (sai antes da entrega)

- modelo e geometria de entidade vanilla (`ModelLayers.*`);
- textura de entidade vanilla;
- renderer vanilla;
- animações vanilla;
- dimensões e hitbox herdadas da espécie emprestada;
- sons da espécie emprestada;
- IA **específica** de uma espécie vanilla;
- `extends` de entidade vanilla **concreta** (`extends PolarBear`).

## O que é reúso de infraestrutura (fica para sempre)

`PathNavigation`, `MoveControl`, `LookControl`, `GoalSelector`, atributos,
sistema de dano, gravidade, rede, pathfinding, `Animal`/`PathfinderMob` como base
genérica — e tudo que é nosso: `BaseHxHMob`, `EnemyBrain`, `AttackTimeline`,
`ChargeRules`, `GrabRules`, `AmbushRules`, `DisguiseRules`, `WeakPointResolver`,
`NestGuardRules`, o framework de spawn e o de facção.

Compartilhar classe **não** significa compartilhar aparência.

---

## Enquanto o empréstimo durar, ele se declara

Todo arquivo que empresta identidade vanilla carrega o marcador literal
`PLACEHOLDER`, dizendo **o que** foi emprestado e **o que** vai substituí-lo:

```java
// TODO: PLACEHOLDER -- geometria do hoglin vanilla. Sai quando o modelo
// GeckoLib proprio do great stamp existir (geo + animation + textura).
```

O portão `PlaceholderDeclaradoTest` varre a fonte e **morde dos dois lados**:
renderer que empresta sem se declarar reprova, e renderer que consta na lista de
dívida mas parou de emprestar também reprova — senão a lista passa a cobrir em
silêncio o dia em que o empréstimo acabar.

---

## Definition of Done de um inimigo principal

Um mob só pode ser marcado como **DONE** quando:

- [ ] `EntityType` próprio, no namespace do mod;
- [ ] entidade própria, sem `extends` de espécie vanilla concreta;
- [ ] modelo Blockbench/GeckoLib próprio (`geo/entity/<id>.geo.json`);
- [ ] skeleton e bones próprios, que permitam as animações da ficha;
- [ ] animações próprias (`animations/entity/<id>.animation.json`);
- [ ] renderer próprio, sem renderer nem `ModelLayers` vanilla;
- [ ] textura própria (`textures/entity/<id>/<variante>.png`), autoral (ADR-007);
- [ ] dimensões e hitbox próprias, testadas com o modelo final;
- [ ] sons próprios;
- [ ] IA própria, não "goals da espécie vanilla com ajustes";
- [ ] ataques próprios, telegrafados;
- [ ] loot, spawn e testes próprios;
- [ ] multiplayer e servidor dedicado testados;
- [ ] nenhum comportamento residual da espécie emprestada;
- [ ] o `PLACEHOLDER` e a linha na lista de dívida **removidos no mesmo PR**.

O teste final não é um portão: é alguém olhando para o bicho e pensando
*"esse é um Foxbear"*, e não *"é um urso polar modificado"*.

---

## A ordem da migração

```
placeholder vanilla
      ↓
comportamento próprio, medido em gametest      ← é onde os cinco estão hoje
      ↓
modelo + skeleton próprios
      ↓
animações próprias
      ↓
renderer + textura próprios
      ↓
dimensões e hitbox próprias
      ↓
sons próprios
      ↓
placeholder removido, dívida quitada
```

Comportamento vem antes de arte de propósito: comportamento é o que os portões
conseguem medir, e arte não trava IA.

---

## Dívida aberta hoje

| Inimigo | O que empresta | Comportamento próprio |
| --- | --- | --- |
| Foxbear | **nada** — modelo, esqueleto, animações, textura e renderer próprios | territorial, em jogo |
| Great Stamp | **nada** — modelo, esqueleto, animações, textura e renderer próprios | carga, testa, manada |
| Frog-In-Waiting | **nada** — modelo, esqueleto, animações, textura e renderer próprios | emboscada, agarrão |
| Man-faced Ape | **nada** — duas silhuetas próprias (disfarce humano e forma revelada) | disfarce, bando |
| Spider Eagle | **nada** — modelo, esqueleto, animações, textura e renderer próprios | ninho, mergulho, coleira |

A lista viva — a que reprova o build — é a de `PlaceholderDeclaradoTest`. Esta
tabela é para leitura humana e pode envelhecer; aquela não pode.

**A dívida visual fechou: os cinco têm corpo próprio.** Nenhum deles, mas nenhum dos
dois é DONE pela ficha acima: faltam **sons próprios**. Os cinco são silenciosos —
não emprestam som de vanilla, simplesmente não emitem. E isso hoje está
**bloqueado por ferramenta**, não por esforço: o Minecraft só toca `.ogg`
Vorbis e esta máquina não tem `ffmpeg` nem encoder Vorbis. Um `.wav`
renomeado carregaria mudo, que é o falso verde que este projeto passa o dia
evitando. Isso não reprova nenhum portão, e é justamente por isso
que está escrito aqui e em `o-que-nao-provamos.md`.

A coerência do que ele ganhou é cobrada por `CoerenciaDeGeckoLibTest`, que
descobre os mobs no disco: quem ganhar um `.geo.json` entra na varredura sem
que ninguém precise lembrar.
