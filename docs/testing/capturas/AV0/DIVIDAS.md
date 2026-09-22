# AV0 — as dívidas, e de quem é cada uma (#169)

Atualizado em **2026-09-22**, contra `ffb7e47`.

**As treze perguntas de julgamento já foram respondidas, e todas passaram.**
O que falta não é opinião — é **procedência**: prova de onde o julgamento veio,
em que montagem, sobre qual commit, e revisável por quem não estava lá.

> **Por que separar por dono.** A lista de pendências de um gate mistura o que
> só um humano no jogo responde com o que uma régua responde melhor. Misturadas,
> as duas esperam uma pela outra, e a que o código podia ter fechado fica
> esperando alguém abrir o cliente.

---

## 1. Só você responde

### D1 · As 14 imagens — **a dívida principal**

A sessão de 2026-09-21 foi verificação **ao vivo**. `capturas/AV0/` tem três
documentos e **nenhum PNG**.

```
ten_dia          ten_noite         ten_caverna
ten_slim         ten_overlay_skin  ten_armadura
ten_correndo     ten_agachado      ten_nadando
ten_2b   ten_5b  ten_10b  ten_20b  ten_40b
```

Cada arquivo com **data, commit e bloom no nome** (§6.3 da campanha).

> **Por que o veredicto sozinho não fecha.** Você olhou e a shell não descola —
> isso é verdade e está registrado. Mas ninguém pode **discordar** disso depois:
> não há o que reabrir, comparar com a referência, nem mostrar a outra frente.
> Um gate cuja evidência é irrevisável não é evidência, é memória. O PNG existe
> para que o julgamento sobreviva a quem o fez.

- [ ] as 14 capturas arquivadas

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

### D3 · A sessão de ontem envelheceu — e a regra sabe disso

A §6.3 da campanha exige a comparação com a referência de arte **na mesma
sessão**, *"lado a lado — olho humano cansa, e comparação de memória dias depois
não vale"*.

Seus veredictos são de **2026-09-21**. Hoje é **2026-09-22**. Se as imagens
forem tiradas agora, veredicto e imagem vêm de sessões diferentes — exatamente
o que a regra proíbe.

**Isso não invalida o que você respondeu.** A saída barata é uma só:

> **Ao tirar as 14 imagens, responda a folha de novo na mesma sessão.** São as
> mesmas treze perguntas, com as imagens na tela. Os veredictos de ontem ficam
> como **pré-checagem datada** — eles já valeram por uma coisa cara: acharam
> duas perguntas mal escritas antes de a sessão de captura ser gasta.

- [ ] `PERGUNTAS.md` respondida de novo, na sessão que produzir as imagens

---

### D4 · O bloco C4 nunca foi julgado item a item

O critério de aceite lista **cinco** movimentos — correr, pular, atacar, agachar
e nadar. Três têm captura própria; **pular e atacar não**.

O veredicto *"funciona perfeitamente"* foi dado ao bloco C inteiro, e está
registrado como **PASSA por arrasto**, não como prova separada. É honesto, e é
menos do que o critério pede.

- [ ] pular: acompanha? `____________`
- [ ] atacar: acompanha? `____________`

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

### D7 · As árvores de captura derivaram de novo

`C:/dca-a` e `C:/dca-b` estão em `053ed81`; a `main` está em `ffb7e47`. **Três
commits de diferença.**

Os três são documento e teste — nenhum código de jogo mudou, então o que aparece
na tela é o mesmo. O que fica errado é o **commit no nome da captura**, e a
captura que mente sobre o próprio commit é pior que a captura que falta.

**Avise antes de abrir a sessão e eu movo as duas** — leva segundos, e tem de
ser feito *imediatamente antes*, porque a `main` anda.

- [ ] as três árvores no mesmo commit, conferido no início da sessão

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

1. **Me avise** — eu movo as duas árvores para o commit do dia (**D7**).
2. Suba o dedicado e **dois** clientes (**D2**).
3. Tire as 14 imagens **e responda a folha na mesma sessão** (**D1**, **D3**).
4. No caminho, julgue pular e atacar (**D4**).
5. Ao terminar, **leia o log do servidor** — agora com jogador tendo entrado (**D5**).
6. Peça o teste de morte e troca de dimensão quando quiser (**D6**).

Fechando D1 a D5, o **#169 fecha**. D6 não bloqueia o gate — é dívida de
cobertura, e ela sobrevive ao gate se ninguém escrever o teste.
