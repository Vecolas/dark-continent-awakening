# ADR-009 — Aura composta, sem uma segunda stamina de Nen

- **Status:** aceita
- **Data:** 2026-09-11
- **Marco:** M2

## Contexto

Hunter × Hunter separa capacidade total de aura, reserva restante e o quanto o
usuário consegue manifestar de uma vez. Controle, eficiência por categoria e o
estado (Ten, Ren, Zetsu etc.) mudam consumo, output, defesa e recuperação.
Criar uma segunda barra de stamina para as mesmas ações de Nen duplicaria o
custo sem representar uma grandeza diferente.

O Minecraft já fornece fome, exhaustion, sprint, velocidade de ataque e vida
como sinais de esforço físico. Uma barra especial adicional só deve nascer
quando um sistema de combate futuro tiver um consumidor explícito (dash,
dodge, bloqueio, parry ou equivalente).

## Decisão

O Nen Foundation terá **uma única barra visível de Aura**. O modelo server-side
é composto por:

- `maxAura`: capacidade total;
- `currentAura`: reserva restante;
- `auraOutput`: teto de aura manifestada simultaneamente, não uma barra;
- `auraControl`: desperdício e precisão do uso;
- `typeEfficiency`: eficiência por categoria;
- estado de Nen: Ten, Ren, Zetsu e futuros estados;
- exaustão: consequência derivada de reserva, output, controle e estado.

`AuraPool` continua sendo a fronteira da reserva atual/máxima. Output,
controle, eficiência e estado modificam as fórmulas de custo, regeneração,
defesa e aplicação quando seus consumidores entrarem no roadmap.

Zetsu poderá recuperar aura rapidamente ao interromper o fluxo externo, mas
também reduzirá output e defesa contra Nen. Ren poderá consumir aura por tick e
elevar output. Os multiplicadores são tuning de configuração acompanhado de
regua; este ADR fixa as grandezas, não os valores finais.

## Governança da decisão

Por determinação do responsável pelo projeto nesta sessão, alterações deste
modelo exigem decisão explícita registrada pelos **dois desenvolvedores**.
O registro deve identificar ambos, indicar a aprovação de cada um, explicar a
mudança e seu custo, e apontar este ADR. Sem esse registro, a alteração do
modelo é recusada. Um agente não pode presumir nem assinar a aprovação do outro.
Implementar ou corrigir o comportamento já aprovado não reabre a decisão.

## Custo assumido

- O jogador não terá uma barra customizada de fôlego no MVP.
- A distinção entre cansaço físico e exaustão de Nen dependerá inicialmente
  das mecânicas vanilla e do estado de Aura.
- Um sistema de combate futuro que precise de fôlego exigirá uma decisão e uma
  barra novas, em vez de reutilizar Aura como stamina física.

## O que NAO muda

- Aura continua runtime, autoritativa no servidor e não persistida a cada tick,
  conforme o [ADR-002](ADR-002-persistente-e-runtime.md).
- Nenhum cliente pode escolher `currentAura`, output, controle ou custo, conforme
  o [ADR-001](ADR-001-servidor-autoritativo.md).
- A M2 ainda entrega pool, regeneração, output, exaustão, dirty sync e HUD; a
  barra mostra current/max e não inventa os valores derivados.
- A stamina física do Minecraft não é removida nem reinterpretada pelo mod.
