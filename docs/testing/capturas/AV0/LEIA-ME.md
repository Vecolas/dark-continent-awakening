# AV0 — a shell acompanha as animacoes em servidor dedicado (#169)

**Nenhuma captura existe.** O código deste marco está na `main` desde antes da
branch do AV4; o que falta é evidência.

> **A aparência final NÃO está em julgamento aqui.** No AV0 a shell é crua, sem
> shader próprio: o que se avalia é **aderência** — se ela acompanha o corpo —,
> e não semelhança com a referência B. Os filamentos são AV2, o tuning é AV3, e
> a nuvem de partícula antiga continua ligada até lá.

---

## As catorze capturas

```
ten_dia          ten_noite         ten_caverna
ten_slim         ten_overlay_skin  ten_armadura
ten_correndo     ten_agachado      ten_nadando
ten_2b   ten_5b  ten_10b  ten_20b  ten_40b
```

## O que cada grupo responde

| Captura | A pergunta |
| --- | --- |
| `ten_dia/noite/caverna` | a shell se lê nas três luzes, sem estourar nem sumir? |
| `ten_slim` | o cliente slim usa o modelo slim? O braço fino não fica com aura larga? |
| `ten_overlay_skin` | uma skin com segunda camada completa (`hat`/`jacket`/`sleeve`) **engole** a shell? |
| `ten_armadura` | com armadura completa, a aura ainda aparece e a armadura continua reconhecível? |
| `ten_correndo/agachado/nadando` | a shell **descola nas articulações**? É a pergunta central do AV0 |
| a série `2b..40b` | **tirada com um SEGUNDO jogador** — a distância do jogador local para a própria câmera é zero, e o LOD nunca morde em si mesmo |

## O critério de aceite, além das imagens

- a shell acompanha correr, pular, atacar, agachar e nadar **sem descolar**;
- log do servidor dedicado **sem** `NoClassDefFoundError` nem
  `ClassNotFoundException` de `net.minecraft.client.*` — singleplayer não pega
  classe client-only vazada (erro nº 10 do `CLAUDE.md`);
- `/nenvfx off`, relog, morte e troca de dimensão não deixam estado preso
  — o **logout** já tem teste; morte e troca de dimensão, não;
- ~~`poseStack.scale` não é usado em lugar nenhum da shell~~ → **agora é
  portão**, e não item de sessão: `EscalaDaShellTest`. Ele mira o TIPO
  `PoseStack` (não o nome da variável), ignora menção em comentário — os quatro
  javadocs que proíbem a escala continuam passando —, permite `Vec3.scale`, e
  foi verificado reprovando contra uma violação injetada no código real.

> **As perguntas desta sessão, uma por uma, estão em
> [`PERGUNTAS.md`](PERGUNTAS.md)** — com espaço para o veredicto ao lado de cada
> captura. Catorze nomes de arquivo produzem catorze imagens e nenhuma resposta;
> a evidência do gate é o julgamento escrito ao lado do PNG.

A bancada foi **validada em 2026-09-21** (`CAMPANHA-EVIDENCIAS.md` §5): comando,
overlay F6, sliders, lote, nomenclatura e restauração de câmera passaram item a
item. A sessão custou três defeitos — #299, #300 e #301 —, todos corrigidos e
confirmados em jogo.

Regras que valem para toda captura desta trilha: servidor dedicado (nunca
singleplayer), dois clientes quando o gate pedir, e o overlay **F6 sem o aviso
`OVERRIDE ATIVO`**. A montagem está em
[`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md) §4.
