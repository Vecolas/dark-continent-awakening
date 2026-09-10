# ADR-002 — Dado persistente e estado de runtime sao coisas separadas

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

Um jogador de Nen carrega duas coisas de natureza muito diferente:

- **Progresso**: despertou, categoria, tecnicas conhecidas, proficiencia,
  marcos. Muda raramente. Perde-lo e perder horas de jogo.
- **Estado de combate**: aura atual, tecnicas ativas, cooldowns, canalizacao,
  exaustao. Muda a cada tick. Perde-lo custa alguns segundos.

Guardar os dois no mesmo lugar erra nas duas direcoes ao mesmo tempo: ou se
grava em disco a cada tick, ou se perde progresso num crash.

## Decisao

Dois objetos, dois destinos.

| | `PersistentNenData` | `RuntimeNenState` |
| --- | --- | --- |
| Onde | Data Attachment serializado | estrutura de runtime server-side |
| Sobrevive a logout | sim | nao |
| Sobrevive a morte | sim (`copyOnDeath`) | nao; politica configuravel no respawn |
| Sobrevive a restart | sim | nao |
| Frequencia de escrita | operacoes explicitas | a cada tick, em memoria |
| Versionado | sim, `schemaVersion` desde a v1 | nao precisa |

Regras que decorrem disso:

1. Aura atual **nunca** entra no attachment persistido.
2. Todo dado persistido carrega `schemaVersion` e passa pelo
   `NenProfileMigrator` antes de ser usado.
3. O attachment nao sincroniza sozinho com o cliente. Toda sincronizacao e
   explicita. [NF-3]
4. Narrativa e estado de arco **nao** entram no `PersistentNenData`. Cada arco
   guarda o proprio estado.

## Custo assumido

- **Duas estruturas para uma ideia.** "O Nen do jogador" vira dois objetos, e
  todo mundo tem de saber em qual deles procurar. O `NenContext` existe em parte
  para esconder isso de quem escreve tecnica.
- **Reconexao perde estado de combate.** Quem cai no meio de uma luta volta sem
  as tecnicas ligadas. Aceito: a alternativa e persistir estado de combate, com
  todos os problemas de gravar por tick.
- **A politica de morte fica em dois lugares** — `copyOnDeath` no attachment e a
  regra de respawn no servico de runtime. E a divisao mais facil de esquecer, e
  por isso ela esta na matriz de QA do M1.

## O que NAO muda

- Um dia pode ser desejavel persistir UMA parte do runtime — cooldown longo de
  PvP, por exemplo, para que morrer de proposito nao limpe recarga. Isso nao
  contradiz esta decisao: seria um campo explicito no persistente, com nome
  proprio e migracao, e nao o runtime inteiro virando persistente.
- A separacao nao implica dois attachments. Runtime nao e attachment.

## Fontes

- [NF-3] https://docs.neoforged.net/docs/1.21.1/datastorage/attachments/
