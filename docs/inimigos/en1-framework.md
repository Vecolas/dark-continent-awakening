# EN1 — framework de percepção e combate

Matriz de evidências do gate: [en1-gate.md](../testing/en1-gate.md).

Este documento é o contrato executável das partes de EN1 que já existem no
repositório. Os números são injetados pelos consumidores; nenhum valor de
balanceamento é congelado no controlador.

## Percepção (#136)

`PerceptionSnapshot` é uma leitura server-side de um candidato identificado
por UUID. `TargetEvaluator` recusa alvo morto, fora da dimensão, fora do
alcance, fora do território ou incompatível com a relação de facções. Visão só
é verdadeira quando há linha de visão e o produto escalar está dentro do cone.

`PerceptionController` não varre entidades nem blocos. O servidor fornece um
`Supplier<PerceptionSnapshot>` em intervalos do `PerceptionBudget`: scans normais
ficam entre 5 e 10 ticks, e scans caros entre 20 e 40 ticks; o contrato recusa
valores fora dessas faixas. Ruídos
relevantes entram por `hear(UUID)`. A memória só é renovada por visão, audição
ou o evento de som do mesmo UUID e expira sem novo sinal. A saída carrega o id,
não uma entidade, para que o servidor possa reconstruí-la e reconferir o alvo.

O ponto de extensão para aura permanece inerte: nenhuma decisão de percepção
consulta aura ou Gyo antes de existir o contrato correspondente de Nen.

## Stagger (#137)

`StaggerController` é independente de knockback. O hit ou colisão fornece o
impacto medido; `StaggerDefinition` governa resistência, limiar, decay e
duração; `StaggerResult` informa `IGNORED`, `ACCUMULATED` ou `APPLIED`.
Enquanto ativo, novo impacto é ignorado e a instância não é reaplicada. Morte,
unload e troca de dimensão devem chamar `reset()`.

O Great Stamp já usa esse contrato para a colisão durante `ACTIVE`; seu
`EnemyCombatState.STAGGERED` continua sendo uma decisão da entidade a partir do
resultado server-side, e o empurrão continua sendo apenas efeito físico.

## DummyEnemy (#138)

`DummyEnemyEntity` é a primeira entidade de integração: possui perfil e
definição JSON, registro e placement, alvo reconstruído no servidor, ataque com
`AttackController`, hitbox e weak point resolvidos no servidor, stagger separado,
limpeza em morte/removal e os únicos
campos sincronizados que o renderer precisa (`phase` e `actionId`). O
`DummyEnemyGameTest` prova spawn, seleção de alvo, WINDUP/ACTIVE, dano no
servidor, identidade da ação e encerramento. O modelo, os clipes e a textura
vivem em `assets/.../dummy_enemy`; a ficha editorial vive em
`data/nenfoundation/bestiary/dummy_enemy.json`, com traduções em inglês e
português. O cliente nunca decide a ação.

## Debug e arena (#113)

Em modo de desenvolvimento, a raiz permissionada `/nen` expõe a subárvore
`enemy`: `spawn`, `state`, `ai freeze`, `hitboxes`, `weakpoints`, `animation`,
`target` e `arena`. O estado é transitório, indexado pela identidade da
entidade e limpo no unload; não entra em save nem em payload. `target` aceita
somente uma entidade viva na mesma dimensão e a até 16 blocos. A arena cria um
arranjo determinístico dos inimigos registrados para inspeção manual.

## Provas disponíveis

- `PerceptionControllerTest`: cone/LOS, facção, dimensão, distância, memória,
  audição por evento, orçamento, expiração atrás de parede e benchmark
  determinístico de 64 mobs.
- `StaggerControllerTest`: resistência, limiar, decay, duração, repetição,
  entradas inválidas.
- `GreatStampGameTest`: colisão durante a carga e transição para
  `STAGGERED`.
- `DummyEnemyGameTest`: fluxo end-to-end do EN1, weak point, stagger e autoridade
  de dois jogadores no servidor dedicado de GameTests.
- `NenCommandsTest` e `EnemyDebugStateTest`: caminhos de comando e isolamento
  do estado transitório de debug.

Ainda não é prova de fechamento do gate #139: servidor dedicado interativo com
dois jogadores, inspeção visual do dummy em jogo e aprovação dos dois
desenvolvedores continuam exigindo sua própria execução/artefato. Um build verde
não substitui essas evidências.
