# Instância de teste manual

Uma pasta com **Minecraft instalado e o mod rodando**, para abrir o jogo e
testar com as mãos — sem Gradle, sem launcher, sem conta.

Ela vive em `instancia/`, que é **ignorada pelo git**. O script que a cria é
versionado; ela não. Assim quem clonar o repositório recria tudo com um
comando, e nem o Minecraft instalado nem os mundos entram no histórico.

---

## Começando

```powershell
.\scripts\instancia.ps1 instalar     # uma vez, ~5 min e ~200 MB
.\scripts\instancia.ps1 servidor     # num terminal
.\scripts\instancia.ps1 cliente      # noutro
```

O cliente **entra no servidor sozinho**. Não há tela de "adicionar servidor".

```powershell
.\scripts\instancia.ps1 status       # o que existe e qual versão está lá
.\scripts\instancia.ps1 atualizar    # recompila e troca o JAR, sem subir nada
```

### Depois de mexer no código: nada

**`servidor` já recompila e troca o JAR antes de subir.** Não há passo manual
depois de escrever código, e é de propósito.

Por quê: o servidor daqui roda o JAR de `mods/`, e não o código do
repositório. Se a troca dependesse de alguém lembrar de rodar `atualizar`, um
dia ela não aconteceria — e o teste manual mediria a versão de ontem. **A
falha é silenciosa:** o servidor sobe, o mod carrega, o jogo funciona, e nada
em lugar nenhum diz que aquele não é o código que acabou de ser escrito.

Pior ainda no par: o `cliente` é o de desenvolvimento e **compila o
repositório a cada execução**, então ele sempre está na versão nova. Servidor
velho contra cliente novo produz divergências que parecem bug de
sincronização, e a investigação começa no lugar errado.

O Gradle é incremental: sem mudança no código, isso custa poucos segundos.

A troca **apaga a versão antiga antes de copiar** — dois JARs do mesmo
`mod_id` em `mods/` fazem o NeoForge recusar o boot, e depois de um bump de
versão os nomes são diferentes, então sobrescrever não resolveria.

### Quando o build está vermelho

```powershell
.\scripts\instancia.ps1 servidor -SemAtualizar
```

Sobe o JAR que já está instalado. É o único caso em que isso se usa —
investigar com o jogo aberto, ou comparar com a versão anterior. O script
avisa, em amarelo, que aquilo pode não ser o código atual.

O `status` compara a data do JAR instalado com a do último build, e avisa
quando estão diferentes.

---

## O que está dentro

```text
instancia/
├── servidor/        NeoForge dedicado instalado + mods/ + mundos + logs
└── cliente/         mundos, options.txt, capturas e logs do cliente
```

Os dois sobrevivem a um `atualizar`. É de propósito: perder o mundo de teste a
cada rebuild transformaria "testar" em "recriar o cenário".

---

## Quando a outra lane está no meio de alguma coisa

São duas pessoas neste repositório. Enquanto uma está escrevendo um mob — o
renderer pronto e a entidade ainda não — **a árvore não compila**, e `servidor`
e `cliente` param junto com ela. O trabalho dela não está errado: está pela
metade, que é o estado normal de quem está escrevendo.

**Rode da copia que e dona da instancia**, e aponte `-Codigo` para a limpa --
nao o contrario:

```powershell
cd C:\Users\<voce>\...\dark-continent-awakening   # a copia com instancia/
.\scripts\instancia.ps1 servidor -Codigo C:\dev\dca-lane-a
.\scripts\instancia.ps1 cliente  -Codigo C:\dev\dca-lane-a
```

O reflexo natural e o oposto: entrar na copia limpa, porque foi ela que
apareceu no comando. Dali o script nao acha instancia nenhuma -- e o passo
seguinte "obvio" seria `instalar`, que criaria uma **segunda** instancia,
baixaria o NeoForge de novo e deixaria os mundos para tras sem avisar. Por
isso a mensagem de erro diz o caminho onde procurou e entrega o comando
certo, em vez de so dizer que nao existe.

O mod é compilado **da outra árvore**; a instância não se move. Mundos,
`options.txt` e capturas continuam em `instancia/` desta cópia — é por isso que
`-PdirCliente` passou a ser caminho absoluto: relativo, ele seria resolvido
contra a raiz do *projeto*, e o jogador abriria um perfil vazio achando que
perdeu os mundos.

Use uma worktree em `main` (`git worktree add`), não um clone solto: assim o que
você joga é uma versão que existe no histórico, e não uma cópia particular.

> **A alternativa seria mexer nos arquivos da outra pessoa** para fazer o build
> passar — o erro nº 9 da lista do CLAUDE.md — ou esperar ela terminar. As duas
> são piores.

E há a saída menor, para quando você só quer entrar com o que já está
instalado:

```powershell
.\scripts\instancia.ps1 servidor -SemAtualizar
```

Ela sobe o JAR de `mods/` **sem recompilar**, e avisa em voz alta que aquilo
pode não ser o código atual.

---

## "The server send registries with unknown keys"

O jogo recusa a conexao dizendo que o servidor mandou uma chave que o cliente
nao conhece -- por exemplo
`ResourceKey[minecraft:entity_type / nenfoundation:master_of_the_swamp]`.

Parece dessincronizacao de protocolo ou mod faltando. Quase sempre e outra
coisa: **o cliente esta rodando um JAR velho**, e nao o codigo compilado.

O cliente de desenvolvimento compila o mod a partir do repositorio -- mas ele
tambem varre `instancia/cliente/mods/`, e **um JAR ali ganha do codigo, em
silencio**. O log nao entrega o problema: ele imprime `Nen Foundation
registrado` com versao e protocolo, os do JAR velho.

O script apaga esse JAR antes de subir o cliente, em voz alta. Ele e artefato
de build, refeito a qualquer momento -- nao e dado de ninguem.

> Se a chave desconhecida for de uma entidade que a outra lane acabou de
> escrever, a outra hipotese e mais simples: o servidor esta na versao nova e
> o cliente na antiga, ou o contrario. `status` mostra as duas datas.

---

## "outro processo bloqueou parte do arquivo"

Esse erro do Minecraft quer dizer **ja ha um servidor desta instancia no ar**.
O arquivo e o `session.lock` do mundo, e o processo e o servidor anterior --
mas a mensagem nao diz nem uma coisa nem outra, e vem depois de uma pagina de
stack trace do ModLauncher. Parece defeito do mod: o log mostra o Nen
Foundation carregando normalmente uma linha antes.

Acontece mais do que parece -- a janela do servidor anterior ficou atras de
outra, ou alguem fechou o terminal sem digitar `stop`.

O script agora recusa antes de chegar la, dizendo quem ocupa a porta e com que
PID. Para parar o anterior: `stop` no console dele, ou `Stop-Process -Id <pid>`.

---

## O mundo é normal, e não superplano

Ele já foi superplano — sobe rápido, e dá para andar sem obstáculo. O custo só
apareceu depois: **metade do que este mod faz depende do relevo e do bioma.**

| O que se testa | O que o superplano faz com isso |
| --- | --- |
| Regra de terreno do posto avançado | variação zero em todo lugar: o caso "recusado por terreno" nunca acontece |
| Regra de bioma do posto e dos inimigos | um bioma só, no mundo inteiro |
| Spawn natural de inimigos | sem caverna, sem altura, sem superfície variada |
| A aura vista contra o mundo | sempre o mesmo fundo, sempre a mesma luz |

Testar tudo isso num tabuleiro plano **aprova o que ninguém vai jogar** — e o
verde é indistinguível do verde de um teste que valeu.

> **Trocar `level-type` não regenera mundo nenhum.** O gerador fica gravado no
> `level.dat` quando o mundo nasce; depois disso a propriedade é ignorada em
> silêncio. O script grava uma marca ao lado do mundo e **recusa passar batido**
> quando as duas discordam — `servidor` e `status` avisam, com o comando para
> renomear o mundo antigo e deixar nascer um novo.
>
> `runServer` e o servidor de gametest **já usavam `normal`.** Esta instância —
> justamente a do teste manual, a única em que alguém olha para a tela — era a
> que discordava das outras duas.

---

## O cliente é o de desenvolvimento

E isso é uma limitação real, não um detalhe.

O `cliente` roda `gradlew runClient` com o diretório apontado para
`instancia/cliente`. **Não é** um cliente instalado por launcher.

Por quê: um cliente de launcher exige conta Microsoft e o launcher instalado. O
de desenvolvimento não exige nenhum dos dois, e é o mesmo binário do jogo.

**Se você quiser jogar por um launcher de verdade** — Prism, CurseForge,
MultiMC —, crie uma instância NeoForge 21.1.250 nele e aponte para o mesmo
JAR:

```text
build\libs\nenfoundation-<versao>.jar
```

Aí o `atualizar` precisa ser repetido à mão para aquela instância, porque o
script não conhece a pasta do launcher.

---

## Parâmetros úteis

```powershell
# dois jogadores, cada um com seu diretório
.\scripts\instancia.ps1 cliente -Jogador Gon
.\scripts\instancia.ps1 cliente -Jogador Kurapika

# abrir no menu, sem entrar em servidor nenhum
.\scripts\instancia.ps1 cliente -SemEntrar

# outra porta
.\scripts\instancia.ps1 instalar -Porta 25566
```

> Duas instâncias do Gradle **no mesmo diretório de projeto** travam uma na
> outra por lock de execução. Para dois clientes ao mesmo tempo são precisos
> dois `git worktree`. E há um limite de máquina medido: ver
> [qa-matrix.md](../testing/qa-matrix.md).

---

## O que o script decide por você, e vale saber

**Aceita a EULA da Mojang.** `eula.txt` é escrito como `true` na instalação.
Uma instância que não sobe não serve para teste manual, mas isso é um aceite —
está dito em voz alta no console.

**`online-mode=false` e RCON ligado.** É uma instância **local** de teste: sem
isso o cliente de desenvolvimento não entra. **Não exponha essa porta na
internet.**

**`dev.enabled = true` nos dois lados.** Sem o log, "o payload chegou ao
cliente" só dá para ver olhando o overlay na tela — o que não serve para relato
de bug. Num servidor de jogo isto ficaria desligado.

**Mundo plano e modo criativo.** Para chegar rápido ao que se quer testar.

---

## Três armadilhas que este script já pisou

Estão aqui porque cada uma custou uma execução, e todas voltariam.

**1. `java` no PATH não é o Java 21.** Nesta máquina `java -version` responde
16, porque um JDK antigo está primeiro. O `run.bat` do NeoForge chama `java`
puro e morria com:

```text
Unsupported major.minor version 65.0
```

— uma mensagem que não menciona PATH nem Java 21, e manda quem investiga para o
lado errado. O script põe o `bin` do `JAVA_HOME` na frente do PATH **do
processo filho**; definir `JAVA_HOME` sozinho não basta, porque o `run.bat` não
a consulta.

**2. `Set-Content -Encoding utf8` escreve BOM.** No Windows PowerShell 5.1, sim.
O Minecraft lê `eula.txt` e `server.properties` como properties, e o BOM gruda
na primeira chave: `eula` vira `﻿eula`.

As duas falhas resultantes são de tipos diferentes, e a segunda é a perigosa:

- a EULA recusada é **barulhenta** — o servidor diz que falta aceitar;
- o `online-mode` volta ao padrão **`true` em silêncio**. O servidor sobe, o
  cliente não consegue entrar, e nada em lugar nenhum menciona encoding.

O script escreve sem BOM, por uma função que existe só para isso.

**3. Duas propriedades decidindo o mesmo diretório.** `-PdirCliente` e
`-Pjogador` chamavam `workingDirectory` cada uma, e a segunda vencia em
silêncio: pedir as duas mandava o cliente para `run/client-<nome>` enquanto o
script anunciava `instancia/cliente`, e o diretório anunciado ficava vazio.
Agora o diretório é decidido **uma vez só**.

---

## Recomeçar do zero

```powershell
Remove-Item -Recurse -Force instancia
.\scripts\instancia.ps1 instalar
```

Nada de valor mora lá: a instância é derivada do repositório. Se algo em
`instancia/` importar, ele está no lugar errado.

---

## Um custo que vale dizer

O repositório está dentro do OneDrive
([#19](../../issues/19)), e `instancia/` tem ~200 MB de Minecraft instalado mais
mundos e logs que mudam a cada execução. **O `.gitignore` resolve o git; não
resolve o OneDrive**, que vai sincronizar tudo isso.

Se a sincronização incomodar, a saída é a mesma da #19: mover o repositório
para fora do OneDrive.
