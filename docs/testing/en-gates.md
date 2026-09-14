# Os gates da trilha EN: o que está provado, e por quem

Este documento é a **evidência** dos gates de inimigos — `#139` (EN1), `#123`
(EN3), e os gates de saída de EN4, EN5, EN14 e EN15. Ele existe separado de
[`estado-en.md`](../inimigos/estado-en.md) porque aquele diz *onde a trilha
está* e este diz *o que foi conferido, com o quê*.

> **Nenhum gate desta trilha fecha com teste verde.** Todos pedem
> `runClient`, `runServer`, dois jogadores e olho humano. O que está abaixo é a
> metade automatizável — e a lista do que ela deliberadamente não cobre.

---

## A régua, em números

| Medida | Valor |
| --- | --- |
| Testes JUnit na trilha inteira | 971 executados, 0 falhas |
| GameTests escritos para o Boneco de Treino | 11 |
| GameTests **executados** | **0** — `runGameTestServer` não rodou |
| Ids de inimigo registrados | 24 (7 do exame + 7 de Greed Island + 9 de Chimera + o boneco) |
| Arquivos de som gerados e conferidos | 120, todos Ogg Vorbis validados byte a byte |
| Geradores de arte com validação semântica | Cyclops, Hyper Puffball, Melanin Lizard, Radio Rat, Bubble Horse, Wolf Pack Hunter, King White Stag Beetle, Boneco de Treino |

---

## EN1 — o gate da fundação (#139)

### O que o gate pede, item a item

| Item | Estado | Evidência |
| --- | --- | --- |
| build verde, contagem > 0 | ✅ | 971 testes |
| datagen auditado quando dados mudarem | ✅ | loot, lang, `sounds.json` e tags conferidos por portão, não por leitura |
| GameTests do dummy cobrem percepção, fases, hitbox, weak point e stagger | 🟡 | **escritos, não executados** |
| `runClient` renderiza e anima o dummy | ⬜ | não executado |
| `runServer` chega a `Done` | ⬜ | não executado |
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
| save/reload | 🟡 | ida e volta em memória; **nada passou por disco** |
| sem crash client-only | ✅ | portão estático; sem `runServer` real |

---

## EN4 e EN5 — encontro e Greed Island

O gate de EN4 é *"reiniciar o servidor durante um encontro sem duplicar entidade
nem recompensa"*. O de EN5 é *"o dummy de GI converte em card exatamente uma vez
sob corrida multiplayer"*. **Nenhum dos dois foi executado.**

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

1. **Nada rodou com o jogo de pé.** Nem `runClient`, nem `runServer`, nem
   `runGameTestServer`.
2. **Nenhum save passou por disco.** Encontro, colônia, identidade de Chimera e
   ledger de recompensa foram provados por ida e volta em memória.
3. **Aparência e som não têm régua.** As validações de arte amarram o desenho à
   *regra* — a altura do olho, o alcance do porrete, o contraste do ponto fraco.
   Que o bicho *pareça* o bicho, e que a voz dele *soe* como ele, continua sendo
   olho e ouvido humano. Ninguém olhou e ninguém ouviu.
4. **Nenhuma criatura nova nasce em lugar nenhum.** As dezesseis são
   `ENCOUNTER_ONLY`: a dimensão de Greed Island não existe como datapack, e não
   há controlador de colônia materializando formiga. Isso é deliberado — proibir
   primeiro, abrir depois —, mas quer dizer que ninguém encontrou nenhuma delas.
