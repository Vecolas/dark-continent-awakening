# A copa da World Tree — por que ela não aparecia, e o que a substitui

**Referência de arte:** `arvoremundo.png` — **não versionada**. A imagem nunca entrou em `docs/insp/`, e o link que apontava para ela quebrava. O critério abaixo é o que fecha o gate; a referência era apoio.
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
| `WorldTreeFoliageIndex` | quais prateleiras e vinhas encostam num chunk | **não** |
| `WorldTreeFoliageTexture` | quais folhas são luminosas, e o seno tabelado | **não** |
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

- **O centro sobe acima do eixo do galho** (`centerY = eixoY + raioDoGalho * 0.35 + 1.5`).
  É o que tira a prateleira de dentro da madeira — o defeito 1.1. *(O texto dizia
  `* 0.55`; o código mudou na terceira correção descrita em
  `WorldTreeFoliageShelf` e o texto tinha ficado para trás.)*
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
| ≥ 6 faixas de altitude com folha | copa de um andar só |
| prateleiras vizinhas do mesmo galho se sobrepõem | 1.3: a luva |
| toda prateleira ancorada em galho real, dentro do raio + margem | 1.4: ilha |
| toda prateleira com espessura superior **e** inferior > 0 | folha só em cima |
| toda vinha nasce sob uma prateleira | vinha órfã |
| raio decresce da base para a ponta | copa em forma de haltere |
| teto de volume e de contagem | custo de geração |
| **boa parte de cada galho fica com madeira à mostra** | 5-E: a copa engolia o galho inteiro |
| **o índice devolve o mesmo que o laço linear** | 5-E: otimização que muda a forma |
| **os atalhos de coluna concordam com a função completa** | 5-E: duas fontes para a mesma conta |
| **15% das folhas brilham, e em tufo** | 5-E: chuvisco de fonte de luz |

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

## 5-E. A copa migra para a ponta do galho, e a madeira volta a aparecer

> **Pedido:** folha que emite luz no nível da glowstone, espalhada a 15%; 35%
> menos folha no geral; **dá para ver parte da madeira dos galhos**; e geração
> mais barata. Depois, reforçado: *"boa parte dos galhos devem estar com a
> madeira visível, pode diminuir até mais do que os 35% se for necessário para
> isso."*

### O que a medição mostrou antes de mexer em qualquer coisa

Nenhum dos dezesseis portões da seção 5 perguntava o que sobra da **madeira**.
Todos olhavam a folha — cobertura, sobreposição, ancoragem, espessura, custo — e
por isso ninguém tinha percebido o número:

| | antes |
| --- | --- |
| fração do eixo de um galho **sem folha por cima** | **0,146** |
| galhos com menos de 30% de madeira à mostra | **708 de 766** |
| raio médio de prateleira | 43 blocos |
| comprimento médio de galho | 90 blocos |
| espaçamento entre prateleiras vizinhas | 14 blocos |

As três últimas linhas explicam a primeira: **discos de raio 43 espaçados de 14
sobre um galho de 90 não são uma copa empilhada, são um tubo verde contínuo em
volta do galho inteiro.** Nenhuma quantidade de espaçamento resolve isso enquanto
um único disco cobrir metade do galho.

### A decisão

> **A folhagem deixa de acompanhar o galho inteiro e passa a ocupar o terço
> externo dele.**

É o que a referência mostra — estrutura nua perto do tronco, massa na ponta — e é
a única mudança que faz a madeira aparecer sem mexer nas splines de galho, que
são lidas também pelos checkpoints e pela ecologia (seção 6).

Quatro constantes, e cada uma tem o seu porquê escrito em `WorldTreeFoliagePlan`:

| Constante | De | Para | Por quê |
| --- | --- | --- | --- |
| `PRIMEIRO_T_MAIOR` | 0,34 | **0,66** | é a mudança que descobre a madeira |
| `PRIMEIRO_T_SUB` | 0,42 | **0,68** | idem, no subgalho |
| `PRATELEIRAS_POR_GALHO_MAIOR` | 11 | **7** | a janela de `t` encolheu pela metade; onze discos nela ficariam empilhados |
| `PRATELEIRAS_POR_SUBGALHO` | 5 | **4** | idem |
| raio base | `31 + 2,1·r + u·9` | **`17,5 + 1,25·r + u·5`** | disco de 43 sobre galho de 90 cobre o galho sozinho |
| piso do raio | `2,2·r + 6` | **`1,85·r + 5`** | o piso mordia quase sempre, e por isso encolher a fórmula não encolhia a copa |

E duas correções que **o portão exigiu**, não o gosto:

- **`ENCOLHIMENTO_NA_PONTA` passou a medir a posição na sequência**, e não o `t`
  absoluto. Com a folhagem espremida entre 0,66 e 0,98, o `t` varia só 0,32 e o
  taper encolhia 14% do primeiro disco ao último — catorze por cento não afunila
  nada, lê como haltere. É exatamente o defeito que a constante existe para matar.
- **O raio ganhou um teto igual ao do disco anterior** (`min(raio, anterior·0,94)`).
  O taper sozinho é uma *tendência*: o sorteio de ±5 blocos e a escala por
  altitude (que cresce com `y`, e um galho sobe em direção à ponta) viravam o
  sinal entre dois vizinhos. `afunilaNaPonta` reprovou, e estava certo.

### O que isso deu

| | antes | depois |
| --- | --- | --- |
| madeira nua, média por galho | 0,146 | **0,496** |
| galhos com menos de 30% de madeira à mostra | 708 de 766 | **23 de 766** |
| prateleiras no plano | 752 | 549 |
| vinhas no plano | 5.303 | 3.070 |
| visitas de bloco na copa inteira (3 seeds) | 542 M | **193 M** |
| pior chunk | 1.061.281 | **408.834** |

**A queda de folha é de 64%, e não de 35%.** Foi autorizada: os 35% eram o pedido
inicial, e a mensagem seguinte disse que dava para passar disso se fosse o preço
de ver a madeira. É o preço — os 35% sozinhos deixavam a maior parte dos galhos
ainda enterrada. Quem quiser recuar, os botões são as constantes da tabela acima,
nessa ordem de efeito.

**Nenhum dos portões existentes foi afrouxado.** A cobertura projetada ficou em
0,572 (mínimo 0,55), a sobreposição em 0,438 (mínimo 0,35), e os treze demais
passaram sem mudança. O teto de custo por chunk **desceu** de 2.000.000 para
800.000 — um teto que não acompanha a realidade para de proteger.

### A folha que acende

`world_tree_leaves_luminous` — mesmo material das outras três, `lightEmission`
15. Quem escolhe é `WorldTreeFoliageTexture`, e a escolha é **agrupada**, por dois
motivos:

1. **Leitura.** Folha luminosa sorteada bloco a bloco é o defeito 1.2 outra vez:
   chuvisco, e não mancha.
2. **Custo, e este é o argumento mais forte.** Cada bloco desses é uma fonte de
   luz, e a propagação é um BFS por fonte. Fontes encostadas compartilham quase
   toda a propagação; fontes espalhadas pagam cada uma a sua. Medido: corrida
   média de 8,7 blocos, contra ~1,2 de um sorteio por bloco.

Medido nas vinte seeds: **15,3% das folhas** (faixa de 11,9% a 17,2%; a dispersão
é da copa, não da amostragem). O portão trava a média e deixa a faixa por seed
larga de propósito.

**Ela não entra no tint de bioma**, e isso é deliberado: o tint escurece a
textura, e um bloco que emite luz 15 pintado de verde escuro perde a única coisa
que o distingue em tela.

### A textura, e a profundidade de 2

Duas correções pedidas depois de a folha luminosa existir.

**A textura é autoral** (`art-source/worldtree/folha_luminosa.py`, ADR-007), e
não mais uma folha do vanilla recolorida: verde escuro com manchas douradas de 2
a 4 pixels, como glow lichen. O verde é escuro de propósito — o bloco emite luz
15, então chega na tela já no brilho máximo, e uma textura clara estoura para
branco e perde o desenho.

O gerador errou uma vez, e o erro virou portão: a primeira versão concentrava
buraco na **borda** do quadro, o que parece razoável para uma folha que se desfaz
nas pontas. O bloco **ladrilha** — duas bordas vizinhas encostam, e o resultado
foi uma **grade preta de um pixel** cortando a copa a cada 16 blocos. Olhar o
quadro sozinho não mostra; só ladrilhar mostra. `semEmendaAoLadrilhar` existe por
causa disso.

**A folha luminosa só existe nos 2 blocos mais externos de cada coluna.** Luz não
atravessa bloco sólido: uma folha luminosa enterrada a dez blocos de
profundidade não clareia nada que alguém veja, e continua pagando uma propagação
de luz inteira na engine — que é um BFS por fonte, numa copa de dezenas de
milhões de folhas.

O campo que sorteia **não mudou**: a mancha na superfície continua do tamanho que
era. O que saiu é só o que estava enterrado.

"Os dois mais externos" não dá para saber varrendo de baixo para cima — quais
blocos ficam depende de `keep`, que esgarça a borda, e os limites *geométricos*
da coluna são justamente onde a erosão morde mais. Por isso o laço agora **coleta
a coluna antes de escrevê-la**.

### O índice espacial

O corte por chunk sempre existiu e sempre esteve **certo** — só que era um `if`
de caixa dentro de um laço sobre a lista inteira. Todo chunk da dimensão pagava
3.619 testes antes de descobrir que não tinha nada a desenhar, e **um chunk a
5.000 blocos do tronco pagava exatamente o mesmo que o chunk em cima dele**. É o
mesmo erro do laço de vinhas da seção 5-D, uma camada acima: custo proporcional
ao *plano* onde deveria ser proporcional ao *chunk*.

`WorldTreeFoliageIndex` calcula a pegada uma vez e responde por consulta de mapa.
20.143 entradas por mundo (~80 KB). O portão dele é uma **comparação com o laço
linear em vinte seeds** — uma otimização que muda o resultado não é otimização, é
mudança de forma disfarçada, e apareceria como copa cortada na fronteira de chunk.

A primeira versão do índice devolvia um chunk **a mais** em alguns casos —
`floorDiv(ceil(max))` em vez da inversa exata do teste de caixa. Erra "só para o
lado seguro", nenhuma folha some — e o portão reprovou assim mesmo, com razão:
uma faixa larga faz o gerador visitar prateleiras que não escrevem nada, que é
justamente o custo que o índice existe para matar.

### Os dois atalhos do laço interno

O laço de `y` é o mais interno da geração da copa, e rodava coisas que não
dependem de `y`:

- `shelf.normalized(x,y,z)` recalculava `dx`, `dz` e a divisão pelo raio ao
  quadrado — valores que a própria coluna já tinha calculado para saber até onde
  ir. Virou `normalizedInColumn(horizontalSquared, y)`, com `normalized`
  delegando para ela.
- Os dois campos de ruído têm um termo em `(x, z)` e dois que envolvem `y`. O
  termo de coluna saiu do laço.

**Nenhum dos dois é uma segunda implementação**, e é a parte que merece
desconfiança: as funções de conveniência calculam o termo e delegam para as
mesmas linhas, e há portão comparando as duas formas
(`cascaNaColunaConcordaComKeep`, `atalhoDeColunaNaoDiverge`). Duas versões da
mesma conta divergiriam exatamente no limiar, e a divergência apareceria como a
copa em jogo sendo diferente da copa que esta suíte mede — que é pior do que não
ter portão.

### E o seno virou tabela

Os campos de ruído chamam `Math.sin` dezenas de milhões de vezes por mundo. A
tabela de 4096 entradas com interpolação linear é mais barata — e, o que importa
mais, **mais determinística**: `Math.sin` só promete 1 ulp e pode diferir entre
JVMs, e 1 ulp perto do limiar troca a decisão de um bloco.

Um detalhe que virou portão: a indexação usa `Math.floor`, e não um cast. Cast
trunca em direção a zero, e metade da copa está em coordenada negativa — o erro
resultante é pequeno na média e **concentrado em torno de x=0**, ou seja, uma
emenda reta atravessando a árvore, do tipo que se atribui à geração por chunk.

### O que esta rodada NÃO provou

- **Nada foi visto em jogo.** Nem um bloco. Continua valendo a seção 6.
- **O custo de luz das folhas luminosas não foi medido.** A copa é enorme e luz
  15 propaga por BFS. O agrupamento é a mitigação escolhida; se a dimensão
  engasgar ao carregar a copa, este é o primeiro suspeito, e os botões são
  `LIMIAR_DE_BRILHO` e o próprio `lightEmission`.
- **"Madeira nua" não é "madeira visível".** A régua anda pelo eixo do galho e
  pergunta se algum disco **daquele** galho cobre aquele ponto. Ela não sabe de
  oclusão, de distância, nem da copa de um galho **vizinho** passando por cima.
  É condição necessária, e não suficiente.
- **O ganho de tempo não foi medido em jogo**, só em visitas de bloco e em
  candidatos por chunk. Menos trabalho é menos trabalho, mas o número em
  milissegundos sai de um `runServer` voando pela copa.
- **Mundo já gerado não muda.** Chunks existentes ficam com a copa antiga, e a
  fronteira entre o gerado e o novo será visível. Vale para qualquer mudança de
  worldgen; aqui a diferença é grande o bastante para notar.

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
