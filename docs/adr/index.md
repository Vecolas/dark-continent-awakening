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
| [ADR-010](ADR-010-regeneracao-por-estado-de-nen.md) | A regeneracao de Aura depende do estado de Nen ativo | aceita |
| [ADR-011](ADR-011-descongelamento-do-protocolo.md) | O protocolo de rede descongela, com versao e regra | aceita |
| [ADR-012](ADR-012-geckolib-obrigatorio.md) | GeckoLib e biblioteca obrigatoria do pipeline de entidades | aceita |
| [ADR-013](ADR-013-saldo-so-para-quem-libera-aura.md) | O saldo negativo vale para quem LIBERA aura | Zetsu e Ten ficam mais baratos do que deveriam ate a camada de dano existir (#127); "quem libera" nao e um campo, e desenho |
| [ADR-014](ADR-014-alocacao-de-aura-por-regiao.md) | A aura tem regioes, e elas sao autoritativas | mais um derivado para limpar; mais bytes por delta; duas distribuicoes no repo ate a projecao ser ligada; a soma 1.0 e invariante e nao botao de balanceamento |
| [ADR-015](ADR-015-aura-e-geometria-e-shader.md) | A aura e geometria e shader; particula e acabamento | uma trilha inteira (AV0–AV8) so de visual; shader custom quebra entre versoes; overdraw vira requisito de gate; o efeito barato que ja desenhava sai de cena antes de o novo entrar |
| [ADR-016](ADR-016-pos-processamento-proprio-da-aura.md) | O brilho da aura e pos-processamento proprio, opcional e com fallback | render target vaza em resize e em reload; duas maneiras de produzir o mesmo halo; incompatibilidade provavel com shader pack, detectada e documentada em vez de prometida |

## Como escrever um ADR

Copie a estrutura de qualquer um acima. As secoes obrigatorias sao:

- `## Contexto` — o que forcou a decisao.
- `## Decisao` — o que passa a valer, em regras concretas.
- `## Custo assumido` — o que se perde. **Obrigatoria.**
- `## O que NAO muda` — o que sobrevive a decisao. **Obrigatoria.**

Se a decisao nova **contradiz** uma anterior, aponte qual e em que, e diga o
custo da troca. Trocar de ideia nao e problema; trocar em silencio e.
