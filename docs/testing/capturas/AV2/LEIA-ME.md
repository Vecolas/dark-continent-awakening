# AV2 — os filamentos nascem na superficie e acompanham os membros (#181)

**Nenhuma captura existe.** O código deste marco está na `main` desde antes da
branch do AV4; o que falta é evidência.

> ## As capturas deste gate foram APOSENTADAS
>
> **Decisão de 2026-09-22, e ela vale para a trilha AV inteira** (AV0–AV8). A
> evidência passa a ser o julgamento humano registrado em `PERGUNTAS.md`, datado
> e preso a um commit. O motivo e o custo estão em
> [`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md), na seção 6.1.
>
> **A lista abaixo continua valendo como ROTEIRO do que precisa ser OLHADO** —
> ela só deixou de exigir arquivo.


---

> **As perguntas desta sessão estão em [`PERGUNTAS.md`](PERGUNTAS.md)**, com o
> veredicto ao lado de cada uma e a polaridade uniforme (`PASSA`/`REPROVA`).

## As treze capturas

```
av2_ten_2b            av2_ten_5b            av2_ten_10b
av2_ten_correndo      av2_ten_agachado      av2_ten_nadando
av2_ten_atacando
av2_ten_slim          av2_ten_default
av2_frame_congelado_1 av2_frame_congelado_2   (tem de ser IDENTICAS)
av2_sem_particulas_ten
av2_overlay_contadores
```

## As duas que não são sobre estética

**`av2_frame_congelado_1` e `_2` têm de ser idênticas.** Duas capturas do mesmo
quadro congelado, tiradas em momentos diferentes. Se divergirem, a curva do
filamento está lendo algo que muda entre quadros — e o defeito aparece em jogo
como cintilação que ninguém consegue reproduzir.

> **Esta verificação teria reprovado sem haver defeito na curva.** Até
> 2026-09-22 o `/nenvfx freeze` parava a interpolação de estado mas **não o
> relógio de animação**: o fluxo do shader e o ciclo dos filamentos continuavam
> correndo, e dois quadros "congelados" saíam diferentes. Achado ao preparar
> este gate, corrigido, e travado por `RelogioCongeladoTest` — que prova que o
> **número** fica parado, não que a **tela** fica.

**`av2_overlay_contadores`** fixa quantas ribbons estão vivas. É o número que o
AV8 vai comparar.

A bancada foi **validada em 2026-09-21** (`CAMPANHA-EVIDENCIAS.md` §5): comando,
overlay F6, sliders, lote, nomenclatura e restauração de câmera passaram item a
item. A sessão custou três defeitos — #299, #300 e #301 —, todos corrigidos e
confirmados em jogo.

Regras que valem para toda captura desta trilha: servidor dedicado (nunca
singleplayer), dois clientes quando o gate pedir, e o overlay **F6 sem o aviso
`OVERRIDE ATIVO`**. A montagem está em
[`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md) §4.
