# Arte-fonte do Foxbear

## `referencia-autoral.png`

Atlas de UV autoral, 1254×1254, entregue antes de existir modelo GeckoLib.

Ele **saiu de `src/main/resources`** no PR que deu corpo próprio ao Foxbear, e
o motivo não é organização: a folha declarada no `foxbear.geo.json` é 128×64, e
o portão `CoerenciaDeGeckoLibTest` exige que **toda** PNG da pasta de textura do
mob case com a folha do modelo. Uma imagem de 1254² ali reprova o build — e
está certo que reprove, porque textura com proporção errada não dá erro em
jogo: dá um bicho borrado.

Ela também não pôde ser aproveitada: 1254 não é múltiplo inteiro de 64 nem de
128, então a imagem foi **reamostrada** em algum momento e não tem grade nativa
recuperável. Sem `.bbmodel` e sem grade, não há como derivar de volta qual
layout de caixas ela pinta. Casar um modelo gerado com ela seria adivinhação.

**O que ela ainda vale:** direção de arte. A cara de raposa, o peito creme, as
patas escuras e a ponta clara da cauda de `adulto.png` foram lidas daqui.

**O que fazer com ela:** se alguém modelar o Foxbear no Blockbench a partir
desta pintura, o `.bbmodel` entra nesta pasta e o export substitui
`adulto.png` — aí esta referência vira histórico, e não dívida.
