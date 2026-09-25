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

## A trilha AV fechou — o que sobrou não é pergunta

Em 2026-09-22 os gates AV0–AV8 foram todos marcados como passados. **Isto não
apagou nenhuma ausência:** o que não foi verificado continua em
[`o-que-nao-provamos.md`](o-que-nao-provamos.md), e cada veredito dado continua
em [`RESPOSTAS.md`](RESPOSTAS.md) **com os arquivos que o invalidam**.

O que mudou foi o **rastreador**, e não o conhecimento. A dívida saiu da lista
de tarefas e virou régua.

### A que continua aberta

| # | O que é | Por que não fecha |
| --- | --- | --- |
| **#204** | Aura sobre mobs GeckoLib | **Dívida de implementação**, não de verificação. `GeoAuraAdapter` tem zero referências fora do próprio arquivo; `AuraLivingRenderLayer` não existe. Nenhuma sessão humana ia responder isso — não havia o que olhar |

> **#103 saiu desta lista em 2026-09-22**, em `0e073dc`. A decisão foi a saída 3
> do próprio comentário da issue: eco de corpo inteiro para *"levei dano"*, faixa
> acesa para *"ali"*. O que sobrou não é decisão, é **olho** — ninguém viu a
> diferença entre faixa e eco na tela.

### O que ficou sem olhar, e é bom saber de cor

| | |
| --- | --- |
| **B1/B2** | ninguém viu o `OFF` **depois** do conserto — e a resposta do B2 foi dada no build anterior |
| **B5** | marcado como passa **sem pack instalado**. Se o `FAST` não entrar com Iris, o sintoma é halo duplicado ou ausente |
| **O1–O3** | nenhum número de performance foi medido, e os contadores nunca foram transcritos |
| **Z3** | `gyoDoObservador` chega sempre `false` até Gyo ter camada de percepção (#126) |

---

## O buraco que a trilha não cobria — **três dos quatro fecharam**

Em 2026-09-22 esta seção dizia que **quatro** técnicas não tinham efeito visível
nenhum. `0e073dc` deu um produtor ao caminho por região, e a conta mudou:

| Técnica | Domínio | Visual | Como |
| --- | --- | --- | --- |
| Ten · Ren · Zetsu | ✅ | ✅ | modo visual próprio |
| **Gyo · Ko · Shu** | ✅ | ✅ | **redistribuem a alocação**, e a tela segue |
| **Ken** | ✅ | ✅ | **consertado em 2026-09-25** — desenha como Ren, na cor dele |

**Por que as três acenderam de uma vez.** Gyo, Ko e Shu implementam
`RedistribuiAura`; a alocação viaja no delta; e
`AuraDistribution.daAlocacao` **normaliza pela região mais concentrada**. Ou
seja: concentrar não deixa a região escolhida mais forte que `1.0` — deixa **o
resto do corpo mais fraco**. Foi essa normalização que resolveu o problema que
derrubou a primeira tentativa do ripple (*"um multiplicador com teto em `1.0`
não tem para onde subir"*).

Faltava só o jogador **apontar**, e a tecla `G` é isso.

### Por que o Ken não entrou junto, e não é esquecimento

O javadoc dele diz por escrito: *"a alocação soma 1.0 e diz **onde** a aura
está, não **quanta**: 'alto em todas' é literalmente a alocação uniforme"*. Ken
deliberadamente não toca o modelo de alocação, e essa decisão está certa.

### ⚠️ Mas o Ken não é só ausência de sinal — ele APAGA a própria aura

**Achado no levantamento de 2026-09-23.** Uma versão anterior desta seção dizia
que *"Ken desenha exatamente o que Ten desenha"*. **Está errado.** O que o
código faz é pior, e é assimétrico:

| Quem olha | Caminho | Ken ligado |
| --- | --- | --- |
| **Os outros** | `EstadoVisualDeTerceiro` — `switch` **exaustivo** | ✅ `case REN, KEN -> AuraVisualMode.REN`, na cor dourada do Ken |
| **O próprio jogador** | `ModoVisualDeTecnica` — `List.of(Zetsu, Ren, Ten)` | ❌ Ken não está na lista → `dominante()` vazio → **`OFF`** |

E `Ken.excluidas()` devolve `Set.of(Ten, Ren, Zetsu)`: ligar Ken **desliga** Ten
e Ren. `SessaoDeVfxDeAura` recebe `OFF`, faz `alvo = 0.0F`, e **a aura que
estava acesa se apaga**.

**Em jogo:** o jogador aperta Ken, paga o dreno mais caro depois de Ren, vê a
própria aura sumir — e todo mundo em volta continua vendo-o brilhar em dourado,
com áudio (`DetectorDeAtivacaoDeTen` trata Ken como aura liberada). O HUD mostra
o ícone `MURALHA` na cor certa, e é o único lugar onde ele sabe que ligou.

**É o erro "duas fontes para a mesma verdade", e a ironia está nos arquivos.**
`EstadoVisualDeTerceiro` carrega um comentário comemorando ter pego este caso —
*"quando KEN entrou em SinalDeAura, [um switch não-exaustivo] teria engolido o
caso novo... sem erro nenhum"*. `ModoVisualDeTecnica` usa uma `List`, com a
decisão justificada de que *"técnica desconhecida não acende nada"* para
datapack não inventar visual. Correta para técnica de terceiro; **engoliu uma
técnica de primeira parte em silêncio.**

`VfxDeAuraLigadoTest` cobre Ten, Ren, Zetsu, vazio e `null`. **Não cobre Ken** —
e nenhum portão liga as duas tabelas.

### O que isso muda no D4

Deixou de ser só *"que sinal o Ken ganha"*. São duas coisas, e a primeira não é
direção visual nenhuma:

1. **Fechar o buraco** — as duas tabelas têm de concordar, com portão que morde
   quando uma técnica registrada não aparece nas duas. Isso é defeito, e não
   gosto.
2. **Decidir o sinal próprio** — se Ken desenha como Ren na cor dele (o que os
   outros já veem) ou ganha tratamento próprio de cor, espessura ou borda. **Aí
   sim é direção visual, e não está autorizada.**

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

## AV4 — Ren (#193) · ✅ **FECHADO em 2026-09-22**

Decisão do dono do projeto: o que restava é **tuning fino**, e *"se funciona da
melhor maneira possível outros jogadores é que terão de determinar — e para isso
precisamos antes prosseguir com as implementações"*. Marcado como passado, **sem
dívida registrada para depois**.

| # | O que era | Desfecho |
| --- | --- | --- |
| R1 | anel e detritos em declive, escada, água, folhagem | ✅ tuning — dispensado |
| **R2** | **nenhum bloco quebrado** | ✅ **observado** (*"não deformam o mundo e não quebram nenhum bloco"*) **+ portão** |
| **R3** | **zero `ItemEntity` órfão** | ✅ **observado + portão** |
| R4 | escada de espessuras na aproximação | ✅ tuning — dispensado |
| R5 | estalo na emenda do loop de áudio | ✅ tuning — dispensado |
| R6 | impulso de câmera em 20 ativações | ✅ conforto — mais jogadores decidem |
| **R7** | **observador não recebe impulso de câmera** | ✅ **construção + portão** |
| R8 | áudio de Ten ouvido por duas pessoas | ✅ polimento — dispensado |
| R9–R13 | primeira pessoa, overshoot, linguagem, skin, estado preso | ✅ respondidas em playtest |

### Três não foram carimbadas — viraram régua

R2, R3 e R7 não perguntam *"está bonito"*. Perguntam se o efeito **destrói o
mundo do jogador**, se ele **vaza entidade**, e se a ação de um jogador **mexe a
câmera de outro**. As três falham em silêncio.

Elas não viraram dívida **nem foram dispensadas**: viraram
[`OMundoNaoMudaTest`](../../src/test/java/com/darkcontinent/nenfoundation/client/vfx/OMundoNaoMudaTest.java),
com seis verificações. O portão foi alimentado com um `destroyBlock` deliberado
na sondagem de chão e **reprovou**, nomeando arquivo e chamada.

É o único desfecho que respeita as duas coisas ao mesmo tempo: nada fica pendente,
e nada fica carimbado sem prova.

> **O que continua sem prova, e está declarado:** o portão lê texto. `BlockState`
> comparado antes e depois de cinco minutos reais e a contagem de entidades
> continuam sendo do olho — e o olho já disse que passa. O que a régua garante é
> que a construção **não se desfaz em silêncio**.

---

## AV5 — bloom (#196, #198)

| # | O que responder | Situação |
| --- | --- | --- |
| B1 | Com `bloom off`, a aura se lê? | ❌ **JÁ REPROVOU** (AV3, 2026-09-22) — é o defeito aberto em #196 |
| B2 | `OFF` × `FAST` × `HIGH` do **mesmo frame** são "menos do mesmo jogo", e não três jogos? | nunca comparados |
| ~~B3~~ | Vidro, folhas e água ocluem o halo? | ✅ **RESPONDIDA POR CÓDIGO** — folha e vidro comum **ocluem** (são cutout, entraram na máscara junto com a grama); água, gelo e vidro tingido **não** (translúcidos). Confirmar de olho é opcional |
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
| ~~D1~~ | ~~O ripple é **corpo inteiro** ou **por região**?~~ ✅ **DECIDIDA em 2026-09-22** (`0e073dc`): os dois. O servidor manda a faixa atingida, ela acende com ganho cheio e o resto do corpo recebe 35% dele | #103 |
| ~~D2~~ | ~~Os **seis fatores por região** nunca foram vistos diferentes de `1.0`~~ ✅ **RESOLVIDA em 2026-09-22**: Gyo, Ko e Shu movem a alocação, e a tecla `G` deixa o jogador escolher onde. Deixou de ser config órfã. **Continua sem ninguém ter olhado** | Gyo/Ko/Shu |
| D3 | O `OFF` ganha compensação própria, ou o ADR-016 muda de promessa? | #196 |
| **D4** | O **Ken** ganha sinal visual próprio, e de que natureza? Ele não redistribui aura por construção, então distribuição não serve. ⚠️ **Precedido por um DEFEITO, e não por uma decisão:** Ken ligado apaga a aura do próprio jogador enquanto os outros o veem aceso — as duas tabelas de modo visual discordam. Ver a seção acima | Ken |

> **D1 e D2 ficam riscadas, e não apagadas.** Apagar uma decisão tomada faz a
> próxima pessoa reabrir a mesma discussão sem saber que já houve uma.

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

## A ordem

Está no **quadro único**, no topo deste arquivo. Esta seção existia para
recomendar o AV4 primeiro; o AV4 fechou em 2026-09-22.
