# Perfis de execucao

Tres ambientes, tres perguntas diferentes. Usar o errado custa horas.

| Perfil | O que tem | Responde |
| --- | --- | --- |
| **dev-minimal** | NeoForge + Nen Foundation | "o nucleo esta certo?" |
| **dev-pack** | pack completo com as integracoes aprovadas | "a combinacao funciona?" |
| **server** | servidor dedicado, config versionada, mundo descartavel | "funciona de verdade?" |

---

## A regra

> **Bug do nucleo se reproduz em dev-minimal ANTES de se olhar o pack.**

Depurar dentro do pack completo confunde causa com interacao. Voce passa a
tarde investigando o seu codigo enquanto o problema esta na ordem de
carregamento de dois mods de terceiro — ou o contrario, e voce culpa um mod
inocente.

Se o bug **nao** reproduz em dev-minimal, isso ja e informacao valiosa: e
interacao, e a investigacao muda de forma.

---

## dev-minimal

E o que o projeto Gradle entrega direto:

```bash
./gradlew runClient      # cliente
./gradlew runServer      # servidor dedicado, --nogui
./gradlew gameTestServer # roda os gametests e sai
./gradlew runData        # datagen
```

Diretorios de trabalho: `run/client/`, `run/server/`, etc. Todos ignorados pelo
git.

> **`runServer` e o unico que pega classe client-only vazada para o nucleo.**
> Singleplayer nao pega: ele roda um servidor interno no mesmo processo do
> cliente, onde as classes de cliente estao todas carregadas.

> **`runData` e `runServer` sao recursos de dono unico.** Um escreve em
> `src/generated/resources`, o outro usa porta fixa. Uma frente por vez. Ver
> [fronteira-de-arquivos.md](../processo/fronteira-de-arquivos.md).

---

## dev-pack

Uma instancia **separada** de launcher (Prism, MultiMC, CurseForge), fora deste
projeto Gradle.

Motivo: o pack precisa de JARs de terceiro, e eles nao entram no repositorio
([ADR-007](../adr/ADR-007-assets-autorais.md)). Colocar o pack dentro do
projeto Gradle obrigaria a versionar ou baixar esses JARs no build.

Montagem:

1. Instancia NeoForge 1.21.1 com as versoes de
   [version-lock.md](version-lock.md).
2. Mods de [supported-mods.md](supported-mods.md), nas versoes travadas.
3. `./gradlew build` e copiar `build/libs/nenfoundation-<versao>.jar` para
   `mods/`.

**Existe a partir do M6.** Antes disso nao ha integracao para testar.

---

## server

Servidor dedicado de verdade, numa maquina que nao e a de quem desenvolve, com
duas pessoas conectadas.

E o unico ambiente que responde as perguntas que mais importam neste projeto:

- desync entre dois clientes;
- estado de um jogador vazando para o cache do outro;
- classe client-only carregada onde nao devia;
- comportamento sob latencia real.

Mundo **descartavel**, recriado a cada rodada de QA — exceto os mundos de
regressao de save, que sao guardados de proposito. Ver
[save-migrations.md](../testing/save-migrations.md).

---

## O que cada perfil NAO prova

| Perfil | Nao prova |
| --- | --- |
| dev-minimal | nada sobre interacao com outros mods |
| dev-pack | nada sobre latencia nem sobre dois jogadores |
| server local (mesma maquina) | nada sobre latencia real |
| singleplayer | **nada sobre isolamento client/server** |

Ver [o-que-nao-provamos.md](../testing/o-que-nao-provamos.md).
