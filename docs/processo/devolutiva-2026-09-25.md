# Devolutiva do projeto — 2026-09-25

Um retrato do **Dark Continent Awakening** inteiro, escrito a partir de uma
varredura dos arquivos, e não da memória de quem trabalhou neles.

> **Este documento não é fonte de verdade de nada.** As fontes continuam sendo
> [`marcos.md`](marcos.md) (M0–M8), [`estado-en.md`](../inimigos/estado-en.md)
> (EN0–EN16), [`docs/vfx/`](../vfx/LEIA-ME.md) (AV0–AV8) e
> [`o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md). Isto aqui é a
> leitura transversal delas num dia específico, para quem precisa ver o todo de
> uma vez. **Ele envelhece.**

---

## 1. O que existe, em números

| | |
| --- | --- |
| Java de produção | **688 arquivos**, ~83.000 linhas |
| Java de teste | **247 arquivos**, ~35.000 linhas |
| Testes JUnit executados no `build` | **1.611** |
| `@GameTest` (com o jogo de pé) | **177**, em 27 classes |
| `package-info` declarados | **77** — um por pacote, exigido por portão |
| ADRs | **18**, todos com custo declarado |
| Documentos em `docs/` | **84** arquivos `.md` |
| Issues | **173** na vida do projeto; **143 fechadas**, 30 abertas |
| Chaves de tradução | **474**, idênticas em `en_us` e `pt_br` |
| Protocolo de rede | versão **10**, 12 payloads |
| Chaves de config | 74 em `NenConfig` |

A razão é de cerca de **0,42 linha de teste por linha de produção**. Não é
métrica de qualidade; está aqui só para dar escala.

---

## 2. As quatro frentes, e por que são quatro

O projeto não é uma fila. São quatro trilhas com dependências próprias, e
confundi-las é a forma mais fácil de achar que algo está pronto quando não está.

```
M0 ──> M1 ──> M2 ──> M3 ──> M4 ──> M5 ──> M6 ──> M7 ──> M8     o Nen
                                │
                                └──> AV0 ──> ... ──> AV8        o VISUAL da aura
EN0 ──> EN1 ──> ... ──> EN15 ──> EN16                           os inimigos
World Tree · Posto Avançado Hunter · Bestiário · Greed Island   conteúdo de mundo
```

---

## 3. O Nen (M0–M8) — a fundação

### ✅ M0 · Fundação do repositório e contratos

Estrutura de pacotes, regra de dependência, contratos congelados
([ADR-004](../adr/ADR-004-identidade-congelada.md)), CI e os primeiros portões.

**Um item aberto, e ele não é código:** o CI nunca rodou num PR aberto pela
segunda pessoa. Fecha quando ela abrir o primeiro.

### ✅ M1 · Perfil persistente + protocolo de sync

`PersistentNenData` v1 com codec e migrador, `RuntimeNenState` que não persiste
nada, e a separação que o [ADR-002](../adr/ADR-002-persistente-e-runtime.md)
impõe. O migrador recusa save de versão futura ou inválida.

### ✅ M2 · Aura Engine + HUD

`nen/aura/` com nove classes: `AuraPool`, `MotorDeAura`, `AuraFormulas`,
`ParametrosDeAura`, `SaldoSustentado`, `AlocacaoDeAura`, `RegiaoDoCorpo`,
`FocoDeAura` e `PresencaDeAura`.

A regeneração depende do estado de Nen ativo
([ADR-010](../adr/ADR-010-regeneracao-por-estado-de-nen.md)); o saldo negativo
vale só para quem libera aura
([ADR-013](../adr/ADR-013-saldo-so-para-quem-libera-aura.md)); a escada de custo
é desenhada em segundos
([ADR-018](../adr/ADR-018-escada-de-custo-em-segundos.md)). Não existe uma
segunda stamina ([ADR-009](../adr/ADR-009-modelo-de-aura-sem-stamina-de-nen.md)).

HUD com sete componentes e interpolação puramente visual. A regra: **o HUD nunca
calcula regra** — ele desenha o que o servidor mandou.

### ✅ M3 · Despertar, categoria e afinidade

As sete categorias congeladas com o neutro no ordinal 0, matriz de afinidade, e
a **Water Divination** como teste diegético — a categoria se descobre jogando, e
não num menu de criação de personagem.

### 🟡 M4 · Técnicas fundamentais — **o código está completo; o gate não**

**Sete técnicas registradas em produção**, conferidas em `NenServerLifecycle`:

| Técnica | Domínio | O que ela é, no modelo |
| --- | --- | --- |
| **Ten** | ✅ | película retida; regeneração e proteção base |
| **Ren** | ✅ | teto de Output alto, caro, insustentável |
| **Zetsu** | ✅ | supressão; vira `SinalDeAura.NENHUM` para todos |
| **Gyo** | ✅ | concentra numa região; o resto recebe menos |
| **Shu** | ✅ | estende Ten ao braço dominante |
| **Ken** | ✅ | magnitude: Ten e Ren sustentados juntos |
| **Ko** | ✅ | quase tudo numa região, com relógio próprio |

Mais: camada de dano (`nen/combat/`) com **um único** handler de entrada, roda de
ativação, ajuste de Output por intenção (o payload carrega direção, nunca
número), e a alocação de aura por região
([ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md)).

**O que falta no M4 não é código.** É o gate com dois clientes reais em servidor
dedicado (#91) e a revisão cruzada.

**Uma metade continua fora:** a **percepção** de Gyo — ver aura fraca e aura
escondida — porque não há camada de percepção nem In (#126).

### ⬜ M5 · Framework de Hatsu — não iniciado

Os **contratos existem** e estão congelados desde o M0: `NenAbility`,
`AbilitySpec`, `AbilityRequest`, `ActiveAbility`, `ActivationResult`. O
encanamento de rede existe: `AtivarHabilidadeC2S`, `FxDeHabilidadeS2C`,
validação e limite de pedidos em `NenPedidoService`.

**`nen/ability/` tem só o `package-info`.** Zero habilidades implementadas.

### ⬜ M6, M7, M8 — não iniciados

**As quatro pontes de integração existem como pacote vazio** — `ftbquests`,
`kubejs`, `jade`, `epicfight`, cada uma com apenas o `package-info` de 10 linhas
declarando a regra que vai ter de obedecer. Isso é
[ADR-003](../adr/ADR-003-integracoes-opcionais.md) e
[ADR-006](../adr/ADR-006-epic-fight-fora-da-fundacao.md) cumpridos: o JAR inicia
sem nenhuma delas, e nenhuma nasceu cedo demais.

`api/query/` também é só `package-info`.

---

## 4. O visual da aura (AV0–AV8) — trilha fechada

A decisão que fundou a trilha:
**[a aura é geometria e shader; partícula é acabamento](../adr/ADR-015-aura-e-geometria-e-shader.md)**,
e o brilho é
[pós-processamento próprio, com fallback](../adr/ADR-016-pos-processamento-proprio-da-aura.md) —
não depende de shader pack.

Em código é a parte mais densa do cliente: **73 arquivos, ~11.300 linhas** entre
`client/vfx` e seus quatro subpacotes (`render`, `ribbon`, `shader`, `model`),
mais 1.600 linhas de bancada de tuning em `debug/`.

| Gate | O que cobria | Desfecho |
| --- | --- | --- |
| AV0–AV3 | aderência, shell, filamentos — **Ten por inteiro** | ✅ **sessão humana real**, dois clientes |
| AV4 | Ren | ✅ fechado por decisão: o resto é tuning de gosto |
| AV5–AV8 | bloom, Zetsu, poses/mobs, orçamento | ✅ fechados pela mesma decisão |

**Fechar a trilha não apagou ausência nenhuma** — ela virou régua. Três critérios
do AV4 que não eram gosto (nenhum bloco quebrado, zero `ItemEntity` órfão,
observador não recebe impulso de câmera) viraram `OMundoNaoMudaTest`.

**Uma entrega da trilha não existe:** aura sobre mobs GeckoLib (#204).
`GeoAuraAdapter` tem zero referências fora do próprio arquivo, e
`AuraLivingRenderLayer` nunca foi escrita. É dívida de **implementação**, e ficou
catalogada por engano como dívida de verificação até 22/09.

---

## 5. Os inimigos (EN0–EN16) — a maior frente em volume

**23 entidades registradas**, com 26 modelos GeckoLib, 26 conjuntos de animação e
24 pastas de som. Em código é a maior parte do projeto: ~39.000 linhas em
`enemy/`, das quais 18.400 só em `entity/`.

| Faixa | Estado |
| --- | --- |
| EN0–EN9 | ✅ código completo — fundação, percepção, combate, stagger, facções, squad, encontro persistente, Greed Island, colônia Chimera |
| EN10 | 🟡 decisão tática completa, mas **não ativa técnica nenhuma** |
| EN11–EN15 | ✅ código completo — officers, integração de mundo, áudio/VFX, hardening, balanceamento e telemetria |
| EN16 | ⬜ release candidate dos 23 — não iniciado |

A fronteira que essa frente respeita, e que é a decisão central do projeto:
**`enemy/chimera/nen/` decide QUANDO usar Nen, e nunca O QUE Nen é.**

### O estado real dessa trilha, dito sem eufemismo

**Quinze faixas marcadas "código completo" e nenhum gate de jogo executado.** O
próprio documento da trilha abre avisando: *"não deduza entrega a partir de
código compilável. Um mob registrado que anda, percebe e ataca passa por todos os
sinais que o repositório sabe ler."* Isso virou
[ADR-017](../adr/ADR-017-mob-vanilla-e-andaime-nao-entrega.md).

---

## 6. Conteúdo de mundo

| Frente | O que existe | Estado |
| --- | --- | --- |
| **World Tree** | ~5.700 linhas: dimensão, geradores chunk-locais de tronco, galho, copa, raiz, flora, bosque, oco e acampamento; checkpoints por UUID em `SavedData`; viagem server-authoritative | geração e portões prontos; **nenhum bloco da copa foi visto em jogo** |
| **Posto Avançado Hunter** | `structure/`, 14 arquivos: blockout server-side de 41×41 com oito módulos, placement natural por tag de bioma, loot | contrato V2 congelado; gate Ready to Build (#229) aberto |
| **Bestiário** | `bestiary/` + `client/bestiary` + 23 entradas de dados, item `hunter_bestiary`, `BestiarySnapshotS2C` | entregue e sincronizado |
| **Greed Island** | `enemy/greedisland/`, itens `greed_island_ring` e `greed_island_card`, dimensão da ilha | código completo; as sete criaturas nunca foram vistas |

Vinte e três blockstates autorais, quase todos da World Tree.

---

## 7. As decisões que governam tudo isto

Dezoito ADRs, e um portão (`IndiceDeAdrTest`) que **reprova ADR sem custo
declarado**. As que mais aparecem no código:

| ADR | A decisão | O que ela custa |
| --- | --- | --- |
| **001** | o servidor é a autoridade sobre todo estado de Nen | todo feedback passa por um round-trip; nada é instantâneo no cliente |
| **002** | persistente e runtime são coisas separadas | duas estruturas para o que parece um dado só |
| **003** | toda integração de pack é opcional | as pontes ficam vazias até o M6/M7, e parecem esquecimento |
| **004** | identidade congelada no M0 | descongelar exige ADR novo, degrau de migração e plano para os mundos existentes |
| **014** | a aura tem regiões, e elas são autoritativas | seis fatores viajam no delta mesmo quando ninguém concentra |
| **015** | a aura é geometria e shader | 11.300 linhas de cliente que uma nuvem de partícula não teria custado |
| **017** | mob vanilla e andaime não entregam | quinze faixas EN "completas" que ainda não contam como entregues |

---

## 8. Os portões, e o que eles mordem

| Portão | O que impede |
| --- | --- |
| `NenCategoryTest` | renomear, reordenar ou remover categoria; tradução faltando ou órfã |
| `PersistentNenDataTest` | perder campo no codec; default não-neutro; coleção mutável |
| `NenProfileMigratorTest` | aceitar save de versão futura ou inválida |
| `ProtocoloCongeladoTest` | código e documento discordarem sobre id, direção ou versão |
| `IndiceDeAdrTest` | ADR fora do índice; ADR sem custo declarado |
| `PacotesDeclaradosTest` | pacote sem documentação; **núcleo importando cliente** |
| `TraducaoDeConteudoTest` | bloco ou item sem nome nos dois idiomas; chave órfã após rename |
| `EscalaDaShellTest` | `scale` num `PoseStack` da shell — a causa mais barata de a aura descolar |
| `MorteETrocaDeDimensaoTest` | estado por id de entidade sobrevivendo a morte ou troca de dimensão |
| `RespostasAindaValidasTest` | veredito humano carregado adiante depois de o código mudar |
| `OMundoNaoMudaTest` | o VFX quebrar bloco, vazar `ItemEntity` ou mexer a câmera de outro |
| suite vazia | `build` verde com zero testes executados |

A regra que os mantém honestos: **portão novo é alimentado com um caso que DEVE
reprovar, e a reprovação é confirmada.** Régua que nunca reprova é carimbo.

---

## 9. O padrão que a varredura mostra, e vale mais que qualquer número

### 9.1 "Código completo" é o estado dominante — e não é "entregue"

Somando as trilhas: o M4 espera gate de dois clientes, EN1–EN15 esperam gates de
jogo, a World Tree espera alguém ver a copa, o Posto espera captura visual, e a
trilha AV fechou cinco dos nove gates **por decisão de que o resto é gosto**, e
não por execução.

**A máquina de fazer código está muito à frente da máquina de conferir.** Isso
não é defeito escondido — está escrito em três documentos diferentes. Mas só
fica visível quando se olha tudo junto.

### 9.2 Todo defeito caro deste projeto foi silencioso

Nenhum dos achados que custaram tempo apareceu como exceção:

- o ripple que **disparava e não se via** — três correções por raciocínio, todas
  erradas, e a quarta depois de instrumentar;
- a força do brilho aplicada **duas vezes**;
- oito mobs com a **animação invertida**, de uma premissa sobre o eixo Z deduzida
  da regra da mão direita e nunca conferida;
- o kiriko **esquecendo o veredito** ao descarregar o chunk;
- o nível `OFF` **não cumprindo o ADR-016**, porque `FAST` compensava por
  geometria e `OFF` não compensava com nada;
- e o mais recente: **Ken apaga a aura do próprio jogador** enquanto os outros o
  veem aceso, porque há duas tabelas de técnica → modo visual e elas discordam.

O CLAUDE.md mantém uma lista de dez erros previsíveis. **A varredura encontrou
instâncias reais de pelo menos quatro deles** — e em todos os casos o código que
sobreviveu carrega o comentário explicando por quê.

### 9.3 O que nenhum portão cobre hoje

- **A ligação entre as duas tabelas de modo visual.** `ModoVisualDeTecnica` (o
  que você vê) e `EstadoVisualDeTerceiro` (o que os outros veem) já discordam, e
  nada reprova. `VfxDeAuraLigadoTest` cobre Ten, Ren, Zetsu, vazio e `null` —
  não cobre Ken, Gyo, Ko nem Shu.
- **Carregamento de registro** (#24) — dívida aberta desde o M0.
- **Julgamento visual.** `RespostasAindaValidasTest` invalida um veredito quando
  os arquivos mudam, mas resource pack, driver, GPU e uma mudança do próprio
  Minecraft não movem digest nenhum.
- **Qualquer coisa que exija dois clientes reais.** É o gargalo estrutural do
  projeto, e aparece no M4, no EN14, no AV6, no AV7 e no Posto.

---

## 10. Se fosse para escolher o próximo passo

Não é decisão minha. As quatro abaixo estão em ordem de **custo de adiar**, e não
de esforço:

1. **O defeito do Ken** — é o único caso conhecido em que uma técnica entregue
   torna o jogo pior do que não tê-la ligado. É conserto, não gosto, e o portão
   que falta nasce junto.
2. **O gate do M4 com dois clientes** (#91) — destrava o M5, e o M5 é o framework
   de habilidades, que é metade do MVP.
3. **A camada de percepção** (#126) — virou o único bloqueio estrutural
   compartilhado por In, En e a metade que falta de Gyo.
4. **Um gate de jogo qualquer da trilha EN** — quinze faixas "completas" sem
   nenhuma confirmação em tela é o maior acúmulo de risco não medido do projeto.

---

## 11. O que este documento não prova

Ele foi escrito a partir de uma varredura de arquivos, contagem de testes e
leitura dos documentos de estado. **Nada aqui foi verificado em jogo por causa
deste documento.**

Onde ele diz "entregue", está repetindo o que a fonte de verdade da trilha diz —
e cada uma dessas fontes carrega a própria lista do que não foi olhado. A lista
completa de ausências continua em
[`o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md), com **190 linhas**,
e ela é maior que este arquivo de propósito.
