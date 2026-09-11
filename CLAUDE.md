# CLAUDE.md — Dark Continent Awakening

## O que e este projeto

**Dark Continent Awakening** e duas coisas com o mesmo nome:

1. **Nen Foundation** (`mod_id: nenfoundation`) — um mod autoral de NeoForge
   1.21.1 que implementa aura, categorias, tecnicas fundamentais e um framework
   de habilidades.
2. **O modpack** que se monta em volta dele.

Duas pessoas trabalham nisto em paralelo. Boa parte das regras abaixo existe
por causa disso.

### A decisao central

> **O Nen Foundation e a UNICA autoridade sobre Nen.**

Mods externos complementam quests, loot, spawn, documentacao, animacao,
interface e performance. Nenhum deles controla aura, categoria, tecnica,
cooldown ou dano de Nen.

Duas autoridades sobre a mesma mecanica divergem — e a divergencia nao aparece
como erro, aparece como desbalanceamento que ninguem consegue explicar.

---

## Fonte de verdade

Regras GERAIS de engenharia **nao moram neste arquivo**. Elas moram em
[`disciplina-de-engenharia/`](disciplina-de-engenharia/), copia versionada da
skill de mesmo nome, e fonte de verdade sobre: portoes, falso verde, onde mora
um numero, nomes e comentarios, ponto cego declarado, duas fontes para a mesma
verdade, git, issue como unidade de trabalho e o que uma entrega precisa
declarar.

> **A copia do repositorio e a que vale**, inclusive contra a versao em
> `~/.claude/skills/`. Ela existe porque um agente rodando aqui pode nao ter
> acesso aquela pasta.
>
> Sim, isso e a duplicacao que a secao 7 da propria skill proibe. Foi feita de
> olhos abertos, e a unica coisa que impede a duplicacao de virar divergencia e
> uma das copias ganhar POR REGRA e nao por acaso. A regra e essa, e o custo
> esta escrito em
> [`disciplina-de-engenharia/LEIA-ME.md`](disciplina-de-engenharia/LEIA-ME.md).

Leia, antes de implementar:

| Ordem | Onde | O que |
| --- | --- | --- |
| 0 | [`disciplina-de-engenharia/`](disciplina-de-engenharia/) | as regras gerais |
| 1 | `CONVENCOES.md` | o que e especifico deste repositorio |
| 2 | `docs/adr/index.md` | as decisoes e o que elas custam |
| 3 | `docs/processo/marcos.md` | onde o projeto esta e o que vem depois |
| 4 | `docs/processo/fronteira-de-arquivos.md` | quem toca o que |
| 5 | este arquivo | |

Os documentos-fonte do projeto (plano tecnico, roadmap, pesquisa de
viabilidade) estao em `docs/pesquisa/`.

**Se a implementacao e a documentacao discordarem, o codigo ganha e o texto se
atualiza** — e a discrepancia e relatada, nao corrigida em silencio.

---

## Princípios inegociaveis

1. **O servidor decide.** Cliente envia intencao; servidor valida e aplica.
   Nenhum payload C2S carrega aura, dano, cooldown, unlock ou multiplicador.
   ([ADR-001](docs/adr/ADR-001-servidor-autoritativo.md))
2. **Persistente e runtime sao coisas separadas.** Progresso sobrevive a morte,
   logout e restart; aura atual nao sobrevive a nada.
   ([ADR-002](docs/adr/ADR-002-persistente-e-runtime.md))
3. **Toda integracao e opcional.** O JAR inicia em cliente e em servidor
   dedicado com NeoForge e mais nada.
   ([ADR-003](docs/adr/ADR-003-integracoes-opcionais.md))
4. **Numero e dado; regra de seguranca e Java.** Custo, cooldown, afinidade e
   requisito saem para config e datapack. Validacao de alvo, alcance, linha de
   visao e ciclo de vida ficam em Java.
5. **Script nao valida nada.** KubeJS mexe em receita e conteudo do pack. No
   dia em que uma regra de Nen viver em JS, a autoridade do servidor acabou.
6. **Um unico ciclo de vida por familia.** Uma `NenTechnique`, uma
   `NenAbility`. Nao quatro sistemas parecidos.
7. **Quem liga, desliga** — e o par mora no ciclo de vida de quem ligou, nunca
   espalhado pelos varios pontos de saida.
8. **Recusa sempre tem motivo.** Ativacao que falha em silencio produz o pior
   relato de bug que existe.
9. **Marcos sao ids proprios**, nunca ids de quest do FTB. Trocar o questbook
   nao pode apagar progresso.
10. **Nenhum asset extraido da obra.**
    ([ADR-007](docs/adr/ADR-007-assets-autorais.md))

---

## Stack aprovada

| O que | Versao | Congelado por |
| --- | --- | --- |
| Minecraft | 1.21.1 | [ADR-005](docs/adr/ADR-005-versoes-pinadas.md) |
| NeoForge | 21.1.250 | idem |
| Java | 21 | exigencia do NeoForge 1.21.1 |
| Gradle | 9.2.1 (wrapper) | MDK oficial |
| NeoGradle | 7.1.38 | MDK oficial |
| JUnit | 5.11.4 | — |

Nada sobe sozinho. Uma dependencia por vez, com changelog e matriz de teste.
Ver [version-lock.md](docs/modpack/version-lock.md).

### Nao adicionar sem necessidade demonstrada

Biblioteca de injecao de dependencia, ORM, framework de UI, biblioteca de
serializacao alternativa. O NeoForge ja traz Codec, DeferredRegister,
attachments e payloads.

### Explicitamente fora do MVP

- **Epic Fight como fundacao** — so como adapter no M7, apos gate
  ([ADR-006](docs/adr/ADR-006-epic-fight-fora-da-fundacao.md)).
- **Um segundo mod de Nen** (Mine X Hunter, Nen Unbound, Hunter X Craft) —
  nunca.
- **Editor generico de Hatsu** — pos-MVP (F5).
- **Votos e limitacoes complexos** — pos-MVP (F3).
- **Personagens canonicos** — as habilidades de prova sao arquetipos, nao Gon
  e Killua.
- **Arcos completos** (Exame Hunter, Torre Celestial, Yorknew, Greed Island) —
  cada um e um modulo proprio, depois.
- **Dimensoes proprias dentro do JAR da fundacao.**

---

## Arquitetura

### Regra de dependencia

```
  api          <- nada de dentro depende de fora
  nen        -> api
  network    -> api, nen
  server     -> api, nen, network
  client     -> api, network        (NUNCA o contrario)
  integration-> api                 (o nucleo NUNCA depende daqui)
  datagen    -> tudo; nada depende dele
```

`NenFoundation.java` so registra subsistemas. Ele nao cresce.

**O portao `PacotesDeclaradosTest` reprova import de `net.minecraft.client.*`
ou de `nenfoundation.client.*` dentro do nucleo.** Ele nao pega violacao por
reflexao — isso esta declarado em
[o-que-nao-provamos.md](docs/testing/o-que-nao-provamos.md).

### Estrutura

```
src/main/java/com/darkcontinent/nenfoundation/
  NenFoundation.java        registra subsistemas, e so
  api/                      superficie publica (ability, event, query)
  registry/                 registros do mod
  config/                   ModConfigSpec
  data/                     attachment, codec, definition
  nen/                      dominio: profile, aura, category, technique,
                            progression, combat, ability
  network/                  NenProtocol, payload, handler
  command/                  comandos permissionados
  integration/              ftbquests, kubejs, epicfight, jade
  server/                   scheduler, ciclo de vida
  client/                   hud, keybind, render, screen, particle
  datagen/
```

Cada pacote tem um `package-info.java` dizendo **o que faz, qual decisao
carrega, em que marco nasce e de quem e**. Um portao exige isso de todo pacote.

---

## Contratos congelados

Congelados no M0, por [ADR-004](docs/adr/ADR-004-identidade-congelada.md):

| O que | Onde | Portao |
| --- | --- | --- |
| `mod_id`, package, namespace | `gradle.properties` | — |
| ids das 7 categorias, com o neutro no ordinal 0 | `NenCategory` | `NenCategoryTest` |
| `PersistentNenData` v1 | `nen/profile/` | `PersistentNenDataTest` |
| ids e direcoes de payload, versao do protocolo | `NenProtocol` + `docs/multiplayer/protocol.md` | `ProtocoloCongeladoTest` |
| `NenTechnique` | `nen/technique/` | — |
| `NenAbility` | `api/ability/` | — |

Descongelar exige ADR novo apontando o ADR-004, degrau em
`NenProfileMigrator` e plano para os mundos existentes.

---

## Onde mora um numero

**Numero que alguem vai querer girar numa sessao de balanceamento nao mora no
codigo.** Ele vai para config ou datapack.

E o inverso vale igual: **numero que foi para a config tem de SAIR do codigo.**
Deixado nos dois, o do codigo e sobrescrito em runtime, nenhum teste acusa
nada, e a proxima pessoa passa uma tarde girando o botao morto.

**Numero novo nasce medivel.** A regua entra junto do sistema que ela mede —
nunca antes (regua que mede o vazio e cerimonia) e nunca depois (numero sem
regua vira folclore).

Consequencia direta para este projeto: **`NenConfig` so ganha uma chave quando
o consumidor dela existe.** Nada de declarar "custo de Ren" antes de Ren
existir. Formulas de aura chegam no M2; custo de tecnica no M4; custo e
cooldown de habilidade no M5.

**Limite de design nao e botao de tuning.** Quantos slots de habilidade cabem
na barra e constante no codigo, de proposito.

---

## Como trabalhar

### Uma coisa por vez

**Nunca implementar mais de um marco numerado sem instrucao explicita para
continuar.** Ao terminar um marco, pare, relate e espere.

Trabalho paralelo so entre partes sem dependencia entre si, cada uma na sua
branch, com fronteira de arquivo escrita.

### O ciclo

1. Ler o marco em [`docs/processo/marcos.md`](docs/processo/marcos.md).
2. Abrir issue com os cinco campos (objetivo, entregas, criterio de aceite,
   **fora de escopo**, **bloqueios com nome**).
3. Branch propria. Nunca commitar na `main`.
4. Ler o codigo existente antes de mudar arquitetura.
5. Implementar a menor fatia vertical primeiro.
6. Teste junto da logica, nao depois de tudo pronto. **Nenhum teste e escrito
   antes de existir logica para testar.**
7. `./gradlew build` verde **na maquina**, antes do PR.
8. Smoke test em `runServer` se o PR toca o nucleo.
9. Atualizar docs e ADR se o comportamento ou a arquitetura mudou.
10. Relatar: arquivos mudados, decisoes, o que foi testado e **o que ficou sem
    prova**.
11. Parar na fronteira do marco. Nao "ajudar" comecando o proximo.

### Antes de abrir PR

```bash
./gradlew build          # compila + testes unitarios + portoes
./gradlew runServer      # se o PR toca o nucleo
```

O CI e **rede de seguranca, nao primeira verificacao**.

---

## Portoes que existem hoje

| Portao | O que ele impede |
| --- | --- |
| `NenCategoryTest` | renomear, reordenar ou remover categoria; traducao faltando ou orfa |
| `PersistentNenDataTest` | perder campo no codec; default nao-neutro; colecao mutavel |
| `NenProfileMigratorTest` | aceitar save de versao futura ou invalida |
| `ProtocoloCongeladoTest` | codigo e documento discordarem sobre id, direcao ou versao |
| `IndiceDeAdrTest` | ADR fora do indice; ADR sem custo declarado |
| `PacotesDeclaradosTest` | pacote sem documentacao; nucleo importando cliente |
| suite vazia reprova | `build` sair verde com zero testes executados |

Ao criar um portao novo: **alimente-o com um caso que DEVE reprovar e confirme
que ele reprova.** Regua que nunca reprova e carimbo.

E ao ler qualquer verde, lembre do que ele nao cobre:
[o-que-nao-provamos.md](docs/testing/o-que-nao-provamos.md).

---

## Definition of Done

Uma entrega nao esta pronta ate que:

- `./gradlew build` passa, com contagem de testes maior que zero;
- o comportamento novo tem teste, e o teste reprova sem o codigo;
- recusa e erro tem mensagem para o jogador, com chave de traducao;
- estado criado tem limpeza no ciclo de vida de quem o criou;
- nada client-only foi alcancado pelo nucleo;
- numero ajustavel esta em config **e saiu do codigo**;
- dependencia nova esta em `docs/modpack/supported-mods.md` com pacote,
  versao, licenca, motivo e URL;
- ADR atualizado se a arquitetura mudou;
- o relato diz **o que nao foi verificado**.

Para o MVP inteiro, ver a Definition of Done do plano tecnico e a
[matriz de QA](docs/testing/qa-matrix.md).

---

## Erros que este projeto ja sabe que vai cometer

Estao aqui porque sao previsiveis, silenciosos e caros.

1. **Congelar um multiplicador na ativacao.** A tecnica guarda o custo em
   `onActivate` e ignora todo buff posterior. Nao da erro.
2. **Estado de jogador num campo da classe da tecnica.** A implementacao e
   singleton; dois jogadores escrevem no mesmo campo.
3. **Limpeza espalhada pelos pontos de saida.** Morte, logout e troca de
   dimensao cada um limpando um pedaco. Um deles vai faltar, e o buff fica
   ligado para sempre.
4. **Exclusao de tecnica declarada de um lado so.** Ten sabe de Zetsu, Zetsu
   esquece de Ren. A combinacao ilegal funciona.
5. **Dano multiplicado em dois event handlers.** O numero final e plausivel
   demais para alguem notar sem medir.
6. **Aceitar a entidade que o cliente apontou.** Funciona perfeitamente com
   cliente honesto — e por isso nenhum playtest encontra.
7. **Config declarada e constante paralela no consumidor.** O consumidor ganha,
   a config vira arquivo orfao, e a sessao de balanceamento nao muda nada.
8. **Projetil ou construct orfao.** Nao aparece como erro; aparece como TPS
   caindo devagar ao longo de uma semana.
9. **`git add -A` levando o trabalho da outra pessoa.** Nao da erro. Ver
   [fronteira-de-arquivos.md](docs/processo/fronteira-de-arquivos.md).
10. **Testar em singleplayer e achar que testou servidor.** Singleplayer roda
    um servidor interno no mesmo processo do cliente. Ele nao pega classe
    client-only vazada.

---

## Estado atual

Antes de implementar, testar, revisar ou integrar, aplicar a
[política de skills compartilhadas](docs/processo/skills-do-projeto.md).
As skills aplicáveis em `.claude/skills` devem ser lidas integralmente; suas
sugestões genéricas não revogam decisões aprovadas do projeto.

**Fonte de verdade:** [marcos.md](docs/processo/marcos.md), com evidências de QA
e pendências de integração. Não deduzir entrega a partir de código compilável.

Não iniciar um marco sem autorização explícita. Mudanças no modelo de Aura
exigem decisão conjunta registrada conforme o
[ADR-009](docs/adr/ADR-009-modelo-de-aura-sem-stamina-de-nen.md).
