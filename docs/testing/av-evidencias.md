# Evidências da trilha AV

Registro do que **já foi executado e olhado** na trilha do visual da aura, com
data e commit. Existe para que ninguém precise reconstruir isso de memória, e
para que ninguém confunda "rodou" com "gate aprovado".

O roteiro do que *deve* ser verificado está em
[`av-aura-visual.md`](av-aura-visual.md). Os limites conhecidos da trilha estão
em [`o-que-nao-provamos.md`](o-que-nao-provamos.md). A direção visual — o que
reprova — está em [`../vfx/direcao-visual-da-aura.md`](../vfx/direcao-visual-da-aura.md).

---

## 1. Duas palavras que não são sinônimas

**Verificado** é um comando que rodou e uma linha que alguém leu no log, ou uma
tela que alguém olhou. Vale como fato e fica aqui registrado.

**Gate aprovado** é a matriz inteira daquele gate cumprida: todas as capturas do
conjunto mínimo, comparadas lado a lado com a referência, arquivadas com data e
commit. Isso é um evento de PR, e é o que fecha a issue do gate.

> Até 2026-09-13, a trilha AV tem **verificação**, e **nenhum gate aprovado**.
> Os gates #169 (AV0 — a shell acompanha as animações em servidor dedicado) e
> #176 (AV1 — Ten convincente sem nenhuma partícula) continuam **abertos**.
>
> Em 2026-09-13 a averiguação da [seção 4b](#4b-a-averiguação-dirigida-de-2026-09-13)
> respondeu item a item a matriz de aderência do AV0, e **nada reprovou**. Isso
> continua não fechando o #169: o que fecha é imagem arquivada. A diferença
> agora é que a ferramenta para produzi-la existe (seção 4c), e não existia.

A tentação aqui é escrever "AV0 e AV1 estão prontos, foram vistos em jogo". O
código está na `main` e a pessoa que olhou disse que está no caminho certo —
mas o caminho certo não é o critério de nenhum dos dois gates.

---

## 2. AV0 — a shell inflada veste o jogador

PR #214, commit `2f60428` na `main`, 2026-09-12.

| Comando | Resultado observado |
| --- | --- |
| `gradlew build` (na máquina) | verde, **`Testes executados: 362`** |
| `build` no CI | **`Relatórios: 75` &#124; `Testes executados: 370`** |
| `gradlew runServer` | **`Done (10.676s)`**, zero `NoClassDefFoundError`, **nenhuma linha de aura** no log do dedicado |

A última célula é o ponto do AV0 inteiro: a camada nova é `client/vfx/`, e o
servidor dedicado não pode saber que ela existe. Silêncio, ali, é o resultado
esperado — e é a única vez nesta trilha em que ausência de linha no log conta
como prova, porque o que se quer provar é justamente a ausência.

> A contagem local (362) e a do CI (370) **não batem, e isso é normal**: o CI
> roda a suite completa e a máquina tem tarefas que podem sair `UP-TO-DATE`.
> Quem confere o número no CI é o passo "Contagem de testes", que soma os XML —
> ver a linha `Task :test FROM-CACHE` em [`o-que-nao-provamos.md`](o-que-nao-provamos.md).

### O falso verde que esta execução produziu

A **primeira** execução do `runServer` morreu com uma `IOException` de lock de
arquivo — e o Gradle imprimiu **`BUILD SUCCESSFUL`** assim mesmo.

É o terceiro exemplar registrado da seção 5 de
[`o-que-nao-provamos.md`](o-que-nao-provamos.md) ("a tarefa do Gradle pode
passar enquanto o jogo dentro dela morre"), e o mais barato de reproduzir. A
conclusão operacional não mudou: **de uma tarefa `run*`, o código de saída não
diz nada.** A conclusão vem de procurar `Done (` no log — que é de onde saiu o
`Done (10.676s)` acima.

---

## 3. AV1 — shell autoral, com shader próprio

PR #219, commit `8c73882` na `main`, 2026-09-12.

| Comando | Resultado observado |
| --- | --- |
| `gradlew build` | verde, **`Testes executados: 367`** |
| `gradlew runServer` | **`Done (9.958s)`**, zero `NoClassDefFoundError` |
| `gradlew runClient` | **`Shader da aura carregado: shell com Fresnel, ruido e fluxo proprios.`** e **`Aura anexada a 2 renderer(s) de jogador.`** |

As duas linhas do cliente respondem a duas perguntas diferentes, e nenhuma das
duas responde pela outra:

- a primeira diz que o **shader compilou** (`RegisterShadersEvent` disparou e o
  GLSL passou pelo driver);
- a segunda diz que a **layer foi anexada aos dois modelos** de jogador,
  `default` e `slim` — o `2` é o número que importa. Um `1` ali significaria que
  um dos dois modelos ficou sem aura, e olhar a tela com uma skin só nunca
  perceberia isso.

### O ponto cego que esta execução encontrou

Na **primeira** execução do `runClient`, o log não tinha linha nenhuma sobre o
shader. Nem erro, nem sucesso — porque o código só relatava falha.

E aí não havia como distinguir dois mundos:

| Situação | O que o log mostrava | O que a tela mostrava |
| --- | --- | --- |
| o shader carregou em silêncio | nada | shell desenhada pelo caminho novo |
| o evento nunca disparou | nada | shell desenhada pelo *fallback* do AV0 |

**"Não vi erro" não é evidência.** Foi preciso acrescentar o log de sucesso e
rodar de novo — e é por isso que a linha existe hoje, com o motivo escrito no
comentário de `AuraShaders.registrar`. Esta é a mesma classe de falha da seção 4
de [`o-que-nao-provamos.md`](o-que-nao-provamos.md) ("o detector óbvio mente"),
na variante mais barata: o detector não existia, e a ausência dele parecia
aprovação.

---

## 4. A verificação visual de 2026-09-12

**Uma pessoa, cliente de desenvolvimento, Ten e Ren ligados em jogo.**
Relato textual, integral: *"está no caminho certo"*.

Isso é um fato e fica registrado. É também **todo** o registro visual que a
trilha tem até aqui — não há captura arquivada, não há comparação lado a lado
com nenhuma das referências de [`../aura-art/`](../aura-art/LEIA-ME.md), e não há
segunda pessoa olhando.

### O que **não** foi conferido

Nenhum destes itens da matriz dos gates foi olhado:

| Eixo | O que falta |
| --- | --- |
| pose | correndo, agachado, nadando |
| equipamento | com armadura vestida |
| modelo | skin `slim` (a layer está anexada aos dois; **ninguém viu a `slim` na tela**) |
| ambiente | de dia, de noite, em caverna |
| distância | 2, 5, 10, 20 e 40 blocos |
| câmera | primeira pessoa |
| **o critério do [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md)** | `vfx.densidadeDeParticulas = 0.0` — *Ten ainda se lê sem nenhuma partícula?* |

O último é o que decide o gate #176, e é o único que não pode ser aproximado
por nenhum outro: enquanto o
`EmissorDeParticulasDeAura` continuar ligado, **o que se vê na tela são os dois
efeitos somados**, e não dá para saber quanto da leitura vem da shell. O limite
está declarado em [`o-que-nao-provamos.md`](o-que-nao-provamos.md) ("entre os
dois o jogo mostra os dois").

### Duas perguntas continuam sem resposta

São chutes declarados por quem implementou, e não decisões tomadas:

1. **O sentido do fluxo: a energia sobe ou desce?** O uniforme de fluxo está num
   sentido porque alguém precisou escolher um. Não houve comparação A/B, e as
   referências não desempatam sozinhas.
2. **A escala de ruído `4.0` sobre a UV da skin.** Quantas repetições do ruído
   cabem na superfície é um número que saiu de tentativa, não de medição. Escala
   errada é exatamente o que faz a aura virar "fumaça" ou "plástico" na tabela de
   [`../vfx/direcao-visual-da-aura.md`](../vfx/direcao-visual-da-aura.md).

As duas se resolvem com captura A/B, e não com argumento. Ficam registradas aqui
para que não virem folclore — "sempre foi assim" é a resposta que um número sem
justificativa recebe seis meses depois.

---

## 4b. A averiguação dirigida de 2026-09-13

**Uma pessoa, instância de teste, roteiro de perguntas fechadas.** Diferente do
relato de 2026-09-12 — que era uma frase — esta passada respondeu item a item a
matriz do AV0 e parte da do AV1. **Continua não sendo aprovação de gate**: não
há captura arquivada, não houve comparação lado a lado com a referência B e não
houve segunda pessoa olhando. É relato, e entra aqui como relato.

### Aderência — a pergunta que o AV0 existe para responder

| Verificação | Resposta |
| --- | --- |
| a shell acompanha correr | acompanha **colada**, sem descolar em articulação |
| acompanha o swing de ataque | acompanha |
| acompanha agachar e nadar | acompanha **sempre** |
| modelo `slim` × `default` | **proporcionais a cada skin** — cada modelo usou o seu |
| skin com segunda camada completa | a aura **continua visível**; a camada não engole |
| z-fighting girando a câmera 360° | **nenhum** |
| morte, respawn, dimensão, relog | **não fica presa**; só desativa |

> Nenhum item de aderência reprovou. **A trilha não para no AV0** — o que falta
> ali é a evidência arquivada, e não a resposta.

### O critério do ADR-015

Com partículas no mínimo, Ten e Ren em output 100%:

> *"É claro que está em Ten."*

E, perguntado se a aura se parece com alguma coisa da lista de reprovação da
[direção visual](../vfx/direcao-visual-da-aura.md) seção 5 — fumaça, fogo,
eletricidade, poção, `Glowing`, armadura holográfica, esfera, outline genérico:
**nenhuma delas.**

### Duas respostas que NÃO são aprovação — são achados

Estas duas são o que esta passada produziu de mais útil, e nenhuma das duas
aparece como erro em lugar nenhum:

1. **A leitura está com "densidade contínua".** O critério do AV1 é que *a borda
   seja mais forte que o miolo* a 5 e a 20 blocos — é a borda que carrega a
   leitura a distância, e é dela que vêm os três expoentes de Fresnel separados.
   "Contínua" é a descrição de uma shell que ficou **uniforme**, que é o oposto
   do que os três passes existem para produzir. Não está fechado se é o
   `reforco_da_borda`, o expoente, ou a leitura da pergunta — e é exatamente o
   tipo de dúvida que o slider de Fresnel resolve em minutos.
2. **No Ten o ruído não tem veios; no Ren tem.** O critério é "veios, não
   nuvens". Os dois modos usam o mesmo shader e a mesma textura, e diferem só
   nos números do perfil — então a diferença está em `escala_de_ruido`,
   `velocidade_de_fluxo` ou no alpha que deixa o padrão aparecer. É o primeiro
   caso concreto de tuning que a sessão de arte tem para atacar.

### Uma das duas perguntas em aberto foi respondida

A seção 4 registrava dois chutes de quem implementou. Um deles saiu do folclore:

| Pergunta | Situação |
| --- | --- |
| **o sentido do fluxo** | **respondida: ele SOBE**, e subir é o que se quer para Ten |
| **a escala de ruído `4.0`** | **continua em aberto** — "inconclusivo" foi a resposta, e ela só fecha com A/B lado a lado |

A segunda é justamente o que o slider de ruído existe para resolver: comparar
`2.0`, `4.0` e `8.0` na mesma pose e na mesma luz, sem recompilar entre uma e
outra.

---

## 4c. A bancada de captura existe (issue #168)

PR em `feat/av0-nenvfx-captura`, 2026-09-13.

Até aqui, **nenhuma captura desta trilha podia ser comparada com outra**: cada
imagem saía com hora, clima, HUD, câmera e densidade de partícula diferentes, e
o protocolo A/B da [seção 3](av-aura-visual.md) exige as cinco iguais. O que
mudou:

| O que | Onde |
| --- | --- |
| `/nenvfx` (estado, output, partículas, ribbons, LOD, ajustes, freeze, reset, status) | `client/vfx/debug/AuraDebugCommands` |
| overlay de tuning em **F6**, com custo do quadro e aviso de sobreposição | `client/vfx/debug/AuraDebugRenderer` |
| sliders sem recompilar (`/nenvfx tuning`) | `client/vfx/debug/TelaDeTuningDeAura` |
| modo de captura: hora, clima, HUD e câmera travados | `client/vfx/debug/AuraCaptureMode` |
| lote de um ponto: `ten`, `ten_sem_particulas`, `ren`, `ren_sem_particulas`, `zetsu`, `sem_aura` | `client/vfx/debug/RoteiroDeCaptura` |
| nome com data, commit e nível de bloom | `client/vfx/debug/NomeDeCaptura` + `nenfoundation-build.properties` |
| réguas de custo: chamadas de desenho, filamentos, partículas, jogadores | `client/vfx/MedidorDeVfx` |

| Comando | Resultado observado |
| --- | --- |
| `gradlew build` (na máquina) | verde, **`Testes executados: 614`** |
| quebra deliberada no `limpar()` da sobreposição | **3 testes reprovaram**, incluindo o que existe para isso |

**O que esta entrega NÃO prova:** nada de aparência. Ela não foi rodada em
`runClient` ainda — nenhum comando foi digitado em jogo, nenhuma captura foi
gravada em disco, e o overlay não foi visto na tela. O que está provado é a
lógica sem tela: a máquina do lote, a sanitização do nome, o fechamento das duas
janelas de medição e o fato de a sobreposição não sobreviver ao logout.

---

## 4d. Os últimos números de arte saíram do código (issue #98)

PR em `fix/av0-preset-orfao`, 2026-09-13.

A issue #98 do AV0 pedia, com todas as letras, que **`AuraVisualPreset` saísse**
— "config órfã dentro do código, erro nº 7 do `CLAUDE.md`". Ele continuava lá, e
o levantamento encontrou o quadro completo:

| Record | Campos | Leitores no repositório |
| --- | --- | --- |
| `AuraVisualPreset` | `shellScale`, `edgeIntensity`, `flowIntensity`, `pulseAmplitude`, `pulseFrequency` | **nenhum** |
| `AuraVisualPreset` | `particleIntensity`, `shellOpacity` | só o emissor de partículas |
| `AuraVisualProfile` | os oito | **nenhum**, fora do próprio teste dele |

Os cinco primeiros eram o botão morto na forma mais pura: giravam, e nada do
outro lado. `AuraVisualProfile` era pior — ele tinha um teste, e o teste
comparava dois métodos estáticos da própria classe, ou seja, provava a si mesmo.

O que mudou:

| O que | Para onde |
| --- | --- |
| `AuraVisualPreset`, `AuraVisualProfile` | **removidos** |
| `particleIntensity` | `densidade_de_particula`, em `nen_vfx/*.json` |
| `shellOpacity` (que dimensionava **partícula**, apesar do nome) | `tamanho_de_particula`, no mesmo arquivo |
| `AuraVisualState` | perdeu o componente `preset`; quem desenha busca o perfil por `AuraPerfis.de(estado.mode())` |

Consequência que vale para a sessão de arte: **os dois números de partícula
agora recarregam com `F3+T`**, como os da shell — e um _resource pack_ pode
sobrepô-los.

| Comando | Resultado observado |
| --- | --- |
| `gradlew build` (na máquina) | verde, **`Testes executados: 619`** |
| `gradlew runServer` | **`Done (4.649s)`**, zero `NoClassDefFoundError`, zero `ClassNotFoundException`, nenhuma linha de `client/vfx` |
| densidade de Ren rebaixada abaixo da de Ten, de propósito | **4 testes reprovaram**, em quatro arquivos diferentes |
| `apagado()` deixando a densidade passar, de propósito | **2 testes reprovaram**, incluindo o que existe para isso |

As duas últimas linhas são o que a régua nova vale. Alimentar o portão com o
defeito é a única forma de saber que ele morde; sem isso, `apagadoEZero` seria
carimbo.

**O que esta entrega NÃO prova:** nada de aparência, de novo. Os números são os
mesmos de antes — a mudança é de onde eles vêm, e não de quanto valem. Ninguém
abriu o cliente. Se a partícula mudar de aspecto em jogo, o suspeito é a leitura
do perfil, e não o valor.

---

---

## 4e. Os números de filamento também saíram do código (issue #178)

PR em `feat/av2-filamentos-de-dado`, 2026-09-13.

`AuraRibbonProfile` carregava `ten()` e `ren()` — cinco números de arte cada —
com um javadoc dizendo, em voz alta: *"saem daqui quando o perfil em datapack
existir (#98)"*. **O perfil existia havia dois gates.** A issue #178 já previa
exatamente esta dívida e o que ela vira se ficar: *"número que fica nos dois
lugares vira botão morto"*.

O bloco `filamentos` passou para `nen_vfx/*.json`, e o renderer busca por
`AuraPerfis.de(estado.mode()).filamentos()` — o **mesmo** caminho da shell. O
`switch` por modo que havia no renderer sumiu junto: `ZETSU` e `OFF` já recebiam
o perfil apagado, e o apagado agora vem sem filamento nenhum.

### O portão encontrou um defeito no próprio código desta entrega

Vale registrar porque é o caso limpo de régua que morde antes do merge, e não
depois.

O `AuraRibbonProfile` conferia `comprimento_min ≤ comprimento_max` **no
construtor** — e o construtor **lança**. Como o `Codec` constrói para só depois
validar, um resource pack com os dois trocados derrubaria o **reload de recursos
inteiro** — não só a aura — em vez de virar um erro com motivo e cair no perfil
de emergência.

É exatamente a armadilha que `AuraPerfilVisual` já documentava na ordem do
Fresnel, repetida uma pasta ao lado. Nada em jogo tinha acusado: o construtor só
era chamado com constantes corretas, escritas no próprio código. **Foi o teste
novo do caminho de dado que o encontrou**, no primeiro `test` depois de escrito.
A regra mudou de lugar, para `validate`.

| Comando | Resultado observado |
| --- | --- |
| `gradlew build` (na máquina) | verde, **`Testes executados: 621`** |
| `gradlew runServer` | **`Done (2.465s)`**, zero `NoClassDefFoundError`, zero `ClassNotFoundException` |
| Ren com menos filamentos que Ten, de propósito | **2 testes reprovaram**, em dois arquivos |
| `apagado()` deixando o filamento passar | **1 reprovou**, o que existe para isso |

**O que esta entrega NÃO prova:** nada de aparência. Os cinco números são os
mesmos; mudou de onde vêm. Ninguém abriu o cliente.

**Uma correção de rota, encontrada no caminho:** a linha de
[`o-que-nao-provamos.md`](o-que-nao-provamos.md) que dizia *"o renderer ainda
não lê a distribuição"* estava **obsoleta desde o AV1** — `AuraPlayerRenderLayer`
multiplica por região no passe de shell e nos filamentos. O que continua
faltando do #175 é o outro lado: **o overlay de dev não mostra os seis fatores**,
e multiplicador que ninguém observa é folclore. A linha foi corrigida para dizer
isso.

---

---

## 4f. Os seis fatores por região ganharam régua (issue #175)

PR em `feat/av1-regua-de-regiao`, 2026-09-13.

A issue #175 diz a frase em uma linha: *"a régua entra junto do número, senão o
multiplicador é um valor que ninguém consegue observar"*. O renderer multiplica
intensidade e alpha por `AuraBodyRegion` **desde o AV1** — no passe de shell e
nos filamentos —, e até aqui **ninguém tinha como ver isso acontecendo**: com
tudo em 1.0 o multiplicador é invisível por construção.

O overlay **F6** ganhou a linha `regioes:`, e ela tem três formas, de propósito:

| Situação | O que aparece |
| --- | --- |
| distribuição plana | `regioes: uniforme 1.00` |
| desigual (Gyo, Ko) | `regioes: cab 0.70  tor 0.70  bE 0.70  *bD 1.80  pE 0.70  pD 0.70` |
| sem aura | `regioes: -- (sem aura)` |

Três decisões que o teste guarda:

1. **Plana não repete seis vezes o mesmo número.** Uma linha que se repete deixa
   de ser lida no terceiro dia, e ler é o único propósito dela.
2. **O pico sai marcado com `*`.** É o que torna "Gyo no braço direito" legível
   de relance, em vez de exigir comparar seis números.
3. **Sem aura é `--`, e nunca seis zeros.** Zetsu zera *de propósito*; "não há
   estado" não zera nada. Escrever `0.00` nos dois apagaria a diferença que este
   overlay inteiro existe para preservar.

| Comando | Resultado observado |
| --- | --- |
| `gradlew build` (na máquina) | verde, **`Testes executados: 626`** |
| `gradlew runServer` | **`Done (2.845s)`**, zero `NoClassDefFoundError` |
| overlay escrevendo zeros no lugar do traço, de propósito | **1 teste reprovou** |
| um rótulo a menos que o enum de regiões | **3 reprovaram** |

O segundo é o que fecha um buraco de portão real: `AuraDistribution` tem um
`switch` exaustivo, e o compilador reprova quando uma região nova entra no enum.
**Um vetor de rótulos não reprova em lugar nenhum** — a linha simplesmente
mostraria seis de sete, sem erro.

### Um falso verde apareceu no caminho, e é o da seção 5

Depois de restaurar a segunda quebra deliberada, `gradlew build` respondeu
**`BUILD SUCCESSFUL`** com `> Task :build UP-TO-DATE` — logo depois de uma
execução em que três testes tinham reprovado. O verde era da tarefa pulada, e
não de teste nenhum. Foi preciso `test --rerun-tasks` (2 min) para ter um verde
que significasse alguma coisa. Mesma família do `BUILD SUCCESSFUL` com o jogo
morto dentro, registrado na seção 2.

**O que esta entrega NÃO prova:** que os seis fatores já sejam diferentes de 1.0
em jogo. **Nada no jogo produz distribuição desigual ainda** — Gyo, Ko e Ryu são
marcos de Nen, não da trilha AV. Os valores de Gyo (`1.8` num braço) e de Ko
(quase tudo num membro) estão provados em JUnit, e não em tela. E ninguém abriu
o cliente nesta entrega.

---

## 5. O que cada execução prova, e o que ela não prova

No estilo de [`o-que-nao-provamos.md`](o-que-nao-provamos.md), e com as mesmas
regras de leitura.

| Execução | O que ela **prova** | O que ela **não** prova |
| --- | --- | --- |
| `gradlew build` (362/367 testes) | a lógica sem tela: transição, LOD, visibilidade, orçamento, seed, e os portões de documentação | **nada de aparência**. Render não é unit-testável; um verde aqui vale menos do que em qualquer outro marco |
| `runServer` `Done (...)` sem linha de aura | o servidor dedicado sobe e **não carrega nada de `client/vfx/`** | que a shell apareça, siga o corpo ou esteja correta — o dedicado não desenha nada |
| `runServer` `BUILD SUCCESSFUL` | **nada.** Já saiu verde com o jogo morto dentro | qualquer coisa. A conclusão vem de `Done (` no log |
| `Shader da aura carregado: ...` | o `RegisterShadersEvent` disparou e o GLSL compilou **neste driver, nesta GPU** | que os uniformes estejam certos, que o Fresnel esteja no expoente certo, ou que o resultado se pareça com a referência B |
| `Aura anexada a 2 renderer(s) de jogador.` | a layer foi anexada aos **dois** modelos de jogador | que a `slim` desenhe certo — ninguém viu a `slim` na tela |
| ausência da linha de erro do shader | **nada, antes do commit `8c73882`**; depois dele, com o log de sucesso presente, a ausência dos dois passa a ser sinal de que o evento não disparou | — |
| "está no caminho certo" (Ten e Ren, uma pessoa) | que a shell desenha em jogo e que a direção não foi rejeitada de cara | qualquer item das matrizes dos gates #169 e #176: pose, ambiente, distância, `slim`, armadura, primeira pessoa e o teste sem partícula |
| olhar a tela com partículas ligadas | que **alguma coisa** se vê | **quanto da leitura vem da shell** — os dois efeitos estão somados até o AV3 |
| uma sessão, uma GPU, uma skin | que funciona ali | *jitter*, cintilação e engasgo (que só aparecem em movimento), outras GPUs, e skins com transparência ou overlay completo |

---

## 6. Receita de teste manual, reproduzível

Serve para conferir a aura com as mãos, e é a mesma sequência que produziu o
relato de 2026-09-12. O ambiente é a instância de teste
([`../processo/instancia-de-teste.md`](../processo/instancia-de-teste.md)):

```powershell
.\scripts\instancia.ps1 servidor     # num terminal (recompila e troca o JAR sozinho)
.\scripts\instancia.ps1 cliente      # noutro; entra no servidor sozinho
```

Dentro do jogo, com permissão de OP:

```
/nen awaken
/nen category roll 12345
/nen technique unlock nenfoundation:ten
/nen technique unlock nenfoundation:ren
```

Depois:

| Tecla | O que faz |
| --- | --- |
| **R** (segurar) | abre a roda de Nen; é um **gesto**, não uma tela — soltar fecha |
| **C** | sobe o output em 5 pontos percentuais |
| **Shift+C** | desce o output em 5 pontos percentuais |
| **F5** | terceira pessoa (sem isso, só se vê a aura dos braços) |
| **V** | ficha do jogador, para conferir técnica e categoria |

### O aviso que economiza uma tarde

> **A intensidade da aura vem do OUTPUT, e não da aura atual.**
>
> Com o output em 0%, a técnica fica ligada, o servidor concorda, o perfil está
> certo — e **nada desenha na tela, sem nenhuma mensagem de erro.**

O output nasce em 100% e só desce por ação do jogador (Shift+C), então o caso
normal não pisa nisso. Mas quem tiver baixado o output numa sessão anterior de
teste vai abrir o jogo, ligar Ten, não ver nada e começar a investigar o
renderer — que está certo.

Antes de investigar qualquer coisa, confira o número:

```
/nen debug aura <jogador>
```

A linha traz `output=` junto da aura atual e da máxima. É a resposta autoritativa
do servidor, e não o que a barra da HUD desenhou.

E lembre do outro lado da mesma moeda: **Zetsu leva tudo a zero de propósito**
(shell, ribbons, partículas, bloom). "Não vejo nada com Zetsu ligado" é o
comportamento aprovado, não um defeito — ver
[`../vfx/direcao-visual-da-aura.md`](../vfx/direcao-visual-da-aura.md), seção 2.

---

## 7. Como conferir no log que o shader carregou de verdade

O log do cliente de desenvolvimento fica em `instancia/cliente/logs/latest.log`
(ou `run/client/logs/latest.log`, num `runClient` sem `-PdirCliente`).

Procure **as duas** linhas:

```
Shader da aura carregado: shell com Fresnel, ruido e fluxo proprios.
Aura anexada a 2 renderer(s) de jogador.
```

```powershell
Select-String -Path instancia\cliente\logs\latest.log -Pattern 'Shader da aura|Aura anexada'
```

E o que cada resultado significa:

| O que aparece | Leitura |
| --- | --- |
| as duas linhas | shader compilado **e** layer anexada aos dois modelos |
| só `Aura anexada` | o shader **não** carregou; a shell está desenhando pelo material simples do AV0. Procure a linha de erro `Shader da aura nao carregou;` logo acima |
| só `Shader da aura carregado` | o shader existe mas **ninguém desenha com ele** — `AddLayers` não anexou nada |
| `Aura anexada a 1 renderer(s)` | um dos dois modelos de jogador ficou de fora. Procure o `WARN` `Renderer de jogador '...' nao e um PlayerRenderer` |
| **nenhuma das duas** | o mod carregou sem a camada visual, ou o nível de log do console não chega em `DEBUG` — ver abaixo |

### A pegadinha do nível de log

`Shader da aura carregado` é **`INFO`**. `Aura anexada a N renderer(s)` é
**`DEBUG`**, e só aparece no console porque o `build.gradle` liga
`forge.logging.console.level = debug` em *todas* as execuções `run*`.

Consequência: **num cliente fora do Gradle** — um launcher, uma instância de
modpack —, a segunda linha pode não aparecer sem que nada esteja errado. Ali, a
ausência dela não é evidência de nada. `latest.log` em disco costuma guardar mais
do que o console mostra; comece por ele antes de concluir qualquer coisa.

---

## 8. O que falta para fechar cada gate

Recorte operacional, para quem for retomar a trilha.

> **O bloqueio de governança do AV0 caiu em 2026-09-13.** A aprovação do Dev B
> no [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md), que a issue #98
> listava como condição para fechar, foi **relatada por @Vecolas** — não
> assinada pela própria pessoa. A distinção está registrada na tabela de
> governança do ADR, e o que ela destrava é o bloqueio nominal, não a aprovação
> visual: **capturas continuam sendo o único jeito de fechar o #169.**

**#169 (AV0)** — a aderência **foi respondida e nada reprovou** (seção 4b), e o
lado do servidor está verificado (seção 2). O que falta é **só evidência**: as
catorze capturas do conjunto do gate, arquivadas. O `/nenvfx off` que este
parágrafo cobrava existe a partir da seção 4c.

Receita, agora que a bancada existe — com dois clientes, um Steve e um Alex:

```
/nenvfx capture modo on
/nenvfx capture lote dia          <- seis imagens do mesmo ponto
```

Depois `/time set night` e repetir com `lote noite`; numa caverna, `lote
caverna`; correndo, `lote correndo`. Ao fim, `/nenvfx capture modo off`, e
mover `screenshots/nenfoundation-av/` para `docs/testing/capturas/AV0/`.

**#176 (AV1)** — Ten convincente **sem nenhuma partícula**. A resposta textual
já veio (*"é claro que está em Ten"*), e continua não fechando o gate. Falta o
conjunto mínimo arquivado e a comparação lado a lado com a referência B **na
mesma sessão**. A captura que decide sai do lote com o nome
`<etiqueta>_ten_sem_particulas__<data>__<commit>__bloom-ausente.png`, e a
linha de base para compará-la é a `sem_aura` do mesmo lote.

E antes de arquivar, os dois achados da seção 4b precisam de uma sessão de
slider: **a borda que não está mais forte que o miolo** e **o ruído sem veios no
Ten**. Arquivar capturas antes disso produz um conjunto que vai ser refeito.

Nenhum dos dois fecha por relato textual. O que fecha é imagem arquivada em
`docs/testing/capturas/AV<n>/`, com data, commit e nível de bloom no nome.

---

## 9. Quando este documento mente

Ele mente no dia em que alguém rodar uma verificação nova e não a registrar
aqui. Por isso ele guarda **execuções com número e linha de log**, e não
adjetivos: um `Done (9.958s)` ou um `Testes executados: 367` ou se bate com o
commit, ou não bate — e quem ler percebe.

O que **não** entra aqui é conclusão sobre aparência sem captura anexada. Esse é
o único tipo de frase que este arquivo não aceita, porque é exatamente a frase
que faz um gate parecer fechado sem estar.
