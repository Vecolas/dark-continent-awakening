# AGENTS.md

Instruções para agentes de codificação (Codex CLI, Claude Code, e afins) que
trabalham neste repositório.

> **Este arquivo é um índice, não uma segunda fonte de verdade.** As regras
> completas estão em [`CLAUDE.md`](CLAUDE.md) e [`CONVENCOES.md`](CONVENCOES.md).
> Duplicá-las aqui criaria duas verdades sobre o mesmo assunto, e a que vale
> costuma ser a errada. **Leia os dois antes de escrever qualquer linha.**

---

## O projeto em cinco linhas

**Dark Continent Awakening** — modpack de Hunter × Hunter para Minecraft
1.21.1, construído em volta de um mod autoral: o **Nen Foundation**
(`mod_id: nenfoundation`, package `com.darkcontinent.nenfoundation`).

NeoForge 21.1.250, Java 21, Gradle 9.2.1, JUnit 5. **Duas pessoas trabalham em
paralelo** — várias decisões existem por causa disso.

Estado: **M0 concluído** (bootstrap, contratos congelados, portões, CI).
Próximo: **M1**.

---

## Ordem de leitura obrigatória

1. **A skill `disciplina-de-engenharia`**
   (`~/.claude/skills/disciplina-de-engenharia/SKILL.md`) — as regras gerais de
   engenharia. Não estão duplicadas em nenhum lugar deste repositório.
   Se você não tem acesso a ela, diga isso explicitamente no seu relato em vez
   de improvisar as regras.
2. [`CLAUDE.md`](CLAUDE.md) — princípios, arquitetura, contratos congelados,
   Definition of Done, e a lista de erros que este projeto já sabe que vai
   cometer.
3. [`CONVENCOES.md`](CONVENCOES.md) — branch, commit, PR, portões, e o que é
   específico daqui.
4. [`docs/processo/marcos.md`](docs/processo/marcos.md) — onde o projeto está.
5. [`docs/processo/fronteira-de-arquivos.md`](docs/processo/fronteira-de-arquivos.md)
   — **leia antes de tocar em qualquer arquivo**, para saber quais são hostis a
   merge.
6. [`docs/testing/o-que-nao-provamos.md`](docs/testing/o-que-nao-provamos.md) —
   o que o verde **não** cobre.

Os documentos-fonte originais (plano técnico, roadmap, pesquisa de
viabilidade) estão em [`docs/pesquisa/`](docs/pesquisa/).

---

## Comandos

```bash
./gradlew build          # compila + 21 testes + portões. Verde antes de PR.
./gradlew runServer      # servidor dedicado. Obrigatório se o PR toca o núcleo.
./gradlew runClient      # cliente
./gradlew gameTestServer # gametests
./gradlew runData        # datagen
```

Requer **Java 21**. Se `java -version` disser outra coisa, o build falha na
configuração.

O build **reprova** se nenhum teste executar. Suíte vazia não é aprovação.

---

## As dez regras que mais custam quando ignoradas

1. **O servidor decide.** Nenhum payload C2S carrega aura, dano, cooldown,
   unlock ou multiplicador — nem "temporariamente para testar".
2. **Alvo chega como id**, nunca como entidade. O servidor reconstrói e
   reconfere distância, dimensão e linha de visão.
3. **Não guarde valor derivado.** Pergunte na hora de usar. Multiplicador
   congelado na ativação ignora tudo que vier depois, em silêncio.
4. **A implementação de técnica/habilidade é singleton.** Estado de jogador
   nela é estado global disfarçado.
5. **Quem liga, desliga**, e o par mora em `onDeactivate`/`stop` — nunca
   espalhado pelos pontos de saída.
6. **Config só ganha chave quando o consumidor existe**, e o número sai do
   código quando entra na config.
7. **Nunca `git add -A`.** Stage arquivo por arquivo, e confira com
   `git diff --cached --name-only` antes de commitar. Há outra frente viva na
   mesma árvore.
8. **Nunca commite na `main`.** Não há proteção de branch (exige plano pago do
   GitHub em repo privado); depende de disciplina.
9. **Um marco por vez.** Não comece o próximo sem instrução explícita.
10. **Sua entrega declara o que NÃO foi verificado.** Isso é parte do trabalho,
    não confissão de fracasso.

---

## Contratos congelados — não mexa sem ADR

`mod_id`, package, namespace, ids das 7 categorias (com `UNDETERMINED` no
ordinal 0), `PersistentNenData` v1, ids e direções de payload, `NenTechnique`,
`NenAbility`.

Ver [`ADR-004`](docs/adr/ADR-004-identidade-congelada.md). Há portões que
reprovam a mudança.

---

## Portões que existem

| Portão | Impede |
| --- | --- |
| `NenCategoryTest` | renomear/reordenar categoria; tradução faltando ou órfã |
| `PersistentNenDataTest` | perder campo no codec; default não-neutro |
| `NenProfileMigratorTest` | aceitar save de versão futura/inválida |
| `ProtocoloCongeladoTest` | código e documento discordarem sobre payload |
| `IndiceDeAdrTest` | ADR fora do índice; ADR sem custo declarado |
| `PacotesDeclaradosTest` | pacote sem documentação; núcleo importando cliente |

Ao criar um portão novo: **alimente-o com um caso que deve reprovar e confirme
que ele reprova.** Régua que nunca reprova é carimbo.

---

## Ao terminar uma tarefa, relate

- arquivos alterados;
- decisões tomadas e por quê;
- o que foi executado de verdade (`build`? `runServer`? com quantos jogadores?);
- **o que ficou sem prova**;
- bloqueios, com nome — "depende da pessoa X decidir Y", não "depende de
  aprovação".
