# Posto Avançado da Associação Hunter

Contrato V2 do marco ST1. O layout de referência ocupa `41x41` blocos
(X/Z de `-20` a `+20`), com dois blocos de folga entre os módulos e o
cercamento modular de três blocos, prédios baixos
de até sete blocos e torre aberta de scaffold até Y14. Os oito módulos são
`portao_logistico`, `patio_operacional`, `comando`, `torre_scaffold`,
`deposito`, `laboratorio`, `manutencao` e `triagem`.

Este arquivo congela a geometria e o contrato do primeiro placement jogável.
O posto é materializado pelo blockout server-side, pelo comando
`/hxh structure spawn` e pelo candidato natural persistente em
`PostoAvancadoNaturalSpawner`. O template `.nbt` continua fora desta entrega:
ele só deve nascer quando a captura visual substituir o blockout sem criar uma
segunda fonte de geometria.

O blockout é materializado diretamente em blocos vanilla pelo placer, portanto
não depende de entidade, prop, comando de debug ou template binário para existir.
Baús de operação recebem loot de suprimentos de campo; o laboratório contém um
cache opcional sob o piso, documentado no roteiro de QA manual.

O roteiro do gate final está em
`docs/structures/posto-avancado-hunter-qa.md`. A revisão visual ainda exige
capturas em cliente real; os testes automatizados não substituem essa leitura.

O placement natural é restrito ao Overworld, exige a tag de biomas do posto e
recusa terreno com variação acima de cinco blocos. A decisão e o material são
aplicados no servidor; nenhum cliente escolhe bloco, cota ou rotação.

## Direção visual congelada

O posto é uma base de campo da Associação Hunter, não uma fortificação
medieval. A leitura deve vir de módulos de exploração, concreto branco, lona,
caixas, mapas, pesquisa e iluminação prática. O perímetro usa apenas iron bars,
sem blocos de ferro maciços; a paliçada é baixa e funcional, não vira muralha,
castelo ou torre feudal. As lâmpadas técnicas são shroomlights.

Critérios de revisão visual estão em
`docs/structures/direcao-visual-posto-hunter.md` (issue #231), e a referência
visual está em `referencia-posto-hunter-v2.png`. Nenhum template ou worldgen
pode ser aceito sem passar por esses critérios.
