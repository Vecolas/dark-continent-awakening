# O que mudou do documento-fonte, e o que acontece com o VFX de hoje

Duas reconciliações que precisam existir por escrito, porque nas duas há **duas
fontes para a mesma verdade**:

1. o documento-fonte em [`docs/aura-art/`](../aura-art/LEIA-ME.md) × o que este
   repositório vai fazer;
2. o `client/vfx/` que **já está na `main`** × a arquitetura do
   [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md).

Um plano "seguido" sem se dizer onde foi contrariado é a forma mais barata de
criar divergência silenciosa.

---

## Parte 1 — Onde este projeto contraria o documento-fonte

O documento-fonte tem 167 seções e foi escrito antes de conhecer as regras
deste repositório. **A direção visual dele foi aceita quase inteira.** O que
mudou foi processo, fronteira e honestidade sobre custo.

| # | Documento-fonte | O que vale aqui | Por quê |
| --- | --- | --- | --- |
| 1 | 11 "milestones" numerados 0–10 | **9 gates AV0–AV8** | AV6 (Zetsu) e AV7 (multiplayer) e AV8 (armadura/poses/performance) foram fundidos onde o gate era o mesmo. Gate que não reprova nada é carimbo. |
| 2 | seção 44: *"shimmer de 1–2% opcionalíssimo"* em Zetsu | **proibido para observadores**; permitido só como feedback de input do jogador local, e some | Num servidor com dois clientes, brilho residual entrega quem está se escondendo. A referência D é literal: ausência é a informação. |
| 3 | seção 74: *"considerar observer-specific payloads"* | **não é consideração, é requisito** | O cliente não pode receber o que não deve enxergar. Mandar a aura de quem está em Zetsu "para o cliente esconder" é entregar informação a um cliente modificado (ADR-001). |
| 4 | seções 106–108: perfis em JSON *"opcionalmente"* | perfil em JSON é **o** caminho | "Onde mora um número" não admite opcional: número de ajuste fora do código, ou a sessão de balanceamento gira botão morto. |
| 5 | seção 88: lista de configs gráficas de uma vez | **cada chave nasce quando o consumidor dela existir** | `NenConfig`/`NenClientConfig` só ganham chave com consumidor. Declarar `vfx.bloom` antes do bloom é o erro nº 7 do `CLAUDE.md`. |
| 6 | seção 19: cor sugerida por perfil | mantido, **e reforçado**: a cor vem da mesma `AparenciaDeTecnica` do HUD e da roda | Duas fontes para "de que cor é Ren" garantem que um dia discordem. |
| 7 | seção 70: *"body weights para Gyo/Ryu futuro"* | as regiões **já existem e são autoritativas no servidor** | [ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md) chegou antes deste plano. O cliente **projeta**; não inventa. |
| 8 | seção 84: LOD em 12/24/48/72 | tabela única, no AV3, reconciliada com o `AuraRenderLod` que já existe (8/20/40) | Duas tabelas de LOD no repositório são duas verdades. A de [`arquitetura-do-render-de-aura.md`](arquitetura-do-render-de-aura.md) ganha. |
| 9 | seção 89: auto-quality por FPS | **fora do escopo AV**, e não "opcional" | Visual que muda sozinho durante o jogo é pior que visual constante ruim. Config manual primeiro. |
| 10 | seção 57: distorção screen-space | **fora do MVP visual**, reavaliada no AV8 | Distorção forte parece água ou portal. Ganho pequeno, risco de compatibilidade grande. |
| 11 | seção 39-B: shader de vegetação lendo campo de pressão | **fora**, permanentemente, até prova em contrário | Invade o pipeline de foliage e briga com Embeddium/Iris. O overlay no chão (opção A) entrega 90% da leitura. |
| 12 | seção 131: regressão por screenshot *"recomendado"* | **é o método de aprovação**, com roteiro escrito | Sem captura comparada contra referência, o ajuste vira achismo. O roteiro está em [`../testing/av-aura-visual.md`](../testing/av-aura-visual.md). |
| 13 | seção 128: divisão A/B de trabalho | mapeada para as lanes reais e para a [fronteira de arquivos](../processo/fronteira-de-arquivos.md) | "Dev A render core, Dev B motion/art" não diz quais arquivos travam o repositório. |
| 14 | seção 164: prompts prontos para IA | **não entram no repositório como processo** | A unidade de trabalho aqui é issue com cinco campos, não prompt. O conteúdo deles virou escopo de issue. |
| 15 | nada sobre GeckoLib | adaptador previsto desde o desenho | [ADR-012](../adr/ADR-012-geckolib-obrigatorio.md): mob customizado usa bones de GeckoLib, e a shell de jogador não serve para ele. |
| 16 | nada sobre `runServer` | **toda entrega da trilha roda em servidor dedicado** | Erro nº 10 do `CLAUDE.md`: singleplayer não pega classe client-only vazada. VFX é justamente a camada que mais vaza. |

### O que foi aceito sem mudança

Vale dizer, porque a lista acima pode dar a impressão errada. Foram aceitos
integralmente: a deformação por cubo em vez de `scale`; a layer no
`PlayerRenderer`; os três passes de shell; o Fresnel; os dois níveis de ruído;
o fluxo vertical; a pulsação com fase por UUID; ribbons presas a anchors de
bone; a pool determinística; Ren como amplificação de Ten; o anel irregular de
chão; os detritos cosméticos; o bloom próprio com fallback; o tratamento
separado de primeira pessoa; e o aviso sobre `EntityRenderState` só existir a
partir de 1.21.2.

---

## Parte 2 — O que acontece com o `client/vfx` que já está na `main`

O pacote nasceu no M4 com contrato completo e **quase nada desenhando**. Ele
não é jogado fora: a maior parte vira a camada de estado que a nova arquitetura
precisa de qualquer jeito.

| Arquivo de hoje | Destino | O que muda |
| --- | --- | --- |
| `AuraVisualState` | **fica, enxugado** | o plano dizia "ganha campos de shell/ribbon/bloom/pressão". **O código foi para o outro lado, e ele ganha:** o estado PERDEU o único campo de arte que tinha. Ele carrega o que muda por tick e por jogador — modo, intensidade, transição, distribuição, cores; os números de arte moram no perfil e se buscam por `AuraPerfis.de(estado.mode())`. Duplicá-los dentro do estado criaria duas fontes para o mesmo Ren |
| `AuraVisualController` | **fica** | a correção de interpolação (origem congelada, e não `atual`) continua valendo e não se toca |
| `AuraVisualMode` | **fica** | inalterado |
| `AuraVisualPreset` | **REMOVIDO** (AV0) | cinco dos sete campos (`shellScale`, `edgeIntensity`, `flowIntensity`, `pulseAmplitude`, `pulseFrequency`) nunca tiveram um leitor sequer. Os dois que tinham — contagem e tamanho de partícula — viraram `taxa_de_faiscas` e `tamanho_de_particula` no perfil de _resource pack_ |
| `AuraVisualProfile` | **REMOVIDO** (AV0) | o perfil de JSON nasceu com outro nome no AV3 — `model/AuraPerfilVisual` — e este record ficou para trás sem nenhum construtor fora do próprio teste dele. Um record que só o teste dele constrói prova a si mesmo |
| `AuraVisualQuality` | **fica** | ganha `ULTRA` e os interruptores separados |
| `AuraRenderLod` | **fica, recalibrado** | os cortes 8/20/40 passam para a tabela de cinco níveis no AV3 |
| `AuraBodyRegion` | **fica** | vira índice de intensidade no shader |
| `AuraDistribution` | **fica, rebaixado** | virou **projeção** do delta do servidor no PR #211, por `daAlocacao(...)`. Os construtores próprios **ficam** — ver a nota abaixo |
| `AuraFlowPattern` / `AuraFlowSample` | **substituídos** | eram posicionamento barato de filamento sem renderer; o lugar deles é `ribbon/AuraCurve` |
| `AuraImpactState` | **fica** | passa a alimentar o ripple localizado de verdade |
| `ModoVisualDeTecnica` | **fica** | a precedência (Zetsu > Ren > Ten) continua sendo a única ponte domínio → visual |
| `SessaoDeVfxDeAura` | **fica** | continua sendo o dono do controlador e o único que o tica |
| `EstadoVisualDeTerceiro` | **fica** | a decisão de derivar em vez de guardar estado por entidade continua certa |
| `EmissorDeParticulasDeAura` | **rebaixado a acabamento** | deixa de ser "a aura" e passa a ser faísca, com o teto que já tem |

#### Uma discrepancia entre o ADR-014 e o codigo que ele gerou

O [ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md), em *Custo assumido*,
promete que `AuraDistribution` **"deixa de ter construtor proprio assim que o
delta carregar o campo"**. O delta passou a carregar o campo no PR #211 — e os
construtores **continuam la**, de proposito.

O codigo esta certo, e o texto do ADR e que envelheceu. A razao esta escrita no
proprio arquivo: a transicao **interpola entre duas distribuicoes**, e Zetsu
**zera todas as regioes** — nenhuma das duas vem do servidor, porque nenhuma
delas e um fato de gameplay. Sao quadros intermediarios de uma animacao local.

A regra que o ADR-014 queria proteger sobrevive inteira, e e mais estreita do
que a frase que ele usou: **ninguem inventa aqui uma distribuicao que influencie
dano, custo ou alcance.** Essa nasce no servidor. Construir uma distribuicao
para desenhar um quadro de transicao nunca foi o risco.

Registrado aqui, e nao corrigido dentro do ADR-014, porque ADR e registro
historico de decisao e porque aquele arquivo e da outra lane. O ajuste da frase
e dela.

### A janela de transição fechou no AV3

Na issue #186, a nuvem cilíndrica de partícula vanilla foi removida. A shell e
os filamentos agora sustentam a identidade da aura; `AuraSpark` é somente uma
faísca autoral ocasional, emitida junto das mesmas âncoras dos filamentos.

Assim, terminou a janela temporária em que o jogo mostrava a nuvem antiga junto
da shell nova. `vfx.densidadeDeParticulas = 0.0` continua disponível, agora como
controle do acabamento: zerá-lo não apaga nem enfraquece a leitura de Ten.

O rascunho da issue nomeava `textures/vfx/nen/aura_spark.png`. No cliente real,
esse caminho não entra automaticamente no atlas `minecraft:particles` e gerou
`Missing particle sprites`. O asset final fica em
`textures/particle/aura_spark.png`, com o descritor em
`particles/aura_spark.json`; o segundo carregamento do cliente confirmou o
sprite no atlas sem o aviso.

---

## Parte 3 — As issues do M4 que esta trilha substitui

As issues #98, #99, #100, #101, #103 e #104 foram abertas com a suposição de
que partícula é a aura. **Elas não foram fechadas como duplicadas: foram
reescritas**, mantidas como âncora de histórico, e movidas para o gate AV
correspondente.

| Issue original | Vira | Gate |
| --- | --- | --- |
| #98 — contrato `AuraVisualState` e presets | contrato visual + perfis em datapack | AV0 |
| #99 — shell corporal aderida | modelos inflados `default`/`slim` + layer | AV0 |
| #100 — filamentos e micro-partículas | sistema de ribbons com anchors | AV2 |
| #101 — transições e presets de técnica | máquina de transição em fases | AV3/AV4/AV6 |
| #103 — ripple de impacto | ripple localizado por região | AV4 |
| #104 — LOD, visibilidade e performance | LOD, visibilidade por observador, orçamento | AV7/AV8 |

O marco M4 deixa de ter linha de FX. O que ele ainda precisa é o **gate com
dois clientes** (#91), que é sobre técnica, não sobre brilho.
