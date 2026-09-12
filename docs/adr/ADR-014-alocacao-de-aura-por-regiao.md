# ADR-014 — A aura tem regiões, e elas são autoritativas

- **Status:** aceita
- **Data:** 2026-09-12
- **Marco:** M4 → M5 (fundação)
- **Toca:** o modelo de Aura, e por isso exige o [ADR-009](ADR-009-modelo-de-aura-sem-stamina-de-nen.md)

## Contexto

Até aqui a aura é **um número**. Isso bastou para Ten, Ren e Zetsu, que são
estados do corpo inteiro.

O documento-fonte
[`tecnicas-canonicas.md`](../pesquisa/tecnicas-canonicas.md) mostrou que isso
não basta para mais nada. Quatro das sete técnicas que faltam **são** operações
sobre distribuição:

| Técnica | O que ela faz com a distribuição |
| --- | --- |
| **Gyo** | concentra numa região; as outras recebem menos |
| **Ko** | ~100% numa região, ~0% no resto |
| **Ken** | muito em **todas** as regiões |
| **Ryu** | **redistribui em tempo real** |

Elas não "usam" alocação como quem usa uma biblioteca. Elas são a alocação,
com políticas diferentes.

**O erro que quase aconteceu:** tratar isso como detalhe interno de Gyo. Aí Ko,
Ken e Ryu nasceriam cada uma com a própria noção de "quanto de aura está no
braço", e a primeira vez que duas se combinassem — que é literalmente o que Ryu
é — as contas discordariam.

E há um agravante: **já existe um `AuraDistribution`**, em `client/vfx/`. Ele é
cosmético, client-side, e serve à renderização. Deixá-lo virar a referência
seria entregar ao cliente a decisão de quanta aura está em cada membro — o
ADR-001 pelo avesso.

## Decisão

**A aura passa a ter uma distribuição por região, autoritativa no servidor.**

1. **Seis regiões**, as mesmas do documento-fonte: cabeça, tronco, braço
   esquerdo, braço direito, perna esquerda, perna direita. Nem mais, nem menos
   — "braços" como um só impediria Ko num punho, e mais regiões que isso não
   aparecem em lugar nenhum do cânone.

2. **A soma é sempre 1.0.** Concentrar num lugar tira de outro; é essa a troca
   que dá risco a Gyo e a Ko. Uma distribuição que não fecha deixaria de ser
   escolha e viraria bônus.

3. **A distribuição é DERIVADA das técnicas ativas**, como o teto de Output e o
   multiplicador de regeneração já são. Ela não é um campo que alguém escreve
   de fora; ela é recalculada, e por isso não sobrevive a um ponto de saída
   esquecido.

4. **O repouso é uniforme.** Sem técnica que mexa em distribuição, todas as
   regiões valem o mesmo. Isso torna "não fiz nada" um estado definido, e não
   ausência de estado.

5. **O cliente recebe, e não decide.** A distribuição viaja no delta de runtime
   para poder ser desenhada. O `AuraDistribution` de `client/vfx/` passa a ser
   **projeção** do que o servidor mandou, e não fonte.

6. **A região não é alvo de dano ainda.** A distribuição existe e é mantida,
   mas nada a consulta para calcular defesa — porque não há dano de Nen
   (#127). Ela nasce medível e sem consumidor de combate, de propósito: sem
   ela, Gyo não pode existir; com ela, Gyo existe e é visível.

## O que NAO muda

- **A aura continua sendo uma reserva única.** Regiões distribuem o *uso* da
  aura, não a armazenam separadamente. Não há seis pools; há um pool e seis
  fatias de como ele é aplicado.
- **O ADR-009** — nenhuma stamina de Nen nasce aqui, e a reserva continua sendo
  a única moeda.
- **O ADR-010 e o ADR-013.** Custo, regeneração e a escada entre os estados
  seguem iguais; distribuição é ortogonal a saldo.
- **Ten, Ren e Zetsu.** Nenhum deles mexe em distribuição, e por isso nenhum
  muda de comportamento. Os três continuam sendo estados do corpo inteiro.

## Custo assumido

- **Mais um campo derivado para esquecer de limpar.** Teto e multiplicador já
  ensinaram isso: quem liga tem de desligar, e a limpeza tem de morar num lugar
  só. A distribuição entra no mesmo `recalcularDerivados`, e não num caminho
  próprio.
- **Mais bytes por delta.** Seis floats por jogador, e a distribuição muda mais
  do que o teto — com Ryu, ela muda constantemente. Se isso pesar, o
  agrupamento por mudança que a presença já usa é o caminho, e não cortar
  campos.
- **Duas distribuições no repositório, por um tempo.** A de `client/vfx/`
  continua existindo enquanto a projeção não for ligada. Duas fontes para a
  mesma verdade é exatamente o que este projeto proíbe; aqui é transição
  declarada, com prazo: ela deixa de ter construtor próprio assim que o delta
  carregar o campo.
- **Uma alavanca que o balanceamento vai querer girar e não pode.** A soma ser
  1.0 é invariante, não número de config. Quem quiser "mais aura total" mexe na
  reserva, não na soma.

## Governanca da decisao

O ADR-009 exige aprovacao registrada dos dois desenvolvedores.

| Desenvolvedor | Papel | Aprovacao |
| --- | --- | --- |
| **@Vecolas** | Dev A — núcleo | **aprovado**, por instrução direta nesta sessão: *"use para corrigir e planejar tudo"*, sobre o documento-fonte que torna a alocação a espinha de quatro técnicas |
| **@jonex-01** | Dev B — superfície | **pendente** |

> A aprovação relatada em 2026-09-12 foi para o **ADR-013**, que estava em
> aberto naquele momento. Este ADR nasceu depois, e estender aquele relato a
> ele seria inventar consentimento sobre um texto que ninguém leu.
>
> A alocação entra no código porque o responsável pelo núcleo decidiu e porque
> ela é pré-requisito de quatro técnicas. O registro diz a verdade sobre quem
> aprovou o quê.
