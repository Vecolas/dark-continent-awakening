# Material-fonte da direção visual da aura

Esta pasta guarda **a fonte**, e não a decisão. O que vale como plano de
trabalho está em [`docs/vfx/`](../vfx/LEIA-ME.md); o que vale como decisão
arquitetural está no [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md) e no
[ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md).

Se esta pasta e `docs/vfx/` discordarem, **`docs/vfx/` ganha** — ela é a
tradução revisada para as regras deste repositório. A discrepância é relatada,
não corrigida em silêncio.

---

## Os quatro níveis de referência

As imagens foram geradas como direção de arte, num cenário de Minecraft com
shader, e estabelecem quatro níveis. **Elas são alvo de leitura, não promessa
de paridade de pixel**: nenhuma delas foi renderizada pelo jogo, e o rodapé de
cada uma nomeia a opção.

| Ref | Arquivo | Rótulo na imagem | O que ela fixa |
| --- | --- | --- | --- |
| **A** | [`referencia-a-ten-base.png`](referencia-a-ten-base.png) | *Base aura direction* | contorno fino azul-branco, película aderida, poucos filamentos, fluxo vertical curto acima da cabeça. Nenhuma nuvem, nenhum halo grande. É o **protótipo** de Ten. |
| **B** | [`referencia-b-ten-em-camadas.png`](referencia-b-ten-em-camadas.png) | *Option B — Layered Ten* | shell ainda colada, múltiplos fios de energia contornando braços/tronco/pernas, borda mais brilhante, ainda controlada, sem impacto ambiental. É o **Ten final**. |
| **C** | [`referencia-c-ren-pressao.png`](referencia-c-ren-pressao.png) | *Option C — Ren pressure aura* | mesma linguagem de B com intensidade muito maior: borda quase branca, filamentos longos, correntes verticais acima da cabeça, anel de pressão no chão e fragmentos de bloco levantados alguns centímetros. É o **Ren**. |
| **D** | [`referencia-d-zetsu.png`](referencia-d-zetsu.png) | *Option D — Zetsu suppression* | personagem visualmente normal. Nenhuma aura, nenhum contorno, nenhum brilho. É o **Zetsu** — e a ausência É a informação. |

### Três leituras que as imagens impõem, e que não são negociáveis

1. **A silhueta do corpo é a base do efeito.** Em A, B e C a energia nasce
   colada em cada membro e acompanha a articulação. Nada nelas é uma esfera em
   volta do jogador.
2. **C é B amplificado, e não um efeito diferente.** Mesmas cores, mesmo tipo
   de filamento, mesma origem corporal. Só a densidade, o comprimento, o brilho
   e o alcance mudam — mais o chão, que só aparece em C.
3. **D não tem "modo discreto".** Não há outline secreto, não há brilho de 2%
   para o jogador saber que Zetsu está ligado. O corpo é o corpo.

### O que as imagens NÃO provam

- **Elas usam shader pack.** O bloom delas vem do renderizador da geração, não
  do jogo. Por isso o [ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md)
  existe — e por isso o efeito precisa continuar legível com o bloom desligado.
- **Elas são poses paradas.** Nenhuma mostra corrida, agachamento, natação,
  armadura, elytra ou primeira pessoa — que é justamente onde a shell aderida
  quebra. Os critérios de aprovação em
  [`direcao-visual-da-aura.md`](../vfx/direcao-visual-da-aura.md) cobrem isso.
- **Elas são uma skin só, de modelo `default`.** O braço `slim` e a segunda
  camada da skin (`hat`, `jacket`, `sleeve`) não aparecem em nenhuma.
- **O fragmento de bloco em C flutua e desaparece.** Ele é cosmético. Nada em
  C quebra bloco, gera `ItemEntity` ou altera o mundo.

---

## O documento-fonte

[`plano_visual_aura_nen_minecraft_ready_to_build.txt`](plano_visual_aura_nen_minecraft_ready_to_build.txt)
— 167 seções, em inglês técnico misturado com português, escritas antes da
tradução para as regras deste repositório.

**Ele não é o plano de trabalho.** Ele é a entrada. As diferenças entre ele e
`docs/vfx/` estão listadas em
[`docs/vfx/o-que-mudou-do-documento-fonte.md`](../vfx/o-que-mudou-do-documento-fonte.md),
porque um documento-fonte que "foi seguido" sem se dizer onde foi contrariado é
a forma mais barata de criar duas verdades.
