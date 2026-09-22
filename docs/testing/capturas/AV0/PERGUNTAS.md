# AV0 — as perguntas da sessão (#169)

Esta folha existe para ser **respondida durante a sessão**, com o jogo aberto.
Ela não substitui o [`LEIA-ME.md`](LEIA-ME.md), que diz o que cada captura é;
aqui está o que cada uma **pergunta**, uma por vez, com espaço para o veredicto.

> **Por que separar em perguntas.** Uma lista de catorze nomes de arquivo produz
> catorze imagens e nenhuma resposta. A evidência do gate não é o PNG — é o
> julgamento que alguém fez olhando para ele, e esse julgamento precisa estar
> escrito ao lado. Comparação de memória, dias depois, não vale.

**Regras que valem para toda pergunta desta folha:** servidor dedicado (nunca
singleplayer), overlay `F6` **sem** o aviso `OVERRIDE ATIVO`, e a comparação com
a referência de arte **na mesma sessão**. A montagem está em
[`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md) §4.

**O que NÃO está em julgamento:** a aparência final. No AV0 a shell é crua, sem
shader próprio. Responder "feia" não reprova nada aqui — filamentos são AV2,
tuning é AV3. A pergunta do AV0 é **aderência**.

---

## Bloco A — a shell se lê? (3 capturas, 1 cliente)

### A1 · `ten_dia`
**A shell se lê em luz plena, sem estourar?**
Estourar = a silhueta do jogador desaparece dentro do brilho.

- [ ] sim  [ ] não — veredicto: `____________________`

### A2 · `ten_noite`
**A shell se lê no escuro, sem sumir?**
Sumir = não dá para dizer se a técnica está ligada.

- [ ] sim  [ ] não — veredicto: `____________________`

### A3 · `ten_caverna`
**Em luz zero, a shell ainda distingue ligado de desligado?**
É o caso mais duro: sem luz ambiente, só a aura emite.

- [ ] sim  [ ] não — veredicto: `____________________`

---

## Bloco B — a shell veste qualquer corpo? (3 capturas, 1 cliente)

### B1 · `ten_slim`
**O cliente slim usa o modelo slim — e o braço fino NÃO fica com aura larga?**
Duas perguntas numa: o modelo certo foi escolhido, e a geometria acompanhou.

- [ ] sim  [ ] não — veredicto: `____________________`

### B2 · `ten_overlay_skin`
**Uma skin com segunda camada completa (`hat`/`jacket`/`sleeve`) engole a shell?**
Use uma skin com as três camadas opacas. Se a aura desaparece sob elas, reprova.

- [ ] engole  [ ] não engole — veredicto: `____________________`

### B3 · `ten_armadura`
**Com armadura completa, a aura ainda aparece E a armadura continua reconhecível?**
As duas metades contam. Aura visível com armadura irreconhecível reprova igual.

- [ ] sim  [ ] não — veredicto: `____________________`

---

## Bloco C — a pergunta central do AV0 (3 capturas, 1 cliente)

> **É aqui que o gate é decidido.** Os outros blocos podem passar com o jogador
> parado; este não. Capture em movimento, não em pose.

### C1 · `ten_correndo`
**A shell descola nas articulações ao correr?**
Olhe ombro, cotovelo e joelho. Descolar = a aura fica atrás ou à frente do membro.

- [ ] descola  [ ] acompanha — veredicto: `____________________`

### C2 · `ten_agachado`
**Agachar deixa a shell fora do corpo?**
Agachar muda a hierarquia de ossos mais que correr.

- [ ] descola  [ ] acompanha — veredicto: `____________________`

### C3 · `ten_nadando`
**Nadando — corpo na horizontal — a shell acompanha?**
A pose horizontal é a que mais expõe erro de pivô.

- [ ] descola  [ ] acompanha — veredicto: `____________________`

### C4 · além das três capturas
**Pular e atacar também acompanham?** O critério de aceite do `LEIA-ME.md` lista
cinco movimentos; três têm captura própria, dois não. Olhe os dois e escreva.

- [ ] sim  [ ] não — veredicto: `____________________`

---

## Bloco D — a série de distância (5 capturas, **DOIS clientes**)

> **Precisa de dois clientes, e o motivo é mecânico:** a distância do jogador
> local à própria câmera é zero, então o LOD **nunca morde em si mesmo**. Quem
> tirar esta série com um cliente só fotografa o LOD mais alto cinco vezes e
> chama de série.

O jogador A liga Ten; o jogador B captura de 2, 5, 10, 20 e 40 blocos.

### D1 · `ten_2b` · `ten_5b` · `ten_10b` · `ten_20b` · `ten_40b`
**A troca de nível de detalhe é visível como DEGRAU?**
Degrau = a aura muda de cara de repente entre duas distâncias vizinhas.

- [ ] há degrau visível  [ ] a transição é contínua — veredicto: `____________________`

### D2
**Em 40 blocos a aura ainda comunica "esta pessoa está com Ten ligado"?**
Se não, o LOD longe economiza demais.

- [ ] sim  [ ] não — veredicto: `____________________`

---

## Bloco E — o que não é imagem

Estas quatro não se respondem com captura. Duas já têm resposta.

### E1 · vazamento de classe client-only
**O log do servidor dedicado tem `NoClassDefFoundError` ou
`ClassNotFoundException` de `net.minecraft.client.*`?**

🟡 *Parcialmente coberto.* `PacotesDeclaradosTest` reprova **import** de cliente
no núcleo, mas não pega vazamento por reflexão nem por nome de classe em string.
**Singleplayer não serve** — ele roda o servidor no mesmo processo do cliente e
nunca acusa (erro nº 10 do `CLAUDE.md`). Leia o log do **dedicado**.

- [ ] log limpo  [ ] achou ocorrência — veredicto: `____________________`

### E2 · estado preso
**`/nenvfx off`, relog, morte e troca de dimensão deixam estado preso?**

🟡 *Parcialmente coberto.* O **logout** tem teste: `SobreposicaoDeVfxTest` prova
que `limpar()` apaga todos os campos, e `NenFoundationClient` o chama no ponto
de saída da sessão. **Morte e troca de dimensão não têm teste** — são as duas
que a sessão precisa responder.

- [ ] morte deixa estado preso? `____________________`
- [ ] troca de dimensão deixa estado preso? `____________________`

### E3 · `poseStack.scale`
**A shell escala a pilha em algum lugar?**

✅ **RESPONDIDA POR PORTÃO.** `EscalaDaShellTest` varre `client/vfx` e reprova
qualquer `scale` aplicado a um `PoseStack`. Ele mira o TIPO (não o nome da
variável), ignora menção em comentário, permite `Vec3.scale`, e foi verificado
reprovando contra uma violação injetada no código real. **Não gaste sessão
nisto** — se o `build` está verde, está respondida.

### E4 · aderência, a causa não coberta
**Descolou mesmo com o portão do E3 verde?**

Se o bloco C reprovar com `EscalaDaShellTest` verde, a causa **não** é escala de
pilha. Suspeite de pivô de osso, do adaptador (`HumanoidAuraAdapter` /
`GeoAuraAdapter`) ou de um `Matrix4f` montado à mão — e registre qual, porque
isso é um defeito novo, não um item de checklist.

- veredicto: `____________________`

---

## Ao fechar

Do `LEIA-ME.md` e da §6.3 da campanha:

- [ ] as 14 capturas em `capturas/AV0/`, com **data, commit e bloom no nome**;
- [ ] esta folha preenchida, na mesma sessão;
- [ ] `o-que-nao-provamos.md` com o que a sessão **não** provou;
- [ ] `../../../processo/marcos.md` atualizado — é a fonte de verdade do estado;
- [ ] `compatibility.md`, se a sessão tocar renderer ou shader pack.

> **O commit no nome do arquivo tem de ser o commit das três árvores.** Conferido
> em `4bf378e`: `C:/dca-a` e `C:/dca-b` estavam três commits atrás e foram
> movidas. Se a sessão demorar e a `main` andar, confira de novo — a captura que
> mente sobre o próprio commit é pior que a captura que falta.
