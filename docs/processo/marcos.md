# Marcos M0 a M8: o caminho ate o MVP

Os marcos sao sequenciais **por dependencia tecnica**, nao por calendario. Nao
ha prazo aqui de proposito.

Dentro de cada marco, as duas lanes correm em paralelo. Um marco so fecha
quando o **gate de saida** esta verde em cliente **e** em servidor dedicado.

```
M0 Bootstrap
 └─> M1 Persistencia + sync
      └─> M2 Aura Engine + HUD
           └─> M3 Despertar + categoria
                └─> M4 Tecnicas fundamentais
                     └─> M5 Framework de habilidades
                          └─> M6 Progressao + camada de modpack
                               └─> M7 Hardening
                                    └─> M8 Vertical slice + RC

  AV0 ─> AV1 ─> ... ─> AV8      trilha do VISUAL da aura, em paralelo a M5+
                                (nao gasta contrato de servidor nem protocolo)
```

---

## Regras que valem em todos os marcos

1. `main` sempre buildavel.
2. As duas pessoas nao trabalham sobre a mesma implementacao concreta quando um
   contrato pode ser separado primeiro.
3. Cada marco produz **algo testavel no jogo**. Nao acumular infra invisivel por
   varios marcos.
4. Epic Fight fica fora da linha critica ate o M7 ([ADR-006](../adr/ADR-006-epic-fight-fora-da-fundacao.md)).
5. Mudanca em schema de save ou em protocolo recebe versao e migracao
   explicitas.
6. Ideia nova vai para o backlog pos-MVP. Ela so entra em M0–M8 substituindo
   outra entrega do mesmo peso.
7. **Nunca implementar mais de um marco sem instrucao explicita para
   continuar.**

---

## M0 — Fundacao do repositorio e contratos  ✅ ENTREGUE

Estabelecer um projeto NeoForge 1.21.1 que compila, inicia em cliente e
servidor, e ja impoe as regras de colaboracao.

**Dev A:** MDK e Java 21, registries minimos, package e mod_id, skeleton de
api/domain/network, ADR-001, primeiro `runServer`.

**Dev B:** CI, templates de PR e issue, perfis de execucao, estrutura de
cliente/assets/datagen, matriz inicial de mods, workspace do pack de testes.

**Conjunto:** ownership dos arquivos quentes, convencao de branch e commit,
congelamento dos ids das seis categorias e do namespace, checklist de smoke
test.

### Gate de saida

- [x] Estrutura de pacotes criada, cada um com `package-info` declarando o que
      faz e qual decisao carrega.
- [x] ADR-001, ADR-002 e ADR-003 no repositorio (mais 004–007).
- [x] Contratos congelados: `NenCategory`, `PersistentNenData` v1, ids e
      direcoes de protocolo, `NenTechnique`, `NenAbility`.
- [x] Portoes que mordem: ids de categoria, traducao, protocolo x documento,
      indice de ADR, package-info, dominio sem import de cliente.
- [x] CI de build em todo PR.
- [x] **`gradlew build` verde**, com `Testes executados: 21` e JAR gerado.
- [x] **Servidor dedicado inicia com o JAR** — `Done (8.041s)`, sem
      `NoClassDefFoundError`.
- [x] **Nenhuma classe client-only carregada no servidor** — portao estatico
      mais `runServer` real.
- [x] **`runClient` executado** — camada de cliente carrega, sem erro (#18).
- [x] **JDK 21 instalado na maquina** — Temurin 21.0.12; `build` verde sem JDK
      portatil (#17).
- [ ] **CI verde num PR de cada PESSOA** — o CI roda verde em todo PR, mas
      todos foram abertos pela mesma pessoa. Fecha quando a segunda abrir o
      primeiro. E o unico item do M0 que depende de gente, e nao de codigo.

Os limites completos, e os defeitos que a execucao encontrou, estao em
[o-que-nao-provamos.md](../testing/o-que-nao-provamos.md).

---

## M1 — NenProfile persistente + protocolo de sync  (concluído em 2026-09-11)

> **Entregue ate agora:** perfil ligado ao ciclo de vida (#2), `RuntimeNenState`
> e scheduler central (#3), fixtures de save e portao de regressao em NBT (#8),
> cache de cliente e overlay (#6), comandos de debug (#7), records de payload,
> registro S2C e handshake de versao (#4), e o primeiro gametest (#35).
>
> **Entregue:** validação e rate limit dos payloads C2S (#5), mundo de regressão
> (#27), matriz de QA com dois jogadores (#9), e a correção de reenvio após
> mutação (#43). A execução dedicada foi registrada em
> [`qa-matrix.md`](../testing/qa-matrix.md).

Provar save, clone e sincronizacao **antes** de criar gameplay que dependa
disso.

**Dev A:** `AttachmentType` ligado ao ciclo de vida, politica de clone na
morte, API de query e mutacao server-side, `RuntimeNenState`, scheduler central
de tick.

**Dev B:** records de payload e `StreamCodec`, cache somente-leitura no
cliente, comandos de debug permissionados, overlay tecnico, fixtures de save,
teste de restart.

**Conjunto:** handshake de versao de protocolo; testar jogador novo, relog,
restart, morte e volta do End; revisar que nenhum payload C2S carrega valor
autoritativo.

### Gate de saida

- [x] Categoria e unlocks persistem em teste artificial.
- [x] O perfil de um jogador nunca aparece no cache de outro.
- [x] Restart do servidor mantem os dados.
- [x] Morte respeita exatamente a politica definida.

---

## M2 — Aura Engine + HUD  (concluído em 2026-09-11)

**Integrado na `main` pelo PR #54, com CI verde.** 131 testes JUnit, 7
GameTests, servidor dedicado com dois clientes, medição de deltas e o primeiro
perfil de spark arquivado. Evidências e limites em
[m2-aura-sync.md](../testing/m2-aura-sync.md).

Gate conferido item a item:

| Item do gate | Como ficou |
| --- | --- |
| Aura nunca negativa, NaN ou acima do máximo | `AuraPool` recusa não-finito e faz clamp; coberto por `AuraPoolTest` e `MotorDeAuraTest` |
| HUD reflete o estado depois de lag e reconexão | medido a 2 TPS e com reconexão em processo novo |
| O servidor nega gasto inválido | negativo, zero, NaN, infinito, reserva insuficiente e output excedido |
| Aura não gera pacote desnecessário | `ControleDeSync` com dirty check e cadência; 100 ticks limpos = zero pacotes |
| Régua junto do número | contadores em modo dev e spark em `docs/testing/perfis/` |

Uma divergência ficou registrada em vez de corrigida no marco:
**[#56](../../issues/56)** — gasto de aura zero é recusado, o que contradiz o
`AbilitySpec`. Não morde hoje porque não existe habilidade; morde no M5.

A primeira mecanica jogavel.

**Dev A:** `AuraPool`, regeneracao, output, exaustao, formulas e configs,
dirty flags de sync, testes unitarios dos invariantes.

**Dev B:** barra de aura, feedback de exaustao, interpolacao no cliente,
framework de keybind, teste de frequencia de pacote.

**Conjunto:** definir os invariantes numericos; instrumentar contadores de
sync e de tick em modo dev; primeiro perfil com spark.

### Regra de arquitetura da M2

Aura nao e mana nem uma stamina comum. O jogador ve uma unica barra de
`currentAura / maxAura`; o motor tambem calcula `auraOutput`, `auraControl`,
`typeEfficiency` e o estado de Nen. Output e controle sao grandezas, nao barras
concorrentes. Exaustao e derivada da reserva, output, controle e estado.

Stamina fisica continua representada por vida, fome, exhaustion, sprint e
velocidade de ataque do Minecraft. Uma barra customizada de folego fica fora do
MVP e so volta ao plano se um sistema de combate futuro tiver um consumidor
claro (dash, dodge, bloqueio, parry ou ataque pesado). Esta decisao esta no
[ADR-009](../adr/ADR-009-modelo-de-aura-sem-stamina-de-nen.md).

Exemplo de HUD do desenho-alvo (output/estado dependem de evolução versionada
do protocolo e das técnicas; não estão no payload runtime v1 da M2):

```
AURA 16.840 / 21.500  ████████████████░░░░ 78%
Output maximo: 1.800   Estado: REN
```

O output nao e uma segunda barra e nao deve ser confundido com a reserva.

### Gate de saida

- Aura nunca negativa, nunca NaN, nunca acima do maximo.
- O HUD reflete o estado depois de lag e de reconexao.
- O servidor nega gasto invalido.
- Aura nao gera pacote desnecessario.

> **Aqui nasce o primeiro numero ajustavel de verdade.** Ele nasce com a regua
> junto: contador em modo dev e um perfil de spark arquivado. Numero sem regua
> vira folclore.

---

## M3 — Despertar, categoria e afinidade  (concluído em 2026-09-11)

**Integrado na `main` pelos PRs #64, #68, #69, #76, #77 e #81, com CI verde.**
214 testes JUnit, 37 GameTests, e o gate executado num servidor dedicado com
**dois clientes reais**. Evidências e limites em
[m3-categoria.md](../testing/m3-categoria.md).

Gate conferido item a item:

| Item do gate | Como ficou |
| --- | --- |
| Duas pessoas com categorias diferentes e persistentes | `Gon=emission` e `Kurapika=conjuration`, simultâneos; revelar um não revelou o outro |
| Categoria pode existir escondida até a revelação | provado **no log do cliente**: servidor com `category=emission` e cliente recebendo `undetermined`, até o `reveal` |
| Consulta de afinidade é determinística | matriz em datapack recarregável; 60 000 sorteios sem categoria degenerada |
| Quest dispara o despertar por API ou comando, nunca escrevendo NBT | `/nen awaken` passa por `NenAwakeningService`; nenhum caminho escreve o attachment direto |
| Núcleo sem FTB Quests | `mods/` com só o `nenfoundation`; zero erro e zero classe client-only no log do dedicado |
| Recarga de datapack não corrompe perfil | `/reload` com os dois conectados; perfis idênticos campo a campo |

**Duas dívidas saem do M3 declaradas, e nenhuma delas é silenciosa:**

**1. ~~O gatilho da Water Divination ainda não é Ren.~~ PAGA na issue #90.**
Ren chegou no M4, o teste que fixava a dívida reprovou como prometido, e a
condição passou a exigir Ren sobre o copo — com recusa própria para quem está
desperto e sem Ren, distinta da de quem nunca despertou. A condição continua
isolada, agora em `NenAguaDivinatoria.recusaPara`.

**2. Nada abaixa o máximo de Output ainda.** O modelo selecionado/máximo/efetivo
existe e é testado, mas o `min` nunca morde em produção porque nenhum código
reduz o teto. Técnica e exaustão são consumidores deste marco seguinte.

**Dev A:** API e evento de despertar, atribuicao e revelacao de categoria,
matriz de afinidade orientada a dado, regra propria de Specialist, marcos base.

**Dev B:** UI de descoberta, item ou ritual de Water Divination, assets, lang,
tooltips, primeiro hook com FTB Quests no ambiente do pack.

**Conjunto:** garantir que o nucleo funciona sem FTB Quests; comandos de seed
para reproduzir todas as categorias; testar recarga de recurso sem corromper
dado de jogador.

### Gate de saida

- Duas pessoas com categorias diferentes e persistentes.
- Categoria pode existir escondida ate a revelacao.
- Consulta de afinidade e deterministica.
- Quest dispara o despertar por API ou comando, **nunca** escrevendo NBT.

---

## M4 — Tecnicas fundamentais

**Dev A:** ciclo de vida de `NenTechnique`, Ten e Ren server-side,
modificadores defensivos e ofensivos, motor de exclusao e interrupcao.

**Dev B:** Zetsu e Gyo sobre a mesma interface, icones, feedback, keybinds, FX,
testes de percepcao de Gyo.

**Conjunto:** revisao cruzada (A revisa Zetsu/Gyo, B revisa Ten/Ren); testar
todas as combinacoes invalidas; definir `StopReason` e as mensagens de UX.

### Gate de saida

- As quatro funcionam em servidor dedicado.
- Zetsu cancela e impede estados conforme a matriz.
- Aura zero encerra tecnica corretamente.
- Morte e logout nao deixam estado fantasma.

### Estado — 2026-09-12

**Sao sete tecnicas.** Ten (#86), Ren (#87), Zetsu (#125), Gyo, Shu, Ken e Ko
estao registradas em producao e exercitadas em `runGameTestServer`.

O texto anterior desta secao dizia que **Gyo nao foi implementado** e que
`nen/combat/` tem so o `package-info`. Os dois bloqueios cairam em 2026-09-12:
a alocacao de aura por regiao ganhou modelo proprio e ADR
([ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md)), e a camada de dano
nasceu em `nen/combat/` com um unico handler de entrada. **Fica registrado que
este paragrafo ficou falso por um dia inteiro sem que nada acusasse** — e o
codigo ganha do texto por regra, nao por sorte.

**Uma metade de Gyo continua fora:** perceber aura fraca e aura escondida.
Nao ha camada de percepcao e nao ha In, entao nao existe nada escondido para
revelar. E so essa metade que #126 ainda descreve.

| Entrega | Estado |
| --- | --- |
| Ciclo de vida de `NenTechnique`, exclusao, interrupcao | entregue (#85) |
| Ten, Ren | entregues (#86, #87) |
| Zetsu, e a regra de quem ABAIXA o teto de Output | entregue (#125) |
| Gyo | metade entregue: concentra por regiao (ADR-014). A percepcao continua fora (#126) |
| Shu, Ken, Ko | entregues (#162) |
| A camada de dano: a aura segura golpe | entregue (#127) |
| Modificadores defensivos **e ofensivos**, com uma ordem documentada | entregue: a aura do atacante soma ao golpe, e a ordem esta em [m4-tecnicas.md](../testing/m4-tecnicas.md) |
| Ativacao por roda / menu radial | entregue (#102, #106) |
| Tecnicas ativas visiveis no HUD, por forma e cor | entregue (#129) |
| A Water Divination passa a exigir Ren | entregue (#128) |
| Icone por tecnica e FX distinto | **fora do M4, e agora por decisao e nao por falta de tempo**: o FX de aura virou a trilha propria AV0–AV8 ([ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md)) |
| A vulnerabilidade de Zetsu ao dano de Nen | entregue (#127): com Zetsu, o golpe dói o mesmo que sem aura nenhuma — e desde #246 ele também bate como quem não tem Nen |
| Ajuste de Output por **intenção**, sem o cliente escolher o número | entregue (#71): o payload passou a carregar só a direção; protocolo 6 → 7 pelo procedimento do [ADR-011](../adr/ADR-011-descongelamento-do-protocolo.md) |
| Gate executado em servidor dedicado | **pendente**: roteiro em [m4-tecnicas.md](../testing/m4-tecnicas.md), execucao manual exige dois clientes |

**O M4 NAO ESTA FECHADO, E O QUE FALTA NAO E CODIGO.** Falta a execucao do
gate com dois clientes reais, que esta maquina so aguenta com memoria livre
suficiente (ver [qa-matrix.md](../testing/qa-matrix.md)), e a revisao cruzada.
Nao deduzir entrega a partir de build verde.

**As issues do M4 foram conferidas item a item contra o codigo em 2026-09-12**,
e nao contra a memoria. #12 e #88 fecharam; #89 e #126 tiveram o escopo
reduzido ao que sobrou de verdade; #91 continua aberta e e o gate. A conferencia
achou quatro coisas que nenhum verde acusava:

1. **Nenhum teste olhava para o `StopReason`.** `OUT_OF_AURA` existia so em
   codigo de producao -- e este documento e o roteiro davam o item por provado.
2. **`key.nenfoundation.ajustar_output` nao existia em nenhum dos dois
   idiomas**, e nao havia portao nenhum sobre nomes de tecnica nem de tecla.
3. **A classe `Gyo` nao tinha um teste so.** Toda a prova de alocacao rodava
   sobre um duble; fixar a regiao dentro dela passava pelos 116 gametests.
4. **O modificador OFENSIVO nao existia.** Ren, que no canone e o aumento de
   poder de ataque, so levantava o teto de Output -- e o sintoma era silencioso,
   porque ninguem reclama de um golpe que nao ficou mais forte.

As quatro foram consertadas (#242, #246). O que elas tem em comum e o que vale
levar para o M5: **o defeito nao estava no que o codigo fazia, e sim no que
nenhuma regua media.**

### O FX de aura saiu do M4 (2026-09-12)

As issues #98–#104 nasceram assumindo, sem dizer, que **particula e a aura**.
A direcao de arte fechada em [`docs/aura-art/`](../aura-art/LEIA-ME.md) mostrou
que isso nao chega ao alvo: uma nuvem de poeira colorida e lida como pocao, e
Zetsu vira "poeira desligada" em vez de supressao.

O [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md) trocou a fundacao —
geometria propria mais shader proprio, com particula como acabamento — e o
[ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md) resolveu o
brilho sem depender de shader pack.

**Isso nao cabe num item de marco.** Virou a trilha **AV**, descrita abaixo. As
seis issues de VFX do M4 foram **reescritas** e movidas para os gates AV
correspondentes — nao fechadas como duplicadas, para nao perder o historico.

O M4 continua devendo apenas o **gate com dois clientes** (#91), que e sobre
tecnica, e nao sobre brilho.

---

## Trilha AV — O visual da aura  (paralela a M5+)

**Fonte de verdade:** [`docs/vfx/LEIA-ME.md`](../vfx/LEIA-ME.md).
**Decisoes:** [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md) e
[ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md).
**Como se prova:** [`av-aura-visual.md`](../testing/av-aura-visual.md).

Ela e **paralela**, e nao um degrau da escada M1–M8: nao gasta contrato de
servidor, nao toca protocolo nem save, e nao bloqueia o M5. O prefixo `AV`
evita colisao com `M1–M8` e com `EN0–EN16`.

```
AV0 Tech spike        shell inflada segue as animacoes     <- se falhar, PARA
 └─ AV1 Shell autoral    shader, Fresnel, ruido, fluxo vertical
     └─ AV2 Ribbons        filamentos presos a bones
         └─ AV3 TEN final    tuning, primeira pessoa, audio, LOD
             └─ AV4 REN        transicao, colunas, pressao de chao, detritos
                 └─ AV5 Bloom    AuraGlowTarget, blur, composite, fallback
                     └─ AV6 ZETSU  supressao e visibilidade por observador
                         └─ AV7 Multiplayer, armadura, poses, GeckoLib
                             └─ AV8 Performance e release do visual
```

| Gate | O que fecha ele |
| --- | --- |
| **AV0** | a shell acompanha corrida, ataque, agachar e nadar, nos dois modelos de jogador, e o servidor dedicado nao carrega classe client-only |
| **AV1** | Ten fica convincente **sem nenhuma particula** |
| **AV2** | os filamentos nascem na superficie e acompanham os membros |
| **AV3** | todos os criterios de "TEN aprovado se", mais primeira pessoa e a tabela de distancias |
| **AV4** | Ren e claramente mais intenso, na mesma linguagem, e **nao altera o mundo** |
| **AV5** | os tres niveis de bloom funcionam, o fallback funciona, e a aura **nao aparece atraves de parede** |
| **AV6** | ausencia total para observadores, e o cliente **nao recebe** o dado de quem esta suprimido |
| **AV7** | dois clientes reais, armadura, capa, elytra e as poses; nenhum estado visual preso apos relog, morte ou dimensao |
| **AV8** | o orcamento de 10 jogadores em Ren, medido com `spark`, antes e depois |

**Uma trilha por vez continua valendo.** Nao iniciar o AV sem autorizacao
explicita, e nao comecar o AV(n+1) sem fechar o AV(n) — o AV0 existe
precisamente para ser um ponto de parada barato.

### Estado da trilha (2026-09-13)

O codigo de AV0, AV1, AV2 e de quatro issues do AV3 esta na `main`. **Nenhum
gate esta aprovado** — nem o #169, nem o #176 —, e nao ha nenhuma captura
arquivada em `docs/testing/capturas/`.

Duas coisas mudaram neste dia, e nenhuma das duas fecha gate:

1. A **averiguacao dirigida** respondeu item a item a matriz de aderencia do
   AV0, e nada reprovou. Ela tambem produziu dois achados de tuning (a borda
   nao esta mais forte que o miolo; no Ten o ruido nao tem veios) e respondeu
   um dos dois chutes registrados (o fluxo **sobe**).
2. A **bancada de captura** da issue #168 existe: `/nenvfx`, overlay em F6,
   sliders e lote de capturas nomeadas com data, commit e nivel de bloom. Ate
   entao nenhuma captura desta trilha podia ser comparada com outra.
3. Os **ultimos numeros de arte sairam do codigo** (issue #98):
   `AuraVisualPreset` e `AuraVisualProfile` foram removidos, e os dois campos
   deles que alguem lia viraram `taxa_de_faiscas` e
   `tamanho_de_particula` no perfil de resource pack. Cinco campos nao tinham
   leitor nenhum. Nao muda um pixel: muda de onde o pixel vem.

O detalhe, com numero e linha de comando, esta em
[`av-evidencias.md`](../testing/av-evidencias.md) secoes 4b, 4c e 4d. **Entrega
nao se deduz de codigo compilavel, e gate nao se deduz de relato textual.**

O que falta para o **#169** e so evidencia: as catorze capturas do conjunto,
tiradas com dois clientes reais (um Steve, um Alex) e arquivadas em
`docs/testing/capturas/AV0/`. Nenhuma linha de codigo do AV0 continua em
aberto.

---

## M5 — Framework de Hatsu / habilidades

**Dev A:** `NenAbility`, pipeline de validacao, custo, cooldown e alvo comuns,
instancia e canalizacao. Amostras: Enhancement, Emission, Manipulation.

**Dev B:** registro, slots de UI, feedback de cooldown, FX. Amostras:
Transmutation, Conjuration, Specialization. Documento "como criar uma
habilidade".

**Conjunto:** as duas pessoas criam habilidades sobre a mesma interface **sem
editar o engine da outra**. Depois da sexta, revisao arquitetural procurando
condicionais que precisam virar componente. Testar spoof de alvo e spam de
pacote.

### Gate de saida

- Seis habilidades de arquetipos diferentes funcionam.
- Acrescentar uma habilidade simples **nao** exige editar AuraEngine, registro
  de rede ou HUD.
- Cooldown e custo sobrevivem as tentativas comuns de exploit.
- Projetil e construct sao limpos ao trocar de dimensao e ao deslogar.

---

## M6 — Progressao e camada de modpack

**Dev A:** servico de progressao e proficiencia, anti-spam e retornos
decrescentes, eventos e API para quest e trainer, config inicial de balanco.

**Dev B:** onboarding em FTB Quests, manual em Patchouli, receitas em KubeJS,
loot em LootJS, spawns de teste em In Control, QA de JEI/EMI e Jade.

**Conjunto:** o fluxo jogavel inteiro — sem Nen, despertar, categoria,
tecnicas, habilidade, missao de combate. Verificar que **remover** FTB e KubeJS
ainda deixa o nucleo iniciar. Congelar a lista de versoes testadas do pack.

### Gate de saida

- Onboarding completo sem nenhum comando de admin para o jogador.
- Progressao persiste corretamente.
- Quest nao escreve NBT interno.
- Receita, loot e spawn mudam sem recompilar o nucleo.

---

## M7 — Hardening de multiplayer, saves e compatibilidade

Atacar os problemas que so aparecem depois que o sistema parece pronto.

**Dev A:** validacao de abuso e rate limit, suite de migracao de save, revisao
do pipeline de dano, otimizacao dos pontos quentes, adapter experimental de
Epic Fight.

**Dev B:** matriz de dois ou mais jogadores em servidor dedicado, smoke tests
de compatibilidade, profiling com ModernFix/FerriteCore/Embeddium, varredura de
colisao de keybind, testes de cliente e animacao do Epic Fight.

**Conjunto:** cenarios de lag, relog, exploit de morte, spam de dimensao e spam
de habilidade. Medir com spark antes e depois. **Decisao formal e escrita sobre
o Epic Fight: aprovado, experimental ou fora.**

### Gate de saida

- Nenhum exploit conhecido permite se auto-conceder aura, unlock ou dano.
- Save anterior abre e migra.
- O nucleo nao e o gargalo dominante no cenario de estresse definido.
- A lista de incompatibilidades esta documentada.

---

## M8 — Vertical slice e Release Candidate

**Dev A:** freeze da API 0.x, correcao de P0 e P1, balance pass, build de RC,
changelog.

**Dev B:** questline final do slice, polimento de HUD, manual e feedback,
manifesto e version-lock do pack, QA de instalacao limpa e de servidor.

**Conjunto:** QA completa; mundo novo percorrido do inicio ao fim **sem
comandos**; a mesma jornada com duas pessoas; catalogar o backlog pos-MVP fora
do RC.

### Gate de saida

- Despertar, categoria, aura, quatro tecnicas e habilidade funcionam ponta a
  ponta.
- Logout, restart, morte e dimensao mantem as regras.
- Servidor dedicado estavel no cenario de aceitacao.
- Documentacao de instalacao, compatibilidade e autoria de habilidade pronta.
- Tag de release candidate reproduzivel no Git.

---

## Decisoes que NAO devem ser antecipadas

- O formato final do Hatsu Builder, antes de o M5 revelar quais componentes sao
  realmente comuns.
- Acoplar combate ao Epic Fight antes de o pipeline vanilla passar pela QA do
  M4 e do M5.
- Dezenas de formulas de balanceamento antes de existir o vertical slice do M6.
- Persistencia de narrativa ou de arco dentro do `PersistentNenData`.
- Promessa de compatibilidade ampla antes de o M7 rodar os testes.

---

## Backlog pos-MVP, priorizado

| | Objetivo | Primeiro passo tecnico |
| --- | --- | --- |
| F1 | Ken, Ko, Ryu | mesma `NenTechnique`, usando modifiers e regras de foco/output ja existentes. **O lado visual ja esta pronto a partir do AV1**: intensidade por regiao e mudanca de numero, e nao renderer novo |
| F2 | Shu, En, In | generalizar contexto de item, area e percepcao; criar servico de sensing. Shu reusa shell e ribbon num `AuraItemRenderLayer`; **En exige renderer proprio** — a shell corporal nao serve; In usa o resolvedor de visibilidade do AV6 |
| F3 | Votos e limitacoes v1 | templates declarativos (condicao + bonus + penalidade), sem scripting livre |
| F4 | Percepcao de Nen em multiplayer | o servidor calcula quem percebe o que e envia filtrado por observador |
| F5 | Hatsu Builder v1 | grafo de componentes limitado + orcamento por categoria/afinidade + preview |
| F6 | Trainers e NPCs | API de desbloqueio e treino; conteudo em addon separado |
| F7 | Framework de boss | fases, telegrafos e IA em content mod; GeckoLib para animacao |
| F8 | Exame Hunter | estruturas, objetivos, regras de tentativa, Hunter License |
| F9 | Torre Celestial, Yorknew | modulos narrativos separados |
| F10 | Greed Island | projeto proprio: cartas, regras, dimensao e economia. **Nao** absorver no nucleo |
| F11 | Nen Beasts | owner, ciclo de vida, persistencia, IA e limpeza sobre o framework de construct |
| F12 | API publica 1.0 | versionar contratos, eventos, docs e compatibilidade para addons |
