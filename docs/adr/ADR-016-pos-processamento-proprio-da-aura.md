# ADR-016 — O brilho da aura é pós-processamento próprio, opcional e com fallback

- **Status:** aceita
- **Data:** 2026-09-12
- **Marco:** AV5, dentro da trilha aberta pelo [ADR-015](ADR-015-aura-e-geometria-e-shader.md)
- **Toca:** `client/vfx/shader/` e o ciclo de vida de render target. Nada mais.

## Contexto

O halo luminoso das referências
[B](../aura-art/referencia-b-ten-em-camadas.png) e
[C](../aura-art/referencia-c-ren-pressao.png) **não sai do jogo**. As imagens
foram geradas com shader pack. Sem bloom, uma ribbon branca continua sendo
apenas um punhado de pixels claros; com bloom, ela vira luz.

Há três caminhos, e dois deles são armadilha:

1. **Exigir shader pack.** Viola o [ADR-003](ADR-003-integracoes-opcionais.md):
   o núcleo passaria a depender de Iris/Oculus e de um pack específico para
   *parecer certo*. E shader pack é justamente o que mais varia entre as
   instalações reais de um modpack.
2. **Usar `Glowing` / outline vanilla.** É contorno por entidade, atravessa
   parede por desenho, e é a mesma coisa que o espectador vê num mob com efeito
   de brilho. Ele diz "entidade marcada", não "energia".
3. **Fazer o próprio passe.** Mais caro, mas é o único que não terceiriza a
   identidade visual do mod.

E há um risco específico que empurra a decisão: **aura visível através de
parede é vazamento de informação**, não só feiura. Enquanto In e Zetsu
existirem, um halo que sangra por trás de um bloco entrega posição de jogador.
Um bloom ingênuo — desenhado sem respeitar profundidade — faz exatamente isso.

## Decisão

**A aura tem bloom próprio, em três níveis, e o sistema principal funciona
inteiro com ele desligado.**

1. **Níveis: `OFF`, `FAST`, `HIGH`.**
   - `OFF` — nenhum passe extra. A aura continua legível pela borda Fresnel e
     pelas ribbons.
   - `FAST` — sem framebuffer extra: o halo externo da shell é engrossado e
     clareado para *simular* o brilho.
   - `HIGH` — passe real: `AuraGlowTarget` em meia resolução, downsample, blur
     horizontal, blur vertical, composite aditivo.

2. **O alvo de brilho contém SÓ a contribuição da aura** — borda da shell,
   ribbons, sparks e colunas de Ren. Nunca a skin, nunca o mundo, nunca a UI.
   Colocar o jogador inteiro no alvo borra o personagem, e o personagem é a
   primeira coisa na hierarquia de leitura.

3. **O halo respeita profundidade.** A máscara é gerada com o teste de
   profundidade da cena. Sangrar poucos pixels na silhueta é aceitável;
   **revelar um jogador atrás de uma parede não é**, e isso é item de gate com
   caso de teste próprio, não observação de code review.

4. **Falha em compilar ou criar target não derruba nada.** O sistema cai
   sozinho para `FAST` e registra uma linha no log. **Efeito visual nunca
   crasha o jogo.**

5. **Resize de janela e `F3+T` recriam os targets.** Os dois são caminhos de
   vazamento de framebuffer e de referência velha, e os dois entram no gate do
   AV5 com passo escrito. Um framebuffer não recriado não dá erro: dá uma tela
   que some, ou memória que sobe devagar.

6. **Nenhum framebuffer por entidade.** Um alvo compartilhado para todas as
   auras da cena. Dez jogadores em Ren são dez contribuições no mesmo target, e
   não dez targets.

7. **O passe é pulado inteiro quando não há aura visível.** Sem aura na tela,
   custo zero — e não "custo pequeno".

8. **Kernel pequeno, de propósito.** Ten com raio de 2–3 px; Ren com 4–7 px,
   escalado pela resolução. Trinta pixels de halo não é anime: é névoa.

## O que NAO muda

- **O ADR-003.** Continua sem dependência nova. Iris, Embeddium e shader packs
  entram na matriz de compatibilidade como *ambientes testados*, jamais como
  requisito.
- **O ADR-015.** A identidade visual é shell mais ribbons. Bloom é melhoria. Se
  a aura só fica boa com bloom ligado, o AV1 e o AV2 não fecharam — e a resposta
  é voltar para eles, não subir o bloom.
- **O ADR-001.** Nada aqui atravessa a rede nem decide regra.
- **A regra de visibilidade.** Quem não deve ver a aura não vê, e o bloom não é
  a exceção que a revela.
- **Iluminação do mundo.** O bloom não altera block light, não acende bloco e
  não muda `packedLight`. A aura é emissiva no próprio material; ela não
  ilumina o cenário.

## Custo assumido

- **Render target é a classe de bug mais chata desta trilha.** Vazamento em
  resize, referência morta após reload de recurso, tela preta em GPU antiga. É
  por isso que existe um nível `OFF` que não é castigo: é um caminho de fuga que
  mantém o jogo jogável.
- **Duas maneiras de produzir o mesmo halo.** `FAST` (halo geométrico) e `HIGH`
  (blur real) são duas fontes para a mesma verdade visual, e elas vão divergir
  com o tempo. A trava é que o `FAST` **só** existe como aproximação do `HIGH`,
  com captura de comparação arquivada — e não como um segundo estilo.
- **Custo de GPU concentrado em fill-rate.** Meia resolução é o padrão, e não a
  melhor qualidade, porque um blur de cinco passos em resolução cheia por
  padrão é o tipo de escolha que só aparece na máquina de outra pessoa.
- **Incompatibilidade provável com pipelines de shader pack.** Um pack que
  substitui o pipeline pode ignorar ou duplicar o passe. O compromisso é
  detectar e cair para `FAST`, **e documentar a combinação na matriz** — não
  prometer que funciona com todos.
- **Um número que o balanceamento vai querer e não terá.** Intensidade de bloom
  é config **de cliente**. Dois jogadores no mesmo servidor podem ver brilhos
  diferentes, de propósito: é conforto visual, não regra de jogo.

## Governanca da decisao

| Desenvolvedor | Papel | Aprovacao |
| --- | --- | --- |
| **@Vecolas** | Dev A — núcleo | **aprovado**, na mesma instrução que aceitou o ADR-015 |
| **@jonex-01** | Dev B — superfície | **pendente** |
