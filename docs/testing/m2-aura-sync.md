# Aura e sync — evidências da M2

Execução em 2026-09-11, clone `C:\dev\dark-continent-awakening`.
NeoForge 21.1.250 / Minecraft 1.21.1, Temurin 21, dois clientes reais em
loopback. Mundo descartável `m2-qa`; nenhum save anterior foi sobrescrito.

## Integração corrigida

As peças anteriores compilavam, mas nenhum subsistema registrava regeneração
e sync no scheduler. O motor agora é registrado e removido no ciclo de vida do
servidor. Capacidade enviada e reserva validada vêm do mesmo runtime.
O contexto de Nen consulta o perfil e a config no instante da operação.

## Contrato implementado

- Perfil não desperto: reserva e output zero; 0/0 não é exaustão.
- Desperto: capacidade = base configurada + potencial persistente; output =
  base configurada + output persistente. Aumentar capacidade não concede Aura;
  reduzir aplica teto imediatamente.
- Recuperação = regeneração por segundo / 20 ticks de simulação.
- Gastos instantâneos no mesmo tick compartilham output. Técnicas contínuas,
  controle/eficiência e modificadores de estado ganham consumidores nos marcos
  próprios. Não existe stamina adicional.
- Negativo, zero, NaN, infinito, falta de reserva ou output excedido não debitam
  Aura. A reserva fica em `[0, máxima]`, sempre finita.
- Runtime nasce neutro e não persiste. Exaustão deriva de reserva zero com
  capacidade positiva; não há booleano paralelo para divergir.

## Medições reais

Config inicial: capacidade base 100, regeneração 1/s, output base 10,
sync 5 ticks e interpolação 5 ticks. As fixtures v1 despertas têm potencial
42,5 e output 1,25: máximo 142,5 e output 11,25.

| Cenário | Resultado |
| --- | --- |
| Gon, pool cheio, ticks 8864 → 8987 | enviados 825 → 825; cliente recebeu 825 |
| Kurapika, pool cheio, ticks 4655 → 4777 | enviados 309 → 309; cliente recebeu 309 |
| Gasto 12 com output 11,25 | recusa por output; reserva preservada |
| Gasto 1 permitido | reserva 142,5 → 141,5; recuperação até o teto |
| Reserva 1, gasto 1, novo gasto 1 | exaustão registrada; segundo gasto recusado |
| Gon 0, Kurapika 71,25, regeneração 0 | reservas independentes, sem vazamento |
| Recarga de base 100 → 80 e output 10 → 5 | ambos os jogadores existentes: máximo 122,5 e output 6,25, sem reinício |
| Recarga de regeneração 1 → 0 → 1 | recuperação para e retorna sem recriar jogadores |
| Servidor a 2 TPS, depois 20 TPS | delta e HUD atualizados; número bruto não é interpolado |
| Kurapika sai e reconecta em novo processo | snapshot #1, delta #1, reserva 0/142,5, sem runtime anterior |

Capturas inspecionadas e perfil spark em [perfis](perfis/README.md).
Somente o preenchimento é interpolado; o número é autoritativo.

## Testes automatizados

Execução final de código: **131 testes JUnit e 7 GameTests aprovados**.

- `MotorDeAuraTest`: config viva, output independente da reserva, orçamento
  compartilhado no tick, invalidez, exaustão, isolamento e métricas dev.
- `AuraPoolTest`: invalidez sem mutação, teto dinâmico e recuperação extrema
  sem overflow intermediário.
- `ControleDeSyncTest`: 100 ticks limpos sem pacote; 100 alterados com 20
  deltas na cadência 5; canal ausente/exceção preservam dirty; reentrância e
  alterações menores que a precisão do float.
- `AuraRegressaoTest`: atraso, frames fracionários, deltas sobrepostos,
  máximo reduzido, payload inválido atômico e limpeza do cache.
- `NenAuraGameTest`: 120 ticks pelo evento global real, dois ServerPlayers,
  config carregada, gasto/recuperação, canal recusado/reaberto e reset.
  Seu listener captura envelopes; não substitui a rede real medida acima.

## Reprodução

1. Configure Java 21; rode `./gradlew build` e `./gradlew runGameTestServer`.
   Exija contagem não zero, resumo dos GameTests e ausência de crash.
2. Use mundo novo. `PrepararQaM2`, nas fontes de teste, copia fixtures v1 para
   `run/server/m2-qa/playerdata` e recusa sobrescrita. Não integra o JAR nem
   adiciona comando de despertar ao produto.
3. Ligue `dev.enabled` só em QA. Inicie servidor e dois clientes:
   `./gradlew runClient '-PentrarEm=127.0.0.1:25585' '-Pjogador=Gon'`,
   repetindo com Kurapika. As aspas importam no PowerShell.
4. Como operador, consulte `/nen debug aura Gon`; subcomandos
   `gastar <quantidade> Gon` e `definir <quantidade> Gon` alteram só runtime,
   com validação. Nenhum deles desperta ou modifica perfil persistente.
5. Compare diferença de ticks e deltas nas duas pontas. Menos de 100 ticks
   não prova o cenário de estabilidade.

## Limites

Loopback e TPS reduzido não simulam perda/reordenação TCP nem Internet ruim.
A reconexão real usou novo processo; limpeza na mesma instância tem teste de
cache. Capturas não medem fluidez percebida. Spark é linha de base leve, não
aprovação de estresse ou modpack. Despertar e técnicas não pertencem à M2.

A revisão automática recusou a remoção temporária do registro de produção
para provocar falha. Essa mutação não foi aplicada. Os controles negativos
executados usam entradas inválidas e falhas de transporte isoladas; não se
afirma que a variante com registro removido foi executada.
