# Medição de sync da Aura (M2)

Esta regua responde uma pergunta específica: quantos deltas o servidor
entregou e quantos o cliente recebeu durante uma sessão. Ela não prova latência,
perda de pacote ou correção do balanceamento.

## Instrumentação

- Servidor: `NenSyncService.metricas().deltasEnviados()` conta somente payloads
  cujo canal foi aceito pelo transporte.
- Cliente: `NenClientCache.deltasRecebidos()` conta payloads entregues ao cache.
- Ambos reiniciam no ciclo de vida da sessão/servidor; não compare contadores de
  sessões diferentes.

## Cenário mínimo

1. Entrar com o canal negociado e registrar os contadores iniciais.
2. Manter o jogador parado por 100 ticks, sem mudar Aura.
3. Confirmar que nenhum delta novo foi enviado nesse intervalo.
4. Gastar ou recuperar Aura uma vez.
5. Confirmar exatamente um delta adicional no servidor e no cliente.
6. Repetir sem mudança e confirmar que o contador permanece estável.

O valor histórico deve ser arquivado junto do perfil de spark da M2. A regra
que fecha o gate é a invariância (tick limpo não gera pacote), não um número
fixo de pacotes por segundo.
