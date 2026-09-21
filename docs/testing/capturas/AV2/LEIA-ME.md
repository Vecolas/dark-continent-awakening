# AV2 — os filamentos nascem na superficie e acompanham os membros (#181)

**Nenhuma captura existe.** O código deste marco está na `main` desde antes da
branch do AV4; o que falta é evidência.

---

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
