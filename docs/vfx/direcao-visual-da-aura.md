# Direção visual da aura

O que Ten, Ren e Zetsu precisam **parecer**, e o que reprova. Como se constrói
está em [`arquitetura-do-render-de-aura.md`](arquitetura-do-render-de-aura.md);
os números estão em [`perfis-visuais.md`](perfis-visuais.md).

---

## 1. O princípio

> **A silhueta do corpo é a base do efeito.**

A aura pertence ao personagem. Ela nasce na superfície dele, acompanha os
membros dele, e some quando ele suprime. Tudo que flutua solto no ar é
acabamento — e acabamento se desliga sem que a identidade desapareça.

Consequência direta, e é o critério de aprovação do AV3:

> **Com todas as partículas desligadas, Ten ainda tem de ser claramente
> perceptível.**

---

## 2. Os três estados

### TEN — a película consciente

Referências [A](../aura-art/referencia-a-ten-base.png) (protótipo) e
[B](../aura-art/referencia-b-ten-em-camadas.png) (**alvo final**).

- Película energética fina e **aderida** ao corpo: cabeça, tronco, braços,
  pernas.
- Borda clara (branco-azulada) mais forte que o miolo — é o Fresnel que dá o
  contorno.
- Poucos filamentos, contornando braço e tronco, com fluxo predominantemente
  vertical.
- Extensão pequena acima da cabeça. Nenhuma nuvem, nenhum halo grande.
- Parece **viva mesmo parada**: ruído lento e pulsação quase imperceptível.
- Zero efeito no chão. Zero tremor de câmera. Zero detrito.

A leitura pretendida é **"existe energia sendo mantida conscientemente"** — e
não "o personagem está carregando um ultimate".

### REN — a mesma energia, sob pressão

Referência [C](../aura-art/referencia-c-ren-pressao.png).

Ren **não é um segundo efeito**. É Ten com os mesmos elementos amplificados:

| Elemento | Ten | Ren |
| --- | --- | --- |
| shell | fina | mais densa, borda quase branca |
| filamentos | 6–12, curtos | 14–28, longos |
| colunas verticais | não existem | 4–8, subindo até ~2 blocos |
| fluxo | lento | rápido |
| pulsação | quase imperceptível | perceptível |
| brilho | discreto | forte |
| chão | nada | anel irregular de pressão |
| detritos | nada | poucos fragmentos subindo alguns centímetros |

**A skin tem de continuar legível.** Se Ren esconde o personagem, Ren reprovou.

Os fragmentos de C são **puramente cosméticos**: não quebram bloco, não viram
`ItemEntity`, não têm colisão e não alteram o mundo. Isso não é uma limitação
técnica aceita a contragosto — é requisito, porque VFX que altera o mundo é VFX
que precisa de autoridade de servidor.

### ZETSU — a ausência é a informação

Referência [D](../aura-art/referencia-d-zetsu.png).

Depois da transição:

```
shell     = 0
ribbons   = 0
particles = 0
bloom     = 0
pressure  = 0
```

**Nenhum contorno secreto.** A tentação de deixar 2% de brilho "para o jogador
saber que Zetsu está ligado" derrota o propósito da técnica — e num servidor
com dois clientes, entrega o jogador que está se escondendo.

O feedback de que Zetsu foi ativado é a **transição**: os filamentos retraem, a
shell perde intensidade, a borda fecha no corpo, e então nada. Para o jogador
local, um brilho de meio segundo no momento do input é aceitável; para os
observadores, não existe nada em momento nenhum.

---

## 3. Hierarquia de leitura

Nesta ordem, sempre:

```
1. o personagem
2. a borda da aura
3. os filamentos
4. a pressão no chão
5. as faíscas
```

Faísca nunca compete com o personagem. Se, olhando uma captura, a primeira
coisa que se vê é partícula, a hierarquia inverteu.

E a hierarquia de **brilho**, dentro da aura:

```
ribbon core   ← mais brilhante (quase branco)
borda da shell
shell
halo externo  ← mais fraco
```

Sem isso, tudo satura junto e a aura vira um borrão branco.

---

## 4. Cor

A paleta é **branco-azulada**, com o branco dominando e o azul aparecendo
principalmente na borda externa. Não saturar o azul.

| Camada | Aproximação |
| --- | --- |
| miolo | `0.65, 0.78, 1.00` |
| borda | `0.82, 0.92, 1.00` |
| núcleo quente / Ren | quase branco |

**Cor não é categoria.** Enhancer não é vermelho, Emitter não é amarelo. Cor é
perfil visual, personagem ou Hatsu — e quando houver cor por técnica, ela sai
da mesma `AparenciaDeTecnica` que o HUD e a roda de Nen já usam. Duas fontes
para "de que cor é Ren" garantem que um dia as duas discordem.

---

## 5. O que a aura NÃO é

Lista curta, e cada item existe porque é uma falha provável.

| Não pode parecer | Sintoma de que virou isso |
| --- | --- |
| **fumaça** | blobs macios grandes, deriva lenta, alpha arrastado, raio grande de névoa |
| **fogo** | laranja/amarelo, sprites com ponta de chama, cintilação idêntica ao `fire` |
| **eletricidade** | zigue-zague de raio em vez de curva fluida |
| **poção** | nuvem de pontinhos coloridos em volta do corpo |
| **`Glowing`** | contorno chapado que atravessa parede |
| **armadura holográfica** | uma camada translúcida só, uniforme, sem ruído |
| **esfera** | o efeito não acompanha o membro; ele envolve o jogador |
| **shader genérico de outline** | a borda existe mas nada nasce na superfície |

Nen genérico **não é eletricidade**. Quando existir uma Hatsu elétrica
(Transmutation), ela ganha uma camada de arcos **por cima** da aura base — e
não substitui a aura base.

---

## 6. Critérios de aprovação

Marcados na captura, e não na memória. O roteiro de captura está em
[`../testing/av-aura-visual.md`](../testing/av-aura-visual.md).

### TEN aprovado se

- [ ] a shell acompanha cabeça, tronco, braços e pernas;
- [ ] a borda é clara e mais forte que o miolo;
- [ ] os braços ficam **visualmente separados** do tronco;
- [ ] os filamentos nascem na superfície, e não no ar;
- [ ] nada parece fumaça;
- [ ] não há halo grande;
- [ ] não parece armadura;
- [ ] funciona correndo, agachado e nadando;
- [ ] funciona com armadura vestida;
- [ ] é legível de dia e não estoura à noite;
- [ ] **continua reconhecível com as partículas em zero.**

### REN aprovado se

- [ ] é claramente mais intenso que Ten;
- [ ] usa a mesma linguagem visual de Ten;
- [ ] tem correntes verticais;
- [ ] tem pressão no chão;
- [ ] tem detritos pequenos, e nenhum bloco quebrado;
- [ ] a skin continua legível;
- [ ] não parece raio elétrico genérico;
- [ ] não parece transformação de outro anime;
- [ ] não bloqueia a visão em primeira pessoa;
- [ ] não destrói o FPS em multiplayer.

### ZETSU aprovado se

- [ ] a aura some por completo;
- [ ] não resta contorno;
- [ ] não resta bloom;
- [ ] não resta partícula;
- [ ] a transição é suave, entre 200 e 500 ms;
- [ ] **os observadores veem ausência**, e não um efeito discreto;
- [ ] o jogador local recebe feedback do input, e só do input.

---

## 7. Testes de distância e de ambiente

Uma aura pode estar certa a dois blocos e errada a quarenta.

| Distância | O que se espera de Ten |
| --- | --- |
| 2 blocos | detalhe: ruído, filamentos, fluxo |
| 5 | shell e filamentos |
| 10 | silhueta clara |
| 20 | ainda reconhecível |
| 40 | só a borda |

Se Ten some a 5 blocos, a borda está fraca. Se Ten parece neon a 40, está forte
demais.

| Ambiente | Falha típica |
| --- | --- |
| dia claro | a aura some no sol |
| noite | a aura estoura, queimando a vista |
| caverna | vira um borrão branco fullbright |
| Nether | a cor deixa de ser legível contra o vermelho |
| neve | a borda branca some contra o branco — precisa do componente azul |
| água | ordenação de transparência quebra |
| chuva | conflito visual com as partículas de chuva |

---

## 8. Repartição de esforço visual

Serve para decidir o que ajustar quando algo está errado. Se Ten precisa de
cem partículas para ficar bom, o problema está na shell.

| | shell + borda | filamentos | chão | faíscas |
| --- | --- | --- | --- | --- |
| **Ten** | 70% | 25% | — | 5% |
| **Ren** | 45% | 35% | 15% | 5% |
| **Zetsu** | 0% | 0% | 0% | 0% |

---

## 9. O que a arquitetura já precisa permitir (e que ainda não se implementa)

Está aqui para que ninguém desenhe o renderer só para Ten e Ren.

| Técnica futura | O que ela exige do renderer | Estado |
| --- | --- | --- |
| **Gyo** | intensidade maior numa região, menor nas outras | multiplicador por `AuraBodyRegion` existe desde o AV1 |
| **Ko** | uma região em ~4.0, resto em ~0 | idem |
| **Ryu** | a distribuição **muda em tempo real** durante o combate | idem, mais interpolação por região |
| **Ken** | shell mais densa e estável, menos colunas que Ren | perfil novo, sem código novo |
| **Shu** | a aura segue um item empunhado | `AuraItemRenderLayer`, reusando shell e ribbon |
| **En** | volume esférico em torno do jogador | renderer **separado**; a shell corporal não serve |
| **In** | a aura some para observadores específicos | resolvedor de visibilidade por observador (AV6) |
| **Hatsu** | camada extra por cima da aura base | `AuraVisualModifier`, e nunca editar o renderer central |

A distribuição por região é autoritativa no servidor
([ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md)). O renderer **lê**, e
a `AuraDistribution` de `client/vfx/` deixa de ter construtor próprio quando o
delta passar a carregar o campo.
