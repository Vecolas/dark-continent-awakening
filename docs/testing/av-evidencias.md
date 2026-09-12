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

> Até 2026-09-12, a trilha AV tem **verificação**, e **nenhum gate aprovado**.
> Os gates #169 (AV0 — a shell acompanha as animações em servidor dedicado) e
> #176 (AV1 — Ten convincente sem nenhuma partícula) continuam **abertos**.

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

**#169 (AV0)** — a shell acompanha corrida, pulo, ataque, agachar e nadar, nos
dois modelos, e o dedicado continua limpo. O lado do servidor está verificado
(seção 2). Falta o lado da tela: captura em movimento, terceira pessoa, `slim` e
`default`, mais `/nenvfx off`, relog, morte e troca de dimensão sem estado preso.

**#176 (AV1)** — Ten convincente **sem nenhuma partícula**. Falta o conjunto
mínimo de capturas de [`av-aura-visual.md`](av-aura-visual.md) seção 3, a
comparação com a referência B, e o teste com `vfx.densidadeDeParticulas = 0.0`.

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
