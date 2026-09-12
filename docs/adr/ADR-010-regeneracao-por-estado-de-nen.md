# ADR-010 — A regeneracao de Aura depende do estado de Nen ativo

## Contexto

O M4 pede Ten, e Ten no cânone impede a aura de vazar: ele a segura em volta do
corpo, e conserva melhor que o vazamento descontrolado de quem nao treinou.

Ao implementar, duas entregas da issue #86 nao tinham em que se apoiar:

1. **"a aura vaza menos com Ten"** — o motor nao tem vazamento. `MotorDeAura`
   so regenera; nao existe perda passiva para reduzir.
2. **"defesa passiva contra aura hostil"** — nao existe dano de Nen.
   `nen/combat/` tem apenas `package-info.java`, e nao ha nenhum handler de dano
   no projeto.

A segunda espera um sistema de combate e nao e assunto deste ADR. A primeira
obrigava a escolher entre tres caminhos:

- **inventar vazamento** — acrescentar perda passiva de aura ao modelo;
- **ligar Ten a regeneracao** — quem esta em Ten recupera melhor;
- **entregar Ten sem efeito** — uma tecnica que cobra aura e nao faz nada.

O terceiro e um contrassenso para quem for testar. O primeiro e uma mecanica
nova, e mais cara de reverter.

O segundo ja esta prescrito pela arquitetura do proprio projeto. A skill
`nen-architecture`, na secao *Aura regeneration*, diz:

> Make regeneration a gameplay abstraction based on: **active stance**, combat
> state, mastery, sleep/rest, **Zetsu**, buffs/debuffs, configuration.

Ou seja: regeneracao variando por estado ativo nao e invencao — e o modelo que
a arquitetura manda usar, e ele ja prevê Zetsu, que chega na issue #88 pedindo
"recuperacao de aura melhor durante Zetsu". A mesma alavanca serve as duas.

## Decisao

**A taxa de regeneracao de Aura passa a depender do estado de Nen ativo.**

Em regras concretas:

1. A regeneracao base continua vindo de `aura.regeneracaoPorSegundo`, na
   config, exatamente como hoje.

2. Cada tecnica ativa pode declarar um **multiplicador de regeneracao**. O
   multiplicador efetivo do jogador e o **produto** dos multiplicadores das
   tecnicas ativas. Sem tecnica ativa, o multiplicador e `1.0` e nada muda.

3. O multiplicador e **derivado do conjunto de tecnicas ativas**, com
   recalculo explicito a cada ativacao e desativacao. Ele nunca e escrito por
   uma tecnica diretamente: duas tecnicas escrevendo no mesmo campo e a forma
   mais barata de uma esquecer de limpar a sua parte.

4. Os multiplicadores sao **numeros de balanceamento** e moram em config, uma
   chave por tecnica, e **nao no codigo**.

5. **O produto tem TETO, e o teto e config.** O multiplicador efetivo e
   limitado por `aura.multiplicadorMaximoDeRegeneracao` antes de ser aplicado.
   Sem teto, tres tecnicas acumulaveis produziriam um numero absurdo -- e hoje
   o que impede isso e a exclusao entre as fundamentais, que e outra regra e
   pode mudar. Teto no modelo nao depende de nenhuma outra regra continuar
   valendo.

6. **USAR NEN CUSTA AURA, e o saldo de um estado sustentado e NEGATIVO.** Uma
   tecnica cuja manutencao renda mais do que custa vira o estado obviamente
   sempre-ligado, e o jogo perde a escolha. A regeneracao melhorada REDUZ o
   custo de manter; ela nao o paga. Ha portao exigindo isso dos numeros
   distribuidos -- ver o custo assumido.

   > **EMENDADO pelo [ADR-013](ADR-013-saldo-so-para-quem-libera-aura.md) em
   > 2026-09-12:** esta regra passou a valer para os estados que **liberam**
   > aura. Zetsu e Ten recuperam de proposito -- os precos deles sao a
   > vulnerabilidade e o teto de Output, e nao a reserva. Ren continua sendo o
   > unico que paga em aura.
7. **Ten** e a primeira tecnica a usar isto: ela cobra manutencao continua e
   melhora a regeneracao. O saldo liquido e ajustavel em config, o que permite
   que Ten seja quase gratuito para quem treinou -- como o cânone descreve --
   sem que "quase gratuito" vire uma constante escondida.

## O que NAO muda

- **Continua havendo UMA barra visivel de Aura.** Nenhuma barra nova, nenhum
  recurso novo. O ADR-009 permanece valido no que ele decidiu: nao ha stamina
  de Nen separada, e folego fisico continua sendo vida, fome e exhaustion do
  vanilla.
- **As tres grandezas do modelo continuam as mesmas**: capacidade, reserva e
  output. A regeneracao ja existia; o que muda e de onde sai a taxa.
- **Nada disto e persistido.** Multiplicador de regeneracao e estado de runtime
  e morre em morte, logout, troca de dimensao e restart, como toda tecnica
  ativa. O ADR-002 nao e tocado.
- **O servidor continua sendo a autoridade.** O cliente nao envia, nao calcula e
  nao adivinha taxa de regeneracao. O ADR-001 nao e tocado.
- **O protocolo nao muda por causa desta decisao.** O delta ja carrega a aura
  resultante; a taxa nunca viaja.

## Custo assumido

- **A regeneracao deixa de ser um numero unico e legivel de fora.** Antes,
  "quanto eu recupero por segundo" tinha uma resposta so, na config. Agora
  depende do que esta ativo, e diagnosticar exige olhar o estado do jogador
  junto da config. Isto e mitigado pelo contador em modo dev, que ja existe.

- **O empilhamento foi previsto e limitado, e isso custa um numero a mais.** O
  teto do item 5 existe porque a alternativa era confiar na exclusao entre
  tecnicas -- uma regra de OUTRO lugar, que pode mudar sem ninguem lembrar
  desta. O preco e mais uma chave de config para alguem entender, e um teto que
  pode morder alguma combinacao legitima no futuro. Foi escolhido conscientemente:
  um teto que incomoda e melhor que um numero que explode.

- **O saldo negativo do item 6 contraria o cânone em parte.** O material
  descreve Ten como quase automatico para quem tem experiencia, e saldo sempre
  negativo impede sustentar Ten indefinidamente. A decisao do responsavel foi
  explicita: usar Nen gasta, e nenhum estado vira permanente. Quando houver
  maestria (M6), ela pode reduzir o custo ate quase zero -- que e o caminho pelo
  qual o cânone e o saldo negativo se reconciliam, sem que "quase gratuito"
  nasca como constante.

- **Uma alavanca a mais para o futuro medir.** Toda formula que envolva
  regeneracao passa a ter mais uma entrada.

## Governanca da decisao

O ADR-009 exige aprovacao registrada dos dois desenvolvedores, identificando
ambos e indicando a aprovacao de cada um.

| Desenvolvedor | Papel | Aprovacao |
| --- | --- | --- |
| **@Vecolas** | Dev A — nucleo | **aprovado**, por instrucao direta ao agente nesta sessao |
| **@jonex-01** | Dev B — superficie | **aprovado**, relatado por @Vecolas nesta sessao |

**A PROCEDENCIA DE CADA APROVACAO ESTA ESCRITA, e nao e a mesma.** A do Dev A
veio direto; a do Dev B chegou relatada pelo Dev A. O agente nao assinou por
ninguem -- ele registrou o que foi dito e por quem. Se a segunda aprovacao nao
corresponder ao que @jonex-01 entendeu, esta linha e o lugar de corrigir.

**O responsavel acrescentou duas condicoes a aprovacao**, e elas viraram os
itens 5 e 6 da decisao: teto para o empilhamento, e saldo negativo para que
nenhum estado vire permanente. Nao sao observacoes -- sao parte do que foi
aprovado.
