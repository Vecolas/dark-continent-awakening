# As dívidas que precisam de uma pessoa

Tudo aqui é **julgamento humano em jogo**. Nada disto vira verde sozinho, e
nenhum portão vai fechar por baixo.

> **O que NÃO está aqui:** dívidas que se pagam com código, com medição
> automática ou com decisão de arquitetura. Estas exigem alguém olhando,
> ouvindo ou contando **na tela**.

**Fonte de verdade das ausências:**
[`o-que-nao-provamos.md`](o-que-nao-provamos.md). Este arquivo é a fatia dela
que **se responde jogando**, organizada por quem deve perguntar.

---

## Primeiro: o que o Ren já jogado cobre, e o que não cobre

O Ren foi jogado várias vezes fora dos gates. Isso vale — mas vale **para uma
parte**, e a parte é decidível por um fato do dado, não por opinião:

```
Ten:  colunas 0, anel 0.0,  detritos 0      <- tudo zero, e ESCRITO no perfil
Ren:  colunas 6, anel 0.65, detritos 8      <- toca o chão
```

**Ren é a MESMA shell de Ten**, com a borda aberta e o fluxo rápido — o próprio
`ren.json` diz isso na primeira linha. Mesma geometria, mesmas âncoras, mesmo
caminho de desenho.

### ✅ Carrega de Ten para Ren — é maquinário, não perfil

Aderência em movimento, modelo slim × default, âncora dos filamentos, primeira
pessoa, oclusão por bloco sólido, série de LOD por distância, mecanismo da borda
com armadura. **Estas não precisam ser reperguntadas para Ren.**

Estão em [`RESPOSTAS.md`](RESPOSTAS.md) com invalidador declarado; se o código
de que dependem mudar, o build reprova e elas voltam a ser perguntadas.

### ❌ NÃO carrega — Ten literalmente não tem isso

Tudo que encosta no chão, mais o que é específico da intensidade de Ren. **É
exatamente o que o AV4 existe para cobrar**, e nenhuma quantidade de playtest
casual responde, porque quase tudo exige *medir antes e depois*, e não olhar.

---

## AV4 — Ren (#193) · **o próximo gate**

| # | O que responder | Como | Playtest casual cobre? |
| --- | --- | --- | --- |
| R1 | O anel e os detritos tocam o chão **em declive, escada, dentro d'água, em folhagem e sobre bloco não-cúbico**? | ir até cada terreno com Ren ligado | ❌ exige ir aos terrenos de propósito |
| R2 | **Nenhum bloco quebrado:** `BlockState` na área do anel, antes e depois de **5 min** de Ren contínuo sobre terra, pedra, areia, grama alta e água rasa | comparar antes/depois | ❌ exige medir, não olhar |
| R3 | **Contagem de entidades** antes e depois: zero `ItemEntity` novo, zero órfã | `/data` ou F3 | ❌ idem |
| R4 | A **escada de espessuras** é invisível durante a aproximação? | andar de 40 b até 2 b devagar | ⚠️ talvez, se alguém olhou para isso |
| R5 | O **loop de áudio de Ren** estala na emenda? | Ren sustentado, ouvindo | ❌ exige escutar procurando |
| R6 | O **impulso de câmera** incomoda depois de **20 ativações seguidas**? | ligar Ren 20 vezes | ❌ exige a repetição |
| R7 | **Observadores não recebem impulso de câmera** | segundo cliente olhando | ❌ exige 2º cliente |
| R8 | O **áudio de Ten** — duas pessoas já o ouviram? | dois clientes | ❌ |
| ~~R9~~ | Ren não bloqueia a visão em primeira pessoa | — | ✅ **RESPONDIDA** (playtest) |
| ~~R10~~ | A transição TEN→REN tem overshoot visível | — | ✅ **RESPONDIDA** (playtest) |
| ~~R11~~ | Ren é mais intenso na mesma linguagem, sem virar raio elétrico | — | ✅ **RESPONDIDA** (playtest) |
| ~~R12~~ | A skin continua legível sob Ren | — | ✅ **RESPONDIDA** (playtest) |
| ~~R13~~ | Relog, morte e dimensão não deixam anel, coluna, detrito ou loop de áudio presos | — | ✅ **RESPONDIDA** (playtest) |
| R14 | Log do dedicado limpo | meu | — |

> **O R13 estava certo em ser perguntado, e a resposta foi melhor que um
> "passa".** A pergunta tinha mudado de significado sem mudar de nome: o AV0 já
> respondera *"morte e troca de dimensão desativam tudo"* — mas **para Ten**,
> que tem anel, coluna, detrito e loop de áudio todos em **zero**. Nenhuma das
> quatro existia para ficar presa.
>
> Resposta: *"o anel não fica preso"*. E ao procurar o porquê, a razão é
> **estrutural**: anel, colunas e detritos não guardam estado — são desenhados
> por quadro a partir do `AuraVisualState` vivo, atrás da mesma guarda. Não há
> objeto com vida própria para vazar. O loop de áudio é o único com vida real, e
> o `ZumbidoDeRen` já cobre os quatro caminhos por construção.
>
> **O erro nº 3 do `CLAUDE.md` não se aplica aqui**, e agora sabemos por quê —
> ele fala de coisas que *seguram* estado. Fica sem prova apenas que alguém
> **ouviu** o loop parar; isso é do AV4.

**R1 a R8 são o gate.** É por isso que "já testei Ren" não fecha o AV4: o que
sobra é justamente o que exige montagem deliberada.

> **Duas coisas no corpo da #193 estão obsoletas.** Ela diz que o ripple está
> bloqueado por #127 (fechou, e o ripple existe) e que "bloom ainda não existe"
> (existe desde o AV5). Corrigir ao abrir o gate.

---

## AV5 — bloom (#196, #198)

| # | O que responder | Situação |
| --- | --- | --- |
| B1 | Com `bloom off`, a aura se lê? | ❌ **JÁ REPROVOU** (AV3, 2026-09-22) — é o defeito aberto em #196 |
| B2 | `OFF` × `FAST` × `HIGH` do **mesmo frame** são "menos do mesmo jogo", e não três jogos? | nunca comparados |
| B3 | **Vidro, folhas e água** ocluem o halo? | nunca olhado — só grama foi, e por acidente |
| B4 | O halo respeita parede **na tela**? | hoje é afirmação de construção |
| B5 | Com um **shader pack** instalado, a detecção cai para `FAST`? | nunca exercitada com pack |
| B6 | Os contadores de alvo **criado/liberado** batem depois de **dez resizes** de janela? | nunca lidos |
| B7 | O passe de brilho **derrubou o cliente** numa GPU em 21/09 — voltou a acontecer? | conferir |

---

## AV6 — Zetsu (#201)

| # | O que responder |
| --- | --- |
| Z1 | O **pulso de supressão** nunca foi visto — ele aparece? |
| Z2 | O **modo permissivo** de visibilidade nunca foi usado em jogo |
| Z3 | A coluna **observador** do resolvedor nunca foi exercitada com duas pessoas |
| Z4 | "O cliente de B **não recebe** o dado de A" — não é verificável por teste de unidade; precisa de dois clientes e de olhar o tráfego |

---

## AV7 — dois clientes, armadura e poses (#205)

| # | O que responder |
| --- | --- |
| P1 | **Nenhuma das nove poses** foi vista: nadar, rastejar, dormir, montar, arco, besta, escudo, elytra, queda |
| P2 | **Capa e elytra não recebem tratamento nenhum** — isso é a entrega declarada; confirmar que o resultado é aceitável e não quebrado |
| P3 | O **`GeoAuraAdapter` nunca desenhou nada** — aura sobre mob GeckoLib |

---

## AV8 — orçamento (#206, #207, #209)

| # | O que responder |
| --- | --- |
| O1 | **Nenhum número de performance foi medido.** Os quatro cenários: 10 em Ren a 16 b, 20 em Ten a 32 b, **zero aura = custo zero**, 10 min de Ren medindo memória |
| O2 | **Tick time do dedicado** com 10 em Ren comparado a 10 parados |
| O3 | Os **quatro contadores** do `F6` — nunca transcritos, em dois gates seguidos. Esta issue passa a ser o primeiro número escrito |
| O4 | A **distância máxima** nunca foi girada em jogo |
| O5 | A matriz de renderização: vanilla, Embeddium, Iris com e sem pack |

---

## Decisões, e não observações

Estas não se respondem olhando — elas se **decidem**.

| # | A decisão | Onde |
| --- | --- | --- |
| D1 | O ripple é **corpo inteiro** (entregue) ou **por região** (pedido)? O objetivo declarado da issue não foi alcançado: informa que levou dano, não onde | #103 |
| D2 | Os **seis fatores por região** nunca foram vistos diferentes de `1.0` em jogo. O caminho existe, é testado, e **não tem consumidor visível** — config órfã em forma de campo | Gyo/Ko/Ryu |
| D3 | O `OFF` ganha compensação própria, ou o ADR-016 muda de promessa? | #196 |

---

## Fora da trilha AV

Estas são muitas e de outra natureza — **"ninguém viu X na tela"** para o
framework de inimigos e a World Tree. Agrupadas para não afogar a lista acima;
a fonte continua sendo [`o-que-nao-provamos.md`](o-que-nao-provamos.md).

| Grupo | O que falta ver | Quantas |
| --- | --- | --- |
| **Corpos e silhuetas** | Great Stamp, Frog-In-Waiting, Foxbear nascendo, a troca de silhueta do macaco, a ave voando, o sapo enterrado | 6 |
| **Comportamento nunca observado** | a interrupção acontecendo, a carga perseguindo alvo móvel, a manada nascendo, o bando fora da arena, o peixe nadando até a isca, o agarrão prendendo um **jogador** | 6 |
| **Sobrevivência a save/restart** | o ninho atravessando restart de verdade, o agarrão sobrevivendo ao save, o veredito do Kiriko sobrevivendo ao save | 3 |
| **Disfarce** | o Man-faced Ape nunca enganou um jogador, nem ninguém na tela | 2 |
| **Áudio** | os mobs continuam mudos, o Great Stamp é mudo | 2 |
| **Greed Island** | as sete criaturas nunca foram vistas; nenhuma nasce naturalmente | 2 |
| **Chimera** | a fundação natural não foi observada em servidor real; a intenção de Nen não ativa nada; a porta de aura continua inerte dos dois lados | 3 |
| **World Tree** | nenhum bloco da copa foi visto em jogo; "madeira nua" ≠ "madeira visível"; o custo de luz das folhas nunca foi medido | 3 |
| **Multiplayer de inimigos** | a matriz EN14 com 2 e 4 jogadores reais; Gyo específico por observador | 2 |

---

## A ordem que eu recomendaria

1. **AV4**, porque é o gate aberto e porque `R1`–`R3` (o mundo intacto) são o
   tipo de defeito que não aparece em teste e custa caro depois.
2. **O `OFF` do AV5** (`D3` + `B1`), porque é meia hora de código e porque todo
   julgamento visual feito antes dele roda com um nível quebrado por baixo.
3. **Os contadores** (`O3`), que já falharam duas vezes e custam quatro linhas
   de `F6`.

O resto segue a ordem dos gates.
