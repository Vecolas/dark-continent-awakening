# ADR-001 — O servidor e a autoridade sobre todo estado de Nen

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

Nen toca exatamente as coisas que um cliente modificado quer tocar: dano,
recurso, recarga, alcance e alvo. Um modpack de Hunter x Hunter jogado com
amigos num servidor dedicado tem PvP por construcao.

A tentacao arquitetural e deixar o cliente calcular — ele ja sabe quanto custa
Ren, ja tem o perfil para desenhar o HUD, e o servidor so confirmaria. Essa
arquitetura funciona perfeitamente ate o dia em que nao funciona, e a falha e
silenciosa: com um cliente honesto, tudo bate.

## Decisao

O servidor decide **todo** estado de Nen. O cliente envia intencao e desenha o
que recebe.

Concretamente:

1. Nenhum payload C2S carrega aura, dano, cooldown, unlock ou multiplicador.
   Ver `docs/multiplayer/protocol.md`.
2. Alvo chega como id de rede. O servidor resolve o id no proprio mundo e
   reconfere distancia, dimensao e linha de visao.
3. O HUD nunca recalcula regra. Ele desenha o numero que veio no delta.
4. Todo handler C2S tem rate limit por jogador.
5. KubeJS e datapack nao validam nada de seguranca. Numero sai de dado; regra
   fica em Java.

## Custo assumido

- **Latencia visivel.** Apertar a tecla e ver o efeito custa uma ida e volta. Em
  conexao ruim, Ten "demora". A mitigacao e feedback local imediato de INPUT
  (o icone acende) separado do feedback de ESTADO (a barra muda quando o
  servidor confirma). Nunca prever o resultado no cliente.
- **Mais codigo.** Cada acao existe em dois lados: intencao no cliente,
  validacao no servidor. E mais trabalho que um metodo so.
- **Mais trafego.** Deltas de aura sobem a contagem de pacotes. O plano ja
  prevê throttle, delta e interpolacao, e isso vira medicao no M2 e no M7.
- **Nao da para "so testar rapido" no cliente.** Toda experiencia de gameplay
  passa por servidor, inclusive em desenvolvimento.

## O que NAO muda

- O cliente continua dono da apresentacao inteira: HUD, particula, som,
  animacao, interpolacao e feedback de input. Nada disso vira responsabilidade
  do servidor.
- Singleplayer continua funcionando: no Minecraft ele roda um servidor interno,
  e o mesmo caminho de codigo vale.
- Esta decisao **nao** implica validar tudo a cada tick. Validar caro e raro
  (na ativacao) e barato e frequente (no tick) continua sendo a otimizacao
  correta.

## Fontes

- [NF-4] https://docs.neoforged.net/docs/1.21.1/networking/payload/
