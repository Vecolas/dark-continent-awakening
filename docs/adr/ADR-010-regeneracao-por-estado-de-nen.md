# ADR-010 — A regeneracao de Aura depende do estado de Nen ativo

> **ATENCAO — ESTA DECISAO AINDA NAO ESTA COMPLETA.**
>
> O [ADR-009](ADR-009-modelo-de-aura-sem-stamina-de-nen.md) exige a aprovacao
> registrada dos **dois** desenvolvedores para alterar o modelo de Aura, e diz
> com todas as letras que *"um agente nao pode presumir nem assinar a aprovacao
> do outro"*.
>
> **Falta a aprovacao de @jonex-01.** Ver [Governanca](#governanca-da-decisao).

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

5. **Ten** e a primeira tecnica a usar isto: ela cobra manutencao continua e
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

- **Abre a porta para empilhamento.** Com produto de multiplicadores, tres
  tecnicas somando poderiam produzir um numero absurdo. Hoje isso nao acontece
  porque as tecnicas fundamentais se excluem entre si, mas a excludencia e que
  segura -- nao o modelo. Quando existir tecnica acumulavel, este ADR precisa
  ser revisitado com um teto.

- **Ten fica "bom demais" se o numero errar.** Uma regeneracao alta demais com
  custo baixo demais transforma Ten no estado permanente obvio, e o jogo perde a
  escolha. O risco e de balanceamento, nao de arquitetura, e a mitigacao e a
  regua: o contador de aura recuperada em modo dev ja mede isto.

- **Uma alavanca a mais para o futuro medir.** Toda formula que envolva
  regeneracao passa a ter mais uma entrada.

## Governanca da decisao

O ADR-009 exige aprovacao registrada dos dois desenvolvedores, identificando
ambos e indicando a aprovacao de cada um.

| Desenvolvedor | Papel | Aprovacao |
| --- | --- | --- |
| **@Vecolas** | Dev A — nucleo | **aprovado** nesta sessao, por instrucao direta ao agente |
| **@jonex-01** | Dev B — superficie | **PENDENTE** |

**O agente nao assina pelo segundo desenvolvedor.** O ADR-009 proibe isso
explicitamente, e a proibicao existe justamente para o caso em que so uma das
duas pessoas esta presente -- que e este.

**Consequencia pratica, escrita para nao ser esquecida:** a implementacao que
acompanha este ADR entrou na `main` sob a direcao do Dev A, com a segunda
aprovacao em aberto. Se @jonex-01 discordar, o que se reverte e o multiplicador
de regeneracao e o efeito de Ten -- nao o registro de tecnicas, que independe
desta decisao.

**Bloqueio, com nome:** @jonex-01 precisa registrar aprovacao ou objecao neste
arquivo. Enquanto a linha acima disser PENDENTE, a decisao esta pela metade.
