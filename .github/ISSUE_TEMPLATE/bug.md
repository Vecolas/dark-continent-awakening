---
name: Bug
about: Algo se comporta diferente do esperado
title: ""
labels: bug
---

## Prioridade

<!-- Marque uma. Ver docs/processo/board-e-issues.md. -->

- [ ] **P0** — crash, corrupção de save, duplicação, exploit de autoridade.
      Bloqueia qualquer gate seguinte.
- [ ] **P1** — técnica/habilidade principal não funciona; desync reproduzível.
      Corrigir antes de fechar o marco.
- [ ] **P2** — balanceamento, FX errado, UX ruim sem travar gameplay.
- [ ] **P3** — cosmético, melhoria, ideia.

## Onde reproduz

<!-- O ambiente importa: os modos de falha são diferentes em cada um.
     Ver docs/modpack/perfis-de-execucao.md. -->

- [ ] dev-minimal, `runClient`
- [ ] dev-minimal, `runServer`
- [ ] dev-pack
- [ ] servidor dedicado, 1 jogador
- [ ] servidor dedicado, 2+ jogadores

**Reproduz em dev-minimal?** sim | não | não testei

<!-- Se NÃO reproduz em dev-minimal, isso já é informação valiosa: é
     interação com outro mod, e a investigação muda de forma. -->

## Passos

1.
2.
3.

## O que aconteceu

## O que se esperava

## Sintoma

<!-- O sintoma separa os modos de falha, e conferir qual é vem ANTES de
     investigar qualquer outra coisa. Diagnóstico errado consome sessões
     inteiras confirmando a hipótese errada. -->

- [ ] Crash, com stack trace
- [ ] Erro explícito no log, sem crash
- [ ] Travou / congelou
- [ ] **Nada no log — o comportamento é só errado**

## Log

<!-- Trecho relevante. Se não há nada no log, escreva "nada no log" — isso é
     um dado, não uma omissão. -->

```
```

## Versões

- Nen Foundation:
- NeoForge:
- Outros mods (se dev-pack):
