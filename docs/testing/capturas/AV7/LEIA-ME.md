# AV7 — dois clientes, armadura e poses (#205, #104)

Duas issues de gate no mesmo marco: #205 (armadura e poses) e #104 (aura de
terceiros com dois clientes reais, e o ciclo de vida).

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

## As vinte e uma capturas

Cada pose sai nos **dois modelos**: `default` **e** `slim`. Não é opcional —
usar `AURA_DEFAULT` num braço `slim` deixa a aura do braço larga demais, e o
defeito só aparece quando alguém com skin Alex entra no servidor.

```
ten_armadura_couro   ten_armadura_ferro
ten_armadura_diamante   ten_armadura_netherite
ren_armadura_ferro
ten_capa   ten_elytra_fechada   ten_elytra_planando
ten_overlay_skin   ten_armadura_slim

ten_nadando   ten_rastejando   ten_dormindo   ten_montado
ten_arco      ten_besta        ten_escudo
ten_correndo  ten_agachado
ren_nadando   ren_correndo
```

---

## O que cada grupo responde

| Grupo | A pergunta |
| --- | --- |
| armadura | com ferro completo, ainda se lê *essa pessoa está em Ten* a **5 e a 10 blocos**? A armadura continua reconhecível, ou a aura virou casca opaca? |
| capa e elytra | a elytra planando deixa a aura para trás, ou produz um rastro que ninguém pediu? |
| poses deitadas | **nenhuma coluna sobe do lugar errado** — dormindo, nadando e de elytra elas são suprimidas de propósito |
| montado | a aura atravessa a montaria, ou some dentro dela? |
| arco, besta, escudo | o braço continua **visualmente separado** do tronco? |
| nadando | a ordenação de transparência inverte contra a água? |

---

## O ciclo de vida (#104), com dois clientes

Este é o item que nenhum teste unitário alcança:

- A entra em Ren; B vê. A **desloga** — B não fica com aura presa no lugar.
- A **morre** em Ren — nada sobra: nem anel, nem coluna, nem detrito, nem loop
  de áudio.
- A **troca de dimensão** em Ren — idem.
- A sai do alcance de B e volta — o estado reaparece certo, e o zumbido não
  duplica. O overlay **F6** mostra `zumbidos: 1`, nunca 2.

---

## O limite que é decisão, e não achado

**Armadura de outros mods está fora**, e é custo assumido no
[ADR-015](../../../adr/ADR-015-aura-e-geometria-e-shader.md): uma peça modded
arbitrariamente grande engole qualquer deformação que caiba no teto de design, e
perseguir isso é perseguir o infinito. A **silhueta screen-space** não foi feita
e não será no MVP.

Capa e elytra **não ganham shell própria nem ribbons** — elas entram na leitura
pela luz do halo. Isso é a entrega, e não uma lacuna.
