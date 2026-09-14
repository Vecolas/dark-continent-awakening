# Gate EN1 — evidências da Enemy Framework

Este arquivo acompanha as issues #113, #138 e o gate #139. Ele separa prova
automatizada de prova manual; nenhum item manual é marcado como concluído por
inferência a partir de um build verde.

| Requisito | Evidência local | Estado |
| --- | --- | --- |
| Build com testes reais | `./gradlew build`: `Testes executados: 659` | aprovado |
| Datagen | `./gradlew runData`: `BUILD SUCCESSFUL` | aprovado |
| Dummy registra, nasce, percebe, ataca, causa dano e morre | `DummyEnemyGameTest` e `EnemyDefinitionGameTest` | aprovado em GameTest |
| Fases, hitbox, weak point e stagger | `DummyEnemyGameTest`, `HunterExamProfilesTest`, `GreatStampGameTest` e `DummyEnemyAnimationContractTest` | aprovado em código/GameTest |
| Ação sincronizada sem estado autoritativo no cliente | campos sincronizados limitados a `phase` e `actionId`; cenário com dois mock players | aprovado em GameTest; rede física pendente |
| Cliente carrega renderer e assets | `CoerenciaDeGeckoLibTest`, `ModeloProprioTest` e `runClient` | carregamento aprovado; inspeção visual pendente |
| Servidor dedicado | `runServer` chegou a `Done` sem classe client-only | aprovado |
| Canário common/client | `PacotesDeclaradosTest.dominioNaoDependeDeCliente` | aprovado |
| Canário de asset/JSON | `CoerenciaDeGeckoLibTest`, `DummyEnemyAnimationContractTest`, codecs de definition | aprovado |
| Orçamento de percepção e ausência de packet por tick | `PerceptionBudget`, `PerceptionControllerTest` e esta documentação | aprovado em código; medição de rede física pendente |
| Debug e arena | `/nen enemy spawn`, `state`, `ai freeze`, `hitboxes`, `weakpoints`, `animation`, `target` e `arena`; `NenCommandsTest` | aprovado na árvore e no estado transitório |
| Cenários mínimos de #113 | melee do Dummy, charge e weak point/stagger do Dummy/Great Stamp, pack da manada e multiplayer no lote de GameTests | aprovado em GameTest |

## O que ainda depende de execução humana

- entrar com dois clientes reais no servidor dedicado e conferir que ambos veem
  o mesmo `actionId`;
- observar o Dummy no mundo durante locomotion, WINDUP, ACTIVE, RECOVERY,
  stagger e morte;
- registrar aprovação dos dois desenvolvedores antes de congelar a API.

## O que pode mudar antes do vertical slice

- `AttackTimeline`: durações e parâmetros de ataques nos perfis/definitions;
- `WeakPoint`: regiões, geometria e multiplicadores de conteúdo;
- `EnemyBrain`: tuning de awareness, memória e transições de conteúdo.

Esses pontos continuam extensíveis até a revisão do Great Stamp. O contrato de
identidade, direção server-side, action/state sincronizados e separação
common/client não deve ser alterado sem ADR.
