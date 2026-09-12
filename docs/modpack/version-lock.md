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

Ferramenta isolada de QA M2 (não adicionada ao pack nem ao Gradle): spark
`1.10.124-neoforge-1.21.1`, arquivo `spark-1.10.124-neoforge.jar`, testado em
2026-09-11. Licença GPL-3.0-only;
[versão publicada](https://modrinth.com/mod/spark/version/v5qtqRQi).
SHA-512 conferido antes de executar:
`f86ce34f2759c69df82578c397ff55b666c84626229a98f598458b960c21b38c95d6bfef4772af7f963c4f4868e5e2d9aef6b99c1d51bab55bf45e0e6e6b5ed4`.
JAR não versionado. Perfil medido em [perfis](../testing/perfis/README.md).

| Mod | Versao travada | Data do teste | Quem testou |
| --- | --- | --- | --- |
| GeckoLib | 4.8.3 para NeoForge/Minecraft 1.21.1 | 2026-09-12 | Codex (clone limpo: 298 testes; `runData`, 68 GameTests e client smoke verdes; `runServer` chegou a `Done` com spark de QA presente) |

Artefato resolvido do Maven oficial:
`geckolib-neoforge-1.21.1-4.8.3.jar`. SHA-512:
`4b8f3bcdc04450aa97af136353341f136921259f16f2363f3cfe77956860455f27c38f9701b3f0e8a78ea86067411ecd35d391803b753276934057481e770556`.
O JAR nao e versionado.

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
