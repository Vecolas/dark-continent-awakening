# ADR-013 — O saldo negativo vale para quem LIBERA aura

- **Status:** aceita
- **Data:** 2026-09-12
- **Marco:** M4 — Tecnicas fundamentais
- **Emenda:** estreita o item 6 do [ADR-010](ADR-010-regeneracao-por-estado-de-nen.md)

## Contexto

O item 6 do ADR-010 diz:

> **USAR NEN CUSTA AURA, e o saldo de um estado sustentado e NEGATIVO.**

Ele foi escrito para impedir uma coisa concreta e real: um estado que se paga
vira o estado obviamente sempre-ligado, e o jogo perde a escolha. Isso nao da
erro — da um jogo pior, devagar.

**Aplicado a todos os estados, ele produziu um absurdo.** Zetsu e o estado de
descanso: no canone e assim que se recupera aura. Exigir que ele tambem drene e
exigir que o descanso canse. E Ten, que no material e a manutencao quase
automatica de quem tem experiencia, passava a sangrar reserva o tempo todo.

A contradicao nao era teorica. Ela estava escrita em dois lugares do
repositorio ao mesmo tempo:

- `TenTest.zetsuEBaratoMasNaoGratuito` **exigia** saldo positivo para Zetsu;
- `TenTest.tenTemSaldoNegativo` **exigia** saldo negativo para Ten;
- e a conferencia de balanceamento, escrita no mesmo dia, reclamava de Zetsu.

Foram escritos pelo mesmo agente, na mesma sessao, sem que nenhum portao
percebesse — porque cada um media uma tecnica, e ninguem olhou os tres juntos.

## Decisao

**O item 6 do ADR-010 vale para os estados que LIBERAM aura.**

Um estado que nao libera nada nao tem como "se pagar em aura": ele nao gasta
aura para existir. O preco dele e outro.

| Estado | Saldo | O preco real |
| --- | --- | --- |
| **Zetsu** | positivo, o maior | fica **sem defesa de Nen** |
| **Ten** | positivo, menor que ficar parado | nao libera acima do teto de repouso |
| **Ren** | **negativo** | a aura, e e o unico que paga assim |

A escada e a regra, e nao cada sinal isolado:

> **Zetsu recupera mais que ficar parado, que recupera mais que Ten, que
> recupera mais que Ren.**

Ficar parado no meio dela e o que da significado a escolha: Ten protege e por
isso recupera menos que nao fazer nada; Zetsu recupera mais porque abre mao da
protecao inteira.

O portao `TenTest.aEscadaDeRecuperacao` guarda a ordem, e nao os numeros. Cada
degrau foi alimentado com defeito e reprova.

## Custo assumido

- **Zetsu e Ten sao mais baratos do que deveriam ser, hoje.** Os precos dos
  dois — vulnerabilidade e teto — dependem de uma camada de dano de Nen que
  **nao existe** (`nen/combat/` tem so o `package-info`; issue #127). Ate la,
  Zetsu e recuperacao acelerada sem desvantagem nenhuma. Isto e divida
  declarada, e nao descuido: esta em
  [o-que-nao-provamos.md](../testing/o-que-nao-provamos.md).

- **A regra ficou mais dificil de verificar.** "Saldo negativo" era uma conta
  por tecnica. "Quem libera paga" exige saber o que cada tecnica faz, e a
  resposta nao esta num campo — esta no desenho. A conferencia de
  balanceamento passou a carregar uma lista de quem libera, e essa lista pode
  ficar desatualizada quando nascer a tecnica seguinte.

- **O ADR-010 continua valendo no resto.** Multiplicador por estado, produto
  com teto e neutralidade sem tecnica ativa nao mudam.

## O que NAO muda

- **O item 6 continua existindo, e continua sendo a razao de tudo isto.** Ele
  nao foi revogado: foi estreitado. Um estado que libera aura e se paga
  continua proibido, pelo mesmo motivo de sempre -- vira o estado obviamente
  sempre-ligado, e a escolha some.
- **O resto do ADR-010.** Multiplicador por estado, produto com teto,
  neutralidade sem tecnica ativa e a regra de que o teto do produto e config
  seguem valendo sem alteracao.
- **Nenhum numero foi congelado aqui.** A escada e a regra; os valores
  continuam em config, para serem girados numa sessao de balanceamento. O que
  nao se pode girar e a ORDEM entre eles.
- **O ADR-009** e a exigencia de decisao conjunta sobre o modelo de Aura.

## Governanca da decisao

O ADR-009 exige aprovacao registrada dos dois desenvolvedores.

| Desenvolvedor | Papel | Aprovacao |
| --- | --- | --- |
| **@Vecolas** | Dev A — nucleo | **aprovado**, por instrucao direta nesta sessao: *"o zetsu tem que regenerar as auras mesmo, e assim que funciona no anime"* e *"o ten regenera um pouco mantendo a protecao"* |
| **@jonex-01** | Dev B — superficie | **aprovado**, relatado por @Vecolas em 2026-09-12 |

> A aprovacao do Dev B foi **relatada por @Vecolas**, e nao dada diretamente ao
> agente -- do mesmo jeito que no ADR-010. Fica registrado assim, e nao como
> aprovacao direta, porque a diferenca importa se algum dia alguem precisar
> saber quem leu o que.
