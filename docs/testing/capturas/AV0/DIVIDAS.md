# AV0 — as dívidas, e de quem é cada uma (#169)

Atualizado em **2026-09-22**, contra `ffb7e47`.

**As treze perguntas de julgamento foram respondidas, e todas passaram.** Com
as catorze imagens **aposentadas em 2026-09-22** (ver D1) e pular/atacar
julgados (D4), o AV0 deve **duas coisas**: a declaração da montagem (D2) e a
releitura do log depois de alguém entrar (D5).

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

### D2 · A montagem, declarada por escrito

A sessão não disse **em que montagem** rodou. A folha exige:

| Exigência | Por quê |
| --- | --- |
| servidor **dedicado**, nunca singleplayer | singleplayer roda o servidor no processo do cliente e nunca acusa classe client-only vazada — erro nº 10 do `CLAUDE.md` |
| **dois clientes** no bloco D | contra a própria câmera a distância é zero e o **LOD nunca morde em si mesmo**; com um cliente a série fotografa o LOD mais alto cinco vezes |
| overlay `F6` **sem** `OVERRIDE ATIVO` | com override, a captura mostra um número que não está em perfil nenhum e seria aprovada como se fosse o jogo |

**Se o bloco D rodou com um cliente só, ele não conta** — e é a única parte do
julgamento de ontem que essa dúvida derruba. Os blocos A, B, C e E2 valem em
qualquer montagem.

- [ ] montagem escrita em `PERGUNTAS.md`: dedicado? dois clientes? F6 limpo?

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

### D5 · A metade do E1 que a varredura não alcança

O log do dedicado está limpo — zero `NoClassDefFoundError`, zero
`ClassNotFoundException`, zero menção a `net.minecraft.client`. **Mas ninguém
entrou.** O servidor subiu, criou as dimensões e desligou sozinho.

Vazamento alcançado **no join, no caminho de payload ou no de render** não
dispara nessa varredura.

> **Esta dívida se paga sozinha**, e por isso não peça sessão própria: a sessão
> de captura conecta cliente de qualquer forma. Só é preciso **ler o log do
> servidor depois** — e é o log da instância, não o do `runServer`.

- [ ] log do dedicado relido **depois** de um cliente ter entrado e jogado

---

## 2. Não é sua — eu fecho no código

### D6 · Morte e troca de dimensão não têm teste

Hoje **a sua resposta é a única prova que existe** de que elas não deixam estado
preso. Ela vale para um commit e uma sessão; no próximo refactor do ciclo de
vida do cliente, nada acusa se a limpeza sumir.

O logout tem teste (`SobreposicaoDeVfxTest`). Morte e troca de dimensão, não.

**Peça e eu escrevo** — é a lacuna mais barata que restou, e converte uma
evidência datada em algo que o `build` responde para sempre.

- [ ] teste de morte e troca de dimensão

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

## A ordem que eu recomendo

1. **Declare a montagem (D2)** — de que forma a sessão de 21/09 rodou. Se foi
   dedicado com dois clientes, o AV0 fica a um item de fechar. Se o bloco D
   rodou com um cliente só, é só esse bloco que precisa de sessão nova.
2. **Suba o dedicado, entre e jogue um pouco; depois me avise (D5)** — eu releio
   o log com jogador tendo conectado. Fecha a metade que a varredura automática
   não alcança.
3. **Peça o teste de morte e troca de dimensão (D6)** quando quiser. Não bloqueia
   o gate, e é a única dívida que sobrevive a ele.

Fechando **D2 e D5, o #169 fecha.**

> **O D6 é o que fica.** Ele não bloqueia nada, e é exatamente por isso que
> dívida de cobertura some da lista: ninguém a cobra. Com o D1 aposentado, a
> folha `PERGUNTAS.md` passou a ser a evidência inteira do AV0 — e ela é humana
> e datada. Um teste é a única parte disso que o `build` consegue repetir sozinho
> amanhã.
