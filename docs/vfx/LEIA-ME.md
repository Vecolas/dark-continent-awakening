# O visual da aura — índice da trilha AV

Esta pasta é **a fonte de verdade sobre como a aura deve parecer e como ela é
construída**. Ela nasceu de um documento-fonte externo
([`docs/aura-art/`](../aura-art/LEIA-ME.md)) e de quatro imagens de referência,
mas o que vale aqui é o texto **traduzido para as regras deste repositório**.

> **Regra de desempate:** se esta pasta e `docs/aura-art/` discordarem, esta
> pasta ganha. Se esta pasta e o **código** discordarem, o código ganha e o
> texto se atualiza — e a discrepância é relatada, nunca corrigida em silêncio.

---

## Por onde ler

| Ordem | Documento | O que ele responde |
| --- | --- | --- |
| 0 | [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md) | por que a aura **não** é partícula, e o que isso custa |
| 1 | [`direcao-visual-da-aura.md`](direcao-visual-da-aura.md) | como Ten, Ren e Zetsu precisam parecer, e o que reprova |
| 2 | [`arquitetura-do-render-de-aura.md`](arquitetura-do-render-de-aura.md) | quais passes existem, quais classes, e as armadilhas do 1.21.1 |
| 3 | [`perfis-visuais.md`](perfis-visuais.md) | os números, e **onde cada um mora** |
| 4 | [ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md) | o brilho, e por que ele é opcional |
| 5 | [`o-que-mudou-do-documento-fonte.md`](o-que-mudou-do-documento-fonte.md) | onde este projeto **contraria** o documento-fonte, e por quê |
| 6 | [`../testing/av-aura-visual.md`](../testing/av-aura-visual.md) | como se prova que ficou certo |

---

## A trilha AV, em uma tela

A trilha é **paralela** aos marcos M5–M8: ela não gasta contrato de servidor,
não toca protocolo e não bloqueia o framework de habilidades. O prefixo `AV`
evita colisão com `M1–M8` (Nen Foundation) e `EN0–EN16` (inimigos).

```
AV0  tech spike       shell inflada segue as animações        ← se falhar, PARA
 └─ AV1  shell autoral   shader, Fresnel, ruído, fluxo vertical
     └─ AV2  ribbons       filamentos presos a bones
         └─ AV3  TEN final   tuning, primeira pessoa, áudio, LOD
             └─ AV4  REN       transição, colunas, pressão de chão, detritos
                 └─ AV5  bloom    AuraGlowTarget, blur, composite, fallback
                     └─ AV6  ZETSU   supressão, visibilidade por observador
                         └─ AV7  multiplayer, armadura, poses, GeckoLib
                             └─ AV8  performance e release do visual
```

**Um gate por vez.** A regra do `CLAUDE.md` — nunca implementar mais de um
marco sem instrução explícita — vale igual aqui. O AV0 existe justamente para
ser um ponto de parada barato: se a shell não acompanha a animação, **nada do
resto adianta**, e descobrir isso custa uma tarde em vez de um mês.

---

## O critério que resume tudo

> **Desligue todas as partículas.**
>
> Se ainda parece Nen, a fundação está certa.
> Se parece o jogador normal, ainda não chegamos.

Isso não é retórica: é a asserção do gate do AV3. Ela existe porque a forma
mais comum de falhar nesta trilha é compensar uma shell fraca com mais
partícula — e o resultado é fumaça colorida, que é exatamente o que as quatro
referências não são.

---

## Quem faz o quê

| Lane | Escopo nesta trilha |
| --- | --- |
| **Dev A — núcleo** | `AuraVisualState` e controlador, projeção da distribuição do ADR-014, visibilidade por observador, budgets e medição, GameTest do que dá para testar sem tela |
| **Dev B — superfície** | modelos inflados, shaders, ribbons, texturas autorais, chão de Ren, detritos, áudio, primeira pessoa, tuning e captura |
| **Conjunto** | AV0 (contrato), AV7 (multiplayer em servidor dedicado) e AV8 (gate de performance) |

Os arquivos que travam o repositório inteiro nesta trilha estão em
[`fronteira-de-arquivos.md`](../processo/fronteira-de-arquivos.md).
