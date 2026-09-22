# AV5 — o halo respeita parede (#198)

O código das quatro issues do AV5 está na branch. **Este gate prova uma coisa
só, e ela não é estética:** aura visível através de parede é *vazamento de
informação*, e não feiura. Enquanto In e Zetsu existirem, um halo que sangra por
trás de um bloco entrega a posição de quem está se escondendo.

> ## As capturas deste gate foram APOSENTADAS
>
> **Decisão de 2026-09-22, e ela vale para a trilha AV inteira** (AV0–AV8). A
> evidência passa a ser o julgamento humano registrado em `PERGUNTAS.md`, datado
> e preso a um commit. O motivo e o custo estão em
> [`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md), na seção 6.1.
>
> **A lista abaixo continua valendo como ROTEIRO do que precisa ser OLHADO** —
> ela só deixou de exigir arquivo.


Decisão que este gate verifica:
[ADR-016](../../../adr/ADR-016-pos-processamento-proprio-da-aura.md) §3.

---

## O que o alvo COMPARTILHADO implica — leia antes de julgar

O bloom é **um alvo para a cena inteira**, nunca um por entidade (dez jogadores
em Ren seriam dez alvos de tela cheia por quadro — é o custo que o AV8 existe
para barrar). Isso tem três consequências visíveis, e duas delas **não são
defeito**:

| O que se vê | É esperado? |
| --- | --- |
| **Dois Ren juntos brilham mais onde os halos se cruzam** | ✅ **sim.** O alvo acumula luz de propósito; duas fontes brilhantes lado a lado somam — é o que bloom faz |
| **Com um Ren na tela, o halo de um Ten engrossa** | ✅ **sim.** Um alvo = **um raio** de borrão, e ele é o maior da tela. Pela média, um Ren ao lado de cinco Ten perderia o halo dele |
| ~~Ativar Ren deixa a aura dos outros mais CLARA~~ | ❌ **era defeito**, corrigido em 2026-09-22 |

> ### O defeito que a sessão do AV1 encontrou
>
> A força do perfil era aplicada **duas vezes**: uma por jogador na escrita do
> alvo, outra no composite — e a segunda com `maiorForca`, o **valor de outra
> pessoa**. Com um Ten na tela:
>
> ```
> observador em Ten -> 0,20 (escrita) x 0,20 (composite) = 0,040
> observador em Ren -> 0,20 (escrita) x 0,55 (composite) = 0,110
> ```
>
> O Ten de um terceiro ficava **2,75× mais claro porque quem olhava trocou de
> técnica**. Não lançava, não aparecia em teste nenhum, e o build ficava verde.
> **Quem achou foi olho humano em jogo** — a sessão do AV1, perguntando "isso é
> esperado?". `ForcaDoBrilhoTest` agora tranca a conta.

---


## As treze capturas

```
ren_atras_de_parede_3b   _8b   _16b        (vfx.bloom = HIGH)
ren_oclusao_parcial_canto
ren_atras_de_vidro   ren_atras_de_folhas   ren_dentro_dagua
bloom_off   bloom_fast   bloom_high        (MESMO frame, Ten e Ren)
sem_bloom_ten   sem_bloom_ren   pos_f3t_ren
memoria_antes   memoria_depois             (10 resizes)
log_fallback_forcado                       (trecho do log)
```

---

## O critério que não admite grau

> **Revelar um jogador atrás de uma parede reprova o gate. Sem discussão de
> grau: se dá para saber que há alguém ali, reprovou.**

Sangrar poucos pixels na silhueta em oclusão **parcial** é aceitável — a máscara
usa o teste de profundidade da cena, e borda perfeita não é o alvo.

### O limite já conhecido, e que não é achado

**Vidro, folhas e água NÃO ocluem o halo.** A profundidade é copiada depois dos
blocos *sólidos* e antes das entidades, porque é aí que a aura é desenhada;
translúcidos vêm depois e não estão no buffer copiado. As três capturas existem
para **documentar o quanto** isso aparece, e não para reprovar.

---

## `FAST` contra `HIGH`

`FAST` alarga geometria; `HIGH` sangra luz de verdade. São **duas fontes do
mesmo halo**, e vão divergir com o tempo.

> A trava contra a divergência **não é um portão automático**: é a captura de
> comparação do mesmo frame nos três níveis, arquivada, e **refeita a cada
> mudança de qualquer um dos dois**.

Os três precisam dar a mesma leitura *em graus* — e não três estilos.

---

## As três verificações que não são imagem

1. **`F3+T` com Ren ligado** — não crasha, não some com o efeito, não deixa o
   shader mudo.
2. **Maximizar e restaurar 10 vezes** com Ren ligado — a memória volta ao
   patamar anterior, e os contadores `alvos: N criados / N liberados` do overlay
   **F6** batem. Framebuffer não liberado não dá erro: dá memória subindo
   devagar.
3. **Falha de shader forçada** — o jogo continua, o nível cai para `FAST`
   (nunca `OFF`), e o log tem **uma** linha com motivo legível. O trecho vai
   para `log_fallback_forcado`.

Com nenhuma aura na tela, o overlay marca `passe pulado: sim` e `alvo: nenhum`.
Custo **zero**, e não "custo pequeno".
