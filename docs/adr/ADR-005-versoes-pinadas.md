# ADR-005 — Versoes pinadas e uma dependencia por vez

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

A pesquisa de viabilidade lista atualizacao como um dos riscos serios: um update
do loader, de uma biblioteca de animacao ou de worldgen pode alterar registries
e quebrar personagem e mundo.

Um modpack e mais fragil que um mod sozinho. A combinacao testada e uma
propriedade do CONJUNTO, e ela nao sobrevive a uma atualizacao que ninguem pediu.

## Decisao

1. Toda versao fica pinada em `gradle.properties`, e nenhuma sobe sozinha.
2. Uma dependencia por vez, com changelog e matriz de smoke test.
3. As versoes testadas do pack ficam em `docs/modpack/version-lock.md`.
4. Bug do nucleo se reproduz primeiro no perfil **dev-minimal** — NeoForge mais
   Nen Foundation e nada mais. Depurar dentro do pack completo confunde causa
   com interacao.

Versoes de partida, verificadas em 2026-09-10:

| O que | Versao | Fonte |
| --- | --- | --- |
| Minecraft | 1.21.1 | — |
| NeoForge | 21.1.250 | maven.neoforged.net (metadata) |
| Java | 21 | exigencia do NeoForge 1.21.1 [NF-1] |
| Gradle | 9.2.1 | wrapper do MDK oficial |
| Parchment | 1.21.1 / 2024.11.17 | MDK oficial |

## Custo assumido

- **Ficar para tras de proposito.** Correcao de bug em dependencia demora a
  chegar. Aceito.
- **Trabalho manual a cada atualizacao.** Nao ha bot de dependencia neste
  projeto; subir versao e uma tarefa com issue, teste e registro.
- **Divergencia entre ambientes de desenvolvimento.** Sem pin de JDK por
  ferramenta, cada maquina usa o Java que tiver. O toolchain do Gradle cobre a
  compilacao; ele nao cobre o que a IDE faz.

## O que NAO muda

- Correcao de seguranca em dependencia nao espera o ciclo. Ela sobe sozinha, com
  a mesma matriz de teste, mas sem esperar a proxima janela.
- Esta decisao nao congela o Minecraft em 1.21.1 para sempre. Ela diz que a
  troca e um projeto com issue, nao um efeito colateral.

## Fontes

- [NF-1] https://docs.neoforged.net/docs/1.21.1/gettingstarted/
