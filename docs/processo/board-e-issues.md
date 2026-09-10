# Board, issues e prioridade de bug

## Colunas do board

| Coluna | Definicao |
| --- | --- |
| Backlog | aceito no escopo atual, contrato ainda nao pronto |
| Ready | issue com criterio de aceite, arquivos esperados e owner |
| Dev A | em implementacao pela Dev A |
| Dev B | em implementacao pela Dev B |
| Review | PR esperando a outra pessoa |
| Server QA | build que precisa rodar em servidor dedicado |
| Blocked | dependencia explicita, com link. Nunca "parado" sem causa |
| Done | mergeado na `main` e passou no gate do marco |

**Server QA e uma coluna propria de proposito.** Sem ela, "funcionou no meu
`runClient`" vira o criterio de pronto — e a classe de defeito que so aparece em
servidor dedicado e justamente a mais cara deste projeto.

---

## Quando abrir issue ANTES de codar

Qualquer um destes basta:

- a mudanca envolve **mais de uma decisao** que alguem poderia questionar
  depois;
- **algo fora do seu alcance bloqueia parte do trabalho** (conta, credencial,
  aprovacao, decisao da outra pessoa);
- o trabalho **nao cabe numa revisao de uma sentada**;
- ja se sabe de **divida que ficara para depois**.

**Divida descoberta no meio do caminho vira issue no MESMO dia.** Divida que so
existe na cabeca de quem escreveu nao existe: some junto com o contexto.

**Uma issue por assunto.** Se o titulo precisa de um "e", provavelmente sao
duas.

---

## Os cinco campos de uma issue

O quarto e o quinto sao os que sempre faltam.

```
Titulo: [M4] Implementar o ciclo de vida de Zetsu
Owner: Dev B
Contrato do qual depende: NenTechnique v1 (ja congelado)
Arquivos principais: nen/technique/zetsu/*, client/hud/*
Arquivos hostis a merge tocados: nenhum

1. Objetivo
   O jogador consegue suprimir a propria aura, e as tecnicas incompativeis
   sao canceladas por regra e nao por condicional espalhada.

2. Entregas
   - [ ] Zetsu implementando NenTechnique
   - [ ] declaracao de incompatibilidade nos DOIS lados
   - [ ] mensagens de StopReason
   - [ ] teste de morte e de logout
   - [ ] smoke test em servidor dedicado

3. Criterio de aceite
   - ativa e desativa no servidor
   - cancela Ten e Ren, e impede a ativacao deles enquanto estiver ligada
   - sincroniza corretamente para o proprio cliente, e nao para os outros
   - nenhum estado fantasma depois de morte ou logout

4. Fora de escopo
   In, stealth avancado, percepcao por NPC, efeito visual definitivo.

5. Bloqueios, com nome
   Nenhum. (Se houvesse: "depende de a Dev A mergear o PR do scheduler
   central de tick" — nunca "depende de infra".)
```

Sem o campo 4, uma frente invade a seguinte e ninguem percebe ate a revisao.

Sem o campo 5 com **nome**, o bloqueio nao e acionavel. "Depende de aprovacao"
nao serve; "depende de a pessoa X decidir Y" serve.

---

## Prioridade de bug

| | Exemplo | Acao |
| --- | --- | --- |
| **P0** | crash, corrupcao de save, duplicacao, exploit de autoridade | bloqueia qualquer gate seguinte |
| **P1** | tecnica ou habilidade principal nao funciona; desync reproduzivel | corrigir antes de fechar o marco |
| **P2** | balanceamento, FX errado, UX ruim sem travar gameplay | polimento antes do RC |
| **P3** | cosmetico, melhoria, ideia | backlog pos-MVP |

---

## O que uma entrega precisa declarar

Toda entrega — PR, relato, mensagem — termina dizendo **o que ficou sem prova**.

Nao e confissao de fracasso. E a parte que impede a proxima pessoa de assumir
que o verde cobre mais do que cobre. Exemplos do formato:

- "o contrato roda contra duble, nao contra a dependencia real";
- "isto nunca rodou em servidor dedicado, so em `runClient`";
- "os numeros de custo sao chute; nunca foram medidos".

Regra pratica: se voce escreveria *"acho que funciona"*, escreva **por que**
acha, e **o que faltou** para saber.

O arquivo [`docs/testing/o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md)
guarda a versao acumulada disso.

---

## Nunca afrouxar a verificacao para ficar verde

Proibido, sem excecao:

- desligar uma protecao "temporariamente";
- apagar ou pular teste vermelho em vez de entender por que;
- baixar o nivel de um scanner para esconder achado;
- adicionar supressao de lint sem comentario dizendo o motivo;
- trocar uma asercao por outra mais fraca so para parar de reclamar.

**Ou a verificacao esta errada e conserta-se a verificacao, ou o codigo esta
errado e conserta-se o codigo.** Nao ha terceira saida.

Corolario: portao que dispara em falso positivo se conserta com a mesma
urgencia de um que falha em negativo. Os dois destroem a confianca, e sem
confianca ninguem le a saida.
