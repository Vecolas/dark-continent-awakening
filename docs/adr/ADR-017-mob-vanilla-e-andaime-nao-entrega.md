# ADR-017 — Mob vanilla e andaime, nao entrega

**Status:** aceito
**Data:** 2026-09-12

## Contexto

Os cinco inimigos que existem hoje renderizam com geometria e textura VANILLA
emprestadas: Foxbear veste urso-polar, Great Stamp veste hoglin, Frog-In-Waiting
veste sapo, Man-faced Ape veste aldeao e piglin, Spider Eagle veste phantom.

Isso foi deliberado e continua certo como etapa: emprestar silhueta deixou o
comportamento nascer primeiro, e comportamento e o que os portoes conseguem
medir. Spawn, IA, pathfinding, territorio, ataque telegrafado, agarrao,
disfarce, ninho e multiplayer foram todos provados com corpo emprestado.

O risco nao e tecnico, e de MEMORIA. Um mob emprestado spawna, anda, ataca, tem
loot, tem traducao e passa em 112 gametests. Ele parece pronto por todos os
sinais que o repositorio sabe ler. A unica coisa que denuncia que ele nao esta
pronto e alguem abrir o jogo e reconhecer um hoglin -- e isso nenhum portao ve.
Uma regra que so vive numa conversa nao sobrevive a duas semanas e tres
agentes; ela vira folclore, e o folclore perde para o build verde.

## Decisao

**Mob vanilla e ANDAIME. Ele sustenta a obra e sai antes da entrega.**

1. Emprestar modelo, textura, som ou IA de entidade vanilla e permitido
   enquanto o inimigo estiver em construcao.
2. Todo emprestimo se DECLARA no codigo, com o marcador literal
   `PLACEHOLDER`, dizendo o que foi emprestado e o que vai substitui-lo.
3. Nenhum inimigo principal conta como entregue enquanto depender da identidade
   visual ou comportamental de uma entidade vanilla. A ficha completa esta na
   diretriz de mobs customizados: modelo proprio, skeleton proprio, animacoes
   proprias, renderer proprio, textura propria, dimensoes e hitbox proprias,
   sons proprios, IA propria.
4. Reutilizar INFRAESTRUTURA vanilla continua certo e nao conta como
   emprestimo de identidade: `PathNavigation`, `MoveControl`, `LookControl`,
   `GoalSelector`, atributos, sistema de dano, gravidade, rede, pathfinding.
5. Herdar de uma entidade vanilla CONCRETA (`extends PolarBear`) fica proibido
   no estado final. A base e `BaseHxHMob`, e o que vem de vanilla vem de classe
   generica (`Animal`, `PathfinderMob`), nunca de uma especie.
6. O portao `PlaceholderDeclaradoTest` varre a FONTE e morde dos dois lados:
   renderer que empresta geometria vanilla sem se declarar reprova, e renderer
   declarado na lista de divida que parou de emprestar tambem reprova -- senao
   a lista cobre em silencio o dia em que o emprestimo acabar.

## Custo assumido

O trabalho de arte deixa de ser opcional e entra na linha critica: cinco mobs
que hoje funcionam em jogo passam a contar como NAO entregues, e a lista de
divida nasce com cinco linhas. Cada migracao custa modelo, skeleton, animacoes,
textura, renderer, hitbox e sons -- e nenhuma delas tem portao que prove
qualidade visual, entao o custo e alto e a verificacao final continua sendo
humana. O portao novo tambem cobra manutencao: quem migrar um mob precisa tirar
a linha da lista no mesmo PR, e quem criar um mob emprestado precisa se
declarar. Ha ainda um custo de honestidade que vai doer: enquanto a migracao
nao acontecer, `marcos.md` e qualquer relato de entrega tem de dizer "com corpo
emprestado", mesmo quando o comportamento estiver completo e medido.

## O que NAO muda

O comportamento ja entregue continua valendo -- carga, testa, emboscada,
agarrao, disfarce, bando, ninho e a coleira que poupa quem recua nao dependem
de quem empresta o corpo, e os 112 gametests que os provam seguem verdes depois
da troca de modelo. A pratica de emprestar durante o desenvolvimento continua
aprovada, e explicitamente: ninguem precisa esperar arte para escrever IA.
`BaseHxHMob`, `EnemyBrain`, `AttackTimeline`, `ChargeRules`, `GrabRules`,
`AmbushRules`, `DisguiseRules`, `WeakPointResolver` e `NestGuardRules` sao
reuso correto e nao sao afetados. O ADR-007 (nenhum asset extraido da obra)
continua valendo por cima deste: arte propria quer dizer AUTORAL, nao
"recortada de outro lugar". E o ADR-012 (GeckoLib obrigatorio) e o que torna
esta decisao executavel -- o pipeline de modelo animado ja esta pinado.
