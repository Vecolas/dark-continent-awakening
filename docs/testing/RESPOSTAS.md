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
