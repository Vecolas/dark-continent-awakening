# AV8 — orçamento medido e release do visual (#209, #206, #207)

**Este gate não é de imagem. É de número medido e arquivado.**

> Aplicar otimização antes de ter a medição arquivada é otimizar o terceiro item
> da ordem de custos e relatar vitória.

---

## Os quatro cenários (#206)

Perfil de `spark` **antes e depois**, arquivado em [`../perfis/`](../perfis/)
com data, commit, GPU, resolução e nível de bloom no nome.

1. **10 jogadores em Ren** dentro de 16 blocos
2. **20 jogadores em Ten** dentro de 32 blocos
3. **nenhuma aura visível** — custo **zero**, e não *custo pequeno*
4. **10 minutos de Ren contínuo**, medindo memória

> **OS DEZ MINUTOS NAO CABEM NA RESERVA BASE, e a saida e o config do
> servidor de QA.** Com `aura.maximaBase = 100`, sustentar Ren por 10 min
> exigiria custo 1,17/s -- abaixo de Ten. A escada do
> [ADR-018](../../adr/ADR-018-escada-de-custo-em-segundos.md) da a Ren **29
> segundos**, e isso e o desenho, nao um defeito.
>
> Suba `aura.maximaBase` no config do servidor do **mundo de regressao**. Config
> e por servidor; o balanceamento distribuido nao muda.
>
> **Isso e legitimo porque a reserva nao toca o visual** -- `perfis-visuais.md`
> §7: a intensidade vem do *output efetivo*. Um Ren sustentado com reserva 5.000
> e visualmente IDENTICO ao de reserva 100, e a captura continua mostrando o que
> o jogador vera.
>
> **O que NAO vale:** forcar com `/nenvfx state ren`. Isso acende `OVERRIDE
> ATIVO` no overlay, e a captura deixa de valer como aprovacao.

E a medição do **servidor dedicado**: tick time com 10 jogadores em Ren
comparado a 10 parados.

> Se o tick time com 10 em Ren for distinguível de 10 parados, **algo de VFX
> vazou para o servidor** — e isso é **P0**, não tuning.

---

## O alvo é RELATIVO, e nunca absoluto

A aura não passa de ~**20% do tempo de frame alvo** — a 60 fps, ~3,3 ms de
16,7 ms. Um número absoluto de GPU seria verdade numa máquina só e folclore em
todas as outras.

### A ordem dos custos, e por que ela importa

```
1. overdraw de transparencia   (tres passes de shell + ribbons + particulas)
2. blur do bloom
3. vertices de ribbon
4. passes multiplos de shell
5. varios jogadores proximos
```

**Medir primeiro, e nessa ordem.**

---

## A matriz de renderização (#207)

Quatro ambientes, cada um nos três níveis de `vfx.bloom`, com `F3+T` e dez
redimensionamentos em cada:

- renderer vanilla, sem shader pack — **a referência**
- Embeddium (ou equivalente Sodium)
- Iris/Oculus **sem** pack carregado
- Iris/Oculus **com** pack carregado, em 2 ou 3 packs reais do parque

A tabela vive em [`../compatibility.md`](../compatibility.md).

> **Ela nasce vazia e PRECISA crescer.** Tabela vazia depois do gate significa
> que ninguém procurou — não que está tudo bem. Se um ambiente realmente não
> teve achado, isso se escreve como linha, com nome e data.

---

## O que já está no código, e o que falta

| Item | Estado |
| --- | --- |
| réguas no overlay F6 (ribbons, colunas, anéis, detritos com o teto, partículas, zumbidos, tamanho do alvo, passe pulado, alvos criados/liberados) | **existe** |
| detecção de pipeline substituído → `FAST`, uma linha no log | **existe** |
| auditoria de alocação por quadro (memo de perfil, `BlockPos` mutável, sem record por jogador) | **existe** |
| tetos de segurança fora da config | **existe** |
| **qualquer número medido** | **não feito** — e é isto que o gate é |

`spark` já está em
[`supported-mods.md`](../../modpack/supported-mods.md) (GPL-3.0-only, M2).
