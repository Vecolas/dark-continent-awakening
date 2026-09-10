# Casos de abuso

Um pack de Hunter x Hunter jogado num servidor tem PvP por construcao. Estes
casos nao sao hipoteticos.

O que os une: **nenhum deles produz erro no log.** Todos produzem uma partida
injusta que parece um problema de balanceamento.

---

## Por classe

### 1. O cliente afirma o proprio estado

| Tentativa | Defesa |
| --- | --- |
| pacote com aura arbitraria | impossivel: nenhum payload C2S carrega aura |
| pacote com dano ja calculado | impossivel pelo mesmo motivo |
| pacote com cooldown zerado | idem |
| pacote com unlock de habilidade | idem |

A defesa e o **formato do protocolo**, e nao uma verificacao. Um campo que nao
existe nao precisa ser validado. Ver [protocol.md](protocol.md) e
[ADR-001](../adr/ADR-001-servidor-autoritativo.md).

### 2. Alvo forjado

| Tentativa | Defesa |
| --- | --- |
| id de entidade em outra dimensao | o servidor resolve o id no proprio mundo |
| alvo alem do alcance do spec | reconferido contra `AbilitySpec.alcance()` |
| alvo atras de parede | linha de visao reconferida quando a habilidade a exige |
| id de entidade que nao existe | recusa com motivo |
| id do proprio jogador em habilidade hostil | regra explicita por habilidade |

**A entidade nunca vem do cliente.** Vem o id; o servidor reconstroi.

### 3. Spam

| Tentativa | Defesa |
| --- | --- |
| centenas de pacotes de ativacao por segundo | rate limit por jogador em todo handler C2S |
| ligar e desligar tecnica a cada tick | idempotencia mais custo de ativacao |
| habilidade no limite exato do cooldown, em loop | o cooldown e do servidor, em ticks de servidor |

Rate limit **corta**; nao enfileira. Enfileirar transforma spam em atraso
acumulado, que e pior.

### 4. Ciclo de vida usado como reset

| Tentativa | Defesa |
| --- | --- |
| morrer de proposito para limpar cooldown de PvP | politica explicita e testada, nunca acidente |
| deslogar e voltar para limpar estado | idem |
| trocar de dimensao para largar debuff | estado vinculado a entidade e revalidado, nao descartado |
| suicidio para sair de uma condicao de Manipulation | decisao de design consciente, escrita |

Esta e a classe mais facil de errar **por omissao**. Ninguem decide que morrer
limpa cooldown; simplesmente ninguem decidiu que nao limpa, e o comportamento
padrao vira a regra.

### 5. Vazamento de informacao

| Tentativa | Defesa |
| --- | --- |
| ler a categoria de outro jogador no proprio cliente | o snapshot vai so ao dono |
| ler a categoria antes da revelacao | o servidor envia `categoriaVisivel()` |
| ver aura alheia por tooltip do Jade | desligado por padrao |
| deduzir Zetsu pela ausencia de um pacote | o servidor nao muda a cadencia de pacote conforme o estado |
| deduzir estado alheio pelo texto de uma recusa | motivo nunca cita estado do alvo |

O penultimo e sutil e vale o cuidado: **o padrao de trafego tambem e
informacao.** Se estar em Zetsu faz parar de chegar um tipo de pacote, um
cliente modificado detecta Zetsu sem nunca receber o dado.

### 6. Recurso orfao

| Tentativa | Defesa |
| --- | --- |
| deslogar com construct no mundo | limpeza no ciclo de vida do dono |
| trocar de dimensao canalizando | encerra com `DIMENSION_CHANGE` |
| habilidade sobre alvo que morre no mesmo tick | encerra com `TARGET_LOST` |
| recarga de datapack com habilidade ativa | encerra com `DEFINITION_RELOADED` |

Nao e so seguranca: e TPS. Construct orfao nao aparece como erro, aparece como
o servidor ficando lento ao longo de uma semana.

---

## Como testar

Estes casos entram na
[matriz de QA](../testing/qa-matrix.md#cenarios-de-abuso) e sao **executados
com intencao**, no M7, por quem esta tentando quebrar — nao por quem esta
tentando confirmar que funciona.

Um cenario de abuso que "passou" porque ninguem tentou de verdade e pior que
nenhum: ele produz confianca sem produzir evidencia.

---

## Regra ao descobrir um caso novo

1. Issue com prioridade **P0** — exploit de autoridade bloqueia qualquer gate
   seguinte.
2. Acrescentar a linha nesta tabela **antes** de corrigir.
3. Corrigir.
4. Escrever o teste que reproduz o abuso e verifica a recusa.
5. Nunca corrigir sem o passo 4: sem o teste, a correcao volta a se perder no
   proximo refactor, e ninguem vai notar.
