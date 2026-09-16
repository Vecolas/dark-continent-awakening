# Os gates da trilha EN: o que está provado, e por quem

Este documento é a **evidência** dos gates de inimigos — `#139` (EN1), `#123`
(EN3), e os gates de saída de EN4, EN5, EN14 e EN15. Ele existe separado de
[`estado-en.md`](../inimigos/estado-en.md) porque aquele diz *onde a trilha
está* e este diz *o que foi conferido, com o quê*.

> Os testes automatizados fecham a parte server-side dos gates; cliente, dois
> jogadores e julgamento visual continuam sendo prova manual. O que está abaixo
> separa as duas coisas para o verde não prometer mais do que mediu.

---

## A régua, em números

| Medida | Valor |
| --- | --- |
| Testes JUnit na trilha inteira | **1.415 executados, 0 falhas** |
| GameTests da trilha de inimigos | **145 executados, 0 falhas** |
| Ids de inimigo registrados | 24 (7 do exame + 7 de Greed Island + 9 de Chimera + o boneco) |
| Arquivos de som gerados e conferidos | 120, todos Ogg Vorbis validados byte a byte |
| Geradores de arte com validação semântica | Cyclops, Hyper Puffball, Melanin Lizard, Radio Rat, Bubble Horse, Wolf Pack Hunter, King White Stag Beetle, Boneco de Treino |

---

## EN1 — o gate da fundação (#139)

### O que o gate pede, item a item

| Item | Estado | Evidência |
| --- | --- | --- |
| build verde, contagem > 0 | ✅ | 1.414 testes |
| datagen auditado quando dados mudarem | ✅ | loot, lang, `sounds.json` e tags conferidos por portão, não por leitura |
| GameTests do dummy cobrem percepção, fases, hitbox, weak point e stagger | ✅ | executados dentro dos 145 GameTests |
| `runClient` renderiza e anima o dummy | ⬜ | não executado |
| `runServer` chega a `Done` | ✅ | servidor dedicado chegou a `Done` nesta passagem |
| dois jogadores confirmam autoridade e action sync | ⬜ | não executado |
| gate de import client/common com caso canário | ✅ | `PacotesDeclaradosTest`, pré-existente |
| gate de asset/JSON ausente com caso canário | ✅ | `CoerenciaDeGeckoLibTest` reprovou de verdade 4 vezes durante esta trilha |
| budget de percepção documentado | ✅ | `PerceptionBudget` valida a faixa; `PerceptionControllerTest` conta as varreduras |
| ausência de packet por tick documentada | ✅ | nada nesta trilha criou payload novo |
| contrato aprovado por ambos antes de abrir Great Stamp | ⬜ | decisão humana |

### O que escrever os GameTests **achou**

Quatro defeitos, e nenhum levantava exceção:

1. **A caixa de golpe do boneco apontava para trás.** `-Z` é a frente da
   *geometria* Bedrock; a transformação de mundo de `AttackHitbox` usa
   `yaw 0 → +Z`. Ele atacava, animava e não encostava em quem estava na frente.
2. **A morte não publicava o repouso.** Depois de morto o passo de IA não roda,
   e a fase ficava congelada no cliente em `WINDUP`/`ACTIVE`.
3. **A régua de arte reprovou o alvo pintado.** O anel fica em `0.46` da altura
   e o `WeakPointResolver` dizia `0.55`, copiado de outro mob.
4. **`CoerenciaDeGeckoLibTest` reprovou o atalho de loop.** No GeckoLib 4.8.3 o
   `LoopType` do Java vence o JSON.

Os dois primeiros vieram da **leitura que escrever o cenário forçou** — não de
um verde. Isso é o argumento a favor de escrever GameTests mesmo antes de poder
executá-los, e também o limite dele.

---

## EN3 — o gate do primeiro pacote (#123)

| Item | Estado | Observação |
| --- | --- | --- |
| Great Stamp completo | ✅ (outra frente) | corpo próprio, carga, testa, manada |
| Frog e Foxbear funcionais | ✅ (outra frente) | migrados nesta trilha para o `GrabController` compartilhado |
| autoridade do servidor | ✅ | nenhum payload novo; toda região de dano é resolvida no servidor |
| spawn válido | 🟡 | os cinco perfis existem e são cobrados; **densidade em mundo gerado nunca foi vista** |
| telegrafos | 🟡 | as janelas são medidas; que elas *leiam* como aviso é olho humano |
| weak points | ✅ | `WeakPointResolver` + validação de arte amarrando desenho e regra |
| multiplayer | 🟡 | as regras estão cobertas (EN14); a matriz 1/2/4 não foi executada |
| save/reload | 🟡 | round-trip de desfechos, autor, travas e cards coberto por `EncounterSavedDataTest`; restart real e disco continuam manuais |
| sem crash client-only | ✅ | portão estático; sem `runServer` real |

---

## EN4 e EN5 — encontro e Greed Island

O gate de EN4 é *"reiniciar o servidor durante um encontro sem duplicar entidade
nem recompensa"*. O de EN5 é *"o encontro de GI converte em cards sem duplicar
por corrida multiplayer"*. A lógica server-side está coberta; restart com disco,
dois clientes reais e entrega no inventário continuam dependendo de sessão manual.

O que está provado é a regra que os dois vão exercitar:

- `EncounterController.reconciliarAoIniciar` distingue *"a entidade sobreviveu"*
  de *"o chunk está dormindo"* — e é essa distinção que impede dois chefes;
- episódio interrompido pelo restart vira `FAILED`, e não `COMPLETED`: a
  diferença decide se a recompensa é paga;
- `RewardLedger.travar` decide e grava na mesma chamada — entre um *"já pagou?"*
  e um *"então pague"* cabe o outro jogador;
- a trava sobrevive à ida e volta do save e continua bloqueando;
- o limite de cópias é cobrado no mesmo ato, e a tentativa recusada **não**
  consome cópia.
- cada entidade capturada possui uma trava própria; duas criaturas da mesma
  espécie no mesmo encontro podem emitir dois cards, mas a mesma entidade nunca
  emite duas vezes;
- desfechos, autor, travas e contagem mundial de cards sobrevivem ao round-trip
  do formato de save em `EncounterSavedDataTest`.

---

## EN15 — a régua de balanceamento

A tabela em [`balanceamento.md`](../inimigos/balanceamento.md) é **gerada**: o
portão a regrava e reprova quando ela diverge dos atributos. Uma tabela escrita à
mão envelhece no primeiro número girado, e envelhece em silêncio.

A régua achou uma divergência real — o Man-faced Ape é `DANGEROUS` e morre em
2,2 s — e ela **não** foi resolvida afrouxando a faixa. A faixa mede
durabilidade; o `ThreatTier` mede ameaça de encontro. A exceção existe, se
declara com motivo, e a lista **morde dos dois lados**.

---

## O que nenhum gate desta trilha cobre

Está tudo em [`o-que-nao-provamos.md`](o-que-nao-provamos.md), e a lista ganhou
onze linhas nesta passagem. As quatro que mais importam:

1. **Aparência e som continuam manuais.** O `runClient` ainda precisa confirmar
   terreno, modelos, animações, telegraphs, ataques, hitboxes, pontos fracos e
   áudio dos sete inimigos.
2. **Restart real e dois clientes ainda não foram feitos.** O formato de save é
   coberto em round-trip automatizado; a prova de disco, reconexão e corrida real
   continua manual.
3. **Os sete inimigos de Greed Island são `ENCOUNTER_ONLY`.** Isso é deliberado:
   eles entram pelo controlador de encontros, não por spawn natural. O teste real
   de entrar na dimensão, ativar o encontro e observar a cena continua manual.
