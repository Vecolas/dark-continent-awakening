# AV5 — o halo respeita parede (#198)

O código das quatro issues do AV5 está na branch. **Este gate prova uma coisa
só, e ela não é estética:** aura visível através de parede é *vazamento de
informação*, e não feiura. Enquanto In e Zetsu existirem, um halo que sangra por
trás de um bloco entrega a posição de quem está se escondendo.

Decisão que este gate verifica:
[ADR-016](../../../adr/ADR-016-pos-processamento-proprio-da-aura.md) §3.

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
