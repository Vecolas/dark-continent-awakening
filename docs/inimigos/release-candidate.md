# EN16 — o que falta para os 23 encontros virarem Release Candidate

Este documento é o **gate final** da trilha [INIMIGOS] (issue `#151`), e ele
separa o que já foi provado automaticamente do que ainda exige uma sessão real
de jogo. A existência de conteúdo completo não é, sozinha, aprovação de RC.

Ele existe agora, e não no fim, por um motivo prático: um checklist escrito
depois que o trabalho acabou lista o que foi feito. Escrito antes, ele lista o
que falta — e a diferença entre os dois é a única coisa que um gate deveria
medir.

---

## O que existe hoje

| Eixo | Estado |
| --- | --- |
| Ids registrados | **24** (7 do exame + 7 de Greed Island + 9 de Chimera + o Boneco de Treino) |
| Corpo próprio (geo + animação + textura) | **24 de 24** |
| Voz (5 sons cada) | **24 de 24**, todos Ogg Vorbis validados byte a byte |
| Comportamento da própria ficha | **24 de 24** |
| Loot, tradução, perfil publicado, renderer, ficha de bestiário | 24 de 24 |
| Perfis de interrupção alcançáveis | **17 de 17** (os 7 do exame não usam `StaggerState`) |
| Assets reproduzíveis byte a byte | **199 de 199** |
| Testes JUnit | **1.415 executados, 0 falhas** |
| GameTests escritos / **executados** | **145 / 145**, 0 falhas |

---

## Os oito bloqueios de RC, por ordem de custo

### 1. Os portões de jogo ainda não estão todos executados

`runGameTestServer` foi executado e passou os 145 cenários obrigatórios. O
servidor dedicado também chegou a `Done` em uma execução. Ainda faltam a
inspeção visual pelo `runClient`, a matriz com jogadores reais e a repetição
dedicada com save/restart. O que está provado por GameTest é regra server-side;
o que falta é comportamento observado no jogo.

### 2. Nenhum save passou por disco

Encontro, colônia, identidade de Chimera, ledger de recompensa e telemetria foram
provados por ida e volta **em memória**. O gate de EN4 — *reiniciar o servidor no
meio de um encontro sem duplicar entidade nem recompensa* — é exatamente o que
esses testes não podem fazer.

### 3. ~~Os nove de Chimera não têm corpo nem comportamento~~ — FECHADO

Os nove ganharam geo, esqueleto, animações, textura, renderer e comportamento
próprio. Nenhum `PLACEHOLDER` de identidade sobrou no repositório, e nenhum
javadoc diz mais "ANDAIME DECLARADO".

**O que esse bloqueio ensinou fica:** enquanto durou, *todos* os portões
aprovavam aqueles nove. Cada régua media a coluna dela — tradução, loot, perfil,
voz, fila única — e **nenhuma media a ausência de corpo**, porque
`CoerenciaDeGeckoLibTest` descobre os mobs varrendo o disco e quem não tem
`.geo.json` simplesmente não entra na varredura. `CorpoDeTodoInimigoTest` fecha
isso varrendo a lista de publicados. A regra geral que sobra é maior que o caso:
**a fonte de uma varredura tem de ser o que deveria existir, não o que existe.**

### 4. Nenhuma criatura nova nasce sozinha

As dezesseis são `ENCOUNTER_ONLY`. Há dimensão de Greed Island, há receita de
encontro e há spawner — mas **não há gatilho de mundo**: nada cria uma
`EncounterInstance` a não ser `/nenenemy encounter`. Sem worldgen ou estrutura, o
jogador nunca encontra nada por acaso.

### 5. A materialização da colônia de Chimera ainda não existe em jogo

`ChimeraColony`, `ChimeraColonySavedData`, a simulação offline e o ciclo de vida
do servidor estão completos. Ainda não há ninho gerado, produtor que funda uma
colônia ou materializador das formigas autorizadas pela recuperação.

### 6. O Nen das formigas decide e não ativa

`TacticalNenController` responde *qual técnica usar*; a ligação com o registro de
técnicas **não existe**, e não podia nascer no pacote de inimigos sem virar uma
segunda autoridade sobre Nen.

### 7. Ninguém olhou e ninguém ouviu

As validações de arte amarram o desenho à **regra** — a altura do olho do
Cyclops, o alcance do porrete, o contraste do ponto fraco, o degrau entre bolha e
pelagem. Nenhuma delas diz que o bicho *parece* o bicho, e nenhuma ouve o som.

### 8. A matriz de multiplayer não foi executada

1 integrado, 2 em LAN, 4 em dedicado, ping alto. As **regras** que ela exercita
estão cobertas (`HardeningMultiplayerTest`); a ordem real dos eventos não.

---

## O que **pode** ser marcado, e o que não pode

Da lista da issue `#151`:

| Item | Pode marcar? |
| --- | --- |
| 23 definitions | ✅ 24 ids, todos com ficha completa |
| 23 assets | ✅ 24 de 24 com corpo e voz próprios, 199 assets reproduzíveis byte a byte |
| 23 recompensas | ⬜ há loot e card; não há recompensa de quest nem de bestiário por criatura |
| descoberta natural | ⬜ não há gatilho de mundo |
| GI isolada | ✅ dimensão própria, perfil `ENCOUNTER_ONLY`, portão amarrando constante e datapack |
| Chimera com colônia | 🟡 as nove entidades existem; o domínio da colônia continua sem produtor |
| Nen real | 🟡 decide, não ativa |
| Bestiário progressivo | ✅ entregue por outra frente |
| sem dupe | 🟡 provado por regra, não por restart real |
| sem missing asset | ✅ quatro portões distintos cobram isso |
| sem debug obrigatório | ✅ `/nenenemy` é opcional e permissionado |
| new/existing world, três restarts, dimensão/unload/death/relog | ⬜ nenhum executado |
| 4 players | ⬜ |
| smoke dedicado de 2h | ⬜ |

---

---

## O oitavo bloqueio que este documento não previa

Ele não estava na lista porque ninguém sabia dele: **quatorze dos dezessete
perfis de interrupção nunca disparavam.** Não por um bug de código — por
aritmética. O decaimento entre dois golpes comia mais do que um golpe somava, e o
acumulado subia e voltava a zero para sempre.

Isso não deu erro, não apareceu em teste unitário (eles alimentam o acumulador
direto, e provam que a *máquina* funciona) e não apareceria em playtest, porque
ninguém reporta "o stagger não funciona" — reporta-se "esse bicho é chato".

Está corrigido, com régua (`StaggerAlcancavelTest`) e com a derivação movida para
`StaggerPorPapel`, onde um perfil morto deixou de ser possível de **escrever**.
Mas ele fica registrado aqui pelo que diz sobre os outros sete: **esta lista é o
que sabemos que falta, e não o que falta.** Um oitavo item apareceu depois de a
lista estar escrita, achado por uma régua nova e não por revisão.

---

## A regra que este documento defende

> **Conhecidos ficam documentados; P0 e P1 bloqueiam release.**

Os oito bloqueios acima são P0. Nenhum deles é surpresa: todos estão também em
[`o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md) e em
[`en-gates.md`](../testing/en-gates.md), escritos no momento em que a lacuna
nasceu — e não descobertos no fim.

Isso é o oposto de um relatório de release que diz "pronto" e deixa a diferença
implícita. A trilha entregou fundação, conteúdo e régua; ela **não** entregou um
jogo verificado, e as duas frases cabem no mesmo relato.
