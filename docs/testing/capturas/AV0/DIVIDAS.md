# AV0 — as dívidas, e de quem é cada uma (#169)

Atualizado em **2026-09-22**, contra `ffb7e47`.

**As treze perguntas de julgamento foram respondidas, e todas passaram.** Com
as catorze imagens **aposentadas em 2026-09-22** (ver D1) e pular/atacar
julgados (D4), a montagem conferida contra o log (D2), o E1 fechado em produção com jogador
dentro (D5) e morte/troca de dimensão convertidas em portão (D6), **o AV0 não
deve mais nada. O #169 FECHOU em 2026-09-22.**

> **Por que separar por dono.** A lista de pendências de um gate mistura o que
> só um humano no jogo responde com o que uma régua responde melhor. Misturadas,
> as duas esperam uma pela outra, e a que o código podia ter fechado fica
> esperando alguém abrir o cliente.

---

## 1. Só você responde

### D1 · As 14 imagens — **APOSENTADA em 2026-09-22**

**Decisão do dono do projeto:** a inspeção visual ao vivo foi suficiente, e o
AV0 **não exigirá mais as catorze capturas**.

```
ten_dia          ten_noite         ten_caverna        ← dispensadas
ten_slim         ten_overlay_skin  ten_armadura       ← dispensadas
ten_correndo     ten_agachado      ten_nadando        ← dispensadas
ten_2b   ten_5b  ten_10b  ten_20b  ten_40b            ← dispensadas
```

#### O que esta decisão custa

Está escrito porque decisão sem custo declarado é a que ninguém consegue revisar
depois — e porque o custo é real, ainda que aceito:

- **o julgamento deixa de ser revisável.** Não há o que reabrir, comparar com a
  referência de arte, nem mostrar à outra frente. Quem discordar em dezembro não
  tem contra o que discordar;
- **não há linha de base para o AV1.** O AV1 acrescenta shader próprio à mesma
  shell. Sem o "antes", a única forma de saber o que o shader mudou é a memória
  de quem viu os dois;
- **uma regressão visual passa calada.** Se a shell começar a descolar em três
  meses, nada diz que um dia ela não descolava — e este é o tipo de defeito que
  o repositório inteiro está organizado para não deixar acontecer em silêncio.

#### O que passa a carregar a prova

Com a imagem fora, **sobra uma única fonte de procedência: a declaração da
montagem (D2)**. Ela deixa de ser um item entre vários e passa a ser o que
sustenta o gate — e a folha `PERGUNTAS.md`, com os veredictos datados, passa a
ser a evidência inteira do AV0.

> **Isto não se estende aos outros gates por tabela.** AV1 a AV8 continuam
> pedindo captura pelo texto das próprias issues. Aposentar a evidência visual
> de um gate cujo objeto É visual é uma decisão por gate, e esta valeu para o
> AV0 — onde o que se mede é aderência, que o olho resolve em movimento melhor
> do que um PNG parado resolve.

- [x] **dispensadas** — decisão de 2026-09-22

---

### D2 · A montagem — **DECLARADA em 2026-09-22**

**Servidor dedicado, dois jogadores.** Declarado pelo dono do projeto e
**confirmado no log**, que foi localizado em `run/server/logs/latest.log`:

| Fato | Evidência no log |
| --- | --- |
| dedicado, não singleplayer | `--launchTarget forgeserverdev`, processo próprio |
| **dois** jogadores | `Kurapika logged in ... entity id 16`, `Gon logged in ... entity id 36` |
| sessão real, não um join de teste | 19:17 → 20:10 de 2026-09-21 — **51 minutos** |
| o mod estava ativo | `Nen Foundation registrado. Protocolo de rede v9` |
| o protocolo foi exercitado | `C2S: 15 pedidos admitidos, 0 cortados antes da fila` |

**O bloco D é válido:** a série 2b→40b foi tirada com um segundo jogador de
verdade, então o LOD foi medido contra outra entidade e não contra a própria
câmera.

E a varredura daquele log não achou **nada**: zero `NoClassDefFoundError`, zero
`ClassNotFoundException`, zero menção a `net.minecraft.client` ou
`nenfoundation.client`, zero `[ERROR]`, `[FATAL]`, `Exception` ou `Caused by` —
em 51 minutos de dois jogadores.

- [x] **montagem declarada e conferida contra o log**

---

### D3 · A comparação na mesma sessão — **DISSOLVIDA pela aposentadoria do D1**

Esta dívida existia porque a §6.3 exige a comparação com a referência de arte
**na mesma sessão** da captura, e os veredictos eram de 21/09 enquanto as
imagens sairiam depois.

**Sem imagem, não há duas sessões para conciliar.** O julgamento ao vivo de
2026-09-21 é a evidência, e ele foi dado na própria sessão em que o jogo estava
aberto — que é o que a regra protege.

> **O que sobrou dela, e não é nada:** a regra existia porque *"olho humano
> cansa, e comparação de memória dias depois não vale"*. Isso continua verdade.
> O julgamento vale para `5a9182b`, e o que ele afirma é o que foi visto naquele
> dia — não uma propriedade permanente da shell.

- [x] **não se aplica** — sem captura, não há defasagem entre veredicto e imagem

---

### D4 · Pular e atacar — **RESPONDIDO em 2026-09-22**

O critério de aceite lista cinco movimentos; três tinham captura própria e dois
não. Agora os cinco estão julgados um a um:

- [x] **pular: acompanha** — *"funciona, não descola do personagem"*
- [x] **atacar: acompanha** — idem

Sai o "PASSA por arrasto": o bloco C deixa de depender de um veredicto dado ao
conjunto e passa a ter os cinco movimentos respondidos.

---

### D5 · O E1 — **FECHADO em 2026-09-22**

O cruzamento que faltava aconteceu: **instância de produção, com jogador
dentro.** 23 minutos, `Gon` e `Kurapika`, 12:49→13:12.

| Caminho exercitado | Evidência |
| --- | --- |
| join em produção | `Gon` 12:50, `Kurapika` 12:51 |
| técnicas / C2S | **37 pedidos admitidos, 0 cortados** |
| combate com mobs do mod | **8 mortes** — Kiriko, Cheetah Leader, Zumbi |
| morte e respawn | as 8, mais `Gon fell out of the world` |
| troca de dimensão | chunks novos em `world_tree` **e** `greed_island` |

Varredura nos dois logs (`latest.log` 118 linhas, `debug.log` 588):

| Procurado | Ocorrências |
| --- | --- |
| `NoClassDefFoundError` / `ClassNotFoundException` | **0** |
| `net.minecraft.client` / `nenfoundation.client` | **0** |
| `[ERROR]` / `[FATAL]` / `Exception` / `Caused by` | **0** |

Os 22 WARN são vanilla e de terceiros: 12 `moved too quickly` (teleporte em
creative), refmap da GeckoLib, URL de assets do NeoForge e o aviso de
`offline-mode`. **Nenhum do `nenfoundation`.**

> **E a lacuna do shutdown FECHOU em 2026-09-22.** Esta caixa dizia que
> "produção + jogador + shutdown limpo" nunca tinha acontecido numa execução só.
> Aconteceu: a sessão do AV1 rodou na instância com dois jogadores e foi
> encerrada com `stop`, salvando **todas as dimensões** — `world`, `world_tree`,
> `greed_island`, `DIM1` e `DIM-1` —, com `All dimensions are saved` e **zero**
> `NoClassDefFoundError`, `ClassNotFoundException`, menção a `client` ou
> `[ERROR]` nos dois logs. O save-ao-parar, que é onde um defeito de
> persistência apareceria, foi exercitado e está limpo.

- [x] **fechado** — produção com jogador, log varrido

---

## 2. Não é sua — eu fecho no código

### D6 · Morte e troca de dimensão — **VIRARAM PORTÃO em 2026-09-22**

`MorteETrocaDeDimensaoTest`, cinco testes. A investigação achou algo melhor do
que o handler que eu ia escrever:

**Não existe ponto de saída para morte nem para troca de dimensão, e isso é
proposital.** O cliente não faz logout nesses casos — ele **troca a entidade**,
que volta com **id novo**. Quem guarda estado por id de entidade não é avisado
de nada. O que impede o vazamento é a poda por presença:
`DetectorDeAtivacaoDeTen.reterSomente(...)`, chamada a cada tick com quem está
na tela **agora**. O id velho simplesmente deixa de estar na lista.

Esse desenho é melhor que um handler por evento, e o motivo já estava escrito na
própria classe: *"bastaria um `reterSomente` esquecido para as bordas de Ren
continuarem sendo detectadas para alguém que já saiu do alcance — e o sintoma
seria um zumbido tocando sem dono"*. **Handler cobre os eventos que alguém
lembrou; poda por presença cobre todos.**

O portão prova as duas metades:

- **o mecanismo** — morrer em Ren não deixa o id antigo no mapa, renascer não
  toca ativação fantasma, trocar de dimensão não acumula, e um teste-controle
  confirma que **sem** a poda o estado ficaria preso;
- **a ligação** — que `AudioDeAura` realmente chama `reterSomente` no tick.
  Verificado removendo a chamada do código real: os quatro primeiros testes
  continuaram **verdes** e só esse reprovou. Mecanismo correto não vale nada se
  ninguém chama.

- [x] **fechado** — `MorteETrocaDeDimensaoTest`

---

### D7 · As árvores de captura — **só se houver sessão de dois clientes**

`C:/dca-a` e `C:/dca-b` estão em `053ed81`; a `main` está em `ffb7e47`.

A razão original desta dívida era o **commit no nome da captura**, e ela caiu
junto com o D1. Ela volta a existir **apenas se o D2 pedir uma sessão nova** com
dois clientes para refazer o bloco D — nesse caso as duas árvores precisam estar
no commit do dia antes de começar.

**Avise antes e eu movo as duas** — leva segundos, e tem de ser imediatamente
antes, porque a `main` anda.

- [ ] *(só se houver sessão nova)* as três árvores no mesmo commit

---

## 3. O que já está pago

Para a lista não parecer maior do que é:

| Item | Como foi pago |
| --- | --- |
| A — luz (dia, noite, caverna) | ✅ julgamento humano, 2026-09-21 |
| B — corpo (slim, overlay, armadura) | ✅ idem; o overlay **não engole** a shell |
| C — aderência (correr, agachar, nadar) | ✅ idem — **a pergunta central do gate** |
| D — distância (2b→40b) | ✅ idem — transição contínua, comunica em 40b |
| E1 — log do dedicado | ✅ varredura automatizada, 4 buscas, 0 ocorrências |
| E2 — morte e troca de dimensão | ✅ julgamento humano (mas ver **D6**) |
| E3 — `poseStack.scale` | ✅ **portão** `EscalaDaShellTest` — não gasta sessão |
| bancada (§4.1) | ✅ build, GameTests, JAR por hash, árvores — em `4bf378e` |

---

## O #169 fechou

Nada em aberto. O que o gate deixou para trás, e que vale lembrar:

- **a evidência do AV0 é humana e datada** — `PERGUNTAS.md`, preso a um commit.
  Sem imagem, ela não é revisável por terceiro (ver o custo em D1);
- **dois itens viraram portão** e sobrevivem ao gate: `EscalaDaShellTest` e
  `MorteETrocaDeDimensaoTest`. São a parte que o `build` repete sozinho amanhã;
- **"produção + jogador + shutdown limpo"** nunca aconteceu numa execução só —
  está declarado em D5 e em `o-que-nao-provamos.md`.

O AV1 fica destravado pela regra "não começar o AV(n+1) sem fechar o AV(n)" —
mas **não autorizado**: marco não começa sem instrução explícita.
