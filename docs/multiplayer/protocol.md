# Protocolo de rede do Nen Foundation

**Este documento e o par legivel de `NenProtocol.java`.** Os dois dizem a mesma
coisa, e o portao `ProtocoloCongeladoTest` reprova se divergirem — em qualquer
direcao, payload faltando ou sobrando, direcao trocada ou versao diferente.

Se voce mudou um e nao o outro, o build fica vermelho. E de proposito: quando o
codigo e o documento discordam sobre direcao de pacote, quem executa e o codigo
e quem e lido antes de escrever codigo e o documento.

- **Versao do protocolo:** 3

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
| `activate_technique_request` | C2S | id da tecnica |
| `adjust_output_request` | C2S | variacao (float, ex: +0.10) |
| `deactivate_technique_request` | C2S | id da tecnica |
| `activate_ability_request` | C2S | id da habilidade, slot, alvo/posicao **candidatos** |
| `nen_profile_snapshot` | S2C | estado de leitura para a interface, so ao dono |
| `nen_runtime_delta` | S2C | aura, auraMaxima, outputPercent, cooldown e tecnica alterados |
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

### `adjust_output_request` (C2S)

O jogador ajusta seu AOP via atalho (ex: aumentar em 10%). O servidor soma a
variacao ao Output atual e limita o resultado entre 0.0 e 1.0.

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

O campo `outputPercent` representa o AOP atual do jogador.

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

## Implementação atual

### Recepção C2S no M1 (#5)

Os três payloads estão registrados com `HandlerThread.NETWORK`. A barreira
`PedidosC2S` admite pedidos antes de `enqueueWork`; ler perfil, runtime e
entidades só acontece na thread principal. A composição injeta
`NenPedidoService`, preservando a dependência `server -> network`.

A cota `network.requestsPerSecond` é compartilhada pelos três pedidos de cada
conexão, em janela de um segundo monotônico. O padrão é 20; o intervalo de
configuração é 1–200. Há ainda um teto fixo de oito tarefas pendentes por
conexão, para limitar fila mesmo quando o servidor atrasa. Excesso recebe
`nen_error_feedback` diretamente pela conexão, sem tarefa na thread do jogo.
Cada recusa responde; o cliente apresenta o último motivo na action bar,
inclusive com debug desligado. Isso não é proteção contra saturação da própria
conexão/decodificação: essa camada permanece responsabilidade do transporte.

Login cria a cota; logout e parada do servidor a removem. Respawn e dimensão
invalidam tarefas pendentes sem renovar o orçamento. Contadores de admitidos
e recusados aparecem no logout com `dev.enabled`, sem UUID.

**Limite do marco:** ainda não há registros executáveis de técnicas ou
habilidades. Um unlock de debug não cria uma definição. Jogador não desperto,
estado inválido, id desconhecido e candidatos malformados são recusados;
nenhuma solicitação ativa estado no M1. A validação comum contém unlock e
cooldown, exercitados com catálogo simulado nos unitários. M4/M5 precisam ligar
os registros e motores reais, validar slot equipado, custo, incompatibilidades,
alcance e linha de visão conforme o spec. Não há alcance/slot fictício que
autorize uma ação enquanto esses consumidores não existem.

### Snapshot após mutação (#43)

`NenProfileService.atualizar` grava a mudança e depois notifica pelo contrato
do armazenamento. O attachment do jogador reenvia o snapshot ao próprio dono
via `NenSyncService`, com a guarda `hasChannel` existente. Mudança idêntica não
grava nem envia. O login continua enviando o snapshot inicial. Delta de aura e
cooldowns segue fora desse mecanismo (M2).

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

- O formato binário detalhado: a fonte são os records e seus `StreamCodec`.
- Aceitação de ativações pelos motores M4/M5, ainda inexistentes.
- Hardening e saturação do transporte em condições reais (M7).

[NF-4]: https://docs.neoforged.net/docs/1.21.1/networking/payload/
