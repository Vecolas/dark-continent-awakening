# Gate do M4 — as técnicas fundamentais em servidor dedicado

**Issue:** #91 · **Estado:** procedimento escrito, **execução manual pendente**

Este documento é o roteiro do gate, não o registro dele. Ele vira registro
quando alguém executar e preencher as colunas de evidência — e uma linha sem
evidência preenchida conta como **não verificada**, nunca como aprovada.

---

## O que o M4 entregou, e o que ele não entregou

O gate original falava em **quatro** técnicas. São **três**.

| Técnica | Estado | Onde |
| --- | --- | --- |
| Ten | entregue | #86 |
| Ren | entregue | #87 |
| Zetsu | entregue | #125 |
| **Gyo** | **não existe** | #126, com bloqueios nomeados |

Gyo ficou de fora porque as duas metades dele — concentrar aura numa região e
perceber o que está escondido — não têm substrato: não há modelo de alocação de
aura, não há camada de percepção, e `nen/combat/` tem só o `package-info`.
Implementar assim mesmo produziria uma técnica que custa aura e não faz nada
observável.

**Este gate cobre três técnicas.** Dizer "as quatro passaram" seria falso verde
sobre uma técnica que não existe.

---

## O que já está automatizado

Rodar antes de começar; se algum destes falhar, a QA manual não deve começar.

```bash
./gradlew build            # 263 testes unitários e portões
./gradlew runGameTestServer # 68 gametests, servidor de verdade
```

| Critério do gate | Cobertura automática | O que ela **não** prova |
| --- | --- | --- |
| Exclusão Ten/Ren ↔ Zetsu nas duas ordens | `zetsuETenSeExcluemNasDuasOrdens` | que a roda mostra a queda ao jogador |
| Quem abaixa o teto vence quem levanta | `quemAbaixaVenceQuemLevanta` | nada em jogo exercita isto: Zetsu exclui Ren |
| Aura zero encerra a técnica | `tenCaiQuandoAAuraAcaba`, `semAuraAsTecnicasCaemAteSobrarOQueCabe` | o `StopReason` visto pelo jogador |
| Zetsu fecha o Output e devolve ao desligar | `zetsuFechaOOutputEDevolveAoDesligar` | o efeito sentido em jogo |
| Zetsu drena aura | `zetsuDrenaAuraEnquantoLigado` | se a drenagem é perceptível |
| O limite simultâneo é a aura, e não um teto de slots | `aAuraEOLimiteDeTecnicasSimultaneas` | — |
| Logout/morte/dimensão desligam tudo | `desligarTodas` chamado direto nos gametests | **os handlers de evento reais**; ver dívida abaixo |

> **Dívida declarada:** os gametests de ciclo de vida chamam `desligarTodas`
> diretamente. Os handlers de `PlayerLoggedOutEvent`, `PlayerEvent.Clone` e
> `PlayerChangedDimensionEvent` em `NenPlayerLifecycle` **não** são disparados
> por teste automático. É exatamente o erro nº 3 da lista do CLAUDE.md
> ("limpeza espalhada pelos pontos de saída"), e é por isso que os itens de
> morte, logout e dimensão abaixo são manuais e obrigatórios.

---

## Preparação

O limite desta máquina já foi medido e está em
[qa-matrix.md](qa-matrix.md): com ~1,9 GB livres **dois clientes não sobem**.
Com 7,7 GB livres a matriz do M1 rodou. Antes de começar:

```bash
./gradlew --stop                 # daemons do Gradle comem memória
.\scripts\instancia.ps1 atualizar  # OBRIGATÓRIO: sem isto o teste roda o JAR velho
```

Dois clientes exigem **dois diretórios de execução** — dois `git worktree`, o
segundo podendo ser `--detach` no mesmo commit. Dois clientes no mesmo
`run/client` brigam pelo `options.txt` e pelo log, e o segundo sobrescreve o
diagnóstico do primeiro.

Jogadores sugeridos: `Gon` e `Kurapika`. Com `online-mode=false` o UUID vem do
nome, então são jogadores de verdade com perfis separados.

---

## Roteiro

Cada linha tem um resultado esperado escrito **antes** da execução. Preencher a
coluna de evidência com o que apareceu na tela ou no log — não com "ok".

### A. As três em servidor dedicado

| # | Passo | Esperado | Evidência |
| --- | --- | --- | --- |
| A1 | Entrar com `Gon`, despertar (`/nen awaken` ou ritual) | as três aparecem ao segurar **R** | |
| A2 | Ligar Ten | indicador de Ten acende no HUD e **continua** aceso ao soltar R | |
| A3 | Observar a aura por ~20 s | cai devagar e a regeneração está maior que em repouso | |
| A4 | Ligar Ren junto de Ten | os dois acesos; a aura cai **bem** mais rápido | |
| A5 | Desligar os dois | os indicadores somem | |

### B. Combinações inválidas, uma a uma

A matriz é pequena o bastante para ser exaustiva. **Nenhuma linha pode ser
pulada** — uma exclusão declarada pela metade passa numa ordem e falha na
outra, e a ordem em que se testa costuma ser sempre a mesma.

| # | Ligar nesta ordem | Esperado | Evidência |
| --- | --- | --- | --- |
| B1 | Ten → Zetsu | Ten **cai**, Zetsu fica | |
| B2 | Zetsu → Ten | Zetsu **cai**, Ten fica | |
| B3 | Ren → Zetsu | Ren **cai**, Zetsu fica | |
| B4 | Zetsu → Ren | Zetsu **cai**, Ren fica | |
| B5 | Ten + Ren → Zetsu | **as duas** caem | |
| B6 | Ten → Ren | os dois ficam (convivem de propósito) | |

### C. Aura zero

| # | Passo | Esperado | Evidência |
| --- | --- | --- | --- |
| C1 | Ligar Ren e esperar a aura zerar | Ren desliga sozinho; o indicador some | |
| C2 | Conferir o log do servidor | motivo `OUT_OF_AURA` | |
| C3 | Tentar religar com aura zero | liga e cai de novo no tick seguinte, sem travar | |

### D. Pontos de saída — o item mais importante

Aqui mora o erro nº 3 do CLAUDE.md, e nenhum teste automático cobre os handlers
reais.

| # | Passo | Esperado | Evidência |
| --- | --- | --- | --- |
| D1 | Ligar Ren e **morrer** | ao renascer, nenhum indicador aceso | |
| D2 | Após D1, conferir o Output | voltou ao teto de repouso, **não** ao de Ren | |
| D3 | Ligar Ren, **deslogar e voltar** | nenhum indicador aceso; aura recomeça | |
| D4 | Ligar Ren, **ir ao Nether** | os indicadores somem na travessia | |
| D5 | Após D4, o Output | de repouso, e não o elevado | |

> **D2 e D5 são o ponto cego deste gate.** Um teto elevado que sobrevive ao
> ponto de saída não dá erro nenhum: o jogador simplesmente fica com o limite
> de quem está em Ren, para sempre, sem nada acusar. Se a barra de Output
> parecer normal, medir mesmo assim com `/nen dump` ou o overlay de debug.

### E. Dois jogadores ao mesmo tempo

| # | Passo | Esperado | Evidência |
| --- | --- | --- | --- |
| E1 | `Gon` liga Ren, `Kurapika` liga Zetsu | cada HUD mostra só o seu | |
| E2 | `Gon` desliga tudo | o HUD de `Kurapika` não muda | |
| E3 | `Kurapika` morre | o estado de `Gon` não é afetado | |

> E1–E3 existem por causa do erro nº 2 da lista do CLAUDE.md: estado de jogador
> num campo da classe da técnica. As implementações são singleton, e dois
> jogadores escrevendo no mesmo campo **não dá erro** — dá estado trocado.

### F. O ritual, agora exigindo Ren (#90)

| # | Passo | Esperado | Evidência |
| --- | --- | --- | --- |
| F1 | Caldeirão com água, folha na mão, clicar **sem Ren** | *"A água fica parada. Falta aura sobre ela."* | |
| F2 | Ligar Ren e clicar | o ritual roda e revela a categoria | |
| F3 | Clicar de novo com Ren | mesma categoria, **sem** reanunciar | |

### G. Revisão cruzada

| Quem revisa | O quê | Feito |
| --- | --- | --- |
| Dev A | Zetsu (e Gyo, que não existe — revisar os bloqueios de #126) | |
| Dev B | Ten e Ren | |

---

## O que este gate não vai provar

Escrito antes da execução, para não virar racionalização depois.

- **Gyo.** Não existe (#126).
- **A vulnerabilidade de Zetsu.** Não há dano de Nen; hoje o único custo real
  dele é não liberar Output (#127).
- **Balanceamento.** Os números são config e mudam numa sessão de tuning; este
  gate prova *comportamento*, não se Ren custa caro demais.
- **A ordem limitador/levantador em jogo.** Zetsu exclui Ren, então nada em
  partida exercita as duas passagens juntas. A prova é só o gametest
  `quemAbaixaVenceQuemLevanta`, com técnicas falsas que convivem.
- **Desempenho sob carga.** M7.
- **Ícones e FX por técnica.** Não existem (#89); as formas do HUD são
  desenhadas em código.
