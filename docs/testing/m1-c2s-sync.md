# Verificação das issues #5 e #43

Branch: `codex/m1-c2s-sync`. Contratos de save e payload permanecem v1.

## Cobertura adicionada

- `LimiteDePedidosTest`: rajada de 500 pedidos, recuperação após janela,
  limite de pendências com servidor parado, invalidação de geração sem renovar
  cota, isolamento entre conexões e alteração da cota em uso.
- `PedidosC2STest`: os três payloads passam pela mesma barreira; excesso não
  chega a `enqueueWork`; cada recusa produz feedback; sessão encerrada não é
  recriada e falha de agendamento libera a vaga.
- `ValidacaoDePedidoTest`: estado, despertar, existência, unlock, cooldown e
  traduções. Os caminhos com definição existente usam um catálogo simulado;
  não provam integração com registros M4/M5.
- `NenProfileServiceTest`: aviso apenas após escrita, ausência de aviso em
  no-op e isolamento entre armazenamentos.
- `NenRedeGameTest`: mutação de attachment real chega ao segundo snapshot,
  no-op não envia, outro jogador não recebe e canal ausente é respeitado.
  Também exercita recusa C2S com jogador real, id desconhecido, alvo inexistente
  e posição NaN. O transporte do snapshot é capturado no listener; não é socket.

## Execução local — 11/09/2026

- JDK Microsoft 21.0.7, NeoForge 21.1.250, perfil dev-minimal.
- `gradlew.bat build runGameTestServer`: verde; 97 testes unitários e os
  6 GameTests obrigatórios aprovados.
- `gradlew.bat runServer`: servidor dedicado chegou a `Done (11.822s)`,
  sem jogadores conectados. Não equivale a uma sessão multiplayer validada.
- Canários deliberados: retirar a barreira de admissão, uma tradução e o
  aviso de mutação fez a suíte filtrada reprovar (7 falhas em 16 testes).
  Manter apenas o aviso removido também reprovou o GameTest do segundo
  snapshot. Todas as quebras foram restauradas antes da execução verde.
- `git diff --check`: sem erros. Sem dependências novas nem mudança de contrato.

## Verificação manual restante

Com cliente real conectado e `dev.enabled=true`, executar
`/nen technique unlock nenfoundation:ten <jogador>` e conferir o segundo
snapshot no log do cliente. Repetir o mesmo comando não deve aumentar a
contagem. Conferir a action bar recebendo recusa, com debug desligado.

Spam pela rede real, dois clientes simultâneos, latência e integração de
ativação com motores futuros não são provados pelos testes unitários nem pela
captura de transporte. A matriz de dois jogadores continua sendo a #9.
