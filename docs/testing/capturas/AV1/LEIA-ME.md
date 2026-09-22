# AV1 — Ten convincente sem nenhuma particula (#176)

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


> **O critério do [ADR-015](../../../adr/ADR-015-aura-e-geometria-e-shader.md):**
> rodar com `vfx.densidadeDeParticulas = 0.0`. **Aprovado** se ainda se lê
> *"essa pessoa está em Ten"*. **Reprovado** se parece o jogador normal.
>
> Existe porque a maneira mais comum de falhar nesta trilha é **compensar uma
> shell fraca com mais partícula**. Se reprovar, a resposta é **voltar para a
> shell** — não subir a partícula nem o brilho.

---

> **As perguntas desta sessão, uma por uma, estão em
> [`PERGUNTAS.md`](PERGUNTAS.md)** — com espaço para o veredicto ao lado de cada
> captura, e a polaridade uniforme (`PASSA`/`REPROVA`). Dezessete nomes de
> arquivo produzem dezessete imagens e nenhuma resposta; a evidência do gate é o
> julgamento escrito ao lado.

## As dezessete capturas

```
ten_dia   ten_noite   ten_caverna   ten_neve   ten_nether   ten_chuva   ten_agua
ten_slim  ten_overlay_skin
ten_2b    ten_5b      ten_10b       ten_20b    ten_40b
ten_ruido_ampliado    ten_360          (serie girando a camera 360 graus)
sem_particulas_ten    <- ESTA decide o gate
```

## O que cada grupo responde

| Captura | A pergunta |
| --- | --- |
| as sete de ambiente | legível de dia, não estoura à noite, não vira borrão em caverna, sobrevive contra a neve e contra o vermelho do Nether |
| `ten_ruido_ampliado` | **o ruído tem veios, e não nuvens?** |
| `ten_360` | há z-fighting ao girar a câmera? |
| `sem_particulas_ten` | **o gate inteiro** |

## O que NÃO fica provado aqui

- **A referência B tem filamentos, e o AV1 não os entrega.** Ribbons são AV2, e
  a comparação com B é **parcial** — isso precisa estar na legenda de cada
  captura, senão alguém lê o gate como "ficou aquém da referência".
- Bloom é AV5, e este gate assume a ausência dele de propósito.

A bancada foi **validada em 2026-09-21** (`CAMPANHA-EVIDENCIAS.md` §5): comando,
overlay F6, sliders, lote, nomenclatura e restauração de câmera passaram item a
item. A sessão custou três defeitos — #299, #300 e #301 —, todos corrigidos e
confirmados em jogo.

Regras que valem para toda captura desta trilha: servidor dedicado (nunca
singleplayer), dois clientes quando o gate pedir, e o overlay **F6 sem o aviso
`OVERRIDE ATIVO`**. A montagem está em
[`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md) §4.
