# Decisoes de arquitetura

Uma decisao entra aqui quando **reverte-la e caro**: ela mudaria formato de
save, contrato de rede, fronteira entre modulos, ou o que o projeto se
compromete a nao fazer.

Todo ADR registra **o custo assumido**, e nao so o beneficio, e diz **o que NAO
muda** — a parte que evita erosao. Um portao (`IndiceDeAdrTest`) exige as duas
secoes em todo ADR, e cruza este indice com o diretorio nos dois sentidos: ADR
no disco e fora daqui reprova, e entrada aqui sem arquivo tambem.

Ele varre o DIRETORIO, nao esta lista. Verificacao que percorre a lista nunca
acusa o que nunca entrou nela.

| ADR | Assunto | Status |
| --- | --- | --- |
| [ADR-001](ADR-001-servidor-autoritativo.md) | O servidor e a autoridade sobre todo estado de Nen | aceita |
| [ADR-002](ADR-002-persistente-e-runtime.md) | Dado persistente e estado de runtime sao separados | aceita |
| [ADR-003](ADR-003-integracoes-opcionais.md) | Toda integracao com mod externo e opcional | aceita |
| [ADR-004](ADR-004-identidade-congelada.md) | mod_id, package e ids de categoria congelados | aceita |
| [ADR-005](ADR-005-versoes-pinadas.md) | Versoes pinadas e uma dependencia por vez | aceita |
| [ADR-006](ADR-006-epic-fight-fora-da-fundacao.md) | Epic Fight e adapter opcional e tardio | aceita |
| [ADR-007](ADR-007-assets-autorais.md) | Todo asset e autoral; nada e extraido da obra | aceita |
| [ADR-008](ADR-008-licenca-e-protecao-de-branch.md) | Licenca do codigo e ausencia de protecao de branch | aceita |
| [ADR-009](ADR-009-modelo-de-aura-sem-stamina-de-nen.md) | Aura composta e uma unica barra visivel, sem stamina de Nen | aceita |
| [ADR-010](ADR-010-regeneracao-por-estado-de-nen.md) | A regeneracao de Aura depende do estado de Nen ativo | **pela metade**: falta a aprovacao de @jonex-01 |
| [ADR-011](ADR-011-descongelamento-do-protocolo.md) | O protocolo de rede descongela, com versao e regra | aceita |

## Como escrever um ADR

Copie a estrutura de qualquer um acima. As secoes obrigatorias sao:

- `## Contexto` — o que forcou a decisao.
- `## Decisao` — o que passa a valer, em regras concretas.
- `## Custo assumido` — o que se perde. **Obrigatoria.**
- `## O que NAO muda` — o que sobrevive a decisao. **Obrigatoria.**

Se a decisao nova **contradiz** uma anterior, aponte qual e em que, e diga o
custo da troca. Trocar de ideia nao e problema; trocar em silencio e.
