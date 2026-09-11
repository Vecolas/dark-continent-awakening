# Primeira linha de base — M2, 2026-09-11

Arquivo: [m2-2026-09-11-dois-jogadores.sparkprofile](m2-2026-09-11-dois-jogadores.sparkprofile).
Protobuf spark, não gzip; parseado e confirmado não vazio antes do arquivo.

| Medida | Valor |
| --- | --- |
| Duração | 75.758 ms |
| Ticks observados | 1.515 |
| Jogadores | 2, loopback |
| Engine | Java sampler (Windows), intervalo 4 ms |
| Threads amostradas | 1, servidor |
| TPS, última janela de 1 min | 19,99993 |
| MSPT, mediana de 1 min | 1,067 ms |
| MSPT, p95 de 1 min | 3,7408 ms |
| MSPT, máximo de 1 min | 15,8043 ms |

Cenário leve: duas reservas diferentes, regeneração temporariamente zero,
recarga de capacidade/output; mundo flat com entidades vanilla. Não é estresse,
benchmark comparativo nem prova de ausência de regressão sob carga.
As estatísticas de 1 min são a janela do spark ao parar, não percentis
recalculados sobre os 75,758 segundos inteiros.

Comandos usados: `spark profiler start --force-java-sampler` e
`spark profiler stop --save-to-file`. Não houve upload.
Fonte dos comandos: [documentação oficial do spark](https://spark.lucko.me/docs/Command-Usage).

Antes de versionar, foram removidos do protobuf: configurações de servidor,
identificação do criador, metadados extras e estatísticas do sistema. Árvore
de amostras, contagem de ticks e estatísticas da plataforma foram preservadas.
SHA-256 da cópia sanitizada:
`6ac934fbc6f284242730bf06ffba3d3473f3d72ac2b2c037df11758c0d6d7f26`.

Capturas inspecionadas: [Aura cheia de Gon](m2-gon-cheia.png),
[Kurapika com reserva independente](m2-kurapika-metade.png) e
[HUD com servidor a 2 TPS](m2-kurapika-atraso.png).
São capturas estáticas, não medição de fluidez.
