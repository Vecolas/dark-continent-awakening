# ADR-012 — GeckoLib e biblioteca obrigatoria do pipeline de entidades

- **Status:** aceita
- **Data:** 2026-09-12
- **Marco:** EN0 — Baseline de inimigos

## Contexto

O plano Ready to Play dos 23 encontros exige modelos nao humanoides, tres
controllers concorrentes, action ids sincronizados e dezenas de animacoes. O
ADR-003 classificava GeckoLib junto das integracoes opcionais do modpack e
exigia que o Nen Foundation iniciasse com apenas NeoForge.

Manter essa regra produziria duas arquiteturas: entidades simples no JAR
principal e conteudo animado num addon ainda inexistente. O plano aprovado
escolhe uma base de codigo unica enquanto os sistemas compartilhados amadurecem.

## Decisao

1. GeckoLib 4.8.3 para Minecraft 1.21.1/NeoForge e dependencia obrigatoria em
   cliente e servidor do Nen Foundation.
2. A versao fica pinada em `gradle.properties`, registrada no manifesto e no
   version lock. Atualizacao segue o ADR-005, uma dependencia por PR.
3. Codigo de animacao e renderer continua em fronteira client-only. Gameplay,
   dano, fases e selecao de ataque continuam server-side.
4. O perfil dev-minimal passa a conter NeoForge, GeckoLib e Nen Foundation.
5. Esta excecao substitui somente a afirmacao do ADR-003 de que GeckoLib seria
   opcional. Quests, scripts, UI externa, combate e performance continuam
   adapters opcionais.

## Custo assumido

- O JAR deixa de iniciar quando GeckoLib estiver ausente ou fora da faixa
  declarada.
- Uma falha ou abandono da biblioteca passa a exigir migracao de todos os
  modelos animados, nao apenas desligar uma integracao.
- Cliente e servidor carregam mais uma dependencia mesmo em mundos que ainda
  nao encontraram uma criatura animada.
- O build passa a depender do Maven oficial do GeckoLib alem dos repositorios
  ja usados.

## O que NAO muda

- O servidor continua autoridade sobre IA, hitbox, dano, weak point, captura,
  recompensa e estado de Nen.
- Nenhuma animacao ou keyframe decide gameplay.
- Nen Foundation continua sem depender de FTB Quests, KubeJS, LootJS, Jade,
  Epic Fight ou outro mod de Nen.
- Assets continuam autorais e JAR de terceiro nao e versionado no repositorio.
