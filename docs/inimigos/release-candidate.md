# EN16 — o que falta para os 23 encontros virarem Release Candidate

Este documento é o **gate final** da trilha [INIMIGOS] (issue `#151`), e ele
começa dizendo o que ainda não é verdade: **nada aqui está pronto para RC.**

Ele existe agora, e não no fim, por um motivo prático: um checklist escrito
depois que o trabalho acabou lista o que foi feito. Escrito antes, ele lista o
que falta — e a diferença entre os dois é a única coisa que um gate deveria
medir.

---

## O que existe hoje

| Eixo | Estado |
| --- | --- |
| Ids registrados | **24** (7 do exame + 7 de Greed Island + 9 de Chimera + o Boneco de Treino) |
| Corpo próprio (geo + animação + textura) | os 7 do exame, o boneco, e os 7 de Greed Island |
| Voz (5 sons cada) | **24 de 24**, todos Ogg Vorbis validados byte a byte |
| Comportamento da própria ficha | os 7 do exame, o boneco, os 7 de Greed Island |
| Loot, tradução, perfil publicado, renderer | 24 de 24 |
| Testes JUnit | 1.087 executados |
| GameTests escritos / **executados** | 11 / **0** |

---

## Os oito bloqueios de RC, por ordem de custo

### 1. Nada rodou com o jogo de pé

Nem `runClient`, nem `runServer`, nem `runGameTestServer`. É o bloqueio maior, e
ele invalida qualquer afirmação de "funciona" — inclusive as que este repositório
faz com cuidado. O que está provado é regra; o que falta é comportamento.

### 2. Nenhum save passou por disco

Encontro, colônia, identidade de Chimera, ledger de recompensa e telemetria foram
provados por ida e volta **em memória**. O gate de EN4 — *reiniciar o servidor no
meio de um encontro sem duplicar entidade nem recompensa* — é exatamente o que
esses testes não podem fazer.

### 3. Os nove de Chimera não têm corpo nem comportamento

Estão registrados, com voz, loot, tradução, perfil e molde genético — e o
javadoc de cada entidade diz, em maiúsculas, que ela é andaime. Em jogo eles são
invisíveis.

### 4. Nenhuma criatura nova nasce sozinha

As dezesseis são `ENCOUNTER_ONLY`. Há dimensão de Greed Island, há receita de
encontro e há spawner — mas **não há gatilho de mundo**: nada cria uma
`EncounterInstance` a não ser `/nenenemy encounter`. Sem worldgen ou estrutura, o
jogador nunca encontra nada por acaso.

### 5. A colônia de Chimera não existe em jogo

`ChimeraColony`, `ChimeraColonySavedData` e a simulação offline estão completos e
sem produtor: não há ninho gerado, não há quem funde uma colônia, e não há quem
materialize as formigas que a recuperação autoriza.

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
| 23 assets | 🟡 15 de 24 têm corpo; **todos** têm voz |
| 23 recompensas | ⬜ há loot e card; não há recompensa de quest nem de bestiário por criatura |
| descoberta natural | ⬜ não há gatilho de mundo |
| GI isolada | ✅ dimensão própria, perfil `ENCOUNTER_ONLY`, portão amarrando constante e datapack |
| Chimera com colônia | ⬜ domínio completo, sem entidade |
| Nen real | 🟡 decide, não ativa |
| Bestiário progressivo | ✅ entregue por outra frente |
| sem dupe | 🟡 provado por regra, não por restart real |
| sem missing asset | ✅ quatro portões distintos cobram isso |
| sem debug obrigatório | ✅ `/nenenemy` é opcional e permissionado |
| new/existing world, três restarts, dimensão/unload/death/relog | ⬜ nenhum executado |
| 4 players | ⬜ |
| smoke dedicado de 2h | ⬜ |

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
