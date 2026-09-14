# A âncora de escalada — por que ela flutuava

**Relato de jogo:** *"o lugar spawna flutuando de qualquer jeito e com a base
totalmente bugada."*

---

## 1. O diagnóstico — três defeitos, e nenhum dava erro

A âncora de escalada aparece em dois lugares: nos **sete checkpoints** da
dimensão da árvore e no **pé da árvore**, no Overworld. Os dois estavam quebrados,
por motivos diferentes.

### 1.1 A âncora media o raio NOMINAL, e encostava na casca REAL

```java
return new BlockPos(Math.max(18, (int) Math.ceil(layout.trunk().radiusAt(y))) + 2, ...);
```

Duas coisas erradas na mesma linha:

- **`Math.max(18, ...)`** — um piso cravado, sem relação com a árvore. Onde o
  tronco tem raio 10, a âncora ia para x=20.
- **`radiusAt(y)`** devolve o raio do *perfil*. A casca que o gerador escreve é
  `irregularRadius`, com **lobos de até 4,5 blocos e ruído de até 2** — ou seja,
  a superfície real fica entre 6,5 blocos para dentro e 6,5 para fora do nominal.

Medido na seed 1000, checkpoint CLOUD: âncora em **x=28**, casca em **21,1**.
**6,9 blocos de ar** entre as duas.

> **A forma da casca só existia dentro do laço que escreve blocos.** Quem
> precisava encostar nela usou o número que estava à mão. É o erro nº 7 do
> `CLAUDE.md` na forma geométrica: duas fontes para a mesma verdade, uma delas
> escondida num laço de desenho.

### 1.2 E a plataforma ia junto

Com a âncora deslocada, a ponta interna da varanda parava em **x=22** com a casca
em **21,1**: o balcão inteiro nascia no ar, sem tocar o tronco. Não cai — nada cai
em Minecraft —, só fica pendurado. É a "base bugada".

### 1.3 A copa comia a plataforma

A ordem em `buildSurface` era: tronco → galhos → coroa → **copa** → checkpoints.

A plataforma do checkpoint só preenche **ar**. A copa também. Com a copa
primeiro, cada folha já posta virava um buraco na varanda — e mais furada quanto
mais alto o checkpoint, porque CROWN (y=1250) e SUMMIT (y=1450) ficam debaixo dos
discos do líder central, que têm raio de até 168.

**Dois geradores educados, cada um respeitando o que o outro já tinha escrito.**
Nenhum erro em lugar nenhum; uma varanda em forma de peneira.

### 1.4 No Overworld, três números cravados

```java
BlockPos anchor = new BlockPos(layout.overworldOriginX() + 49, BASE_Y + 16, ...);
// e UM bloco de lenho morto embaixo
```

O `49` não consulta o fuste; o `+16` não consulta nada. **Dezesseis blocos acima
do plano da base**, com um único bloco pendurado por baixo. E o "um bloco embaixo"
é a mesma varanda dos checkpoints, duplicada e já divergida até virar um pingente.

---

## 2. O que mudou

| Peça | Antes | Depois |
| --- | --- | --- |
| forma da casca | só dentro de `WorldTreeTrunkGenerator` | `WorldTreeTrunkSurface`, puro, com o escritor **delegando** |
| x da âncora (tronco) | `max(18, nominal) + 2` | `ceil(casca real) + 1` |
| x da âncora (Overworld) | `origem + 49` | `origem + ceil(casca real) + 1` |
| y da âncora (Overworld) | `64 + 16` | `64 + 2` |
| base no Overworld | um bloco de lenho | a mesma varanda dos checkpoints |
| forma da varanda | duplicada nos dois geradores | `WorldTreeAnchorPlatform` |
| ordem em `buildSurface` | copa **antes** do checkpoint | checkpoint **antes** da copa |

### O ponto fixo da casca

`irregularRadius` depende de `x`, e `x` é o raio — a conta é circular por
construção. O termo que cria a circularidade é o ruído, com período de ~33 blocos
e amplitude 2: um bloco de erro em `x` move o resultado menos de 0,4. Três passos
levam o resíduo para baixo de 0,01, e o portão `umaFonteSoParaACasca` verifica que
o raio devolvido é um raio que `irregularRadius` **confirma** naquela coordenada.

### Por que o `y` do Overworld não vem do heightmap

Seria o óbvio: assim a varanda acompanha o relevo em vez de flutuar. Mas o
heightmap só responde por colunas do chunk **atual**, e a varanda tem 13 × 9
blocos — ela cruza fronteira de chunk quase sempre. Chunks vizinhos, gerados em
momentos diferentes, leriam alturas diferentes e cada um desenharia o seu pedaço
num nível próprio: **uma plataforma partida em degraus na fronteira**.

Seria trocar "flutuando" por "quebrada", e a segunda é mais difícil de atribuir à
causa. Fixo em `BASE_Y + 2`, a varanda pousa sobre a saia da base do fuste, que o
próprio gerador escreve até y=65 em toda a volta.

---

## 3. O portão — `AncoraEncostaNaMadeiraTest`, em 20 seeds

Havia dois testes para `anchorPosition`, e os dois perguntavam a mesma coisa: em
que **chunk** a âncora cai, e se a conta bate com ela mesma. **Nenhum perguntava
se ela encosta no tronco** — e por isso ficaram verdes durante todo o tempo em que
a âncora nasceu flutuando.

| Verificação | Contra o quê |
| --- | --- |
| a âncora fica fora da casca, e a até 2,5 blocos dela | 1.1: enterrada ou flutuando |
| a ponta interna da varanda cai dentro da casca | 1.2: balcão solto |
| a âncora do Overworld encosta no fuste | 1.4: os três números cravados |
| o ponto fixo da casca converge | a "casca" ser um número que o escritor não reconhece |
| a âncora acima do tronco segue o líder | o topo |
| o checkpoint é escrito antes da copa | 1.3: a varanda em forma de peneira |

### O que aconteceu ao alimentar os portões

**Duas quebras mordem, uma não — e a que não mordeu ensinou mais.**

Reintroduzir `max(18, nominal) + 2` reprova **os dois** primeiros casos, com os
números do diagnóstico acima.

Encurtar o alcance da varanda **não reprova**, e está certo não reprovar. No
conserto eu tinha alargado a elipse de -6 para -9, achando que a ponta interna
afinada fazia parte do defeito. Com a âncora no lugar, seis blocos já entram cinco
blocos casca adentro, e a elipse só afina onde não se escreve nada. **A mudança
foi desfeita.**

> Registrado porque *"a régua não pegou"* e *"a mudança não era necessária"* são
> coisas diferentes, e confundir as duas afrouxa portão bom. Quem soltava a
> varanda no ar era a âncora, sozinha.

E o portão ganhou um defeito **próprio** no caminho: a primeira versão tinha uma
**cópia** do alcance da plataforma. Encurtar o laço do gerador não reprovava nada
— o teste media o 9 dele contra a árvore. Uma régua com a cópia do número que ela
vigia não vigia coisa nenhuma. Hoje ela lê `WorldTreeAnchorPlatform`.

---

## 3-B. A prateleira virou CABANA

**Relato de jogo, segunda rodada:** a âncora parou de flutuar — e continuou lendo
mal. Uma tábua nua projetada de uma parede de casca parece pedaço de mundo
quebrado, não lugar construído. O pedido: *"faça um revamp da lógica de spawn
deles… localizados em uma pequena casa, em um galho da própria árvore."*

### O que a cabana resolve que a prateleira não resolvia

A prateleira só preenchia **ar**: ela era educada com tudo o que já estivesse no
lugar, e por isso saía furada. A cabana **afirma o próprio volume** — o interior é
esvaziado e as paredes são escritas por cima do que houver. Não existe estado do
mundo que produza meia cabana.

Pegada 7 × 7, interior 5 × 5 × 3. Piso, paredes de casca com cantos escuros,
teto de alburno, porta de 2 blocos na face voltada para o eixo, uma janela em
cada face lateral, e uma **folha luminosa** no forro — o único bloco que emite luz
nesta árvore.

### A trava que decidiu o desenho inteiro

`WorldTreeCheckpoint.nearest(y)` mapeia a altura da âncora de volta para o
checkpoint com tolerância de **24 blocos**. Pôr a cabana na altura do *galho* —
que é o óbvio, e foi a primeira ideia — a tira dessa janela: o jogador clica na
âncora e recebe *"este anchor não pertence à rota"*. Cabana bonita, checkpoint
morto, zero erros.

Por isso o piso fica **sempre** em `checkpoint.y() - 1`, e quem sobe até ele é um
**pilar** de lenho a partir do galho.

### Nem todos os sete ficam num galho, e isso está medido

As cinco zonas de galho vão de y=320 a y=1480.

| | y | onde fica | por quê |
| --- | --- | --- | --- |
| BASE | 48 | eixo central | galho mais próximo a **297** blocos |
| LOWER | 260 | eixo central | galho mais próximo a **85** blocos |
| CLOUD, MID, CANOPY, CROWN | 400–1250 | **galho**, 20/20 seeds | |
| SUMMIT | 1450 | galho **ou** líder | os galhos do topo *nascem* em y=1450; conforme a seed o topo deles fica 8 blocos acima do piso |

O portão não finge que os sete ficam em galho: ele exige os quatro que sempre
podem e deixa o SUMMIT livre entre os dois apoios, com o motivo escrito.

### Três defeitos encontrados escrevendo isto

**O fallback usava o tronco onde não há tronco.** O tronco acaba em y=1200; de
1100 a 1450 quem sobe é o **líder**, com outro eixo e outro raio. A primeira
versão fixava o `y` no topo do tronco e usava o raio de lá para qualquer altura —
acima de 1200 a cabana teria nascido no ar, no raio errado. Literalmente o defeito
que esta trilha veio consertar, reintroduzido pelo conserto.

**O BASE nascia no vazio.** Ele mora em y=48 e o tronco **começa** em y=48: o piso
em `y - 1` caía em 47, e abaixo do pé a dimensão é vazio. Nenhum dos outros
portões via — `cabanaApoiada` comparava a parede com a casca no `y` clampado e a
conta fechava. Nasceu `cabanaAcimaDoPeDaArvore`.

**Havia três cópias da posição da âncora.** O gerador punha em `origem + 49, y=80`;
o teleporte de `WorldTreeCheckpointService` mandava o jogador para
`origem + 49.5, y=80`; e a conferência de proximidade repetia os mesmos números
uma terceira vez. Mover a cabana teria deixado duas delas apontando para o ar, sem
erro nenhum — o jogador só cairia. Hoje as três perguntam a `WorldTreeClimbingPost`.

### O que os portões pegam

| Verificação | Contra o quê |
| --- | --- |
| a âncora fica na janela de `nearest()` | cabana na altura do galho: checkpoint morto |
| a cabana tem madeira embaixo, galho ou eixo | 1.1 e 1.2: flutuando |
| nenhuma cabana abaixo do pé da árvore | BASE no vazio |
| os checkpoints de 400 a 1250 ficam em galho | o pedido |
| o jogador pousa dentro, e não em cima da âncora | ser empurrado para fora ao viajar |
| mesma seed, mesma cabana | 7 × 7 cruza chunk: metades diferentes |
| o checkpoint é escrito antes da copa | folhagem por dentro |

Alimentados com os defeitos: remover o clamp do pé reprova no BASE; pôr a cabana
na altura do galho reprova com *"a âncora em y=1409 é reconhecida como null"*.

---

## 4. O que isto NÃO prova

- **Nada foi visto em jogo.** O portão mede geometria pura; ele não coloca um
  bloco.
- **A cabana nunca foi vista em jogo.** Os portões medem onde ela fica e em que
  ela se apoia; nenhum coloca um bloco. Se a porta ficar virada para o vazio ou o
  pilar nascer torto, só uma captura mostra.
- **"Encosta na casca" não é "dá para subir".** Alcance de pulo, espaço livre
  acima da varanda e se a rota entre dois checkpoints é escalável continuam sem
  régua.
- **O relevo do Overworld não entra na conta.** Com `y` fixo, um balcão pode
  nascer dentro de uma encosta. É o preço declarado de não usar o heightmap
  (seção 2).
- **A ordem em `buildSurface` tem portão de TEXTO, e não de comportamento.**
  `checkpointVemAntesDaCopa` lê a fonte e compara duas posições. Provar a ordem
  de verdade exigiria um chunk, e portanto um gametest. É pouco — e é muito mais
  que o comentário que era toda a proteção que existia.
