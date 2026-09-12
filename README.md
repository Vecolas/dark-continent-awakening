# Dark Continent Awakening

Um modpack de Hunter × Hunter para Minecraft 1.21.1, construído em volta de uma
fundação de Nen **autoral**: o **Nen Foundation** (`nenfoundation`).

> **Estado: M1 em andamento.** O M0 fechou: contratos congelados, portões,
> CI. O M1 já entregou persistência de perfil, runtime e scheduler, protocolo
> S2C, cache de cliente, comandos de debug e o primeiro gametest. Falta a
> validação C2S e a QA com dois jogadores.

---

## O que é

Duas coisas com o mesmo nome de projeto:

| | O quê |
| --- | --- |
| **Nen Foundation** | mod NeoForge 1.21.1 — aura, categorias, técnicas, framework de habilidades |
| **Dark Continent Awakening** | o modpack que se monta em volta dele |

A decisão que organiza tudo:

> **O Nen Foundation é a única autoridade sobre Nen.** Mods externos entram com
> quests, loot, spawn, documentação, animação e performance. Nenhum deles
> controla aura, categoria, técnica, cooldown ou dano.

O núcleo é desenhado em camadas desacopladas justamente para que trocar a
composição do pack não destrua o resto — e para que o mod sobreviva a qualquer
mod de terceiro parar de ser atualizado.

---

## Começando

### 1. Java 21

O NeoForge 1.21.1 exige **Java 21**. Não 17, não 22.

Na máquina onde o projeto nasceu isso **já está instalado**: Temurin
21.0.12, e `JAVA_HOME` aponta para ele. Confira antes de instalar nada:

```bash
./gradlew --version   # a linha "Launcher JVM" tem de dizer 21.x
```

**Cuidado com uma pegadinha real desta máquina:** `java -version` responde
`16.0.2`, porque um JDK 16 antigo continua primeiro no `PATH`. Isso **não
quebra o build** — o `gradlew` usa `JAVA_HOME`, não o `PATH`. Não saia
consertando o `PATH` achando que é problema.

Se você está numa máquina nova, baixe o
[Temurin 21](https://adoptium.net/temurin/releases/?version=21) (Windows x64,
`.msi`) e instale — o instalador define `JAVA_HOME` sozinho.

Se tiver outra versão instalada e não quiser trocar a padrão do sistema,
aponte só o Gradle:

```bash
# na raiz do repositorio, num arquivo LOCAL que o git ignora
echo "org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" >> gradle.local.properties
```

### 2. Build

```bash
./gradlew build
```

Isso compila, roda a suíte unitária e os portões, e gera
`build/libs/nenfoundation-0.1.0.jar`.

A saída termina com `Testes executados: N`. **Se ela disser zero, o build
reprova** — suíte vazia não é aprovação. O número não está escrito aqui de
propósito: ele muda a cada entrega, e um número no README envelhece em
silêncio.

### 3. Rodar

```bash
./gradlew runClient          # cliente
./gradlew runServer          # servidor dedicado
./gradlew runGameTestServer  # roda os gametests e sai
./gradlew runData            # datagen
```

Na primeira vez que rodar cada um, aceite a EULA do Minecraft criando
`run/<perfil>/eula.txt` com `eula=true`.

> **Não conclua nada a partir do `BUILD SUCCESSFUL` de uma tarefa `run*`.**
> Já houve duas vezes neste repositório em que o Gradle saiu `0` com o jogo
> morto dentro. Procure a linha certa no log: `Done (` para o servidor,
> `required tests` para o gametest.

> **`runServer` é o único ambiente que pega uma classe client-only alcançada
> pelo núcleo.** Singleplayer roda um servidor interno no mesmo processo do
> cliente, onde as classes de cliente estão todas carregadas. Se o seu PR toca
> o núcleo, rode `runServer`.

---

## Por onde começar a ler

| Se você quer | Leia |
| --- | --- |
| entender as regras do repositório | [`CONVENCOES.md`](CONVENCOES.md) |
| trabalhar aqui (pessoa ou agente) | [`CLAUDE.md`](CLAUDE.md) |
| saber o que já foi decidido, e o que custou | [`docs/adr/index.md`](docs/adr/index.md) |
| saber onde o projeto está | [`docs/processo/marcos.md`](docs/processo/marcos.md) |
| não pisar no trabalho da outra pessoa | [`docs/processo/fronteira-de-arquivos.md`](docs/processo/fronteira-de-arquivos.md) |
| escrever uma técnica | [`docs/api/techniques.md`](docs/api/techniques.md) |
| escrever uma habilidade | [`docs/api/abilities.md`](docs/api/abilities.md) |
| entender o protocolo de rede | [`docs/multiplayer/protocol.md`](docs/multiplayer/protocol.md) |
| **fazer a aura parecer aura** | [`docs/vfx/LEIA-ME.md`](docs/vfx/LEIA-ME.md) |
| **saber o que o verde não prova** | [`docs/testing/o-que-nao-provamos.md`](docs/testing/o-que-nao-provamos.md) |
| as regras gerais de engenharia | [`disciplina-de-engenharia/`](disciplina-de-engenharia/) |

Os documentos-fonte (plano técnico, roadmap, pesquisa de viabilidade) estão em
[`docs/pesquisa/`](docs/pesquisa/).

---

## Duas pessoas

O projeto é feito por duas pessoas em paralelo. Isso não é um detalhe
organizacional — várias decisões de arquitetura existem por causa disso.

- **Áreas de ownership**, não metades do mod:
  [`docs/processo/ownership.md`](docs/processo/ownership.md).
- **Contratos congelados antes** de as duas frentes começarem:
  [`ADR-004`](docs/adr/ADR-004-identidade-congelada.md).
- **Lista escrita de arquivos hostis a merge** — e a regra de nunca usar
  `git add -A` enquanto a outra frente estiver viva na mesma árvore.
- **Ninguém commita na `main`.** Não há proteção de branch — o risco foi
  aceito conscientemente ([ADR-008](docs/adr/ADR-008-licenca-e-protecao-de-branch.md)),
  e um push direto **não produz aviso nenhum**. A regra continua valendo; ela
  só não tem mecanismo por trás.

---

## Marcos

```
M0 Bootstrap  ✅
 └─> M1 Persistência + sync   (em andamento)
      └─> M2 Aura Engine + HUD
           └─> M3 Despertar + categoria
                └─> M4 Ten / Ren / Zetsu / Gyo
                     └─> M5 Framework de habilidades
                          └─> M6 Progressão + modpack
                               └─> M7 Hardening
                                    └─> M8 Vertical slice + RC
```

O MVP termina com uma fatia jogável de ponta a ponta: despertar → categoria →
aura → quatro técnicas → habilidade → combate → quest → logout/morte/dimensão →
servidor dedicado com dois jogadores.

Arcos completos (Exame Hunter, Torre Celestial, Yorknew, Greed Island) são
módulos posteriores. Tentar todos na primeira versão é a forma mais provável de
abandonar o projeto — e isso está escrito na pesquisa de viabilidade que
originou este repositório.

---

## Stack

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.250 |
| Java | 21 |
| Gradle | 9.2.1 (wrapper incluso) |
| Testes | JUnit 5 |

Nada sobe sozinho. Uma dependência por vez, com changelog e matriz de teste —
[`ADR-005`](docs/adr/ADR-005-versoes-pinadas.md) e
[`version-lock.md`](docs/modpack/version-lock.md).

---

## Aviso legal

Projeto de fã. Sem vínculo com a Shueisha, com o autor de *Hunter × Hunter* ou
com qualquer detentor de direitos da obra.

Nenhum asset é extraído da obra: textura, modelo, VFX, som e interface são
autorais. Nenhum JAR de terceiro entra neste repositório. Ver
[`ADR-007`](docs/adr/ADR-007-assets-autorais.md) e
[`docs/legal/propriedade-intelectual.md`](docs/legal/propriedade-intelectual.md).

A licença do código é **All Rights Reserved**, por escolha e não por omissão
([ADR-008](docs/adr/ADR-008-licenca-e-protecao-de-branch.md)).
