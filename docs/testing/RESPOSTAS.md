# O registro de respostas — o que já foi julgado, e o que o invalida

Este arquivo existe por causa de uma frase dita em 2026-09-22:

> *"eu já tenho respondido algumas dessas perguntas várias vezes seguidas [...] é
> perda de tempo responder a mesma coisa sem que nada tenha mudado"*

Está certo, e o custo era real: `particulas 0` foi perguntada **três vezes**
(AV1, AV2, AV3); movimento, duas; as três regressões, duas.

## Por que não bastava eu prometer lembrar

Carregar um veredito adiante é exatamente como se fabrica **falso verde**: a
resposta continua escrita, o código muda por baixo, e ninguém percebe que o
julgamento passou a descrever outro jogo. A diferença entre "reaproveitar" e
"laundering" é **saber o que invalida cada resposta** — e isso não pode morar na
memória de ninguém.

Então cada linha declara os arquivos de que aquela resposta depende, e
[`RespostasAindaValidasTest`](../../src/test/java/com/darkcontinent/nenfoundation/docs/RespostasAindaValidasTest.java)
**reprova o build** quando um deles muda. A resposta não expira por tempo; ela
expira por causa.

> **Reprovou? Não atualize o digest.** O digest é a consequência, não a causa.
> Reprovar significa *vá olhar de novo* — a régua está fazendo o trabalho dela.
> Recarimbar sem olhar é converter o portão em carimbo, que é o que ele existe
> para impedir.

## O que este registro NÃO cobre

**Ele lê arquivo, e julgamento visual não mora em arquivo.** Um `resource pack`,
um driver de vídeo, uma GPU diferente, uma mudança no próprio Minecraft — nada
disso move um digest, e qualquer um deles pode invalidar um veredito. Os
invalidadores são os que estão **ao nosso alcance**, e é só isso que se promete.

Declarado também em [`o-que-nao-provamos.md`](o-que-nao-provamos.md).

---

## O que o `ren-nao-fica-preso` cobre, exatamente

A observação foi de **uma** das quatro coisas: *"o anel não fica preso"*. As
outras três entram por **construção**, e isso é dito em voz alta porque
construção não é medição:

- **Colunas e detritos** não guardam estado. Os três — anel, colunas, detritos —
  são desenhados **por quadro** a partir do `AuraVisualState` vivo, atrás da
  mesma guarda (`!estado.enabled() || fases().pressao() <= 0`). Não existe
  objeto com vida própria para vazar: o anel ter sumido **é** o estado ter
  sumido, e os outros dois saem pela mesma porta.
- **O loop de áudio** é o único com vida real, e o `ZumbidoDeRen` documenta os
  quatro caminhos: desligamento, morte, logout e troca de dimensão — três pela
  poda por presença no tick, o logout por `AudioDeAura.limpar`.

**O erro nº 3 do `CLAUDE.md`** — limpeza espalhada pelos pontos de saída — se
aplica a coisas que *seguram* estado. Estas não seguram, e é por isso que a
resposta de uma delas vale para as três.

**O que fica sem prova:** ninguém *ouviu* o loop parar. A conta está no código e
é convincente; o ouvido é do AV4.

---

## Procedência: nem toda resposta vem de uma sessão de gate

A coluna **perguntada em** distingue duas origens, e a diferença não é
burocracia:

- **`AV0`–`AV3`** — respondida numa **sessão de gate**: servidor dedicado, modo
  de captura, hora e clima travados, montagem declarada e conferida contra o
  log.
- **`playtest`** — respondida **jogando**, fora de sessão montada.

Uma resposta de playtest **vale**, e é registrada como qualquer outra — mas ela
foi dada sob condições que ninguém anotou. Se alguma vez uma delas discordar do
que a sessão de gate vir, **a sessão ganha**, e é por isso que a origem fica
escrita em vez de todas parecerem iguais.

---

## As respostas

Colunas: **id** · **veredito** · **onde foi perguntada** · **o que a invalida** ·
**digest**.

| id | veredito | perguntada em | invalidadores | digest |
| --- | --- | --- | --- | --- |
| `particulas-zero` | PASSA | AV1 · AV2 · AV3-Z1 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/ribbon;src/main/java/com/darkcontinent/nenfoundation/client/vfx/model;src/main/java/com/darkcontinent/nenfoundation/client/vfx/EmissorDeParticulasDeAura.java;src/main/resources/assets/nenfoundation/nen_vfx/ten.json` | `040b63becc64` |
| `movimento-acompanha` | PASSA | AV2-M1..M4 · AV3-M1 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/model;src/main/java/com/darkcontinent/nenfoundation/client/vfx/ribbon/AuraAnchor.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java` | `c6dd5afa7013` |
| `slim-e-default` | PASSA | AV2-C1 · AV3-M3 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/model/AuraModelAdapter.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/model/HumanoidAuraAdapter.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/model/AuraPlayerModel.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/ribbon/AuraAnchor.java` | `09fa0ecc6578` |
| `sete-ambientes` | PASSA | AV1 · AV3-A | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/shader;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraRenderTypes.java;src/main/resources/assets/nenfoundation/nen_vfx/ten.json` | `63d54ccd9bdf` |
| `distancia-sem-degrau` | PASSA | AV3-D1 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraRenderLod.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraLodEfetivo.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/model/AuraGeometryLadder.java` | `329265814076` |
| `distancia-40b-comunica` | PASSA | AV3-D2 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraRenderLod.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraLodEfetivo.java;src/main/resources/assets/nenfoundation/nen_vfx/ten.json` | `35626c316b30` |
| `primeira-pessoa` | PASSA | AV3-P1..P3 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPrimeiraPessoa.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java` | `612d60ef06fc` |
| `armadura-completa` | PASSA | AV3-M2 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/model/AuraPerfilVisual.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java;src/main/resources/assets/nenfoundation/nen_vfx/ten.json` | `c10f248adc20` |
| `ren-primeira-pessoa` | PASSA | playtest · R9 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPrimeiraPessoa.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java;src/main/resources/assets/nenfoundation/nen_vfx/ren.json` | `eb556194b812` |
| `ren-transicao-overshoot` | PASSA | playtest · R10 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraTransicao.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraTransitionProfile.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraTransitionSample.java;src/main/resources/assets/nenfoundation/nen_vfx/ren.json` | `73b5d5fc9674` |
| `ren-mesma-linguagem` | PASSA | playtest · R11 | `src/main/resources/assets/nenfoundation/nen_vfx/ren.json;src/main/resources/assets/nenfoundation/nen_vfx/ten.json;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java` | `d11cd2aaa66d` |
| `ren-skin-legivel` | PASSA | playtest · R12 | `src/main/resources/assets/nenfoundation/nen_vfx/ren.json;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/model/AuraGeometryLadder.java` | `6b4c540e1b24` |
| `ren-nao-fica-preso` | PASSA | playtest · R13 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraGroundRenderer.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/ZumbidoDeRen.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AudioDeAura.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraVisualSystem.java` | `0c327a4bd2af` |
| `ren-mundo-intacto` | PASSA | playtest · R2 · R3 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/SondagemDeChao.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraGroundRenderer.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/EmissorDeParticulasDeAura.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java` | `501fca6e960e` |
| `ren-impulso-so-do-local` | PASSA | construção · R7 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/ImpulsoDeCamera.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AudioDeAura.java` | `de028574e77b` |
| `grama-oclui-o-halo` | PASSA | AV2-E2 · AV3-E2 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraBloomRenderer.java` | `530f46a51bca` |
| `ren-nao-acopla` | PASSA | AV2-E2 · AV3-E2 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraBloomRenderer.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java` | `578f8fe6b2da` |
| `ripple-acende` | PASSA | AV2-E2 · AV3-E2 | `src/main/java/com/darkcontinent/nenfoundation/client/vfx/DetectorDeImpacto.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraImpactState.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/AuraDistribution.java;src/main/java/com/darkcontinent/nenfoundation/client/vfx/ImpactosDeAura.java` | `335bb0a13b58` |

---

## Por que carregar o que veio do AV2 foi legítimo

Entre o AV2 (`280696b`) e hoje, o caminho de VFX mudou em **quatro arquivos**:
`SobreposicaoDeVfx`, `AuraDebugCommands`, `AuraDebugRenderer` e
`AuraPostProcess` — todos da instrumentação de `/nenvfx bloom`.

Geometria, filamentos, âncoras, camada de render, composite e perfis ficaram
**intocados**. Nenhum invalidador das linhas vindas do AV2 aparece nessa lista, e
é por isso que o digest de hoje é igual ao daquele dia. **Não é confiança: é a
verificação que coube.**
