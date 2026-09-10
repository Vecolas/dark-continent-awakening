# Protocolo de rede do Nen Foundation

**Este documento e o par legivel de `NenProtocol.java`.** Os dois dizem a mesma
coisa, e o portao `ProtocoloCongeladoTest` reprova se divergirem — em qualquer
direcao, payload faltando ou sobrando, direcao trocada ou versao diferente.

Se voce mudou um e nao o outro, o build fica vermelho. E de proposito: quando o
codigo e o documento discordam sobre direcao de pacote, quem executa e o codigo
e quem e lido antes de escrever codigo e o documento.

- **Versao do protocolo:** 1

A versao sobe quando um payload muda de formato, some ou troca de direcao.

---

## A regra que sustenta tudo

> **Cliente manda INTENCAO. Servidor manda ESTADO.**

O cliente diz *"quero ativar Ren"*. Nunca *"ativei Ren e gastei 12 de aura"*.

Nenhum payload C2S pode carregar aura, dano, cooldown, unlock, multiplicador ou
resultado. Se um dia parecer que um deles precisa, a resposta e nao: esse numero
e conta do servidor. Ver [ADR-001](../adr/ADR-001-servidor-autoritativo.md).

Isso nao e paranoia. E que a falha correspondente e **invisivel**: com um
cliente honesto, um protocolo inseguro funciona perfeitamente. Nenhum teste de
gameplay, nenhuma sessao de QA e nenhum playtest com amigos encontra o buraco. O
primeiro a encontra-lo e quem estiver procurando.

---

## A tabela congelada

| id | Direcao | O que pode carregar |
| --- | --- | --- |
| `activate_technique_request` | C2S | id da tecnica e contexto minimo de input |
| `deactivate_technique_request` | C2S | id da tecnica |
| `activate_ability_request` | C2S | id da habilidade, slot, alvo/posicao **candidatos** |
| `nen_profile_snapshot` | S2C | estado de leitura para a interface, so ao dono |
| `nen_runtime_delta` | S2C | aura, cooldown e tecnica alterados |
| `ability_fx_event` | S2C | som, particula, animacao |
| `nen_error_feedback` | S2C | motivo legivel de uma recusa |

Namespace de todos: `nenfoundation:`.

---

## O que cada um significa

### `activate_technique_request` (C2S)

O jogador apertou a tecla de Ten, Ren, Zetsu ou Gyo.

O servidor valida: a tecnica existe, esta desbloqueada, o jogador esta
desperto, ha aura, o estado atual permite, nenhuma incompativel esta ativa, e o
jogador nao esta enviando pacote acima do rate limit.

**Idempotente.** O jogador segura a tecla; o pedido chega varias vezes. Reentrar
numa tecnica ja ativa nao reinicia duracao nem cobra custo de novo.

### `deactivate_technique_request` (C2S)

Toggle de desligamento. O servidor decide se o desligamento e legitimo — nem
toda tecnica pode ser desligada a vontade.

### `activate_ability_request` (C2S)

O alvo vem como **id de rede de entidade**, nunca como entidade. O servidor
resolve o id no proprio mundo e reconfere distancia, dimensao, linha de visao e
se o alvo pode ser atingido. Ver `AbilityRequest`.

### `nen_profile_snapshot` (S2C)

O estado de leitura que a interface precisa. Enviado **so ao dono do perfil**.

A categoria enviada e a `categoriaVisivel()`, nao a `category()` — enviar a
categoria real antes da revelacao entrega a informacao a qualquer cliente
modificado, e a revelacao vira teatro.

### `nen_runtime_delta` (S2C)

Delta, nunca o perfil inteiro. Aura muda toda hora; mandar o perfil completo a
cada mudanca de aura e a forma conhecida de transformar quatro jogadores em
lag de rede.

O HUD **interpola** entre deltas. Ele nao pede um pacote por quadro.

### `ability_fx_event` (S2C)

Som, particula e animacao. **Nao altera nenhuma logica no cliente.** Um FX
perdido tem de produzir, no maximo, um efeito visual que faltou — nunca um
estado divergente.

### `nen_error_feedback` (S2C)

"Aura insuficiente", "voce esta em Zetsu", "em recarga".

Toda recusa manda um motivo. Recusa silenciosa produz o pior relato de bug que
existe: *"aperto a tecla e nao acontece nada"* — e nao ha log que diga qual das
oito validacoes reprovou.

E o motivo **nunca revela estado alheio**. "Alvo protegido por Ten" conta ao
atacante algo que ele nao deveria saber.

---

## Regras de implementacao

1. **Sem objeto Java arbitrario na rede.** Cada payload e um `record` de campos
   minimos com `StreamCodec` escrito a mao. [NF-4]
2. **Rate limit por jogador em todo handler C2S.** Handler sem rate limit e um
   cliente modificado derrubando o TPS sem nenhum erro no log.
3. **Handler que muta mundo ou jogador roda no contexto certo** e trata excecao
   explicitamente. Excecao engolida num handler de rede desconecta o jogador com
   uma mensagem generica e nenhum rastro.
4. **Log de debug nao imprime UUID** salvo quando o diagnostico exigir.

---

## Quem escreve o que

| Parte | Owner |
| --- | --- |
| Definicao dos payloads e envio S2C | Dev A |
| Handlers C2S e validacao | Dev A |
| Leitura no cliente e cache de HUD | Dev B |
| Testes de frequencia de pacote | Dev B |

`NenProtocol.java` e **hostil a merge**: uma pessoa por vez. Ver
[fronteira-de-arquivos.md](../processo/fronteira-de-arquivos.md).

---

## O que este documento NAO cobre

- O formato binario de cada payload — ele nasce no M1, junto dos records.
- Handshake de versao entre cliente e servidor — nasce no M1.
- Casos de abuso e como o servidor responde — pendente de especificacao nos
  marcos M1 (validacao dos handlers) e M7 (hardening e rate limit).

[NF-4]: https://docs.neoforged.net/docs/1.21.1/networking/payload/
