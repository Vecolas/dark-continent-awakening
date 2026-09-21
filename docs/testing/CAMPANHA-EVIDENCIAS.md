# Campanha de evidências: a ordem, a montagem e o checklist

A trilha AV, o gate do M4 e os dois primeiros gates de inimigos **não esperam
código**. Esperam uma pessoa olhando uma tela, com o jogo de pé, e a imagem
arquivada depois. Este documento é o roteiro dessa campanha.

Ele nasce de uma devolutiva de 2026-09-21 sobre as 60 issues abertas, feita
depois do merge da trilha AV na `main` (`25a15cb`).

> **Nada aqui aprova nada.** Este documento diz a ORDEM e a MONTAGEM. Quem
> aprova é o critério de aceite de cada issue, e a evidência é a imagem
> arquivada — não este arquivo.

---

## 0. O que este documento NÃO é

**Ele não é a fonte de verdade das capturas.** Isso importa, e é a regra 7 do
`CLAUDE.md`: duas fontes para a mesma verdade divergem na primeira entrega, e a
divergência não dá erro.

| A verdade sobre | Mora em |
| --- | --- |
| quais capturas cada gate exige, e o que cada uma responde | `docs/testing/capturas/<GATE>/LEIA-ME.md` |
| o critério de aceite de cada gate | a issue |
| o roteiro de captura, matriz por matriz | `av-aura-visual.md` |
| como subir servidor e dois clientes | `qa-matrix.md` |
| onde o projeto está | `../processo/marcos.md` e `../inimigos/estado-en.md` |
| **a ordem de executar tudo isso, e o que travar antes** | **este arquivo** |

A seção 6 é a única exceção, e ela existe para **fechar um buraco**, não para
duplicar: quatro gates têm a lista de capturas só na issue do GitHub, e não no
repositório. Ver a seção 6.

---

## 1. A ordem

```
  DRIFT E DOCS
       |
  MOVER O REPO PARA FORA DO ONEDRIVE
       |
  CRIAR OS DOIS WORKTREES
       |
  VALIDAR A BANCADA DE CAPTURA
       |
  DECIDIR #160 E EMENDAR O ADR-010
       |
  AV0 -> AV1 -> AV2 -> AV3 -> AV4 -> AV5 -> AV6 -> AV7
       |                 |
       |                 +--> #91   as sete tecnicas do M4
       |                 +--> #139  fundacao EN1
       |                 +--> #123  Stamp, Foxbear e Frog
       |
  AV8  PERFORMANCE MEDIDA
       |
  #209  FECHA A TRILHA AV
```

**Por que cada um vem antes do seguinte:**

| Passo | Vem antes porque |
| --- | --- |
| Drift e docs | medir contra um documento que se contradiz produz evidência que ninguém consegue interpretar depois |
| Sair do OneDrive | a QA de dois clientes **exige dois worktrees**, e `git worktree add` falha aqui por caminho longo — ver seção 3 |
| Criar os dois worktrees | é a montagem que o passo seguinte usa, e ela só é possível depois do caminho curto. Falhar aqui é barato; falhar com o servidor de pé custa a sessão |
| Validar a bancada | `/nenvfx`, o overlay F6 e o lote **nunca foram digitados num cliente**. Se a ferramenta estiver quebrada, o achado é dela — melhor descobrir antes dos 127 itens do que no quinquagésimo |
| Decidir #160 | Ren dura ~11 s hoje, e há duas regras contraditórias escritas sobre Zetsu. Capturar Ren sustentado antes disso documenta um balanceamento que vai ser substituído |
| AV(n) antes de AV(n+1) | aprovar Ren sem ter aprovado Ten mede duas mudanças ao mesmo tempo. O AV0 existe para ser o ponto de parada barato |
| #91, #139, #123 juntos | usam a mesma montagem de dois clientes + dedicado. Subir tudo de novo para eles paga o custo duas vezes, por nada |
| AV8 por último | perfil de `spark` tirado antes de AV5 e AV7 estabilizarem mede uma implementação que ainda vai mudar |

**EN6–EN16 corre em paralelo**, e é a única frente onde o próximo passo é
escrever código. A condição é a fronteira de arquivos: uma pessoa por vez em
`EnemyEntityTypes.java`, `NenSoundEvents.java`, `NenFoundationClient.java`,
`NenClientConfig.java`, `AuraRenderTypes.java` e `lang/*`.

> O merge de 2026-09-21 mostrou por que essa regra existe: a mesma correção do
> bootstrap de som entrou duas vezes, com dois nomes
> (`inicializarDuranteBootstrap` e `forcarRegistro`), e o git mesclou os dois
> **sem conflito**. O conflito só apareceu no call site.

---

## 2. Passo 0 — drift e docs

Itens pequenos, todos com evidência já levantada. Nenhum exige o jogo de pé.

**Quatro dos cinco foram feitos em 2026-09-21**, na branch
`docs/higiene-qa-drift`. Ficam escritos aqui, marcados, em vez de apagados: uma
lista que só mostra o que falta esconde o que já custou trabalho, e a próxima
pessoa reabre a discussão do zero.

- [x] **Fechar #298.** O critério era `runGameTestServer` fechar com 143 de 143,
      e a causa escrita. Ambos satisfeitos: o merge de 2026-09-21 respondeu
      `All 143 required tests passed`, e a causa é que os quatro testes usavam
      `makeMockPlayer` — que não entra na lista de jogadores do servidor, a
      mesma que `EncounterController` consulta. **O teste estava errado, não o
      sistema**; `40ec661` trocou para `makeMockServerPlayerInLevel`.
      *Fechada com o critério satisfeito — e isso NÃO é "o sistema de encontros
      foi provado em cliente". O gate de encontro continua pedindo `runClient`.*
- [x] **Desbloquear #103.** #127 fechou e a camada de dano existe. A issue e o
      parágrafo do AV em `../processo/marcos.md` passaram de "bloqueada por
      #127" para "disponível; falta alimentar `AuraImpactState` a partir da
      camada de dano". *Ela continua ABERTA: o que mudou é que a espera acabou,
      e não que o ripple exista.*
- [x] **Corrigir o bloqueio nº 1 de `../inimigos/release-candidate.md`.** Ele
      afirmava que `runGameTestServer` não tinha rodado, contra a tabela do
      próprio arquivo. Agora separa o que está provado (dedicado automatizado)
      do que não está (cliente visual e dois clientes reais).
- [x] **Resolver as contagens de teste duplicadas.** `en-gates.md` e
      `release-candidate.md` deixaram de carregar o número como verdade do
      documento: apontam para a saída do `build`, com o snapshot datado e com
      commit ao lado. Trocar 1.409 por 1.528 só teria criado o próximo número
      envelhecido.
- [ ] **`scripts/instancia.ps1 atualizar`.** A `main` mudou; sem isso o teste
      manual roda o JAR velho, em silêncio. *Único item que sobra, e ele é da
      máquina de quem for rodar a sessão — não tem como ser feito por uma
      branch.* **Ele some sozinho se o clone do #19 vier antes:** a instância
      nasce do JAR do ambiente novo, e não do antigo. Ver a §3.

---

## 3. Passo 1 — tirar o repositório do OneDrive (#19)

Esta issue parecia higiene e deixou de ser: **ela bloqueia a infraestrutura da
campanha inteira.**

`qa-matrix.md` diz, e está certo:

> Duas instâncias do Gradle no MESMO diretório de projeto travam uma na outra
> (lock de execução). Para dois clientes, use dois `git worktree`.

E `git worktree add` **falha neste repositório em caminho longo**, medido em
2026-09-21:

```
error: unable to create file src/main/java/com/darkcontinent/nenfoundation/
       worldtree/generation/WorldTreeFoliageAnchorGenerator.java: Filename too long
fatal: Could not reset index file to revision 'HEAD'
```

O caminho relativo dos arquivos de `worldtree/` passa de 110 caracteres; o
prefixo `C:\Users\alcyn\OneDrive\Documents\dark-continent-awakening` come o
resto do MAX_PATH de 260 do Windows. **Não dá erro de git: dá erro de sistema de
arquivos, e o worktree fica pela metade** — exigindo `git worktree prune` e
`rm -rf` para limpar.

**Clone novo, e não mover a pasta.** Mover arrasta `build/`, `run/`, `.gradle/`
e o estado de IDE junto — exatamente o que causa o problema que se quer resolver.

- [ ] `git clone` em `C:\dev\dark-continent-awakening`.
- [ ] Conferir que `scripts/instancia.ps1` não guarda caminho absoluto do clone
      antigo. **Isto não foi verificado por ninguém ainda.**
- [ ] Conferir o mesmo em configuração local de IDE e em `run/`.
- [ ] `./gradlew build` verde no clone novo.
- [ ] `git worktree add` funciona no clone novo — é o teste que justifica a mudança.

**Antes de abandonar o clone do OneDrive**, comparar os dois lado a lado:

```bash
git status                                   # nada não commitado ficando para trás
git rev-parse HEAD                           # o mesmo commit nos dois
git stash list                               # vazio, ou o stash veio junto
git worktree list                            # nenhum worktree órfão no clone antigo
git branch -vv                               # nenhuma branch só local
git log --branches --not --remotes --oneline # nenhum commit que não foi pushado
```

**Os dois últimos são o motivo de esta lista não ter quatro linhas.**
`git clone` traz o que está no *remoto*: ele **não** leva stash, **não** leva
branch que só existe local e **não** leva commit que nunca foi pushado. O
`stash list` cobre o primeiro caso; os outros dois só aparecem nesses dois
comandos.

Isso não é hipotético aqui: a árvore primária está numa feature branch, e este
repositório já perdeu trabalho exatamente assim duas vezes — `374b07c` resgatou
um portão que só existia num worktree, e `e392ec1` resgatou três assets soltos
fora do git.

#### O sétimo check, que nenhum dos seis pega

```bash
git status --ignored --short | grep '^!!'
```

Os seis acima perguntam ao git. **O git não sabe o que ele ignora** — e o que
ele ignora também não entra no clone.

Executado em 2026-09-21, ele achou `transfer/`, que está no `.gitignore` e
contém **quatro arquivos de código-fonte**:

```
transfer/src/main/java/com/darkcontinent/nenfoundation/NenFoundation.java
transfer/src/main/java/com/darkcontinent/nenfoundation/client/NenFoundationClient.java
transfer/src/main/resources/assets/nenfoundation/en_us.json
transfer/src/main/resources/assets/nenfoundation/pt_br.json
```

São de 2026-09-11, menores que as versões da `main` (138 linhas contra 529 em
`NenFoundationClient`), e **não batem com nenhum commit** da `main` até 12/09.
Parecem uma área de passagem entre as duas frentes, superada por dez dias de
commits — mas "parecem" não é conferido, e os dois arquivos Java são dos mais
hostis a merge do repositório.

> **Ninguém apaga o clone antigo antes de alguém dizer o que `transfer/` é.**
> Esta é a decisão que a quarentena existe para permitir.

#### Quarentena, e não deleção

O clone do OneDrive **não é apagado quando o clone novo nasce.** Ele fica
congelado — ninguém commita, ninguém troca de branch nele — até o gate abaixo
fechar inteiro. Só então ele é arquivado ou removido.

Os dois precedentes deste repositório justificam sozinhos a regra: `374b07c`
resgatou um portão que só existia num worktree, e `e392ec1` resgatou três assets
soltos fora do git. **Estado fora do git já custou recuperação manual aqui duas
vezes**; a terceira não precisa acontecer.

#### O que NÃO se copia para o clone novo

Nada de `build/`, `run/`, `.gradle/`, `.gradle-local/`, `instancia/`, `logs/` ou
`__pycache__/`. Medidos em 2026-09-21: **cerca de 1,3 GB** de artefato de build
(`run/` 601 MB, `build/` 315 MB, `instancia/` 246 MB, `.gradle/` 177 MB) — e é
exatamente esse estado que a migração existe para deixar para trás. Copiá-lo
carrega o problema junto.

> `git status --ignored` chegou a falhar com `Filename too long` dentro de
> `.gradle-local/caches/`. O MAX_PATH já morde ferramenta de leitura, e não só
> o `worktree add`.

Só vai o que for **configuração local necessária e não versionada** — e a lista
começa curta:

| O quê | Decisão |
| --- | --- |
| `run/server/server.properties` | **não copiar.** O atual está com `enable-rcon=false`, `gamemode=survival` e `level-name=world` — não é o da §4.2, e nunca foi. Criar do zero pelo documento |
| `instancia/` | **não copiar.** É recriada por `scripts/instancia.ps1`, que existe para isso |
| `transfer/` | **decisão humana** antes de qualquer coisa |
| configuração de IDE | conferir caso a caso; nada foi identificado ainda |

#### Ordem dentro do clone novo

`instancia.ps1 atualizar` vem **depois** do `build`, e não antes: a instância
tem de nascer do JAR produzido pelo ambiente que a campanha vai usar de verdade.
Isso transforma o último item pendente do passo 0 numa prova da própria migração.

> **Num clone novo não existe instância ainda.** O `atualizar` troca o JAR de
> uma instalação que precisa existir: rode `scripts/instancia.ps1 instalar`
> primeiro, que baixa o servidor NeoForge dedicado, e só então `atualizar`.

#### O oitavo check: **o destino já é um repositório?**

```bash
git -C <destino> rev-parse --git-dir   # e se responder, PARE e leia
```

Os sete anteriores olham para o clone de ORIGEM. Nenhum deles pergunta o que já
existe no destino — e era ali que estava o risco maior.

Em 2026-09-21, `C:\dev\dark-continent-awakening` **já existia**: era o clone de
trabalho da outra frente, com **18 commits que só existiam ali**, sete branches
locais, dois worktrees ativos e **26 mudanças não commitadas** — a camada de
percepção de inimigos inteira (`EnemyPerceptionService`, `PerceptionBudget`,
`TargetEvaluator`, `EnemyHearingBus` e os testes).

Um `git clone` naquele caminho teria falhado por diretório não vazio — mas um
`rm -rf` "para limpar o caminho" teria apagado dias de trabalho, sem erro
nenhum. **A migração para um caminho curto quase virou o terceiro caso de
recuperação manual deste repositório.**

Terminou bem: a outra frente pushou tudo, e o resgate está na `main` em
`8149c9c` — *"percepcao, audicao e avaliacao de alvo (WIP resgatado)"*.

#### O gate do #19

Conferido em 2026-09-21 contra `C:\dev\dark-continent-awakening`, que não
precisou ser criado: **ele já era o clone da outra frente**, e depois da limpeza
dela virou o ambiente oficial.

| Item | Estado |
| --- | --- |
| `origin` correto | ✅ `https://github.com/Vecolas/dark-continent-awakening.git` |
| `main` == `origin/main` | ✅ `b7ba18d`, zero à frente e zero atrás |
| nenhum commit exclusivamente local | ✅ **zero.** Havia um, em `archive/main-pre-pr294`; apagada em 2026-09-21 — ver abaixo |
| nenhum stash ficou para trás | ✅ vazio |
| nenhum worktree ficou para trás | ✅ só o principal |
| **`transfer/` preservado fora do clone** | ✅ ver abaixo — **este era o único que bloqueava** |
| conteúdo de `transfer/` classificado | ⬜ aberto, e **não bloqueia** |
| configurações locais identificadas | ✅ nenhuma precisa migrar; ver a tabela acima |
| `./gradlew build` verde | ✅ **1.540 testes** (`test --rerun-tasks`, para não ler cache) |
| `instancia.ps1 instalar` + `atualizar` | ✅ executados no clone de `C:\dev` — NeoForge 21.1.250 instalado, `nenfoundation-0.1.0.jar` e `geckolib-neoforge-1.21.1-4.8.3.jar` na instância |
| **dois `git worktree` sem `Filename too long`** | ✅ **`C:/dca-a` e `C:/dca-b` criados**, e os dois contêm `worldtree/generation/WorldTreeFoliageAnchorGenerator.java` — o arquivo que estourava o MAX_PATH no OneDrive |
| os dois apontam para o esperado | ✅ `b7ba18d` nos dois |

> **Nenhum commit fixado neste gate.** A versão anterior dizia `== 96f491c` e
> envelheceu em horas, quando a outra frente pushou 25 commits. É o mesmo
> defeito do `1.409` e do `75`: número copiado à mão não sobrevive à entrega da
> outra frente. O critério é `main == origin/main`, e não um hash.

#### `archive/main-pre-pr294`: apagada, e por quê

O nome dizia "arquivo", e isso sugeria que havia algo guardado ali. Não havia.

A branch tinha **um único commit próprio**, `3d707b3`
(*"fix(inimigos): tornar gates de encontro e sons executaveis"*). O teste
decisivo não é comparar as árvores — isso mede tudo que aconteceu entre os dois
commits —, e sim comparar os **patches**:

```bash
git diff 3d707b3^ 3d707b3   # 104 linhas
git diff 40ec661^ 40ec661   # 104 linhas
                            # diff entre os dois: vazio
```

**Byte a byte o mesmo patch**, e `40ec661` está na `main`. Provavelmente um
rebase ou re-commit durante o PR #294.

Os dois outros motivos pelos quais ela poderia ficar também caem:

- *"Guarda o estado pré-PR #294."* O pai dela, `715b26c`, **é ancestral de
  `origin/main`** — o estado anterior ao merge continua alcançável pelo
  histórico, sem branch nenhuma.
- *"Pode ter mais coisa."* `git log 3d707b3 --not --remotes` devolve **1**.

Recuperação, enquanto o reflog viver:
`git branch archive/main-pre-pr294 3d707b3d40ad5f5d1471c105d850ac1fed682a0d`.
Mas `git show 40ec661` mostra o mesmo patch, para sempre.

> **A lição é sobre o nome.** `archive/` fez a branch parecer conteúdo
> preservado por dez dias, e ninguém olhou. Branch de segurança criada "por via
> das dúvidas" precisa de uma linha dizendo **de que dúvida** — senão ela vira
> dívida silenciosa, e quem a encontra não tem como decidir sem refazer a
> investigação inteira.

#### A quarentena de `transfer/`

```
C:\dev\quarantine\dark-continent-transfer-2026-09-21.zip
SHA-256  75CB592222F3539856D4E51CEEE1759CD4D454E64E9B7A5F1DF8417688AC3CFD
```

Os quatro arquivos, com manifesto SHA-256 por arquivo **dentro do próprio zip**.
O arquivo foi extraído num diretório temporário e os quatro hashes reconferidos
contra o manifesto: batem byte a byte. *Hash de zip incompleto não prova nada.*

`transfer/` continua intacta no clone do OneDrive — quarentena é **preservar**,
e não mover.

**A classificação é uma atividade separada, e não bloqueia nada.** Para cada um
dos quatro arquivos, contra a versão atual da `main`:

| Classe | Significa |
| --- | --- |
| SUPERADA | a versão atual implementa a mesma intenção, melhor |
| OBSOLETA | referencia arquitetura que já não existe |
| DUPLICADA | já existe em outro lugar da `main` |
| ÚNICA | há lógica ou conteúdo que só existe em `transfer/` |
| INDETERMINADA | não dá para estabelecer a intenção com segurança |

`transfer/` só é descartável quando os quatro forem SUPERADA, OBSOLETA ou
DUPLICADA. **Um só ÚNICA ou INDETERMINADA e o arquivo é preservado**, com issue
curta de recuperação — nunca injetando código antigo na `main` automaticamente.

#### O que falta para fechar o #19

**Um item.** Aposentar o clone do OneDrive — que é o que sobra da migração, já
que o destino existia antes dela.

Ele depende de `transfer/` classificada, e só disso: tudo o mais foi conferido
em 2026-09-21. E a classificação **não é urgente**, porque o conteúdo já está
preservado com hash fora dos dois clones.

> **O ambiente oficial é `C:\dev\dark-continent-awakening`.** O do OneDrive
> fica em quarentena: ninguém commita, ninguém troca de branch nele. Ele ainda
> guarda `transfer/` e a branch `feat/av4-av8-aura-visual` — já mergeada, e
> mantida só porque apagá-la exigiria mover o HEAD de uma árvore compartilhada.

Alternativa não testada, se mover for indesejável agora:
`git config core.longpaths true`.

---

## 4. A montagem

### 4.1 Antes de qualquer sessão

- [ ] `./gradlew build` verde, e a saída diz quantos testes rodaram.
- [ ] `./gradlew runGameTestServer` diz `All N required tests passed`.
- [ ] `scripts/instancia.ps1 atualizar` rodado **depois** do último merge.
- [ ] Combinada a janela com a outra frente — ver 4.4.

### 4.2 `run/server/server.properties`

```properties
online-mode=false
enable-rcon=true
rcon.password=dev
rcon.port=25575
level-name=mundo-de-regressao
gamemode=creative
```

### 4.3 Os dois worktrees e os dois clientes

Um worktree por cliente, **ambos em caminho curto e ambos `--detach`**:

```bash
git worktree add --detach C:/dca-a main
git worktree add --detach C:/dca-b main
```

> **O primeiro tinha `main` sem `--detach`, e falha.** Git recusa a mesma branch
> em dois worktrees: `fatal: 'main' is already used by worktree at ...`. Se o
> clone principal está em `main` — e normalmente está —, a receita morre no
> primeiro comando. `--detach` nos dois resolve, e nenhum dos dois precisa de
> branch: eles só rodam cliente. Medido em 2026-09-21.

Servidor num, clientes em cada um:

```bash
# terminal 1, em C:/dca-a
./gradlew runServer

# terminal 2, em C:/dca-a
./gradlew runClient "-PentrarEm=127.0.0.1:25565" "-Pjogador=Gon"

# terminal 3, em C:/dca-b
./gradlew runClient "-PentrarEm=127.0.0.1:25565" "-Pjogador=Kurapika"
```

> **AS ASPAS SÃO OBRIGATÓRIAS NO POWERSHELL, e a campanha roda em PowerShell.**
> Sem elas o PowerShell parte o argumento no primeiro ponto depois de um dígito:
> `-PentrarEm=127.0.0.1:25565` vira **dois** argumentos, `-PentrarEm=127` e
> `.0.0.1:25565`, e o Gradle responde `Cannot locate tasks that match
> '.0.0.1:25565'` — uma mensagem que não menciona o PowerShell em lugar nenhum.
> Medido em 2026-09-21. Alternativa: `./gradlew.bat --% runClient -PentrarEm=...`,
> que manda o PowerShell parar de interpretar o resto da linha.
>
> **E é `127.0.0.1`, não `localhost`.** `localhost` resolve para IPv6 nesta
> máquina, e o servidor está preso a `server-ip=127.0.0.1`, que é IPv4: a
> conexão é recusada com `[0:0:0:0:0:0:0:1]`. `localhost` não precisa de aspas —
> não tem dígito antes do ponto —, e foi por isso que funcionou antes de alguém
> trocar pelo IP.

`-Pjogador` passa `--username` **e** dá a cada nome o próprio diretório de
execução. Dois clientes dividindo `run/client` brigam pelo `options.txt` e pelo
log, e o segundo sobrescreve o diagnóstico do primeiro — que é justamente o que
se quer comparar.

Com `online-mode=false` o UUID vem do nome: nomes diferentes são jogadores
diferentes de verdade, com perfis separados no save.

#### Um corpo de cada, e o nome NÃO escolhe o corpo

Um cliente com modelo `wide` (Steve) e um com `slim` (Alex). Não é opcional:
usar `AURA_DEFAULT` num braço slim deixa a aura larga demais, e o defeito só
aparece quando alguém com skin Alex entra.

**Em `online-mode=false`, quem decide o corpo é o UUID — e o UUID vem do nome
por uma função de hash.** O servidor calcula
`UUID.nameUUIDFromBytes("OfflinePlayer:" + nome)`, e o cliente escolhe o modelo
com `(uuid.hashCode() & 1) == 1 ? SLIM : WIDE`. Não há como escolher o corpo
pelo nome; só há como **descobrir** qual nome dá qual corpo.

| `-Pjogador` | UUID offline | Corpo |
| --- | --- | --- |
| `Gon` | `143d4426-466c-3cbf-ba52-6033489ff015` | **SLIM** (Alex) |
| `Kurapika` | `7a9cf585-6597-30cf-a91c-e1fccb448316` | **WIDE** (Steve) |
| `Steve` | `5627dd98-e6be-3c21-b8a8-e92344183641` | **SLIM** — o oposto do nome |
| `Alex` | `36532b5e-c442-3dbb-a24c-c7e55d0f979a` | **WIDE** — o oposto do nome |

> **Por isso a receita usa `Gon` e `Kurapika`, e não `Steve` e `Alex`.** Chamar
> os clientes de Steve e Alex dá um de cada corpo — o par funciona —, mas com os
> rótulos **invertidos**. E várias capturas são nomeadas pelo corpo:
> `ten_slim`, `ren_slim`, `av2_ten_slim`, `ten_armadura_slim`. Quem tirasse
> `ten_slim` no cliente chamado "Alex" fotografaria o corpo errado, e a imagem
> ficaria plausível, arquivada e inválida — o defeito só apareceria quando
> alguém comparasse duas capturas e não entendesse por que o braço mudou de
> largura.
>
> Nomes que não prometem corpo nenhum não têm como mentir sobre ele.

A conta acima foi verificada contra o `ops.json` existente: os UUIDs calculados
para `Dev` e `Gon` batem caractere por caractere com os que o servidor já tinha
gravado.

**Confira na sessão, não no papel.** Aperte **`F5`** em cada cliente e compare a
largura do braço com os dois no mesmo quadro — slim tem 3 pixels, wide tem 4.
**`F3` não serve**: é o debug da vanilla e não mostra o modelo; o overlay F6
também não o reporta. Isolado, cada corpo parece normal. Uma tabela em documento é derivada — a mesma
regra do total de 127.

Ao terminar: `git worktree remove --force C:/dca-a` e `C:/dca-b`.

#### Op para os dois clientes

Comando de dev (`/nenvfx`, `/nen`, `/freeze`) é permissionado. Sem op, a sessão
da bancada não sai do lugar — e a recusa é silenciosa o bastante para custar
meia hora.

`ops.json`, no diretório do servidor que for usado (`run/server/` para o
`runServer`, `instancia/servidor/` para o `instancia.ps1`), com o UUID **offline**
da tabela acima e `"level": 4`. O arquivo é lido no boot: editar com o servidor
de pé não vale.

Em 2026-09-21 os dois servidores do clone oficial receberam `Gon` e `Kurapika`.

### 4.4 Posse do servidor de QA

A `fronteira-de-arquivos.md` cobre um recurso compartilhado: **arquivo**, e a
exclusão é por *edição*. Esta campanha expõe um segundo, que não está escrito em
lugar nenhum: **o servidor de QA**, e a exclusão dele é por *execução*.

| Recurso | Exclusão por | Onde está a regra |
| --- | --- | --- |
| `NenFoundationClient.java`, `NenSoundEvents.java`, … | edição | `../processo/fronteira-de-arquivos.md` |
| `runServer` + porta 25565 + mundo de regressão | **execução** | **aqui** |

A issue #169 já registrava o fato, solto:

> **Só uma frente por vez roda `runServer`** (porta fixa) e mexe no mundo de
> teste compartilhado. Combinar a janela antes de começar.

Vira convenção explícita:

```
POSSE DO SERVIDOR DE QA

Uma frente por vez. Antes de iniciar:
  [ ] declarar a sessão para a outra frente
  [ ] confirmar que nenhum outro teste está usando a porta
  [ ] confirmar o mundo e o commit esperados

Ao terminar:
  [ ] encerrar o servidor
  [ ] liberar a sessão, dizendo que terminou
```

**Convenção, e não lock.** Nada de script de trava, arquivo de posse ou segunda
porta por enquanto: a campanha já é grande, e infraestrutura construída antes de
a convenção provar que não basta é custo sem consumidor. O dia em que duas
sessões colidirem mesmo assim é o dia de automatizar — e aí existe o caso real
que diz qual automação serve.

Trabalho de código em EN6–EN16 continua livre; o que se combina é a janela de
servidor.

### 4.5 A regra de toda captura

- Servidor dedicado, **nunca** singleplayer. Singleplayer roda o servidor
  interno no mesmo processo do cliente e não pega classe client-only vazada
  (erro nº 10 do `CLAUDE.md`).
- Overlay **F6 sem o aviso `OVERRIDE ATIVO`** — uma captura tirada com
  sobreposição ligada não vale como aprovação. A única exceção documentada é
  `sem_particulas_*`, cuja pergunta É a sobreposição.
- **Protocolo A/B:** mesmo local, mesmo FOV, hora e clima travados pelo modo de
  captura, mesma skin, mesma distância de câmera, mesma versão de assets.
- Nome do arquivo carrega **data, commit e nível de bloom**.
- A série `2b..40b` sai **com um segundo jogador**. A distância do jogador local
  para a própria câmera é zero, e o LOD nunca morde em si mesmo.

---

## 5. Passo 2 — a bancada, validada em 2026-09-21 ✅

**A bancada funciona.** Em 2026-09-21 o projeto abriu `runClient` pela primeira
vez, com dois clientes e servidor dedicado, e a bancada de captura (`/nenvfx`,
overlay **F6**, sliders, lote) passou item a item.

| O que a sessão perguntou | Resposta |
| --- | --- |
| `/nenvfx` aparece e responde | ✅ |
| Overlay **F6** abre, com as réguas do AV4–AV8 | ✅ |
| Os sliders (`/nenvfx tuning`) mudam algo visível | ✅ cada config mexe numa coisa distinta |
| O lote fotografa o quadro certo | ✅ |
| Nome com data, commit e nível de bloom | ✅ **depois do #300** |
| O modo de captura devolve a câmera | ✅ |
| `/nenvfx permissivo` acende `OVERRIDE ATIVO` | ✅ |
| `/nenvfx off`, morte e relog não prendem estado | ✅ |

**Ela custou três defeitos, e nenhum deles aparecia em 1.549 testes verdes:**

| | O que era | Sintoma |
| --- | --- | --- |
| **#299** | ribbons seguravam dois `VertexConsumer` | o cliente **morria** ao ligar Ren com dois jogadores |
| **#300** | o nível de bloom estava fixo em `bloom-ausente` | toda captura com o mesmo sufixo — a comparação do gate #198 ficaria impossível de ler |
| **#301** | a posição do `ModelPart` era zerada | shell no centro do corpo em vez do braço; Ten sumia |

Os três foram corrigidos, ganharam portão que **reprova sem o código**, e foram
**confirmados em jogo** na mesma sessão.

> **O argumento inteiro desta campanha, numa sessão.** O código compilava, a
> suíte estava verde e os GameTests passavam 143/143. O jogo morria ao ligar
> Ren. *O defeito não estava no que o código fazia, e sim no que nenhuma régua
> media.*

### O que a sessão NÃO fez

Nenhuma captura dela conta como evidência, e nenhuma foi arquivada em
`capturas/`. O AV0 não começou. Isso era a regra da sessão, e ela foi cumprida.

### A regra, que continua valendo para a próxima primeira vez

```
SESSÃO DE VALIDAÇÃO DA BANCADA

Nenhum arquivo produzido nesta sessão conta como evidência de gate.

Objetivo: provar a FERRAMENTA de captura, e não o efeito.
```

Isto é normativo, e não conselho. Uma imagem aparentemente válida, tirada antes
de alguém descobrir que o FOV, a câmera ou o lote estavam errados, entra no
repositório com cara de evidência e sai de lá como aprovação. **Arquivar em
`capturas/` só começa no passo 4.**

Se a bancada ganhar recurso novo, ele repete este ciclo antes de entrar numa
campanha: a ferramenta se prova primeiro, e sozinha.

<details>
<summary>O roteiro que foi usado, para a próxima ferramenta</summary>

- [x] `/nenvfx` aparece e responde? Os subcomandos existem?
- [x] O overlay **F6** abre, e as réguas novas do AV4–AV8 aparecem (colunas,
      anéis, detritos com o teto ao lado, zumbidos vivos, fase da transição,
      contadores de alvo criado/liberado)?
- [x] O lote fotografa **o quadro certo**, e não o de antes?
- [ ] O modo de captura **devolve a câmera** ao sair?
- [ ] O nome do arquivo sai com data, commit e nível de bloom?
- [ ] `/nenvfx permissivo` acende o aviso de sobreposição no overlay?
- [ ] `/nenvfx off`, relog, morte e troca de dimensão não deixam estado visual
      preso?

Se **qualquer um** deles falhar — `/nenvfx`, F6, slider, lote, nomenclatura,
restauração de câmera, Steve/Alex —, a sessão **para** e vira issue da bancada.

> Não se começa o AV0 "já que o cliente está aberto". Uma bancada meio
> verificada produz 127 itens cuja validade ninguém consegue defender depois.

Uma sessão que termina com uma issue nova e zero capturas é **sucesso**: ela
achou o defeito pelo preço de uma tarde, em vez do preço da campanha inteira.

---

## 6. Passo 3 — o checklist por gate

### 6.1 O quadro

| Gate | Issue | Capturas | Onde mora a lista | 2 clientes |
| --- | --- | --- | --- | --- |
| AV0 | #169 | 14 | **§6.2 deste arquivo** + issue | sim |
| AV1 | #176 | 17 | **§6.2 deste arquivo** + issue | não |
| AV2 | #181 | 13 | **§6.2 deste arquivo** + issue | não |
| AV3 | #187 | 22 | **§6.2 deste arquivo** + issue | sim |
| AV4 | #193 | 20 | `capturas/AV4/LEIA-ME.md` | sim |
| AV5 | #198 | 13 | `capturas/AV5/LEIA-ME.md` | não |
| AV6 | #201 | 7 | `capturas/AV6/LEIA-ME.md` | **sim** |
| AV7 | #205, #104 | 21 | `capturas/AV7/LEIA-ME.md` | **sim** |
| AV8 | #209, #206, #207 | perfis | `capturas/AV8/LEIA-ME.md` | sim (10 e 20) |

**Total: 127 itens de evidência visual nesta revisão (2026-09-21).** Três deles
são vídeo quadro a quadro — a transição Ten→Ren no AV4 e as duas de
Zetsu no AV6, cujos nomes moram no `LEIA-ME.md` de cada gate. O número não
promete 127 PNGs.

> **Este total é DERIVADO, e a fonte de verdade continua sendo cada gate.**
> Ele foi somado a partir das listas em 2026-09-21 e envelhece no dia em que uma
> issue ganhar ou perder uma linha. Refazer a soma é barato; confiar nela sem
> refazer é como o 75 nasceu.

> **O `LEIA-ME.md` do diretório de capturas dizia 75**, e o número está
> corrigido lá junto com o motivo. Ele somava só os gates cuja linha trazia uma
> contagem; AV1, AV2 e AV3 apareciam como "ver issue" e ficavam de fora da
> própria soma — 52 itens que ninguém tinha como ver. O "75" ficou registrado de
> propósito: apagá-lo em silêncio tiraria a trilha de auditoria de como um total
> derivado erra.

### 6.2 Os quatro roteiros que só existiam na issue

AV0, AV1, AV2 e AV3 têm `LEIA-ME.md` dizendo "a lista de capturas está na
issue". **Isso é um buraco**, e não uma escolha: a evidência de como se prova um
gate é do repositório. Uma issue editada, fechada ou renumerada leva o roteiro
junto, e não se lê offline.

As quatro listas estão transcritas abaixo. **Quando forem para os `LEIA-ME.md`
de cada gate, apagar esta seção** — senão ela vira exatamente a segunda fonte
que a seção 0 proíbe.

**AV0 (#169) — 14.** A shell é crua aqui, sem shader próprio: avalia-se
*aderência*, não semelhança com a referência B.

```
ten_dia   ten_noite   ten_caverna
ten_slim  ten_overlay_skin  ten_armadura
ten_correndo   ten_agachado   ten_nadando
ten_2b   ten_5b   ten_10b   ten_20b   ten_40b
```

**AV1 (#176) — 17.** `sem_particulas_ten` é a captura que decide o gate: com
`vfx.densidadeDeParticulas = 0.0`, ainda se lê Ten?

```
ten_dia   ten_noite   ten_caverna   ten_neve   ten_nether   ten_chuva   ten_agua
ten_slim  ten_overlay_skin
ten_2b   ten_5b   ten_10b   ten_20b   ten_40b
ten_ruido_ampliado        ten_360   (serie girando a camera)
sem_particulas_ten        <- decide o gate
```

**AV2 (#181) — 13.** `av2_frame_congelado_1` e `_2` **têm de ser idênticos**.

```
av2_ten_2b   av2_ten_5b   av2_ten_10b
av2_ten_correndo   av2_ten_agachado   av2_ten_nadando   av2_ten_atacando
av2_ten_slim   av2_ten_default
av2_frame_congelado_1   av2_frame_congelado_2   (identicos)
av2_sem_particulas_ten
av2_overlay_contadores
```

**AV3 (#187) — 22.**

```
ten_dia   ten_noite   ten_caverna   ten_nether   ten_neve   ten_agua   ten_chuva
ten_2b    ten_5b      ten_10b       ten_20b      ten_40b
ten_correndo   ten_agachado   ten_nadando
ten_slim       ten_armadura   ten_overlay_skin
primeira_pessoa_ten
sem_particulas_ten     <- o criterio do ADR-015
sem_bloom_ten
overlay_contadores     (ribbons vivas, particulas vivas, draw calls, LOD)
```

### 6.3 Ao fechar cada gate

Não é só arquivar imagem. Cada gate da trilha pede, pelo texto das issues:

- [ ] capturas em `capturas/<GATE>/`, com data, commit e bloom no nome;
- [ ] comparação com a referência de arte **na mesma sessão**, lado a lado —
      olho humano cansa, e comparação de memória dias depois não vale;
- [ ] `o-que-nao-provamos.md` atualizado com o que a sessão **não** provou;
- [ ] `../processo/marcos.md` atualizado — é a fonte de verdade do estado;
- [ ] `compatibility.md` atualizado quando o gate tocar renderer ou shader pack.

---

## 7. Passo 4 — os três gates que pegam carona

Rodar na mesma montagem de dois clientes + dedicado, depois do AV3:

- [ ] **#91 — M4.** As sete técnicas em servidor dedicado, todas as combinações
      inválidas, `StopReason` e as mensagens de recusa. **Fecha o M4**, que hoje
      não deve mais nada de código.
- [ ] **#139 — EN1.** Falta `runClient` renderizando e animando o boneco, e dois
      jogadores confirmando autoridade e action sync. O resto do gate já está
      verde por portão.
- [ ] **#123 — EN3.** Stamp, Foxbear e Frog-In-Waiting.

---

## 8. Passo 5 — AV8, e só então

- [ ] **#206** — os quatro cenários, com `spark` antes e depois em
      `docs/testing/perfis/`: 10 em Ren a 16 blocos; 20 em Ten a 32 blocos;
      **nenhuma aura visível** (custo zero, não custo pequeno); 10 minutos de
      Ren contínuo medindo memória.
- [ ] **A medição do servidor dedicado.** Tick time com 10 em Ren contra 10
      parados. *Se forem distinguíveis, algo de VFX vazou para o servidor — e
      isso é P0, não tuning.*
- [ ] **#207** — a matriz de renderização: vanilla, Embeddium, Iris/Oculus sem
      pack e com pack, cada um nos três níveis de `vfx.bloom`.
- [ ] **#209** — fecha a trilha.

---

## 9. O que esta campanha NÃO resolve

- **EN6 a EN16 continuam sendo código a escrever**, e nenhuma sessão de captura
  os aproxima do fim.
- **M5 a M8 não começaram**, e por regra não devem começar sem autorização
  explícita.
- **As dívidas declaradas seguem declaradas:** #24 (portão de carregamento de
  registro, fecha no M5), #126 (a metade de Gyo que percebe), #56 (gasto zero
  recusado contra o `AbilitySpec`).
- **"28 issues precisam só de evidência" é inferência, e não medição.** O número
  vem do que `marcos.md` declara sobre AV0–AV8 e do que `estado-en.md` declara
  sobre EN1–EN5 — ninguém conferiu o critério de aceite das 60 issues uma a uma.
  Até essa conferência, o termo correto é **candidatas a fechamento por
  evidência**.
- **Este documento nunca foi executado.** Ele foi escrito lendo issues e docs,
  e não rodando o jogo. O primeiro item que estiver errado aqui vai aparecer na
  sessão do passo 2 — e o conserto é deste arquivo.
