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
.\scripts\instancia.ps1 atualizar    # recompila e troca o JAR
```

### Depois de mexer no código

```powershell
.\scripts\instancia.ps1 atualizar
```

Recompila e substitui o JAR. **Ele apaga a versão antiga antes de copiar** —
dois JARs do mesmo `mod_id` em `mods/` fazem o NeoForge recusar o boot, e
depois de um bump de versão os nomes são diferentes, então sobrescrever não
resolveria.

O `status` avisa quando o build está mais novo que o JAR instalado.

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
