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

- [ ] **Fechar #298.** O critério era `runGameTestServer` fechar com 143 de 143,
      e a causa escrita. Ambos satisfeitos: o merge de 2026-09-21 respondeu
      `All 143 required tests passed`, e a causa é que os quatro testes usavam
      `makeMockPlayer` — que não entra na lista de jogadores do servidor, a
      mesma que `EncounterController` consulta. **O teste estava errado, não o
      sistema**; `40ec661` trocou para `makeMockServerPlayerInLevel`.
- [ ] **Desbloquear #103.** Ela está marcada como bloqueada por #127, que está
      fechada — a camada de dano existe. Corrigir também o parágrafo do AV em
      `../processo/marcos.md` que repete o bloqueio.
- [ ] **Corrigir o bloqueio nº 1 de `../inimigos/release-candidate.md`.** Ele diz
      "nem `runClient`, nem `runServer`, nem `runGameTestServer`", e a tabela do
      mesmo arquivo diz 143/143 executados. `en-gates.md` registra o dedicado
      chegando a `Done`. O que falta de verdade é `runClient` e dois jogadores.
- [ ] **Resolver as contagens de teste duplicadas.** `en-gates.md` e
      `release-candidate.md` dizem "1.409"; depois do merge são **1.528**.
      Atualizar, ou trocar o número por "ver a saída do `build`" — a segunda
      opção acaba com a classe inteira de problema.
- [ ] **`scripts/instancia.ps1 atualizar`.** A `main` mudou; sem isso o teste
      manual roda o JAR velho, em silêncio.

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
git status            # nada não commitado ficando para trás
git rev-parse HEAD    # o mesmo commit nos dois
git stash list        # vazio, ou o stash veio junto
git worktree list     # nenhum worktree órfão apontando para o clone antigo
```

O clone antigo só é apagado depois que os quatro respondem igual. Um `git status`
sujo no antigo é trabalho de alguém que ninguém commitou.

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

Um worktree por cliente, **ambos em caminho curto**:

```bash
git worktree add C:/dca-a main
git worktree add C:/dca-b --detach main
```

Servidor num, clientes em cada um:

```bash
# terminal 1, em C:/dca-a
./gradlew runServer

# terminal 2, em C:/dca-a
./gradlew runClient -PentrarEm=localhost:25565 -Pjogador=Gon

# terminal 3, em C:/dca-b
./gradlew runClient -PentrarEm=localhost:25565 -Pjogador=Kurapika
```

`-Pjogador` passa `--username` **e** dá a cada nome o próprio diretório de
execução. Dois clientes dividindo `run/client` brigam pelo `options.txt` e pelo
log, e o segundo sobrescreve o diagnóstico do primeiro — que é justamente o que
se quer comparar.

Com `online-mode=false` o UUID vem do nome: nomes diferentes são jogadores
diferentes de verdade, com perfis separados no save.

**Um Steve (default) e um Alex (slim).** Não é opcional: usar `AURA_DEFAULT`
num braço slim deixa a aura larga demais, e o defeito só aparece quando alguém
com skin Alex entra.

Ao terminar: `git worktree remove --force C:/dca-a` e `C:/dca-b`.

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

## 5. Passo 2 — validar a bancada, antes de aprovar nada

A bancada de captura (`/nenvfx`, overlay **F6**, sliders, lote) entregue em #168
tem teste unitário da lógica e **nunca foi digitada num cliente de verdade**. É
o ponto cego mais antigo da trilha.

```
SESSÃO DE VALIDAÇÃO DA BANCADA

Nenhum arquivo produzido nesta sessão conta como evidência de gate.

Objetivo: provar a FERRAMENTA de captura, e não o efeito.
```

Isto é normativo, e não conselho. Uma imagem aparentemente válida, tirada antes
de alguém descobrir que o FOV, a câmera ou o lote estavam errados, entra no
repositório com cara de evidência e sai de lá como aprovação. **Arquivar em
`capturas/` só começa no passo 4.**

Esta sessão responde:

- [ ] `/nenvfx` aparece e responde? Os subcomandos existem?
- [ ] O overlay **F6** abre, e as réguas novas do AV4–AV8 aparecem (colunas,
      anéis, detritos com o teto ao lado, zumbidos vivos, fase da transição,
      contadores de alvo criado/liberado)?
- [ ] O lote fotografa **o quadro certo**, e não o de antes?
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
são vídeo quadro a quadro (`transicao_ten_ren` no AV4, `ten_para_zetsu` e
`ren_para_zetsu` no AV6) — o número não promete 127 PNGs.

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
