# Perfis visuais e onde cada número mora

Os números do efeito. A direção de arte está em
[`direcao-visual-da-aura.md`](direcao-visual-da-aura.md); a construção está em
[`arquitetura-do-render-de-aura.md`](arquitetura-do-render-de-aura.md).

---

## 1. A regra, aplicada a VFX

O `CLAUDE.md` manda: **número que alguém vai querer girar numa sessão de ajuste
não mora no código**, e **número que foi para a config tem de SAIR do código**.

Aqui isso se reparte em quatro lugares, e a diferença importa:

| Onde | O que mora | Quem decide |
| --- | --- | --- |
| **Perfil visual (JSON de _resource pack_)** | espessura, alpha, Fresnel, fluxo, contagem/comprimento/largura de ribbon, bloom, pressão, cor | o projeto, por sessão de arte |
| **Config de cliente** (`NenClientConfig`) | qualidade, nível de bloom, densidade de partícula, distância máxima, primeira pessoa ligada, ticks de transição | **cada jogador**, para si |
| **Constante de código** | tetos de segurança, limites de design, número de regiões, número de passes | ninguém — é desenho |
| **Servidor** | técnica ativa, output, distribuição, visibilidade | a autoridade ([ADR-001](../adr/ADR-001-servidor-autoritativo.md)) |

Nada de VFX entra em `NenConfig` (o spec COMMON). Um número visual decidido
pelo servidor obrigaria os dois jogadores a ver o mesmo brilho — o que é o
oposto de conforto visual. E `NenClientConfig` **não recebe regra**: se custo,
alcance ou cooldown aparecerem lá, o cliente virou autoridade.

> **Número novo nasce medível.** A régua entra junto: contador em modo dev
> (ribbons vivas, partículas vivas, tamanho do alvo de bloom, *draw calls* de
> aura) e um perfil de `spark` arquivado em `docs/testing/perfis/`. Régua que
> mede o vazio é cerimônia; número sem régua vira folclore.

---

## 2. Deformação da shell

Os números desta seção estão **em blocos**, e **não** são escala percentual.

`CubeDeformation` recebe **unidades de modelo**, e 1 bloco = 16 unidades. Então
o consumidor converte:

```
deformacao = espessuraEmBlocos * 16.0F
```

A conversão mora num lugar só, no construtor do modelo. Espalhá-la por cada
chamada garante que um dia alguém esqueça o `* 16` e a shell fique invisível —
sem erro nenhum, porque `CubeDeformation(0.032F)` é perfeitamente válido: ele
infla 2 milésimos de bloco.

Referência de tamanho, para calibrar o olho: a segunda camada da skin vanilla
usa 0.25 unidade (`jacket`, `sleeve`) e 0.5 unidade (`hat`). A shell interna de
Ten, 0.032 bloco, dá 0.51 unidade — ou seja, ela passa **por fora** do overlay
da skin, que é exatamente o requisito.

| Camada | Ten | Ren | Por quê |
| --- | --- | --- | --- |
| filme interno | 0.032 | 0.045 | presença no corpo |
| borda | 0.052 | 0.072 | o contorno claro |
| halo externo | 0.078 | 0.110 | separa do fundo, alimenta o bloom |

Faixas de partida sugeridas: interno 0.025–0.040, borda 0.045–0.065, externo
0.070–0.095.

As três precisam ser **distintas**. Duas superfícies na mesma posição é
z-fighting garantido.

E há um teto que **não** é botão de ajuste: espessura máxima de ~0.12 bloco em
Ten e ~0.20–0.25 em Ren. Poder extremo aumenta densidade, brilho, velocidade e
pressão — **não tamanho**. Um personagem dez vezes mais forte não vira uma
esfera.

---

## 3. Preset TEN

```
shellInnerDeformation = 0.032
shellEdgeDeformation  = 0.052
shellOuterDeformation = 0.078

innerAlpha = 0.055
edgeAlpha  = 0.20
outerAlpha = 0.035

fresnelPower = 2.7

flowSpeed  = 0.12
noiseSpeed = 0.06

ribbonsNear  = 8          (faixa 6–12)
ribbonWidth  = 0.009      (faixa 0.005–0.015)
ribbonLength = 0.35       (faixa 0.15–0.60 blocos)
ribbonCycle  = 0.7–1.5 s

sparkRate = 0.4 / s

bloomStrength  = 0.20
pulseAmplitude = 0.025

groundPressure = 0        <- Ten NÃO toca o chão
debris         = 0
```

Como JSON de perfil:

```json
{
  "shell": {
    "inner_thickness": 0.032,
    "edge_thickness": 0.052,
    "outer_thickness": 0.078,
    "inner_alpha": 0.055,
    "edge_alpha": 0.20,
    "outer_alpha": 0.035
  },
  "flow":    { "speed": 0.12, "noise_scale": 1.0 },
  "ribbons": { "count": 8, "length": 0.35, "width": 0.009 },
  "bloom": 0.20
}
```

---

## 4. Preset REN

```
shellInnerDeformation = 0.045
shellEdgeDeformation  = 0.072
shellOuterDeformation = 0.110

innerAlpha = 0.09
edgeAlpha  = 0.32
outerAlpha = 0.09

fresnelPower = 1.8        <- menor = borda mais espessa

flowSpeed = 0.32

ribbons = 18              (faixa 14–28)
ribbonWidth  = 0.008–0.030
ribbonLength = 0.40–1.60 blocos
ribbonCycle  = 0.3–0.8 s

columns = 6               (faixa 4–8; sobem 0.8–2.5 blocos)

sparkRate = 4 / s

groundPressure = 0.65     (raio 0.8–1.4 blocos; até ~2.0 em output alto)
debris = 8 máx            (cubos de 0.02–0.08 bloco)

bloomStrength  = 0.55
pulseAmplitude = 0.09
```

---

## 5. Preset ZETSU

Tudo em zero. O perfil existe para que o **alvo** da interpolação seja um
objeto legítimo, e não um caso especial espalhado pelo renderer.

```
shell = 0    ribbons = 0    particles = 0
bloom = 0    pressure = 0   columns = 0
```

---

## 6. Transições

Perfil e transição são coisas separadas. Uma `AuraTransitionProfile` tem
origem, destino, duração e curva (`linear`, `easeOut`, `easeInOut`,
`overshoot`).

### OFF → TEN — ~350 ms, sem explosão

```
0–150    shell aparece localmente
150–350  ribbons entram
350      estável
```

### TEN → REN — ~0.7 a 1.0 s, em fases

```
0        Ten
0–100    shell CONTRAI 3–5%      <- a inspiração antes do golpe
100–220  flash muito breve na borda
220–500  borda cresce; ribbons aceleram
350–700  colunas verticais aparecem
500–900  pressão de chão ativa
900      Ren estável
```

A contração inicial é o detalhe que faz Ren parecer liberação em vez de
interpolação. A borda pode ultrapassar o alvo em 10–15% (`overshoot`) antes de
assentar.

### REN → TEN — mais suave

```
0–300    pressão de chão cai
150–500  colunas reduzem
300–700  shell volta
700      Ten
```

### QUALQUER → ZETSU — 200 a 500 ms

```
0–100    ribbons retraem
100–250  shell perde alpha
250–400  borda fecha no corpo
400+     ZERO
```

**Nunca trocar preset instantaneamente.** Troca seca é lida como bug de render.

---

## 7. Intensidade visual: do output, não da reserva

```
visualIntensity = clamp(sqrt(outputEfetivo / outputReferencia), 0, 1)
```

A raiz existe para que um personagem dez vezes mais forte não tenha uma aura
dez vezes maior. Reserva grande **não** é aura grande — aura é o que está sendo
liberado.

Ligar o brilho à reserva tem um sintoma específico e desagradável: a aura
**apaga** justamente enquanto o jogador está gastando.

---

## 8. Constantes de código — e por que não são config

| Constante | Valor | Por que não vira botão |
| --- | --- | --- |
| regiões corporais | 6 | é o modelo do [ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md); mudar é ADR |
| passes de shell | 3 | é o desenho do efeito |
| teto de partícula por tick | 12 | trava de segurança contra config absurda |
| teto de detrito | 12 | idem |
| espessura máxima da shell | ~0.25 bloco | limite de design, não de ajuste |
| kernel do blur | pequeno | acima disso vira névoa |

Teto de segurança e botão de ajuste são coisas diferentes: quem quer menos
partícula mexe na densidade (config); o teto existe para que uma densidade
errada não trave o cliente.

---

## 9. Config de cliente prevista

O que já existe em `NenClientConfig`:

| Chave | Faixa | Estado |
| --- | --- | --- |
| `vfx.densidadeDeParticulas` | 0.0–2.0 | **existe** |
| `vfx.ticksDeTransicao` | 1–200 | **existe** |

O que a trilha AV acrescenta — **e cada chave só nasce quando o consumidor dela
existir**:

| Chave | Valores | Nasce em |
| --- | --- | --- |
| `vfx.qualidade` | OFF, LOW, MEDIUM, HIGH, ULTRA | AV3 |
| `vfx.bloom` | OFF, FAST, HIGH | AV5 — **existe** |
| `vfx.distanciaMaxima` | blocos | AV3 — **não feita**: o corte por distância mora no LOD, e `vfx.qualidade` já o limita |
| `vfx.primeiraPessoa` | ligado/desligado | AV3 |
| `vfx.detritos` | ligado/desligado | AV4 — **existe** |
| `vfx.distorcao` | ligado/desligado | AV8 (opcional) |

Declarar `vfx.bloom` antes de existir bloom é exatamente o erro nº 7 da lista
do `CLAUDE.md`: config órfã que ninguém lê, e uma tarde girando um botão morto.

---

## 9-B. Onde o perfil visual mora — e por que não é datapack

**Correção ao que este documento dizia até o AV3.** O texto falava em "JSON de
datapack". Está errado, e o código agora diz outra coisa: os perfis vivem em
**`assets/nenfoundation/nen_vfx/`**, carregados pelo gerenciador de recursos do
**cliente**.

A razão é a mesma que separa `NenConfig` de `NenClientConfig`. Perfil visual não
muda custo, alcance, dano nem visibilidade autoritativa — ele é direção de arte
e conforto. Num **datapack**, o *servidor* passaria a ditar como a aura aparece
na tela de cada pessoa, o que contradiz o [ADR-001](../adr/ADR-001-servidor-autoritativo.md)
e a §1 deste próprio documento, que classifica estes números como "sessão de
arte" e não como regra.

Em `assets/`, três coisas passam a valer:

- eles **recarregam com `F3+T`** — e é isso que torna a sessão de arte viável:
  mexer no número, recarregar, olhar;
- um **resource pack** pode sobrepô-los, como sobrepõe qualquer textura;
- um arquivo torto vira **erro com motivo no log**, e o perfil de emergência
  assume. O de emergência é *visivelmente mais fraco* que qualquer perfil real,
  de propósito: "não carregou" precisa ser perceptível, e não indistinguível de
  "carregou".

O que **não** saiu para dado: a **geometria** (as três espessuras). Ela é
consumida uma vez, na construção das malhas, e não pode recarregar sem
reconstruí-las — então ela continua no código, e isso é limite real, não
preguiça.

**Correção do AV4:** a geometria deixou de ser *uma* malha. `CubeDeformation`
entra na construção da malha, e o AV4 precisou de duas coisas que não cabem num
alpha — Ren mais espesso que Ten (0.072 contra 0.052 na borda) e a shell
*contraindo* 3–5% nos primeiros 100 ms da subida. A saída foi assar uma
**escada** de espessuras (`AuraGeometryLadder`) e escolher o degrau mais próximo
por quadro: sem alocação, sem desenho a mais, e com degraus tão finos na faixa
de Ten a Ren que a troca é invisível. Os três caminhos descartados —
`poseStack.scale`, deslocamento por normal no shader e cruzar duas malhas —
estão documentados no javadoc da classe, com o motivo de cada um.

### O esquema que o código lê hoje

**As §3–§5 acima são o PLANO, e o código diverge delas.** O JSON real é
**plano**, sem os grupos `shell`/`flow`/`ribbons` que aquelas seções desenham, e
os nomes estão em português como o resto do repositório. Regra do projeto: o
código ganha, o texto se atualiza, e a discrepância se relata em voz alta em vez
de sumir. Quem for mexer num perfil usa esta lista, e não as de cima.

`assets/nenfoundation/nen_vfx/<modo>.json` — um arquivo por modo com brilho
(`ten`, `ren`); `zetsu` e `off` não têm arquivo, porque a ausência é a
informação e o código responde com o perfil apagado.

| Chave | Faixa | O que é |
| --- | --- | --- |
| `alpha_interno` | 0–1 | filme interno; presença no corpo |
| `alpha_borda` | 0–1 | a borda, que carrega a leitura |
| `alpha_externo` | 0–1 | halo externo |
| `fresnel_interno` | > 0 | expoente da camada interna |
| `fresnel_borda` | > 0 | expoente da borda |
| `fresnel_externo` | > 0 | expoente do halo |
| `velocidade_de_fluxo` | ≥ 0 | com que rapidez a energia sobe |
| `escala_de_ruido` | > 0 | repetições do ruído na superfície |
| `reforco_da_borda` | ≥ 0 | quanto o Fresnel soma à intensidade |
| `taxa_de_faiscas` | ≥ 0 | **acabamento**: quantas faíscas acompanham a shell por segundo |
| `tamanho_de_particula` | 0–1 | **acabamento**: o quanto cada faísca cresce com a intensidade |
| `filamentos` | objeto | o bloco das ribbons, abaixo |
| `pressao` | objeto | o bloco de coluna, anel e detrito, abaixo — **chegou no AV4** |
| `bloom` | objeto | `forca` (0–1) e `raio` (0–8 px de TELA) do halo — **virou objeto no AV5, quando o raio ganhou consumidor** |
| `amplitude_de_pulso` | 0–0.25 | o quanto a shell respira. Ten **não pisca**: 0.02–0.05 |

O bloco `filamentos` é **obrigatório** — um perfil sem ele é recusado inteiro.
Assumir um padrão daria um Ren que carrega e desenha filamento de Ten, e
"carregou e errado" não tem sintoma.

| Chave em `filamentos` | Faixa | O que é |
| --- | --- | --- |
| `quantidade` | 0–28 | filamentos por jogador no detalhe cheio; 28 é teto de segurança |
| `comprimento_min` | > 0 | em blocos |
| `comprimento_max` | > 0 | em blocos; a curva sorteia dentro da faixa |
| `largura` | 0–0.05 | em blocos. Acima de 0.05 vira **tubo de neon**, que é modo de falha da direção visual |
| `ciclo_segundos` | > 0 | quanto uma curva dura antes de ser trocada |

Invariante conferida na leitura: **`comprimento_min` ≤ `comprimento_max`**.

O bloco `pressao` é **obrigatório**, e em Ten ele é todo zero — escrito, e não
omitido. A razão é a mesma do bloco `filamentos`: um bloco opcional com padrão
zero daria o mesmo resultado hoje e permitiria amanhã um perfil que *esqueceu*
a pressão, e "esqueceu" é indistinguível de "zero de propósito" quando ninguém
escreveu qual era.

| Chave em `pressao` | Faixa | O que é |
| --- | --- | --- |
| `colunas` | 0–8 | correntes verticais; **8 é teto de design**, não de ajuste |
| `altura_minima` | 0–2.5 | em blocos |
| `altura_maxima` | 0–2.5 | em blocos; a curva sorteia dentro da faixa |
| `anel` | 0–1 | quanto o anel de pressão aparece |
| `anel_raio_minimo` | 0–2.0 | raio com intensidade zero, em blocos |
| `anel_raio_maximo` | 0–2.0 | raio com intensidade cheia, em blocos |
| `anel_segmentos` | 0–48 | segmentos da malha horizontal; 48 é teto |
| `detritos` | 0–12 | fragmentos cosméticos; 12 é teto |

Invariantes conferidas na leitura: **`altura_minima` ≤ `altura_maxima`** e
**`anel_raio_minimo` ≤ `anel_raio_maximo`**.

> **O raio do anel é o mesmo que limita de onde os detritos nascem.** Duas
> definições de raio seriam duas verdades, e a divergência apareceria como
> fragmento subindo fora do anel.

Uma invariante viaja com o dado e é conferida na leitura: **o expoente de
Fresnel precisa DIMINUIR** da camada interna para a externa. Com os três iguais
as camadas viram uma só mais opaca, e a profundidade que justifica os três
passes desaparece. Arquivo torto é recusado com motivo, e o perfil de emergência
assume.

O bloco `filamentos` chegou logo depois, pelo mesmo motivo: `AuraRibbonProfile`
tinha `ten()` e `ren()` no código, com um javadoc prometendo tirá-los *"quando o
perfil existir"* — e o perfil já existia havia dois gates.

As **duas chaves de partícula chegaram no AV0**, quando `AuraVisualPreset` foi
removido: elas eram `particleIntensity` e `shellOpacity`, constantes de código
ao lado de cinco irmãs que ninguém lia. A segunda tinha nome de shell e
dimensionava partícula — que é por que ninguém a encontrava procurando pelo
tamanho da faísca.

---

## 10. Assets

Todos autorais ([ADR-007](../adr/ADR-007-assets-autorais.md)), todos potência
de dois.

```
textures/vfx/nen/
  aura_noise_large.png    64x64 ou 128x128, tileable, grayscale
  aura_noise_fine.png     idem, mais fino
  aura_ribbon_core.png    16x64 ou 32x128, centro branco, borda em fade
  aura_ribbon_soft.png    idem, mais suave
  aura_spark.png          16x16
  aura_ground_ring.png    128x128
  aura_fragment.png       16x16
  aura_debris_mask.png    16x16

shaders/
  aura_shell.vsh / .fsh
  aura_ribbon.vsh / .fsh
  aura_blur_h.*  aura_blur_v.*  aura_composite.*
  aura_distortion.*   (opcional)
```

Direção dos assets:

- **ruído** — veios, ondas e filamentos. **Nunca** formas de nuvem: é o que
  transforma o efeito em fumaça sem que ninguém consiga apontar onde;
- **ribbon** — centro claro, borda em *fade*;
- **spark** — ponto ou traço pequeno. Não estrela de desenho animado;
- **chão** — arco irregular. **Sem símbolo, sem runa**: é pressão, não magia.

Uniforms do shader de shell:

```
GameTime  AuraTime  AuraIntensity  AuraOpacity  AuraThickness
FlowSpeed NoiseScale PulseStrength FresnelPower
ColorInner ColorEdge ColorOuter    EntitySeed
```

---

## 11. Distribuição por região

O renderer multiplica a intensidade por região **desde o AV1**, mesmo enquanto
todas valem 1.0:

```
head = body = leftArm = rightArm = leftLeg = rightLeg = 1.0
```

O que as técnicas futuras fazem com isso:

| Técnica | Exemplo de distribuição |
| --- | --- |
| **Gyo** no braço direito | `0.7, 0.7, 0.7, 1.8, 0.7, 0.7` |
| **Ko** no braço direito | `~0, ~0, ~0, 4.0, ~0, ~0` |
| **Ken** | todas altas e estáveis |
| **Ryu** | muda em tempo real, interpolando |

Isso torna Gyo, Ko e Ryu **mudança de número**, e não renderer novo. A
autoridade continua sendo o servidor: `AuraDistribution` de `client/vfx/` é
**projeção** do que chegou, por `AuraDistribution.daAlocacao(...)` — e isso já
está em jogo desde o PR #211 (ADR-014).

---

## 12. Orçamento de performance

Sem número absoluto de GPU — a máquina de cada um é outra. Orçamento relativo:

| Cenário | Alvo |
| --- | --- |
| 10 jogadores em Ren dentro de 16 blocos | a aura não passa de ~20% do tempo de frame alvo |
| 20 jogadores em Ten dentro de 32 blocos | o LOD impede explosão de custo |
| nenhuma aura visível | custo **zero**, não "custo pequeno" |

Medir: tempo de render de CPU, tempo de GPU quando houver *profiler*, *draw
calls*, partículas vivas, ribbons vivas, e memória dos alvos de bloom. Antes e
depois, com `spark`, arquivado.
