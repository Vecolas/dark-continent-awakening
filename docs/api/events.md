# Eventos: como o mundo externo sabe o que aconteceu

## A regra

> **Quem faz algo anuncia. Quem se importa escuta.**

Nada de fora do nucleo navega pela arvore de objetos para descobrir estado de
Nen. Quest, addon, HUD e integracao nao alcancam o `AuraEngine`, nao pedem o
`RuntimeNenState` e nao leem o attachment direto.

O motivo e concreto: um consumidor que depende da POSICAO de alguem na
estrutura quebra assim que essa peca muda de lugar — e o refactor mais banal
do nucleo passa a quebrar cinco integracoes.

Depender da EXISTENCIA de um evento e legitimo. Depender do CAMINHO ate um
objeto nao e.

---

## Convencoes

**Nome no passado.** O evento relata o que ja aconteceu:
`NenDespertadoEvent`, `CategoriaReveladaEvent`, `TecnicaEncerradaEvent`,
`HabilidadeAtivadaEvent`.

**Dois momentos, quando fizer diferenca:** um cancelavel, antes; um
informativo, depois. Um listener que precisa **impedir** algo e um caso
diferente de um que so precisa **saber**, e misturar os dois faz o primeiro
listener registrado decidir por todos.

**Server-side.** Evento de dominio dispara no servidor. O cliente e informado
por payload, nao por evento compartilhado.

**Payload minimo e imutavel.** O evento carrega o jogador e o que mudou. Ele
nao carrega o perfil inteiro nem uma referencia mutavel — um listener que
altera o objeto recebido altera o estado real sem passar pelo servico, e sem
passar pela sincronizacao.

---

## Eventos previstos

| Evento | Quando | Cancelavel | Marco |
| --- | --- | --- | --- |
| `NenDespertandoEvent` | antes do despertar | sim | M3 |
| `NenDespertadoEvent` | depois | nao | M3 |
| `CategoriaAtribuidaEvent` | a categoria foi sorteada (o jogador ainda nao sabe) | nao | M3 |
| `CategoriaReveladaEvent` | o jogador soube | nao | M3 |
| `TecnicaAtivadaEvent` | tecnica ligou | nao | M4 |
| `TecnicaEncerradaEvent` | tecnica desligou, com `StopReason` | nao | M4 |
| `HabilidadeValidandoEvent` | antes de executar | sim | M5 |
| `HabilidadeAtivadaEvent` | depois | nao | M5 |
| `MarcoAlcancadoEvent` | marco de progressao registrado | nao | M6 |
| `ProficienciaAlteradaEvent` | proficiencia mudou | nao | M6 |

**Nenhum implementado.** Eles nascem no marco indicado, junto do sistema que
os emite — nunca antes. Evento sem emissor e uma promessa que alguem vai
escutar em vao.

---

## O cuidado com evento cancelavel

Um listener que cancela `NenDespertandoEvent` pode, sem querer, tornar o
onboarding impossivel. Vale para toda integracao.

Por isso:

- **o nucleo registra o motivo do cancelamento em modo dev.** Cancelamento
  silencioso vira "a quest nao completa e ninguem sabe por que";
- **o nucleo nunca cancela os proprios eventos.** Se ele precisa recusar, ele
  recusa antes de emitir.

---

## O que NAO vira evento

- **Aura mudou.** Isso acontece a cada tick. Evento por tick e um gerador de
  lentidao com nome bonito. Aura chega ao cliente por delta throttled, e a
  quem precisa no servidor por consulta.
- **Estado interno da maquina de estados.** Transicao interna nao e API.
- **Qualquer coisa que ainda nao tenha consumidor.** Ver a tabela acima: o
  evento nasce com o sistema.
