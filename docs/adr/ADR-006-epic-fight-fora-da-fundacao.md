# ADR-006 — Epic Fight nao e fundacao; e adapter opcional e tardio

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0 (a decisao), M7 (o gate)

## Contexto

Epic Fight muda animacao, deteccao de acerto e a sensacao inteira do combate —
exatamente as areas em que um mod de Nen tambem mexe. A pesquisa de viabilidade
aponta isso como uma das colisoes mais provaveis do projeto, e o plano tecnico
dedica uma secao propria ao assunto.

O modo de falha e caro: dano aplicado duas vezes, cooldown ignorado, animacao
divergindo entre clientes. Nenhum deles aparece como excecao. Todos aparecem
como "esse mod esta desbalanceado".

## Decisao

O MVP fecha em combate **vanilla**.

1. Epic Fight nunca e dependencia obrigatoria do nucleo.
2. `integration/epicfight` so recebe codigo no M7, e so depois de o pipeline de
   dano vanilla passar pela QA do M4 e do M5.
3. A inclusao no pack depende de um gate com criterios objetivos, em
   `docs/testing/compatibility.md`.
4. Reprovar no gate **nao** bloqueia o MVP. Bloqueia so a entrada do Epic Fight
   no pack.
5. No M7 a decisao e formal e escrita: aprovado, experimental ou fora.

## Custo assumido

- **O combate do MVP e o do Minecraft.** Clicar e bater. Para um pack de
  Hunter x Hunter isso e uma decepcao real, e ela dura ate o M7 no melhor caso.
- **Trabalho possivelmente jogado fora.** Se o gate reprovar, o adapter do M7
  vira codigo morto — e sai do repositorio, nao fica "por seguranca".
- **Risco de expectativa.** Quem acompanhar o projeto vai perguntar por
  animacao. A resposta esta escrita aqui.

## O que NAO muda

- Nada impede polimento de FEEDBACK de combate antes do M7: particula, som,
  camera, telegrafo. Isso e apresentacao, nao pipeline de dano.
- A decisao vale para Epic Fight e para qualquer mod que reescreva deteccao de
  acerto. Better Combat cai na mesma regra.
- Se o gate aprovar, Epic Fight entra como camada do PACK — nao como fundacao.
