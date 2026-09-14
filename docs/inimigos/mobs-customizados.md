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

**Nenhuma.** As 24 criaturas registradas têm modelo, esqueleto, animações,
textura, renderer e voz próprios. `PlaceholderDeclaradoTest` varre a fonte e não
encontra um único `PLACEHOLDER` de identidade — e ele morde dos dois lados, então
o vazio desta tabela é uma afirmação verificada, e não uma tabela que ninguém
atualizou.

| Grupo | Criaturas | Corpo próprio | Comportamento próprio |
| --- | --- | --- | --- |
| Exame Hunter | Kiriko, Master of the Swamp, Foxbear, Great Stamp, Frog-In-Waiting, Man-faced Ape, Spider Eagle | 7 de 7 | julgamento, fisgada, território, carga, emboscada, disfarce, ninho |
| Greed Island | Cyclops, Hyper Puffball, Melanin Lizard, Radio Rat, Bubble Horse, King White Stag Beetle, Wolf Pack Hunter | 7 de 7 | cone de visão, esporo, camuflagem, alarme, fuga viva, tombo, matilha |
| Chimera (peões) | Crab Heavy, Bat Scout, Wolf Runner | 3 de 3 | linha de frente, relato, flanco |
| Chimera (oficiais) | Spider Webber, Mosquito Officer, Multiarm Centipede, Cheetah Leader, Scorpion Leader, Avian Commander | 6 de 6 | teia, dreno, sequência, arranque, ferrão, comando aéreo |
| Ferramenta | Boneco de Treino | 1 de 1 | fases visíveis, sem IA de propósito |

**A dívida de som fechou, e vale dizer como.** A frase que ficou aqui por meses —
"bloqueado por ferramenta" — envelheceu sem que nada acusasse, e uma dívida com a
causa errada escrita ao lado dela não é lembrete: é desculpa. O bloqueio real
nunca foi `ffmpeg`: era escopo. Hoje as 24 criaturas têm cinco momentos cada
(ambiente, alerta, ataque, dano, morte), 120 arquivos Ogg Vorbis sintetizados por
`art-source/sons/`, validados no cabeçalho e **reproduzíveis byte a byte** —
`art-source/verificar.py` regera tudo e compara md5.

Esse "byte a byte" também custou uma correção: a primeira versão afirmava
determinismo total e estava **errada** em 120 arquivos. O `libsndfile` sorteia o
serial do fluxo Ogg a cada escrita, então 24 bytes por arquivo mudavam enquanto o
áudio decodificado era idêntico. Quem descobriu foi a régua, não a revisão — e é
por isso que a régua roda.

**Uma fila, e não duas.** Até a issue #266 o Foxbear era registrado num
`DeferredRegister` próprio, num pacote paralelo ao dos outros seis. As duas filas
funcionavam — e era esse o problema: o mob de uma delas ficava de fora de tudo
que a outra ganhava depois. O Foxbear passou meses com a faixa de luz do perfil
**morta** (o placement dele ignorava a `SpawnRule`) e sem tag de bioma nem biome
modifier, ou seja, **sem nascer no mundo**, sem que nada reprovasse. Hoje o
registro é um só, e `FilaUnicaDeInimigosTest` reprova o segundo — e também o mob
que entre na fila sem atributos, placement, perfil publicado, loot ou tradução.

---

## Os dois portões de corpo, e por que são dois

`CoerenciaDeGeckoLibTest` descobre os mobs **no disco**: ele varre os
`.geo.json` existentes e cobra, para cada um, animação, textura, ossos e clipes
coerentes. É a escolha certa para o que ele mede — quem ganhar um `.geo.json`
entra na varredura sem que ninguém precise lembrar.

Mas ela tem uma consequência que só aparece do outro lado: **uma criatura sem geo
nenhum não é varrida.** Ela some da conta, o portão fica verde, e o mob é
invisível em jogo. Foi exatamente o estado em que as dezesseis criaturas novas
existiram por um tempo: registradas, com atributos, loot, tradução, voz, ficha de
bestiário e perfil publicado — e sem corpo. Todos os portões aprovavam, porque
cada um media a coluna dele e **nenhum media a ausência**.

`CorpoDeTodoInimigoTest` fecha isso varrendo a lista de **publicados**, e não o
disco. A diferença é a regra inteira:

> A fonte da varredura tem de ser **o que deveria existir**, e não o que existe.

Ele também trava a contagem em 24: um mob removido por engano aparece como
reprovação, e não como uma varredura menor.
