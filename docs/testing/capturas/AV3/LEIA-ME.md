# AV3 — TEN aprovado, com capturas (#187)

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


> **É aqui que Ten fecha.** O AV0 provou aderência, o AV1 provou a shell sozinha
> e o AV2 provou os filamentos. O AV3 junta os três com tuning, primeira pessoa
> e a tabela de distâncias — e a partir dele a nuvem de partícula antiga sai de
> cena.

---

> **As perguntas desta sessão estão em [`PERGUNTAS.md`](PERGUNTAS.md)**, com o
> veredicto ao lado de cada uma.

## As vinte e duas capturas

```
ten_dia   ten_noite   ten_caverna   ten_nether   ten_neve   ten_agua   ten_chuva
ten_2b    ten_5b      ten_10b       ten_20b      ten_40b
ten_correndo   ten_agachado   ten_nadando
ten_slim       ten_armadura   ten_overlay_skin
primeira_pessoa_ten
sem_particulas_ten     <- o criterio do ADR-015
sem_bloom_ten
overlay_contadores     (ribbons vivas, particulas vivas, draw calls, LOD)
```

> **`sem_bloom_ten` não era possível até 2026-09-22.** O nível de bloom só vinha
> da config, e este mod não registra tela de config: mudar exigia sair do jogo,
> editar o `.toml` e reiniciar — o que quebra a regra de comparar **na mesma
> sessão**. `/nenvfx bloom off|fast|high|auto` foi criado ao preparar este gate,
> e o AV5, que compara os três níveis do mesmo quadro, dependia dele igual.

## `primeira_pessoa_ten`

A primeira pessoa foi **consertada em 2026-09-21** (#301): a shell desenhava no
centro do corpo em vez de no braço, e Ten ficava invisível por causa disso.
Agora ela espelha a pose que a vanilla monta, e três filamentos por braço
nasceram junto.

O que a captura responde: a borda **acompanha o braço** ao mover e atacar, Ten
**aparece**, e **nenhuma coluna** entra em cena.

A bancada foi **validada em 2026-09-21** (`CAMPANHA-EVIDENCIAS.md` §5): comando,
overlay F6, sliders, lote, nomenclatura e restauração de câmera passaram item a
item. A sessão custou três defeitos — #299, #300 e #301 —, todos corrigidos e
confirmados em jogo.

Regras que valem para toda captura desta trilha: servidor dedicado (nunca
singleplayer), dois clientes quando o gate pedir, e o overlay **F6 sem o aviso
`OVERRIDE ATIVO`**. A montagem está em
[`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md) §4.
