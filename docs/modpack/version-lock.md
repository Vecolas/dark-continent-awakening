# Version lock

A combinacao testada e uma propriedade do **conjunto**, nao de cada peca. Ela
nao sobrevive a uma atualizacao que ninguem pediu.

Nada nesta tabela sobe sozinho. Ver
[ADR-005](../adr/ADR-005-versoes-pinadas.md).

---

## Base (verificada em 2026-09-10)

| O que | Versao | Onde vive | Fonte |
| --- | --- | --- | --- |
| Minecraft | 1.21.1 | `gradle.properties` | — |
| NeoForge | 21.1.250 | `gradle.properties` | [maven metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml) |
| Java | 21 | toolchain do Gradle | [NeoForged Docs](https://docs.neoforged.net/docs/1.21.1/gettingstarted/) |
| Gradle | 9.2.1 | `gradle/wrapper/` | MDK oficial |
| NeoGradle | 7.1.38 | `build.gradle` | MDK oficial |
| Parchment | 1.21.1 / 2024.11.17 | `gradle.properties` | MDK oficial |
| JUnit | 5.11.4 | `gradle.properties` | — |
| Nen Foundation | 0.1.0 | `gradle.properties` | este repositorio |

## Mods do pack

| Mod | Versao travada | Data do teste | Quem testou |
| --- | --- | --- | --- |
| — | — | — | nenhum mod de terceiro no pack ainda |

---

## Como atualizar uma dependencia

Uma por vez. Sempre.

1. Abrir issue: `[infra] subir <dependencia> de X para Y`.
2. Branch propria: `infra/<dependencia>-Y`.
3. Ler o changelog da versao nova. Registrar na issue o que muda.
4. Trocar **so aquela linha** em `gradle.properties`.
5. `./gradlew build` — verde, com contagem de testes maior que zero.
6. Smoke test de [qa-matrix.md](../testing/qa-matrix.md).
7. Se a dependencia toca combate, rede ou save: a secao correspondente da
   matriz de aceitacao inteira.
8. Atualizar esta tabela com versao, data e quem testou.
9. PR com o changelog no corpo.

**Nunca** duas dependencias no mesmo PR. Quando algo quebra com duas, descobrir
qual delas custa mais do que os dois PRs separados teriam custado.

**Excecao:** correcao de seguranca nao espera janela. Mesma matriz de teste,
sem a espera.

---

## Historico

| Data | O que mudou | De | Para | Motivo |
| --- | --- | --- | --- | --- |
| 2026-09-10 | bootstrap do repositorio | — | ver tabela base | M0 |
| 2026-09-10 | NeoForge (o MDK vinha com 21.1.235) | 21.1.235 | 21.1.250 | ultima 1.21.1 publicada no maven oficial no dia do bootstrap |
