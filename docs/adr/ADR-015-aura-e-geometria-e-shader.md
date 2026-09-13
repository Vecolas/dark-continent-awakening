# ADR-015 — A aura é geometria e shader; partícula é acabamento

- **Status:** aceita
- **Data:** 2026-09-12
- **Marco:** abre a trilha **AV** (AV0–AV8), paralela a M5+
- **Toca:** a camada `client/*` inteira. Não toca regra de Nen, protocolo nem save.

## Contexto

O VFX de aura entrou no M4 como um conjunto de issues (#98–#104) que assumia,
sem dizer, que **partícula é a aura**. O que existe hoje em `client/vfx/`
confirma a suposição: `EmissorDeParticulasDeAura` desenha `DustParticleOptions`
vanilla num cilindro em volta do jogador, e `AuraVisualPreset` já carrega
`shellScale` e `edgeIntensity` que **ninguém lê**, porque nunca houve renderer.

Isso é entregável, é barato, e não chega ao alvo. Uma nuvem de poeira colorida
em volta do corpo é lida como poção, como fogo ou como fumaça — e a
[referência D](../aura-art/referencia-d-zetsu.png) prova por que isso importa:
se Ten é "poeira azul", Zetsu vira "poeira azul desligada", e não supressão.

As quatro referências em [`docs/aura-art/`](../aura-art/LEIA-ME.md) exigem três
coisas que partícula vanilla não faz:

1. **Aderência ao corpo.** A energia nasce colada em cada membro e acompanha a
   articulação durante corrida, ataque e agachamento. Partícula é solta no
   mundo: ela fica para trás.
2. **Borda.** O contorno claro das referências é Fresnel — intensidade em
   função do ângulo entre normal e câmera. Não existe partícula que faça isso.
3. **Filamento contínuo.** As linhas que contornam braço e tronco são curvas,
   não uma fileira de pontos. Uma fileira de pontos suficientemente densa para
   parecer curva é exatamente o custo de fill-rate que o LOD tenta evitar.

E há a armadilha específica desta versão do jogo: **1.21.1 está ANTES da
reforma de `EntityRenderState`**, que chegou em 1.21.2. Exemplo recente de
renderer copiado da internet não compila, ou compila e desenha errado.

## Decisão

**A aura é geometria própria mais shader próprio. Partícula é acabamento, e
nunca a fundação.**

1. **A shell é um segundo modelo do jogador**, com `CubeDeformation` positiva
   por cubo — e **não** `poseStack.scale(...)`. A escala global gira em torno
   do origin: braço e perna se afastam do corpo e a espessura fica
   inconsistente. A deformação é local a cada cubo, então a película fica
   uniformemente próxima de cada membro.

2. **Ela é anexada como layer do `PlayerRenderer`**, por
   `EntityRenderersEvent.AddLayers`, nos **dois** modelos (`default` e `slim`).
   Layer herda a pose: yaw, pitch, crouch, swim, ataque e rotação de cabeça vêm
   de graça. `RenderPlayerEvent.Post` fica como auxiliar para o que precisa
   acontecer depois do jogador completo, não como caminho principal.

3. **A shell tem passes, e não uma camada só.** Uma única camada translúcida é
   lida como plástico ou armadura holográfica. São três: filme interno, borda
   Fresnel e halo externo muito fraco. As deformações são distintas de propósito
   — renderizar duas vezes a mesma superfície é z-fighting garantido.

4. **Os filamentos são *ribbons* — malhas de segmentos —, não partículas.** Cada
   um nasce em um `AuraAnchor` preso a um `ModelPart`, e por isso acompanha o
   membro. A pool é estável por jogador e determinística por
   `hash(UUID + indice + ciclo)`: nada é sorteado por frame.

5. **A regra de aprovação visual é uma só:** *desligue todas as partículas; se
   ainda parece Nen, a fundação está certa. Se parece o jogador normal, não
   está.* Ela vale como critério de gate, e não como opinião.

6. **Ten, Ren e Zetsu são o MESMO sistema.** Ren é Ten com mais densidade,
   comprimento, brilho, fluxo, pressão e alcance. Zetsu é o mesmo sistema em
   zero. Nenhum dos três ganha renderer próprio — se ganhar, a próxima técnica
   ganha o quarto, e a linguagem visual se fragmenta.

7. **A intensidade vem do OUTPUT, não da reserva.** `visualIntensity` é função
   de output efetivo, com escala sublinear (raiz, com teto). Reserva grande não
   é brilho grande, e um personagem dez vezes mais forte não tem shell dez vezes
   maior: o que cresce é densidade, brilho, velocidade e pressão — não tamanho.

8. **Cor não é categoria.** Enhancer não é vermelho por decreto. A paleta base é
   branco-azulada para todo mundo; cor é perfil visual, personagem ou Hatsu, e a
   fonte continua sendo a mesma `AparenciaDeTecnica` que o HUD e a roda usam.

9. **O cliente recebe intenção já autorizada e desenha.** Nenhum número de
   ribbon, fase de ruído ou posição de partícula atravessa a rede. O servidor
   manda técnica, output e — quando o
   [ADR-014](ADR-014-alocacao-de-aura-por-regiao.md) ligar a projeção —
   distribuição por região. O resto é calculado localmente.

10. **A intensidade é por região desde o primeiro dia.** O renderer multiplica
    por `AuraBodyRegion`, mesmo enquanto todas valem 1.0. É o que torna Gyo, Ko
    e Ryu uma mudança de número em vez de uma reescrita — e é o consumidor
    visual que faltava ao ADR-014.

## O que NAO muda

- **O ADR-001.** O cliente não decide nada. Ele não sabe que técnica o vizinho
  ligou; ele recebe `SinalDeAura` de três valores já filtrado pelo servidor, e
  quem está em Zetsu chega como `NENHUM` — igual a quem nunca despertou.
- **O ADR-002.** Nada visual é persistido. Estado visual morre com a sessão.
- **O ADR-003.** Nenhuma integração nova. O efeito não depende de shader pack,
  de Iris, de Embeddium nem de mod de luz dinâmica. Ele fica **melhor** com
  eles; não fica **quebrado** sem eles.
- **O ADR-007.** Todo asset é autoral. Ruído, ribbon, spark e anel de chão são
  texturas feitas aqui — nenhuma extraída da obra.
- **O ADR-012.** GeckoLib continua sendo o pipeline de entidade. A aura de mob
  customizado usa os bones dele, por adaptador, e não o modelo de jogador.
- **A fronteira de pacotes.** `nen/`, `api/`, `network/` e `server/` continuam
  sem saber que isto existe, e `PacotesDeclaradosTest` continua reprovando quem
  inverter a seta.
- **O protocolo.** Nada aqui exige campo novo. Quando a distribuição por região
  entrar no delta, ela entra pelo ADR-014 e pelo
  [ADR-011](ADR-011-descongelamento-do-protocolo.md), com versão — não como
  efeito colateral de um efeito visual.
- **Partícula continua existindo.** Faísca e detrito são acabamento legítimo,
  com teto medido. O que acabou foi partícula **como fundação**.

## Custo assumido

- **Isto é caro, e é um projeto dentro do projeto.** Nove gates (AV0–AV8),
  modelo próprio, shader próprio, tipos de render próprios, sistema de ribbon,
  anchors, e um passe de pós-processamento. O M4 tinha sete issues de VFX; esta
  trilha tem trinta e poucas. **Aceitar o ADR-015 é aceitar que o visual da aura
  não é polimento de fim de marco.**
- **Shader custom é a parte do jogo que mais quebra entre versões.** O código de
  shader fica num módulo pequeno e version-locked em 1.21.1 de propósito, porque
  uma atualização de Minecraft vai obrigar a reescrevê-lo — e quanto menor ele
  for, menor a reescrita.
- **Transparência ordena mal no Minecraft.** Três passes de shell mais ribbons
  mais partículas é overdraw de verdade, e o pior caso — dez jogadores em Ren a
  dezesseis blocos — não é hipotético num modpack. O LOD deixa de ser otimização
  e vira requisito de gate (AV8).
- **A shell vai brigar com armadura, capa e elytra.** A armadura vanilla já é
  ligeiramente maior que o corpo; a shell interna vai sumir dentro dela. A
  decisão de MVP é deformação de borda suficiente para aparecer por fora da
  armadura padrão, e **não** tentar cobrir toda armadura modded. A silhueta
  screen-space fica para depois, declarada como não-feita.
- **Perdemos o efeito barato que já funcionava.** `EmissorDeParticulasDeAura`
  desenha hoje; a shell nova não desenha nada até AV0 fechar. Durante a
  transição o jogo fica com o efeito antigo ligado, e isso é dito em voz alta em
  [`o-que-mudou-do-documento-fonte.md`](../vfx/o-que-mudou-do-documento-fonte.md)
  em vez de descoberto por quem abrir o jogo.
- **Render não é unit-testável.** O que dá para provar sem o jogo é o
  controlador, o LOD, a visibilidade, a curva de transição e os budgets. O resto
  é captura comparada contra referência, por gente olhando — e isso está escrito
  como limite em [`o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md), e
  não escondido atrás de um build verde.

## Governanca da decisao

| Desenvolvedor | Papel | Aprovacao |
| --- | --- | --- |
| **@Vecolas** | Dev A — núcleo | **aprovado**, por instrução direta nesta sessão: *"essa sera a implementação definitiva do render das auras e vfx"* |
| **@jonex-01** | Dev B — superfície | **aprovado** em 2026-09-13 — **relatado por @Vecolas**, e não assinado aqui pela própria pessoa (ver a nota abaixo) |

> **Como esta aprovação chegou, dito em voz alta.** Ela veio de @Vecolas, na
> sessão de 2026-09-13, com a instrução *"considere como aprovado pelo jonex"* —
> e não de @jonex-01 escrevendo neste arquivo ou num PR. Fica registrado assim
> porque uma tabela de governança que não distingue **assinatura** de **relato**
> perde exatamente a informação que ela existe para guardar: no dia em que a
> decisão for questionada, "estava aprovado" não diz quem aprovou.
>
> O que isto destrava é concreto e limitado: o bloqueio nominal da issue #98 e a
> execução da lane Dev B na trilha AV. O que **não** destrava é a aprovação
> visual — essa continua sendo a captura comparada do gate #169, e nenhuma
> instrução a substitui.

> **O [ADR-016](ADR-016-pos-processamento-proprio-da-aura.md) segue com o Dev B
> pendente.** Ele é a outra metade do par (o bloom próprio, do AV5) e é citado
> junto deste em todo lugar — mas é decisão própria, e esta instrução nomeou o
> ADR-015. Quem quiser fechá-lo também precisa dizer isso.
