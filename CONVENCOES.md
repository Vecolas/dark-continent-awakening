# CONVENÇÕES — como se trabalha neste repositório

As regras gerais de engenharia **não moram aqui**. Elas moram em
[`disciplina-de-engenharia/`](disciplina-de-engenharia/) — cópia versionada da
skill de mesmo nome, e **é a cópia do repositório que vale** —, fonte de
verdade para: portões, falso verde, onde mora um número, nomes e comentários,
ponto cego declarado, duas fontes para a mesma verdade, git, issue como unidade
de trabalho e o que uma entrega precisa declarar.

Por que ela está duplicada aqui, e a regra de desempate:
[`disciplina-de-engenharia/LEIA-ME.md`](disciplina-de-engenharia/LEIA-ME.md).

Este arquivo guarda **só o que é específico do Dark Continent Awakening**.
Regra geral vai para a skill; o que é daqui fica aqui.

Leitura obrigatória antes de implementar:

| Onde | O quê |
| --- | --- |
| `disciplina-de-engenharia/SKILL.md`, seção 14 | ramo, commit, PR |
| `disciplina-de-engenharia/references/processo-e-entrega.md` | issue, bloqueio nomeado, entrega que declara o que não foi verificado, **e o comando que engole o trabalho dos outros** |
| `disciplina-de-engenharia/references/o-verificador-tambem-mente.md` | o falso verde na camada das ferramentas |
| `disciplina-de-engenharia/references/portoes-e-reguas.md` | portão que morde dos dois lados |
| `disciplina-de-engenharia/references/armadilhas-silenciosas.md` | defaults, enums, aritmética que some, ciclo de vida |

---

## O que é específico deste projeto

### 1. Ramo, commit e PR

```bash
git checkout -b <tipo>/<assunto-curto>
```

Tipos em uso: `feat`, `fix`, `tune`, `docs`, `infra`, `teste`, `integration`.

Exemplos reais do plano: `feat/aura-engine`, `feat/hud-aura`,
`fix/death-copy`, `integration/epicfight`.

**Se o PR toca um arquivo hostil a merge, diga no título:**

```
feat: aura pool e regeneracao [toca NenFoundation.java]
```

Isso avisa a outra pessoa que aquele arquivo está travado. A lista completa
está em [`docs/processo/fronteira-de-arquivos.md`](docs/processo/fronteira-de-arquivos.md).

**Squash merge** para PR de funcionalidade. **Tag** para release. Sem branch
`develop` — para duas pessoas, integração frequente na `main` reduz
divergência mais do que uma branch de integração ajuda.

**Proteção de branch não está ativa, e isso é uma decisão, não uma pendência.**
Em repositório privado ela exige plano pago do GitHub, e o risco foi aceito
conscientemente ([ADR-008](docs/adr/ADR-008-licenca-e-protecao-de-branch.md)).

Consequência prática, escrita para não ser esquecida: **um push direto na
`main` não produz nenhum aviso** — não é recusado, não alerta ninguém, e só
aparece quando alguém olha o histórico (`git log --first-parent main`).

Aceitar o risco não é autorizar o push direto. A regra continua valendo; ela só
não tem mecanismo por trás.

### 2. Verde local antes de abrir PR

```bash
./gradlew build          # compila + testes unitários + portões
```

E, se o PR toca o núcleo (domínio, rede, persistência, ciclo de vida):

```bash
./gradlew runServer      # servidor dedicado, no perfil dev-minimal
```

`runServer` não é zelo. É o **único** ambiente que pega classe client-only
alcançada pelo núcleo: singleplayer roda um servidor interno no mesmo processo
do cliente, onde as classes de cliente estão todas carregadas. Uma violação
exclusiva de servidor dedicado passa batida em `runClient`.

O CI é rede de segurança, não primeira verificação.

### 3. Um marco por vez

O `CLAUDE.md` manda: nunca implementar mais de um marco numerado sem instrução
explícita. Trabalho paralelo só entre partes sem dependência entre si, cada uma
na sua branch, com fronteira de arquivo escrita.

E **contrato compartilhado se congela ANTES** de as duas frentes começarem: um
PR minúsculo só com a interface, merge imediato, e aí as duas trabalham em
paralelo. Sem isso, as duas inventam nomes diferentes para a mesma coisa.

### 4. Regras que não se afrouxam, específicas daqui

- **O servidor é a autoridade.** Nenhum payload C2S carrega aura, dano,
  cooldown, unlock ou multiplicador. Nunca. Nem "temporariamente para testar".
  (ADR-001)
- **Nunca confiar em entidade vinda do cliente.** Chega o id; o servidor
  reconstrói no próprio mundo e reconfere distância, dimensão e linha de visão.
- **Aura atual nunca entra no attachment persistido.** (ADR-002)
- **Script (KubeJS) nunca valida aura, dano, cooldown ou segurança de rede.**
  (ADR-003)
- **Quest nunca escreve NBT de Nen.** Ela chama a API, e o núcleo decide.
- **O núcleo funciona sozinho.** Se uma funcionalidade do núcleo passar a
  exigir FTB Quests, KubeJS, Jade ou Epic Fight, o ADR-003 foi violado.
- **`UNDETERMINED` é o ordinal zero de `NenCategory`, e continua sendo.**
  Afirmar a categoria errada é pior que não afirmar nenhuma.
- **Recusa sempre tem motivo, e o motivo nunca revela estado alheio.**
- **Nenhum asset extraído da obra.** (ADR-007)
- **Um mod de Nen no pack. Um.**

### 5. Onde mora um número

`NenConfig` só ganha uma chave **quando o consumidor dela existe**. Fórmulas de
aura chegam no M2, custos de técnica no M4, custo e cooldown de habilidade no
M5.

E o inverso vale igual: número que foi para a config tem de **sair do código**.
Deixado nos dois, o do código é sobrescrito em runtime, nenhum teste acusa
nada, e a próxima pessoa passa uma tarde girando o botão morto.

Número novo nasce medível — com contador em modo dev, ou com um perfil de
`spark` arquivado.

### 6. Duas fontes para a mesma verdade, e os portões que as cruzam

Onde a mesma informação existe em dois lugares neste repositório, há um portão:

| As duas fontes | Portão |
| --- | --- |
| `NenCategory` e os arquivos de idioma | `NenCategoryTest` |
| `NenProtocol.TABELA` e `docs/multiplayer/protocol.md` | `ProtocoloCongeladoTest` |
| `docs/adr/*.md` e `docs/adr/index.md` | `IndiceDeAdrTest` |
| a árvore de pacotes e a documentação de cada um | `PacotesDeclaradosTest` |

Todos varrem a **fonte** (o diretório, o enum), nunca a lista. E todos mordem
dos dois lados: faltando reprova, sobrando também.

Ao criar um portão novo, **alimente-o com um caso que deve reprovar e confirme
que ele reprova.** Régua que nunca reprova é carimbo.

### 7. Dependência nova

Entra em [`docs/modpack/supported-mods.md`](docs/modpack/supported-mods.md) com
pacote, versão, **licença**, motivo e URL. Sem os cinco, a Definition of Done
não fecha.

A licença é lida da página, não citada de memória — vários mods populares são
*All Rights Reserved*, e isso muda o que se pode fazer com o JAR.

**JAR de terceiro não entra no repositório.** O pack se descreve por manifesto.

### 8. Uma versão por vez

Nada em `gradle.properties` sobe sozinho. Uma dependência por PR, com changelog
no corpo e registro em
[`docs/modpack/version-lock.md`](docs/modpack/version-lock.md). Quando algo
quebra com duas dependências novas, descobrir qual delas custa mais do que os
dois PRs separados teriam custado.

Exceção: correção de segurança não espera janela.

### 9. Decisão cara de reverter vira ADR

`docs/adr/`, registrando **o custo assumido** e não só o benefício, e **o que
NÃO muda**. As duas seções são obrigatórias e há um portão que exige ambas.

Há também um portão que exige que todo ADR do disco esteja no índice. Ele varre
o diretório, não a lista — índice de decisões que omite uma decisão é pior que
não ter índice.

### 10. Este repositório está dentro do OneDrive

`C:\Users\alcyn\OneDrive\Documents\dark-continent-awakening`.

Isso já causou uma falha real de build: o OneDrive (ou um processo Java
sobrevivente) segurou um arquivo dentro de `build/` e o Gradle não conseguiu
apagá-lo — `Unable to delete file ... outputs.jar`.

**Recomendação: mover o repositório para fora do OneDrive** (por exemplo
`C:\dev\dark-continent-awakening`). O diretório `build/` chega a dezenas de
milhares de arquivos e não deve ser sincronizado.

Enquanto ele estiver aqui, ao encontrar `Unable to delete file`:

```powershell
Get-Process java, javaw -ErrorAction SilentlyContinue | Stop-Process -Force
Remove-Item -Recurse -Force build
```

### 11. Idioma

Código, identificadores e comentários em **português sem acento** (os
identificadores viram nomes de arquivo e chaves; acento em NBT e em path é
fonte de problema no Windows). Documentação em **português com acento**. O que
a plataforma impõe — `onActivate`, `getSerializedName`, nomes de API do
NeoForge — fica como é.

Consistência importa mais que a escolha. Não misture no mesmo arquivo.

### 12. Caminho de datapack é singular, e um bloco só dropa com DUAS coisas

O Minecraft 1.21 renomeou os diretórios de datapack para o **singular**, e
**ignora os antigos em silêncio** — sem erro, sem aviso no build, com o arquivo
bem formado e versionado:

| 1.20 | 1.21 |
| --- | --- |
| `loot_tables/` | `loot_table/` |
| `recipes/` | `recipe/` |
| `advancements/` | `advancement/` |
| `predicates/` | `predicate/` |
| `tags/blocks/`, `tags/items/` | `tags/block/`, `tags/item/` |

> Este repositório viveu meses com 21 loot tables de bloco e 2 receitas nas
> pastas de 1.20, **enquanto metade do repositório já estava migrada**
> (`tags/block`, `structure`, `loot_table/chests`). Metade certa e metade errada
> leem igual num `ls`.

E um bloco com `requiresCorrectToolForDrops()` precisa de **duas** coisas para
dropar:

1. a loot table no caminho vivo (`data/<ns>/loot_table/blocks/<id>.json`);
2. o id numa tag `minecraft:mineable/*` — em
   **`data/minecraft/tags/block/mineable/`**, e não no nosso namespace.

Sem a tag, nenhuma ferramenta é a correta, `hasCorrectToolForDrops` devolve
falso e `dropResources` nunca roda. **Consertar só uma das duas move arquivos e
não muda um drop** — é um conserto que parece conserto.

Escrever as mesmas tags em `data/nenfoundation/tags/block/mineable/` é aceito
sem reclamação: cria uma tag nova, `nenfoundation:mineable/axe`, que ferramenta
nenhuma consulta.

O portão é `CaminhosDeDatapackTest`, e ele verifica o **par**.
