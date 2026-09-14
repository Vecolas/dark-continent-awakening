# A copa da World Tree — por que ela não aparecia, e o que a substitui

**Referência de arte:** [`../insp/arvoremundo.png`](../insp/arvoremundo.png).
**Critério que fecha:** *desligue a madeira e olhe a silhueta. Se lê como copa de
árvore, passou. Se lê como confete, luva ou ilha, reprovou.*

---

## 1. O diagnóstico — quatro defeitos, e nenhum deles dava erro

O gerador anterior desenhava folha e o resultado era "poucas folhas, deslocadas".
Nenhuma linha lançava exceção; o build ficava verde. Os quatro motivos, na ordem
em que doem:

### 1.1 O cluster nascia DENTRO da madeira

`placeCluster` centrava o elipsoide em `bezier(branch, t)` — o **eixo** do galho.
O raio vertical do cluster era `radius * 0.82`, com `radius` entre 6 e 18 vezes
uma escala de 0.58–0.78 — ou seja, **entre 4 e 14 blocos**.

O raio do próprio galho naquele ponto vai de 30 (base) a 4 (ponta).

Como folha só entra onde há ar (`if (chunk.getBlockState(position).isAir())`), na
metade interna de cada galho **o cluster inteiro caía dentro do tronco do galho e
não virava um único bloco**. O que sobrava era a borda que escapava por fora — e
uma borda de elipsoide vista de longe é exatamente "folha deslocada, solta perto
do galho".

> **Este é o defeito principal.** Os outros três agravam; este é o que fazia a
> folhagem sumir.

### 1.2 A densidade era ruído branco a 38%

```java
return Math.floorMod(value, 100) >= 38;   // descarta 62% dos blocos
```

Hash por bloco, sem correlação espacial nenhuma. Isso não é folhagem esgarçada:
é **confete**. Massa vegetal precisa ser sólida no miolo e irregular só na casca
— o inverso do que um ruído uniforme produz.

### 1.3 A forma era uma salsicha, e não uma prateleira

`lengthRadius = radius * 1.85` ao longo do galho, `crossRadius = radius * 0.78`
de través, `verticalRadius = radius * 0.82`. Seção quase circular, alongada na
direção do galho.

A referência não tem nada disso. Ela tem **prateleiras**: discos largos em X e Z,
finos em Y, empilhados em vários níveis. É o que dá a leitura de cedro/figueira
em vez de brócolis.

E foi a salsicha que produziu as "luvas" das tentativas anteriores: vários
clusters de raio parecido, enfileirados ao longo de um galho **sem se
sobreporem**, leem como dedos.

### 1.4 Não havia regra de suporte

Nada exigia que uma massa de folha estivesse ancorada em madeira alcançável.
Qualquer cluster cujo galho ficasse num chunk ainda não gerado aparecia como
**ilha flutuante** — e como o cluster era esparso, a ilha parecia lixo, não copa.

### 1.5 E a Crown não tinha folha nenhuma

`WorldTreeCrownGenerator` desenha o líder central de y=1100 a 1450 em madeira
maciça, e ninguém colocava folha ali. O topo da árvore — a parte que a referência
mais mostra — era um poste.

---

## 2. A decisão

> **A folhagem deixa de ser um efeito do gerador e passa a ser um PLANO
> geométrico puro, calculado antes de qualquer bloco.**

Antes, a geometria da copa só existia dentro do laço que escrevia blocos. Não
dava para medir volume, cobertura, sobreposição ou ancoragem sem subir o jogo —
e por isso nenhum dos quatro defeitos acima tinha régua.

Agora:

| Camada | O que faz | Depende do Minecraft? |
| --- | --- | --- |
| `WorldTreeFoliagePlan` | decide ONDE há copa: prateleiras e vinhas | **não** |
| `WorldTreeFoliageShelf` | uma prateleira: centro, raio, espessura, âncora | **não** |
| `WorldTreeVineStrand` | uma cortina de vinha: origem, comprimento, desvio | **não** |
| `WorldTreeCanopyGenerator` | escreve os blocos do plano no chunk | sim |

A consequência prática é o portão da seção 5: **cobertura, camadas, sobreposição,
ancoragem e ausência de ilha viram asserções de JUnit**, e não opinião sobre uma
captura.

---

## 3. A forma: prateleira, e não bolha

Cada prateleira é uma **superelipse achatada**:

```
r  = hypot(dx, dz) / raio
v  = (dy >= 0) ? dy / espessuraSuperior : -dy / espessuraInferior
n  = r² + v⁴
dentro quando n <= 1
```

O expoente **4** no eixo vertical é a decisão de forma inteira. Com expoente 2
(elipsoide) a massa é uma bolha; com 4, o topo e a base ficam **chatos** e só o
ombro arredonda — que é a leitura de prateleira da referência.

Três coisas mais:

- **O centro sobe acima do eixo do galho** (`centerY = eixoY + raioDoGalho * 0.55`).
  É o que tira a prateleira de dentro da madeira — o defeito 1.1.
- **A espessura inferior é menor que a superior**, mas nunca zero: a referência
  tem folha embaixo do galho, e é ela que impede a copa de parecer um guarda-sol
  visto de baixo.
- **O raio decresce em direção à ponta**, e prateleiras vizinhas **se sobrepõem**
  — é o que impede a leitura de luva.

### Densidade por profundidade, e não por sorteio

```
folga  = 1 - n              (1 no miolo, 0 na borda)
mantém quando folga > limiar, com limiar vindo de ruído suave
```

Miolo sólido, borda esgarçada. O ruído entra **modulando a borda**, e não
decidindo bloco a bloco no volume todo.

---

## 4. Vinhas

Cortinas verticais nascendo da **borda inferior** das prateleiras, entre 6 e 40
blocos de comprimento, com desvio horizontal pequeno ao longo da queda.

Elas não são enfeite: na referência são metade da leitura de escala. Uma copa sem
elas parece um cogumelo; com elas, parece uma árvore de dossel.

Regra dura: **toda vinha nasce dentro do disco de uma prateleira existente.** Uma
vinha órfã é uma ilha flutuante fina, que é pior que uma grossa — ela parece bug,
não paisagem.

---

## 5. O portão — `WorldTreeFoliagePlanTest`, em 20 seeds

Cada item existe por causa de um defeito real da seção 1.

| Verificação | Contra o quê |
| --- | --- |
| cobertura projetada da copa ≥ 55% do disco | 1.1 e 1.2: copa rala |
| diâmetro da copa ≥ 3× o do tronco | copa que não sai de perto do eixo |
| ≥ 4 alturas distintas com folha | copa de um andar só |
| prateleiras vizinhas do mesmo galho se sobrepõem | 1.3: a luva |
| toda prateleira ancorada em galho real, dentro do raio + margem | 1.4: ilha |
| toda prateleira com espessura superior **e** inferior > 0 | folha só em cima |
| toda vinha nasce sob uma prateleira | vinha órfã |
| raio decresce da base para a ponta | copa em forma de haltere |
| teto de volume e de contagem | custo de geração |

E o de sempre: **alimentar cada régua com o defeito que ela existe para pegar, e
confirmar que ela reprova.** Isso foi feito, com um script que quebra o código de
propósito, uma quebra por vez. O resultado está na seção 5-B, e ele encontrou
mais defeitos do que confirmou réguas.

---

## 5-B. O que aconteceu ao alimentar os portões com os defeitos

Oito defeitos foram injetados um a um. **Seis morderam na primeira tentativa.**
Os outros dois não — e nenhum dos dois era culpa do defeito.

### Buraco 1: o defeito ORIGINAL passava por todas as réguas

Recentrar a prateleira no eixo do galho — literalmente o defeito que fez a copa
sumir em jogo — **passou por todos os portões**. O disco continuava ancorado,
chato, com as duas faces e sobreposto ao vizinho. Só que ficava dentro da
madeira, e folha não sobrescreve madeira.

O buraco era estrutural: **toda régua olhava a prateleira sozinha.** Nenhuma
olhava a prateleira *contra a madeira que ela veste*. Nasceu daí o
`escapesWood()`, e com ele a distinção entre os dois suportes — galho é um tubo
horizontal, de onde a folha escapa por cima e em volta; o líder central é uma
coluna vertical, de onde ela só escapa em volta. Modelar os dois é mais honesto
que afrouxar a regra para os dois.

### Buraco 2: uma constante que não mandava em nada

Trocar `EXPOENTE_VERTICAL` de 4 para 2 — o que deveria transformar cada
prateleira numa bolha — **não mudava um bloco**. A constante era decoração: quando
`normalized` trocou `Math.pow(v, EXPOENTE_VERTICAL)` por `v * v * v * v` (por
custo), ela deixou de ser lida por qualquer coisa.

Era o erro nº 7 do `CLAUDE.md` na forma mais pura, e nenhuma leitura do código
tinha percebido. A constante foi removida. O expoente aparece hoje em dois
lugares — a forma e o cálculo de alcance vertical — e o teste
`spanConcordaComAForma` amarra os dois, porque se eles divergirem o laço de
desenho passa a cortar a massa antes da borda: folhagem com o topo raspado, sem
erro nenhum.

### Uma quebra que não deveria morder, e não mordeu

Apagar a guarda de atalho `normalized <= INICIO_DA_CASCA` não muda resultado:
abaixo do início da casca o termo de borda fica negativo e a comparação com o
ruído passa de qualquer jeito. Ela é atalho de custo, não semântica. O defeito de
verdade — ruído branco a 38% no volume inteiro — tem entrada própria, e essa
morde em dois testes.

**Registrado porque "a régua não pegou" e "a quebra era inerte" são coisas
diferentes, e confundir as duas afrouxa portão bom.**

---

## 5-C. A integração com a outra frente

Este trabalho e o da outra frente atacaram o **mesmo problema em paralelo**, sem
saber um do outro. A branch `feature/world-tree-dimension` recebeu, no mesmo dia,
um sistema próprio de folhagem: `WorldTreeFoliageAnchor`,
`WorldTreeFoliageAnchorGenerator` e `WorldTreeFoliageValidation`.

O que ficou, e por quê:

| Peça | De quem | Motivo |
| --- | --- | --- |
| geometria da copa (prateleiras) | desta frente | tem silhueta renderizada e 16 portões em 20 seeds |
| hierarquia de galhos (`branchNodes`) | da outra frente | é uma árvore de verdade, e melhor que a derivação anterior |
| modelos de item, loot, aba do criativo, cor da folha | da outra frente | ortogonal, e esta frente não tinha |
| correções de checkpoint e de viagem | da outra frente | ortogonais |

> **Fica uma decisão em aberto, e ela é das duas pessoas, não de quem integrou:**
> `WorldTreeFoliageAnchor` + `AnchorGenerator` + `FoliageValidation` continuam no
> repositório e **ninguém os chama** — o caminho de geração usa o plano de
> prateleiras. Duas autoridades sobre a mesma coisa é o que o `CLAUDE.md` proíbe
> na primeira página. Uma das duas tem de sair, e apagar o código de outra pessoa
> unilateralmente seria pior do que deixar isso escrito aqui.

E uma correção que veio de graça na integração: `WorldTreeBranchNetwork` chamava
`WorldTreeRootGenerator.bezier`, arrastando `ChunkAccess` para dentro da
matemática de layout. O desenhador de silhueta morria com
`NoClassDefFoundError`. Agora ele chama a própria spline.

---

## 5-D. A revisão adversarial, e o que ela achou

Depois de a implementação estar verde, ela passou por uma revisão adversarial com
três lentes independentes — correção e falha silenciosa, fronteira de chunk e
determinismo, resultado visual — e cada achado foi submetido a um agente
encarregado de **refutá-lo**. Só o que sobreviveu à refutação entrou aqui.

Nove achados sobreviveram. Estes são os que mudaram código:

### O laço de vinhas não tinha corte por chunk

O laço de prateleiras sempre teve; o de vinhas nunca teve — o corte de *bounds*
morava dentro do desenho, **por passo**. Todo chunk da dimensão percorria as
~6.000 cortinas: ~140.000 iterações, **~1,2 ms medidos**, mesmo a 5.000 blocos do
tronco, onde não há uma folha para desenhar.

**E a régua de custo era cega a isso.** `estimatedVisitsForChunk` só somava
prateleiras, então devolvia **zero** exatamente para os chunks que pagavam o preço
inteiro. Medir uma coisa enquanto o gerador paga outra é o pior estado possível
para um portão — pior que não ter portão, porque dá confiança.

### O ruído da casca era um produto separável

`sin(x) * cos(z) * sin(y)`. Quando o primeiro fator passa por zero — a cada ~15
blocos em X — o produto inteiro zera para **todo z e todo y**, e a borda da
folhagem some numa **laje inteira**.

Em tela isso lê como **costura de chunk**: quem visse iria procurar o defeito na
geração por chunk, que está certa. Virou uma **soma** de senoides com frequências
incomensuráveis — num soma, um termo no zero não apaga os outros.

### O portão da luva é vazio para a pilha da coroa

`semDedos` mede `hypot(dx, dz)`, e os discos do líder central ficam todos a menos
de 11 blocos do eixo **por construção**: a sobreposição horizontal dá ~0,95
aconteça o que acontecer. A assertiva não *podia* reprovar.

O espaçamento real da coroa é **vertical**, e ali havia de 16 a 23 blocos de líder
**nu** entre um degrau e o seguinte, em todas as vinte seeds, **com a suíte
verde**. Nasceu `semVaoNaPilhaDoLider`, que mede o eixo certo.

### O cache do plano tinha uma corrida que dava NPE

`forget()` e `planFor()` conversavam por **três** `volatile` separados. Cada um
atômico sozinho; o conjunto, não. `forget()` podia zerar a referência entre a
leitura da bandeira e a do plano na thread de worldgen — e `null` ali não é um
efeito visual falhando, é o **chunk inteiro** falhando. Virou um registro imutável
num campo só.

### E um defeito que não era da folhagem: o layout amontoava os galhos

O portão novo `semFaixaVaziaDeAltitude` acusou **179 blocos de altitude sem uma
folha** na seed 1000. A causa não estava na copa: `generateBranches` sorteava a
altura **livre** dentro de cada zona, e na seed 1000 a `MID_BOUGHS` pôs os seus
entre y=576 e y=697 enquanto a `HIGH_CANOPY` começava em y=957 — **260 blocos de
tronco sem um galho**. A copa não tinha onde nascer.

A altura passou a ser **estratificada**: cada galho recebe uma fatia da zona e
sorteia dentro dela. A distribuição continua irregular, e deixa de ter buraco.
**179 → 25 blocos.** É o único ponto em que este trabalho tocou o layout, e foi
porque a régua apontou a causa raiz em vez do sintoma.

### Um achado que já estava resolvido

A revisão apontou que a copa renderizaria **cinza com buracos pretos** — sem
`render_type` e sem tint. Verdade no código que ela leu; **já corrigido pela outra
frente** no mesmo dia (modelo herdando `minecraft:block/leaves` e
`WorldTreeColorHandlers`). É a evidência mais concreta de que integrar valeu a
pena em vez de escolher um dos dois.

---

## 6. O que este documento NÃO promete

- **Aparência não vira verde.** O portão mede geometria, não beleza. A aprovação
  final é captura comparada com a referência, por gente olhando.
- **Nada aqui foi visto em jogo.** Nem um bloco. O que existe é a projeção do
  plano (seção 4-B), que é geometria desenhada — e não o que o renderer do
  Minecraft mostra com luz, oclusão e distância de névoa.
- **O teto de custo por chunk não foi medido em jogo.** O número (2 milhões de
  visitas no pior chunk) é o que a copa da referência pede mais um terço de
  folga. Ele protege contra regressão de **ordem de grandeza**; ele **não**
  afirma que o custo atual é aceitável. Isso sai de um `runServer` voando pela
  copa, com o tempo de geração medido.
- **A identidade de Hunter × Hunter não é mensurável.** As três folhas por
  altitude (base, densa, pálida) e a resina são a tentativa; se ler como floresta
  genérica, é ajuste de paleta, e não de geometria.
- **A largura da copa está limitada pelo comprimento dos galhos.** A referência
  tem copa proporcionalmente mais larga. Aumentar isso exige mexer nas splines de
  galho em `WorldTreeLayoutGenerator` — e elas são lidas também pelos
  checkpoints e pela ecologia, então é decisão maior que folhagem, e ficou
  **deliberadamente fora**.
