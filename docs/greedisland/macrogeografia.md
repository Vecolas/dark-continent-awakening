# Greed Island: macrogeografia projetada

> **Estado: DECIDIDO, NÃO INICIADO.** Este documento congela a direção. Nenhuma
> linha da macrogeografia foi escrita, e a ilha que existe hoje é um **andaime**
> que a contradiz de propósito — ver §8.

---

## 1. A decisão central

> **Greed Island não é uma dimensão com biomas e cidades espalhadas. É uma ilha
> continental projetada pelos Game Masters, onde a geografia faz parte do jogo.**

O cânone estabelece uma ilha de escala regional — Shalnark a compara à República
de Kotoritana, na ordem de Hokkaido. Ela não é um cenário virtual pequeno: existe
fisicamente, a leste de Yorknew.

A consequência para a geração inverte o pipeline comum:

```
NÃO:   seed → ruído → veremos onde as cidades caem
SIM:   mapa projetado → regiões → procedural preenche o interior
```

### As duas camadas

| | Camada | Determinismo |
| --- | --- | --- |
| **MACRO** | formato da ilha, cordilheiras, grandes rios, cidades, estradas, zonas, landmarks | **igual em todo mundo** |
| **MICRO** | morros, rochas, árvores, cavernas, vegetação, riachos, encontros | procedural por seed |

> Masadora precisa continuar sendo Masadora naquele lugar. Um guia da ilha tem
> de fazer sentido em qualquer servidor.

```
GREED_ISLAND_LAYOUT_SEED     fixa no mod, e não no mundo
GREED_ISLAND_LAYOUT_VERSION  1
```

---

## 2. Escala

Equivalência literal daria ~289.000 × 289.000 blocos. Possível, e péssimo de
jogar.

**Alvo: bounding box de ~80.000 × 70.000 blocos**, com compressão geográfica
declarada:

```
arquitetura:      1 bloco ≈ 1 metro
geografia macro:  1 bloco ≈ 3–4 metros abstratos
```

### A escala não é estética — ela alimenta a mecânica

Se Masadora fica a 600 blocos de Antokiba, **por que gastar uma carta?** A
distância é o que dá valor a transporte, informação, mapas e atalhos — e
Masadora é canonicamente onde se compram Spell Cards.

| Salto | Distância |
| --- | --- |
| Shiso Tree → Antokiba | 1.200–2.500 |
| hub próximo | 3.000–7.000 |
| hub regional | 8.000–15.000 |
| outro extremo da ilha | 30.000–60.000+ |

---

## 3. A progressão é espacial

```
SHISO TREE → tutorial ambiental → trilha → encontros → ANTOKIBA
                                                          ↓
                                                  primeiras cartas
                                                          ↓
                                                  rotas intermediárias
                                                          ↓
                                                      MASADORA
                                                          ↓
                                                     Spell Cards
                                                          ↓
                                              a ilha realmente se abre
```

O momento que esta geografia existe para produzir:

> *"Eu achei que Antokiba era a região. Na verdade eu vi 2% da ilha."*

Dificuldade **não** é radial simples. O jogador pode entrar cedo numa área
perigosíssima se quiser — é Hunter × Hunter.

---

## 4. As cidades

**Posições são design do mod, não coordenadas canônicas.** Mapas que circulam
online derivam do *Battle Collection*, e não de cartografia do mangá — não há
coordenadas oficiais a respeitar, e isso é liberdade, não licença.

| Cidade | Função na geração | Tamanho alvo |
| --- | --- | --- |
| **Shiso Tree** | spawn e tutorial | — |
| **Antokiba** | primeiro grande hub · City of Prizes | 300–500 |
| **Rubicuta** | hub intermediário/comercial | 250–450 |
| **Masadora** | Spell Cards e progressão | 500–800 |
| **Aiai** | social/eventos · City of Love | 400–700 |
| **Dorias** | cassino, risco e recompensa | 500–900 |
| **Soufrabi** | costa, farol, piratas de Razor | 700–1200 |
| **Limeiro** | capital, endgame, castelo | 1000–1600+ |

**Não são vilas do Minecraft.** Cada uma precisa de identidade arquitetônica
inequívoca, e não precisa estar cheia de prédios: praças, parques, bairros,
muralhas e campos fazem parte.

Tecnicamente: `CityAnchor → CityLayout → distritos → estradas → jigsaw`. Uma NBT
monstruosa por cidade é o que isso evita.

---

## 5. A ilha é principalmente natureza

```
~5–10%   urbanizado / landmarks densos
~20–30%  wilderness com conteúdo significativo
~60–70%  natureza relativamente aberta
```

A sensação de distância **é parte do mundo**. Um grande landmark a cada
1.000–2.500 blocos; menores, mais frequentes.

### Região ≠ bioma

| | |
| --- | --- |
| **BIOME** | como o ambiente parece |
| **REGION** | que papel aquele lugar exerce no jogo |

Assim uma espécie se liga a uma região sem exigir um bioma só para ela.

Biomas próprios, e não patches vanilla: `GI Meadowlands`, `GI Old Forest`,
`GI Highland`, `GI Rocky Badlands`, `GI Lake Country`, `GI Coastal Cliffs`,
`GI Wetlands`, `GI Mountain Belt`, `GI Deep Forest`, `GI Southern Coast`. Eles
reutilizam blocos vanilla; o que muda é composição, relevo, densidade, paleta,
estruturas e criaturas.

**20–30 macro-regiões**, cada uma com clima visual, altura, criaturas, recursos,
landmarks, dificuldade e cartas relacionadas.

### Criaturas pertencem à geografia

Não `spawnWeight = 20, biome = forest`. Cada uma tem **habitat**: o Hyper
Puffball numa faixa aberta específica, o Radio Rat numa rota, o Bubble Horse no
seu, o Cyclops numa área de encontro. O que isso produz no jogador:

> *"Preciso dessa carta; sei onde esse bicho vive."*

---

## 6. O pipeline

A ordem importa. **O relevo precisa saber que uma cidade vai existir ali.**

```
 1. island mask          ← NÃO é distanceFromCenter < radius
 2. continental elevation
 3. mountain ridges
 4. valleys
 5. hydrology
 6. biome/region assignment
 7. coastline refinement
 8. road graph
 9. city anchors
10. landmarks
11. vegetation
12. local terrain noise
13. encounters
```

### A costa

```
forma-base desenhada + SDF + ruído de baixa frequência + erosão
```

Resultado obrigatório: penínsulas, enseadas, falésias, praias, baías, ilhotas,
promontórios.

### Montanhas e rios são estrutura, não ruído

Duas ou três cadeias principais determinam nascentes, rios, estradas,
dificuldade, fauna e clima local. **5–8 sistemas hidrográficos** principais:
`nascente → afluentes → rio principal → lago → mar`.

O objetivo é uma ilha **memorizável**: *"Masadora fica além do vale"*, *"para
Soufrabi eu sigo a costa"*.

---

## 7. Arquitetura de código proposta

```
greedisland/worldgen/
  GreedIslandLayout          ← fonte central. Nenhum gerador inventa o próprio mapa
  GreedIslandLayoutVersion
  GreedIslandChunkGenerator
  GreedIslandMask · ElevationField · RegionMap
  MountainGraph · RiverGraph · RoadGraph
  CityRegistry · CityAnchor · LandmarkRegistry
  TerrainGenerator · VegetationGenerator · EncounterGenerator
```

**Performance:** 80.000 blocos não significa gerar 80.000² ao iniciar.
Guarda-se só o macro layout, splines, anchors e metadata — alguns MB. O chunk
pergunta: *que região? que altura? passa rio? passa estrada? há landmark?*

---

## 8. ⚠️ O que existe hoje CONTRADIZ este documento

Em 2026-09-26 Greed Island saiu do `minecraft:flat` (123 camadas de pedra até o
horizonte) para um gerador de ruído com máscara radial.

**A máscara radial é literalmente o que a §6 proíbe:**

```java
// FormaDaIlha — o andaime
if (distancia <= raio) return 1.0;
```

Isso é **andaime declarado**, e não a entrega. Existe porque superplano era pior
que um disco, e porque a macrogeografia é trabalho de outra ordem de grandeza.

| | Andaime de hoje | Alvo deste documento |
| --- | --- | --- |
| costa | disco de raio 620 + 150 de transição | desenhada, SDF, erodida |
| extensão | ~1.760 blocos de diâmetro | ~80.000 × 70.000 |
| cidades | nenhuma | 8, em posições fixas |
| montanhas | ruído | cadeias projetadas |
| rios | nenhum | 5–8 sistemas |
| regiões | biomas vanilla por `multi_noise` | 20–30 macro-regiões próprias |
| determinismo macro | não existe | igual em todo servidor |

---

## 9. Os dois gates, e por que são dois

> **Nunca avaliar Greed Island olhando uma cidade ou uma screenshot.**

| Gate | Pergunta |
| --- | --- |
| **MICRO** | "esse lugar parece bom?" |
| **MACRO** | "isso parece uma ilha enorme?" |

É perfeitamente possível ter Masadora linda, Soufrabi linda, florestas lindas —
e Greed Island inteira parecer um parque temático de 5 km².

O gate macro precisa de um **renderizador de debug** mostrando costa, cidades,
estradas, rios, regiões e distâncias. Ele **reprova** se:

- cidades estiverem agrupadas demais;
- mais de uma cidade importante couber no render distance;
- a wilderness for pequena;
- o jogador puder ignorar Spell Cards e correr para tudo;
- os biomas parecerem patches vanilla;
- o mapa parecer procedural sem intenção.

---

## 10. Cartas e mundo são um sistema só

O sistema de cartas não é inventário — ele manipula a geografia:

| Carta | Efeito geográfico |
| --- | --- |
| `Accompany` | jogador/grupo → outro jogador ou local |
| `Magnetic Force` | jogador → jogador alvo |
| `Return` | volta a landmark/cidade apropriada |
| mapa / rastreio | descoberta e localização |

A consequência é o arco: **depois de certo ponto, distância física ≠ tempo de
viagem obrigatório.** Mais cartas, mais landmarks descobertos e mais
conhecimento = mais mobilidade.

```
começo:  o mundo parece enorme
meio:    o jogador começa a entendê-lo
fim:     ele navega Greed Island como quem dominou suas regras
```

Worldgen, Spell Cards e progressão não são três sistemas. São um.
