# Arquitetura do render de aura

Como o efeito é construído. O que ele precisa **parecer** está em
[`direcao-visual-da-aura.md`](direcao-visual-da-aura.md); os números estão em
[`perfis-visuais.md`](perfis-visuais.md).

Decisões que este documento executa:
[ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md) e
[ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md).

---

## 1. Os passes

```
                    SISTEMA VISUAL DE AURA
                             |
     +-----------------------+-----------------------+
     |                       |                       |
  SHELL                  FILAMENTOS               ACABAMENTO
     |                       |                       |
  filme interno          ribbons de corpo         faíscas
  borda (Fresnel)        colunas verticais        pressão de chão
  halo externo           laços                    detritos
     |                       |                       |
     +-----------------------+-----------------------+
                             |
                     BLOOM (opcional)
                             |
                       IMAGEM FINAL
```

Nenhum passe isolado precisa fazer tudo. É o conjunto que produz a leitura.

---

## 2. Por que um modelo próprio, e não `scale`

O jogador é feito de partes rígidas com **pivôs diferentes**: `head`, `body`,
`rightArm`, `leftArm`, `rightLeg`, `leftLeg`.

```java
poseStack.scale(1.05F, 1.05F, 1.05F);   // ERRADO
```

A escala acontece em torno do origin da parte. Resultado: os braços se afastam
do corpo, a espessura fica inconsistente entre membros, e a película "descola"
justamente nas articulações — que é onde a referência
[B](../aura-art/referencia-b-ten-em-camadas.png) mais depende de aderência.

O caminho certo é **`CubeDeformation` positiva**, aplicada cubo a cubo na
construção do modelo:

```
corpo:  ████████
aura:  ░████████░
```

Cada cubo cresce localmente. A distância à superfície fica uniforme em toda
parte do modelo.

São **dois** modelos, e isso não é detalhe: `AURA_DEFAULT` e `AURA_SLIM`. Usar
o modelo `default` num jogador com braço `slim` deixa a aura do braço larga
demais, e o defeito só aparece quando alguém com skin Alex entra no servidor.

A deformação também precisa ser maior que a **segunda camada da skin** (`hat`,
`jacket`, `sleeve`), senão a aura desaparece dentro dela em skins com overlay
completo.

---

## 3. Onde o efeito se pendura

```
EntityRenderersEvent.AddLayers
        |
        +-- PlayerRenderer "default"  -> addLayer(AuraPlayerRenderLayer)
        +-- PlayerRenderer "slim"     -> addLayer(AuraPlayerRenderLayer)
```

Layer é o caminho principal porque ela **herda a pose**: yaw, pitch, crouch,
swim, ataque, corrida e rotação de cabeça chegam prontos. Um renderer paralelo
teria de reimplementar a pose e divergiria dela na primeira animação nova.

`RenderPlayerEvent.Pre/Post` fica como auxiliar, para passes que precisam
acontecer **depois** do jogador completo (halo externo, contribuição para o
alvo de bloom).

> ### Armadilha de versão, e ela é grande
>
> **1.21.1 está ANTES da reforma de `EntityRenderState`**, que chegou na
> migração 1.21.1 → 1.21.2. Exemplo de renderer publicado depois disso **não
> serve**: ou não compila, ou compila contra uma API que não existe aqui.
>
> Ver o primer da migração
> [1.21 → 1.21.1](https://docs.neoforged.net/primer/docs/1.21.1/) e o da
> [1.21.1 → 1.21.2](https://docs.neoforged.net/primer/docs/1.21.2/) — o segundo
> serve para saber **o que não copiar**.
>
> Pelo mesmo motivo, o código de shader fica num módulo pequeno e isolado: a
> linha de `RenderType`/shader mudou em 1.21 e vai mudar de novo.

---

## 4. A shell, em três passes

| Passe | Papel | Blend | Profundidade |
| --- | --- | --- | --- |
| **1 — filme interno** | presença contínua no corpo | alpha | teste ON, escrita OFF |
| **2 — borda (Fresnel)** | o contorno claro da referência | aditivo moderado | teste ON, escrita OFF |
| **3 — halo externo** | separa a aura do fundo e alimenta o bloom | alpha muito baixo | teste ON, escrita OFF |

Uma camada translúcida só é lida como plástico, vidro ou armadura
holográfica — a referência exige profundidade, e profundidade vem de camadas
com papéis diferentes.

**Alpha em tudo** deixa a aura plástica. **Aditivo em tudo** estoura. A mistura
é requisito, não gosto.

As três deformações são distintas de propósito (ver
[`perfis-visuais.md`](perfis-visuais.md)): renderizar duas vezes exatamente a
mesma superfície é z-fighting garantido.

A iluminação mistura emissivo com a luz do mundo — algo como 60–80% emissivo e
20–40% do `packedLight`. **Fullbright absoluto** transforma a aura num borrão
branco dentro de caverna.

### O Fresnel

```
fresnel   = pow(1 - abs(dot(normal, viewDir)), fresnelPower)
finalAlpha = baseAlpha + fresnel * edgeAlpha
finalColor = mix(innerColor, edgeColor, fresnel)
```

Expoente menor ⇒ região brilhante maior. Ten usa expoente alto (borda fina);
Ren usa expoente baixo (borda espessa).

### O ruído

Dois samples, e o objetivo é **quebrar uniformidade**, não simular nuvem:

```
finalNoise = N1 * 0.65 + N2 * 0.35
```

`N1` lento e largo; `N2` fino e rápido. A textura é *tileable*, grayscale, e
desenhada com **veios e filamentos** — não com bolhas macias, que é o que
transforma o efeito em fumaça.

### O fluxo vertical

```
flowCoord = worldY * noiseScale - time * flowSpeed
```

A energia nasce no corpo e sobe. Ondas horizontais aleatórias destroem a
leitura: o olho lê "vento", não "aura".

### A pulsação

```
pulse = 1 + sin(time * frequency + entityPhase) * amplitude
```

`entityPhase` vem de `hash(UUID)`. Sem ela, todos os jogadores da sala pulsam
em sincronia — e sincronia acidental é a coisa mais artificial que um efeito
orgânico pode fazer.

Ten **não pisca**: amplitude entre 0.02 e 0.05.

### O que a shell não faz

**Não renderizar a textura da skin translúcida por cima do jogador.** Isso
produz um clone fantasma do personagem, não uma aura. A shell usa ruído,
gradiente, fluxo e Fresnel; a skin normal continua desenhada por baixo, intacta.

---

## 5. Ribbons — o componente que faz o efeito parecer anime

Os filamentos das referências **não são partículas**.

| | Partícula | Ribbon |
| --- | --- | --- |
| forma | ponto solto | curva contínua |
| segue o bone? | não | sim, pelo anchor |
| abraça um braço? | não | sim |
| vira fumaça sozinha? | sim, com facilidade | não |

Uma ribbon é uma tira de quads:

```
P0 ── P1 ── P2 ── P3 ── P4
```

Cada segmento vira dois vértices (esquerdo e direito), orientados pela normal
da superfície e parcialmente para a câmera.

### Anchors

Cada `AuraAnchor` pertence a um `ModelPart` e tem um deslocamento local:

```
HEAD_TOP, HEAD_LEFT, HEAD_RIGHT
SHOULDER_LEFT, SHOULDER_RIGHT
FOREARM_LEFT, FOREARM_RIGHT
HAND_LEFT, HAND_RIGHT
CHEST_LEFT, CHEST_RIGHT
BACK_CENTER
HIP_LEFT, HIP_RIGHT
THIGH_LEFT, THIGH_RIGHT
CALF_LEFT, CALF_RIGHT
FOOT_LEFT, FOOT_RIGHT
```

Como o anchor herda a transformação do `ModelPart`, o filamento acompanha
corrida, pulo, ataque, agachamento e natação — em vez de ficar para trás como
fumaça. **Este é o ponto que mais aproxima o resultado da referência.**

### Pool estável, seed determinístico

Não se sorteia ribbon nova a cada frame. Cada jogador tem uma pool fixa
(6–12 em Ten, 14–28 em Ren); ao terminar o ciclo, a ribbon recebe uma curva
nova derivada de:

```
seed = hash(UUID + indice + ciclo)
```

Orgânico sem *jitter*, e reproduzível numa captura de comparação.

### Movimento barato

A curva **não** precisa mover vértices todo frame. Animar UV, variar amplitude
e deslocar poucos nós já dá a sensação de fluxo. Ten troca de curva a cada
0.7–1.5 s; Ren, a cada 0.3–0.8 s.

### Batching

Uma *draw call* por ribbon é inaceitável. `AuraRibbonBatch` acumula todas as
ribbons do mesmo material e emite um ou dois buffers.

---

## 6. Ren: colunas, chão e detritos

- **Colunas verticais** — 4 a 8 ribbons especiais, nascendo em ombros, costas,
  pernas e perímetro da cabeça, subindo de 0.8 a 2.5 blocos. Não trinta.
- **Anel de pressão** — malha horizontal perto do chão, com 24–48 segmentos e
  raio modulado por ruído (**não** um círculo perfeito). Sem símbolo, sem runa:
  é pressão de energia, não magia.
- **Detritos** — cubos minúsculos (0.02–0.08 bloco) que sobem alguns
  centímetros, orbitam de leve e somem. Amostram a aparência do bloco abaixo,
  com cache ocasional em vez de varredura por frame.

**Nada disso toca o mundo.** Sem quebra de bloco, sem `ItemEntity`, sem
colisão. E nada disso decide física: o empurrão de Ren, se existir, é sistema
de gameplay separado.

Ao **ativar** Ren, o jogador local recebe um impulso mínimo de câmera
(0.1–0.25 grau). Observadores não recebem nada, e não existe tremor contínuo —
tremor permanente é irritante em minutos.

---

## 7. Primeira pessoa

Terceira pessoa recebe o efeito completo. Primeira pessoa, **não**.

Ganchos: `RenderHandEvent` e `RenderArmEvent`.

| Estado | O que aparece |
| --- | --- |
| Ten | borda sutil nos braços, 1–2 filamentos curtos, bloom mínimo |
| Ren | borda mais forte, 2–4 filamentos, pulso curto na ativação |
| Zetsu | nada |

Nunca uma shell enorme na frente da câmera, e nunca algo que cubra a mira ou o
item na mão. A pressão de chão de Ren continua visível no mundo, normalmente.

---

## 8. Bloom

Executa o [ADR-016](../adr/ADR-016-pos-processamento-proprio-da-aura.md).

```
aura luminosa
      |
 AuraGlowTarget      <- SÓ borda, ribbons, faíscas e colunas
      |
 downsample 1/2
      |
 blur horizontal
      |
 blur vertical
      |
 composite aditivo
      |
 imagem final
```

Três níveis: `OFF`, `FAST` (halo geométrico, sem framebuffer) e `HIGH` (passe
real). Falha de compilação ou de criação de target **cai para `FAST`** e
registra no log — efeito visual não crasha jogo.

Raio pequeno: 2–3 px em Ten, 4–7 px em Ren, escalado pela resolução. Trinta
pixels de halo é névoa.

**O halo respeita profundidade.** Sangrar alguns pixels na silhueta é
aceitável; revelar um jogador atrás de uma parede não é — e isso tem caso de
teste próprio.

Um alvo compartilhado para a cena inteira. Nunca um framebuffer por entidade.
Sem aura visível, o passe é pulado inteiro.

**Resize de janela e `F3+T` recriam os targets.** Framebuffer não recriado não
dá erro: dá tela que some ou memória que sobe devagar.

---

## 9. Classes previstas

```
client/vfx/
  AuraVisualSystem        orquestra; o único ponto de entrada
  AuraVisualState         snapshot interpolável (JÁ EXISTE)
  model/AuraPerfilVisual  os números de arte, lidos de nen_vfx/*.json
  AuraVisualController    interpolador (JÁ EXISTE)
  AuraVisibilityResolver  observador x alvo -> 0..1
  AuraLodController       distância -> nível de detalhe

  render/
    AuraPlayerRenderLayer
    AuraLivingRenderLayer
    AuraShellRenderer
    AuraRibbonRenderer
    AuraGroundRenderer
    AuraBloomRenderer
    AuraFirstPersonRenderer
    AuraRenderTypes

  model/
    AuraPlayerModel
    AuraPlayerSlimModel
    AuraModelAdapter
    AuraGeometryProfile

  ribbon/
    AuraRibbon
    AuraRibbonEmitter
    AuraRibbonBatch
    AuraAnchor
    AuraCurve

  shader/
    AuraShaderManager
    AuraShaderUniforms
    AuraPostProcess

  particle/
    AuraSparkParticle
    AuraFragmentParticle
    AuraDebrisParticle

  debug/
    AuraDebugRenderer
    AuraDebugCommands
```

**Regras de fronteira, que o portão `PacotesDeclaradosTest` cobra:**

- todo pacote novo nasce com `package-info.java` dizendo o que faz, qual
  decisão carrega, em que gate nasce e de quem é;
- **nada de gameplay entra em `client/vfx/`** — nem custo, nem cooldown, nem
  dano, nem validação;
- `nen/`, `api/`, `network/` e `server/` continuam sem importar nada daqui.

---

## 10. Tipos de render

Criados uma vez e **cacheados**:

```
AURA_SHELL      alpha,    teste ON, escrita OFF
AURA_EDGE       aditivo,  teste ON, escrita OFF
AURA_ADDITIVE   aditivo
AURA_RIBBON     aditivo,  textura de ribbon
AURA_GROUND     misto,    textura do anel
```

Nunca criar `RenderType` novo por entidade ou por frame. Nunca alocar modelo,
textura ou buffer por frame — o modelo é *baked* uma vez e só os uniforms
mudam.

---

## 11. Nível de detalhe

| LOD | Distância | O que desenha |
| --- | --- | --- |
| 0 | 0–12 | shell completa, ribbons completas, bloom, chão, faíscas |
| 1 | 12–24 | shell, ~60% das ribbons, bloom reduzido |
| 2 | 24–48 | shell e borda, 2–4 ribbons, sem detrito |
| 3 | 48–72 | só a borda |
| 4 | > 72 | nada, ou mínimo |

**Reconciliado no AV3.** Havia duas tabelas — este documento e o
`AuraRenderLod`, que cortava em 8/20/40 com quatro níveis. Ficou a de cinco
níveis, e ela agora mora **no código**: `AuraRenderLod` carrega os cortes, a
intensidade, a fração de filamentos e quais camadas cada nível desenha. Este
documento descreve; quem manda é o enum, e `AuraLodTest` o fixa.

A escolha foi pela tabela daqui, e não pela do código, por um motivo concreto:
cortar a aura completa a oito blocos é perto demais — oito blocos é a distância
de uma briga corpo a corpo, exatamente onde o estado de Nen do adversário
precisa ser legível.

A degradação segue a **hierarquia de leitura**: somem primeiro os filamentos,
depois o halo externo, depois o filme interno. **A borda é a última a sair.**

Qualidade escolhida pelo jogador: `OFF`, `LOW`, `MEDIUM`, `HIGH`, `ULTRA`, com
interruptores separados para bloom, distorção, detritos, primeira pessoa e
distância máxima.

---

## 12. Custos, e onde eles moram

Por ordem de peso:

1. **overdraw de transparência** — vários passes translúcidos sobrepostos;
2. **blur do bloom**;
3. **vértices de ribbon**;
4. **passes múltiplos de shell**;
5. **vários jogadores próximos**.

**O servidor não paga nada disso.** A aura é praticamente toda client-side; o
que atravessa a rede é técnica, output e distribuição — e só quando muda.

Tetos de partícula: 0–4 ativas por jogador em Ten, 10–24 em Ren, no máximo 12
detritos. Não 200.

---

## 13. Compatibilidade

O núcleo do efeito — shell mais ribbons — **tem de funcionar sem nenhum
pós-processamento**. Bloom é melhoria.

Ambientes a testar, e o resultado vai para
[`compatibility.md`](../testing/compatibility.md):

| Ambiente | O que se espera |
| --- | --- |
| renderer vanilla, sem shader pack | referência; tudo funciona |
| Embeddium (ou equivalente Sodium) | tudo funciona |
| Iris/Oculus **sem** pack carregado | tudo funciona |
| Iris/Oculus **com** pack carregado | detectar; cair para `FAST` se necessário; documentar |

**Nunca crashar por efeito visual.**

Recarga de recurso (`F3+T`) e redimensionamento de janela recriam targets,
religam shaders e limpam referências velhas. Os dois são teste obrigatório, e
os dois falham em silêncio quando esquecidos.

---

## 14. Tempo

`AuraTime` usa tempo de jogo interpolado por *partial tick*, e não o tempo do
mundo — que salta quando chega um pacote de sincronização ou quando muda a
dimensão. Consequência aceita: em singleplayer pausado o efeito congela; em
multiplayer, continua.

---

## 15. O que é client-side e o que vem do servidor

| Vem do servidor | Fica no cliente |
| --- | --- |
| técnica ativa (`SinalDeAura`, três valores, já filtrado) | espessura, alpha, ruído, fase |
| output efetivo | contagem e curva das ribbons |
| distribuição por região (ADR-014, quando ligada) | velocidade de fluxo, pulsação |
| intenção hostil, quando existir | bloom, LOD, qualidade |
| visibilidade (In / Zetsu) | posição de partícula |

**Nunca sincronizar** posição de partícula, nó de ribbon, tempo de shader ou
quadro de pulsação. Sincronizar só mudança relevante: técnica, ligado/desligado,
variação de output acima de 2–5%, ou um temporizador lento.

E a regra que vale mais que todas: **o cliente não recebe o que ele não deve
enxergar.** Mandar a aura de quem está em Zetsu "para o cliente esconder" é
transformar o efeito num aimbot de informação. Quem está suprimido chega como
`NENHUM`, igual a quem nunca despertou.

---

## 16. Áudio

O áudio é parte do efeito, e é onde a aura passa de visual para presença.

| Estado | Ativação | Loop |
| --- | --- | --- |
| Ten | "whoom" suave | zumbido quase inaudível, alcance curto (pode não existir na v1) |
| Ren | estouro de pressão | zumbido grave mais movimento fino de ar; sobe com o output |
| Zetsu | sucção que fecha | silêncio |

Som abstrato. **Não usar som de eletricidade** se a aura não é elétrica — o
ouvido classifica o efeito antes do olho.

**Decisão do AV3 (#185): a v1 de Ten não tem loop.** A ativação curta já
comunica presença; adicionar agora um zumbido contínuo arriscaria transformar
uma técnica sustentada em ruído permanente sem uma sessão longa que prove o
benefício. O loop só volta por issue e com uma sessão de vinte minutos que
demonstre que ele acrescenta mais do que incomoda. `ten_activate.ogg` é
autoral, sintetizado para este projeto a partir de ruído rosa filtrado e uma
fundamental grave — nenhum sample externo foi usado.

---

## 17. Dano e impacto

Quando Ten absorve dano, o feedback é **local**: um anel curto (120–220 ms) em
volta da região aproximada atingida, com amplitude proporcional ao dano.

**Não piscar a aura inteira.** Piscar tudo comunica "fui atingido em algum
lugar", que é menos informação do que o jogador já tem.

O evento vem de confirmação do servidor. Nunca de um pacote que o cliente
possa forjar, e nunca de dano puramente visual.
