# De onde veio `dev-v1.dat`

**Este arquivo nao foi construido por codigo de teste.** Ele foi gravado pelo
servidor dedicado do Minecraft, salvando um jogador de verdade que estava
conectado.

E essa e a diferenca que importa. Um teste que grava com o mesmo codigo que le
prova que o codigo e consistente consigo mesmo. Este arquivo prova que o
NeoForge, o Minecraft e o nosso attachment produzem — juntos, no caminho real
de save — um `nenfoundation:nen_persistente` que ainda conseguimos ler.

## Conteudo

Jogador `Dev`, schema v1, com quatro tecnicas desbloqueadas:
`ten`, `ren`, `zetsu`, `gyo`. Sem categoria, sem despertar — os comandos que
fazem isso sao do M3.

## Como regerar

```bash
# 1. configure run/server/server.properties com:
#      online-mode=false
#      enable-rcon=true
#      rcon.password=dev
#      rcon.port=25575
#      level-name=mundo-de-regressao
./gradlew runServer

# 2. noutro terminal, um cliente que entra sozinho:
./gradlew runClient -PentrarEm=localhost:25565

# 3. noutro terminal, por RCON:
#      nen technique unlock nenfoundation:ten Dev
#      ... as demais ...
#      save-all flush
#      stop

# 4. copie o .dat:
cp run/server/mundo-de-regressao/playerdata/<uuid>.dat \
   src/test/resources/saves/playerdata/dev-v<N>.dat
```

O uuid varia com o nome do jogador; com `online-mode=false` ele e derivado do
nome, entao `Dev` sempre da o mesmo.

## Quando acrescentar outro

Quando `PersistentNenData.SCHEMA_ATUAL` subir. O portao
`FixturesDeSaveTest` ja cobra fixture por versao de schema para o formato
SNBT; este diretorio e a versao mais forte da mesma ideia, e cresce junto.
