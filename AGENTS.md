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

Estado e próximo marco: consultar [`docs/processo/marcos.md`](docs/processo/marcos.md).
Não inferir conclusão só por classes existentes ou por build verde.

---

## Por onde começar

O trabalho está recortado em **issues do GitHub**, com milestones M1..M8.

**Histórico de divisão do M1** (não é uma fila de trabalho atual):

| # | Issue | Owner |
| --- | --- | --- |
| [#2](../../issues/2) | Ligar `PersistentNenData` ao ciclo de vida do jogador | Dev A |
| [#3](../../issues/3) | `RuntimeNenState` e o scheduler central de tick | Dev A |
| [#4](../../issues/4) | Payloads S2C e handshake de versão do protocolo | Dev A |
| [#5](../../issues/5) | Payloads C2S com validação e rate limit — **P0** | Dev A |
| [#6](../../issues/6) | Cache somente-leitura no cliente e overlay de debug | Dev B |
| [#7](../../issues/7) | Comandos de debug permissionados | Dev B |
| [#8](../../issues/8) | Fixtures de save e regressão de persistência | Dev B |
| [#9](../../issues/9) | Matriz de QA em servidor dedicado, dois jogadores | conjunto |

Ordem sugerida dentro do M1: **#2 → #4 → #5** na lane A (cada uma destrava a
seguinte), e **#8 → #6 → #7** na lane B. A #9 fecha o marco e depende de todas.

Os marcos **M2 a M8** existem como issues guarda-chuva ([#10](../../issues/10)
a [#16](../../issues/16)), com lanes e gate escritos. Elas viram issues finas
quando o marco anterior fechar — **e não antes**. Recortar agora significa
decidir coisas que o marco anterior ainda vai revelar.

Consulte issues abertas e PRs antes de reservar trabalho; os agentes podem
estar em clones diferentes. Nesta máquina a execução usa
`C:\dev\dark-continent-awakening`. A árvore antiga no OneDrive foi preservada;
não a sincronize por sobrescrita nem encerre processos Java de outra frente.
Java 21 está instalado, mas `PATH` pode apontar para 16: confira `JAVA_HOME`.

---

## Ordem de leitura obrigatória

Para implementações, testes, revisão e Git, leia também a
[política de skills compartilhadas](docs/processo/skills-do-projeto.md) e as
skills aplicáveis nela indicadas. Elas estão habilitadas por escopo para Codex
e Claude; exemplos das skills não revogam ADRs nem contratos congelados.

1. **[`disciplina-de-engenharia/SKILL.md`](disciplina-de-engenharia/SKILL.md)**
   — as regras gerais de engenharia, versionadas **dentro do repositório** para
   que você não dependa de acesso a `~/.claude/skills/`. Quando as duas cópias
   discordarem, **a do repositório ganha**; ver
   [`LEIA-ME.md`](disciplina-de-engenharia/LEIA-ME.md).
   Leia também os quatro arquivos em `disciplina-de-engenharia/references/`.
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
./gradlew build          # compila + testes + portões. Confira a contagem real.
./gradlew runServer      # servidor dedicado. Obrigatório se o PR toca o núcleo.
./gradlew runClient      # cliente
./gradlew runGameTestServer # confira o resumo, não só exit 0
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

O modelo de Aura tem governança adicional no
[`ADR-009`](docs/adr/ADR-009-modelo-de-aura-sem-stamina-de-nen.md): mudanças
exigem registro e concordância explícita dos dois desenvolvedores.
O agente não pode substituir essas aprovações.

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
