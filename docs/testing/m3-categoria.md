# Despertar, categoria e afinidade — evidências da M3

Execução em 2026-09-11. NeoForge 21.1.250 / Minecraft 1.21.1, Temurin 21.
**Servidor dedicado da instância** (`instancia/servidor`, JAR instalado — não
`runServer`), com **dois clientes reais** em loopback: `Gon` no clone principal
e `Kurapika` na worktree `C:/dev/dark-continent-awakening-qa-client2`.

Comandos por RCON (`127.0.0.1:25575`), como manda a
[qa-matrix](qa-matrix.md#um-jogador-de-verdade-sem-ninguem-clicar): etapa manual
não acontece toda vez.

---

## Gate de saída, item a item

| Item do gate | Como ficou |
| --- | --- |
| Duas pessoas com categorias diferentes e persistentes | **sim** — `Gon=emission`, `Kurapika=conjuration`, simultâneos |
| Categoria pode existir escondida até a revelação | **sim** — provado no log do cliente, ver abaixo |
| Consulta de afinidade é determinística | **sim** — 214 testes JUnit, incluindo 60 000 sorteios |
| Quest dispara o despertar por API ou comando, nunca escrevendo NBT | **sim** — `/nen awaken` chama `NenAwakeningService`; nada escreve o attachment direto |

---

## A categoria escondida, no log do cliente

É o item central do M3, e é o único que não dá para provar pelo servidor: a
pergunta é o que o **cliente** consegue saber.

Sequência no `Gon`, com o servidor tendo `category=emission` desde o snapshot #3:

```
snapshot recebido #1: categoria=undetermined  marcos=0    <- antes de despertar
snapshot recebido #2: categoria=undetermined  marcos=1    <- depois de /nen awaken
snapshot recebido #3: categoria=undetermined  marcos=1    <- DEPOIS de /nen category set emission
snapshot recebido #4: categoria=emission      marcos=2    <- depois de /nen category reveal
```

E o servidor, no mesmo instante do snapshot #3:

```
NenProfile{schema=1, awakened=true, category=emission, revealed=false, ...}
```

**O servidor sabia `emission` e o cliente recebeu `undetermined`.** É este o
comportamento que faz a Water Divination descobrir em vez de criar; sem ele, um
cliente modificado leria a categoria antes da hora e a revelação viraria teatro.

O marco `nenfoundation:categoria_revelada` só aparece no snapshot #4 — ele viaja
até o cliente, então não pode existir antes de o jogador saber.

---

## Dois jogadores, categorias independentes

```
There are 2 of a max of 20 players online: Gon, Kurapika

Gon      -> category=emission,    revealed=true,  flags=[categoria_revelada, despertou]
Kurapika -> category=conjuration, revealed=true,  flags=[categoria_revelada, despertou]
```

Revelar um não revelou o outro; atribuir a um não mudou o outro.

---

## Recarga de datapack com jogadores online

`/reload` executado com os dois conectados. Perfis comparados antes e depois,
campo a campo: **idênticos**.

```
antes  Gon      NenProfile{... category=emission,    revealed=true, flags=[categoria_revelada, despertou]}
antes  Kurapika NenProfile{... category=conjuration, revealed=true, flags=[categoria_revelada, despertou]}
depois Gon      NenProfile{... category=emission,    revealed=true, flags=[categoria_revelada, despertou]}
depois Kurapika NenProfile{... category=conjuration, revealed=true, flags=[categoria_revelada, despertou]}
```

Numa sessão anterior, com o servidor vazio, também foi verificado que um
datapack sobrescrevendo `nen_afinidade/matriz.json` **muda o resultado da
afinidade sem recompilar**, e que remover o datapack volta ao valor distribuído.

---

## As seis categorias por comando

```
pedido=enhancement    -> gravado=enhancement    revelado=false
pedido=transmutation  -> gravado=transmutation  revelado=false
pedido=emission       -> gravado=emission       revelado=false
pedido=conjuration    -> gravado=conjuration    revelado=false
pedido=manipulation   -> gravado=manipulation   revelado=false
pedido=specialization -> gravado=specialization revelado=false
```

`set` **nunca** revelou. E o sorteio determinista respondeu pelo mesmo caminho
que atribui:

```
Semente 12345 sorteia emission.
Gon recebeu a categoria emission, ainda ESCONDIDA dele.
```

---

## Sem FTB Quests, e sem nenhum mod de conteúdo

```
instancia/servidor/mods/
  nenfoundation-0.1.0.jar        <- e mais nada
```

**O item do FTB não é sobre o FTB: é sobre o ADR-003.** Despertar, atribuir e
revelar funcionaram com o núcleo sozinho. Se algum deles passasse a exigir um
mod de conteúdo, o sintoma seria o servidor não subir no perfil dev-minimal.

No log do servidor, durante a sessão inteira:

- `ERROR` / `Exception` / `NoClassDefFoundError`: **0 ocorrências**
- referência a `net.minecraft.client`: **0 ocorrências**

---

## O que este registro NÃO prova

Escrito aqui porque a próxima pessoa vai ler a tabela acima e parar.

- **Ninguém fez a Water Divination nesta sessão.** O ritual tem gametest e foi
  jogado à mão noutra ocasião, mas não com dois jogadores nem aqui. Em
  particular, **se as seis reações são perceptivelmente distinguíveis** continua
  sem resposta: os portões provam que partícula, som e frase são *diferentes*,
  não que dão para distinguir olhando.
- **O gatilho do ritual ainda não é Ren.** *(Resolvido depois, na issue #90:
  Ren chegou no M4, o teste que fixava a dívida reprovou como prometido, e a
  condição passou a exigir Ren sobre o copo. Mantido aqui porque este documento
  registra o que era verdade no gate do M3.)*
- **O log do cliente do `Kurapika` não registra snapshot**, porque aquela
  worktree está com `dev.enabled = false`. A prova do escondido saiu do log do
  `Gon` — mesmo jogador, antes e depois da revelação, que é evidência melhor que
  dois jogadores com um log cada.
- **Não houve relog.** A persistência da categoria entre sessões é coberta por
  teste de attachment e pelo gametest, não por este registro.
- **Memória:** a sessão rodou com **0,65 GB livres** no pico, de 15,9 GB. O
  limite registrado na [qa-matrix](qa-matrix.md) — dois clientes não sobem com
  ~1,9 GB livres — continua valendo; aqui só coube porque os daemons do Gradle
  foram parados antes (`gradlew --stop`).
- **Nada aqui mede desempenho.** Não houve medição de TPS nem perfil de spark.

---

## De quebra: um ponto cego do #62 fechado

O PR da issue #62 declarou que **o comportamento das variantes `<alvo>` dos
comandos não tinha sido exercitado** — `makeMockServerPlayerInLevel` cria
jogadores que compartilham o nome, então `EntityArgument` resolvia sempre o
primeiro, e o gametest teve de rodar sem alvo explícito.

Nesta sessão todos os comandos foram executados **com alvo explícito**
(`/nen awaken Gon`, `/nen category set emission Gon`, `/nen debug profile
Kurapika`) contra dois jogadores reais e distintos, e cada um atingiu quem
deveria. O ponto cego está fechado.
