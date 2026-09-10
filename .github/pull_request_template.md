<!--
Título: <tipo>: <assunto no imperativo, minúsculo, sem ponto final>
Se toca um arquivo hostil a merge, diga no título:
    feat: aura pool e regeneracao [toca NenFoundation.java]
-->

## O que muda

<!-- Uma frase sobre o que passa a ser possível. -->

Issue: #

## Marco

<!-- M0..M8, ou "fora de marco" com o motivo. -->

## Arquivos hostis a merge tocados

<!-- build.gradle, gradle.properties, settings.gradle, NenFoundation.java,
     NenProtocol.java, NenConfig.java, registry/*, lang/*.json, workflows/*.
     Escreva "nenhum" se for o caso. -->

## Mudança de contrato

- [ ] Nenhuma
- [ ] Schema de save (`schemaVersion` subiu? degrau de migração? fixture?)
- [ ] Protocolo de rede (`NenProtocol.VERSION` subiu? `protocol.md` atualizado?)
- [ ] Interface pública (`api/*`)
- [ ] Chave de config (o número **saiu** do código?)

<!-- Se marcou qualquer coisa além de "Nenhuma", explique aqui. -->

## O que eu rodei

- [ ] `./gradlew build` verde, e a saída diz `Testes executados: N` com N > 0
- [ ] `./gradlew runServer` (**obrigatório** se o PR toca o núcleo)
- [ ] `./gradlew runClient`
- [ ] Smoke test da [matriz de QA](../docs/testing/qa-matrix.md)
- [ ] Servidor dedicado com duas pessoas

## O que NÃO foi verificado

<!-- OBRIGATÓRIO. Não é confissão de fracasso — é o que impede a próxima pessoa
     de assumir que o verde cobre mais do que cobre.

     Exemplos do formato certo:
       "isto nunca rodou em servidor dedicado, só em runClient"
       "os números de custo são chute; nunca foram medidos"
       "o teste roda contra fixture, não contra um save real"

     Se você escreveria "acho que funciona", escreva POR QUE acha e O QUE
     faltou para saber. Se realmente não há nada, escreva "nada". -->

## Checklist

- [ ] Comportamento novo tem teste, e o teste **reprova sem o código**
- [ ] Portão novo (se houver) foi alimentado com um caso que deve reprovar, e
      reprovou
- [ ] Recusa e erro têm mensagem para o jogador, com chave de tradução
- [ ] Estado criado tem limpeza no ciclo de vida de quem o criou
- [ ] Nada de `client/*` nem de `net.minecraft.client.*` foi alcançado pelo
      núcleo
- [ ] Número ajustável está em config **e saiu do código**
- [ ] Dependência nova está em `docs/modpack/supported-mods.md` com pacote,
      versão, licença, motivo e URL
- [ ] ADR atualizado se a arquitetura mudou (com **custo assumido** e **o que
      NÃO muda**)
- [ ] `git diff --cached --name-only` conferido antes do commit — nada da outra
      frente entrou junto
