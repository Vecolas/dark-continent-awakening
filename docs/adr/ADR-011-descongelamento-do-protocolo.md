# ADR-011 — O protocolo de rede descongela, com versao e regra

## Contexto

O [ADR-004](ADR-004-identidade-congelada.md) congelou, entre outras coisas,
**os ids e direcoes de payload e a versao do protocolo**. O CLAUDE.md repete a
regra: descongelar exige ADR novo apontando para o ADR-004.

**A regra ja foi quebrada, e o portao nao pegou.**

O PR #78 mudou `NenProtocol.VERSION` de `1` para `3`, acrescentou o payload
`adjust_output_request` (C2S) e mudou a forma do `nen_runtime_delta`, que passou
a carregar `auraMaxima` e `outputPercent`. Nenhum ADR foi escrito.

O `ProtocoloCongeladoTest` continuou verde -- e continuou **certo**. Ele confere
se o codigo e o documento CONCORDAM sobre id, direcao e versao. Ele nao confere
se a mudanca foi **autorizada**, e nao tem como: autorizacao nao esta no codigo.

Isso e um ponto cego do portao, e nao um defeito dele. Um portao que tentasse
adivinhar autorizacao a partir do diff produziria falso positivo em todo rename.

E a issue #71 -- ajustar Output por intencao, em vez de por percentual
arbitrario do cliente -- **precisa** mudar o formato de novo: o payload atual
carrega um `float` livre, e o desenho correto carrega uma intencao. Ela esta
bloqueada por este ADR nao existir.

## Decisao

**O protocolo de rede sai do congelamento do ADR-004 e passa a ser versionado.**

Em regras concretas:

1. **`NenProtocol.VERSION` e a unica fonte da versao**, e sobe sempre que um
   payload muda de formato, some, nasce ou troca de direcao. Nao ha versao por
   payload: uma so, do protocolo inteiro.

2. **Mudar o protocolo nao exige ADR novo a cada vez.** Exige subir a versao,
   atualizar `docs/multiplayer/protocol.md` no mesmo commit, e passar pelo
   `ProtocoloCongeladoTest`. O que o ADR-004 congelava -- a proibicao de mudar
   -- e substituido por um procedimento.

3. **A versao e comparada no handshake, e a recusa tem motivo.** Cliente e
   servidor com versoes diferentes nao tentam conversar: o jogador le que a
   versao do mod nao bate, em vez de ver campos lidos errado. *Isto ainda nao
   esta implementado; ver o bloqueio abaixo.*

4. **O que continua congelado pelo ADR-004 nao e tocado por este ADR:**
   `mod_id`, package, namespace, os ids das sete categorias com o neutro no
   ordinal zero, e o `PersistentNenData` v1. **Formato de SAVE continua
   congelado.** Este ADR fala de rede, e so.

5. **Mudanca de protocolo nao e mudanca de save.** Um payload pode nascer e
   morrer entre duas versoes sem que nenhum mundo precise migrar, porque
   protocolo nao persiste. E por isso que rede pode ter procedimento e save
   precisa de degrau de migracao.

## O que NAO muda

- **O ADR-001 continua valendo integralmente.** Descongelar o formato nao
  afrouxa a autoridade: nenhum payload C2S carrega aura, dano, cooldown, unlock
  ou multiplicador, e a #71 existe justamente para tirar o `float` livre que
  hoje viaja no `adjust_output_request`.
- **O `PersistentNenData` e os ids de categoria continuam congelados** pelo
  ADR-004, com tudo que ele exige para descongelar.
- **O `ProtocoloCongeladoTest` continua obrigatorio**, e continua reprovando
  divergencia entre codigo e documento nos dois sentidos.
- **A documentacao continua sendo parte da mudanca**, e nao um passo posterior:
  codigo e `protocol.md` andam no mesmo commit.

## Custo assumido

- **Perde-se a garantia mais forte.** Enquanto o protocolo estava congelado, a
  resposta para "isto pode mudar?" era nao. Agora e "pode, com versao e
  documento" -- e procedimento e mais facil de erodir que proibicao.

- **A versao vira responsabilidade humana.** Nada obriga alguem a subir
  `VERSION` ao mudar um codec: o portao compara codigo com documento, e quem
  esquecer de subir em **ambos** passa. O sintoma seria um cliente lendo campos
  errados sem erro visivel. A mitigacao real e o item 3 -- comparar a versao no
  handshake --, que **ainda nao existe**.

- **Este ADR legitima retroativamente a mudanca do PR #78.** Isso e uma escolha:
  a alternativa seria reverter o HUD e o AOP, que funcionam e ja foram testados
  a mao. O custo de legitimar e o precedente; o custo de reverter e jogar fora
  trabalho bom por um processo que ninguem seguiu porque ninguem percebeu.

- **O salto de versao 1 para 3 fica sem explicacao.** A versao 2 nao existiu em
  nenhum commit da `main`. Fica registrado aqui em vez de virar folclore.

## Bloqueios, com nome

- **O item 3 nao esta implementado.** Nao ha comparacao de versao no handshake:
  hoje um cliente de versao diferente conecta e le o que vier. Isto precisa de
  issue propria, e ate la a versao e documentacao, nao protecao.

- **A #71 depende deste ADR**, e agora esta destravada quanto ao processo. O
  desenho dela -- intencao em vez de percentual -- continua de pe.
