# ADR-004 — mod_id, package e ids de categoria congelados no M0

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

O plano tecnico, secao 22, lista o que precisa estar congelado antes de o
trabalho paralelo comecar. A razao e dupla.

A primeira e de colaboracao: duas pessoas trabalhando contra um contrato que
ainda muda inventam nomes diferentes para a mesma coisa, e a integracao vira
renomeacao.

A segunda e mais cara. Estes identificadores vao para NBT de save, ids de
datapack, nomes de asset, chaves de traducao e condicoes de quest. Renomear
qualquer um deles depois do primeiro mundo criado **nao produz crash** — produz
um jogador que perde a categoria, ou uma quest que nunca completa.

## Decisao

Congelados a partir deste commit:

| O que | Valor |
| --- | --- |
| `mod_id` | `nenfoundation` |
| package base | `com.darkcontinent.nenfoundation` |
| namespace de recurso | `nenfoundation:` |
| categorias | `undetermined`, `enhancement`, `transmutation`, `emission`, `conjuration`, `manipulation`, `specialization` |
| schema do perfil | `PersistentNenData` v1 |
| protocolo | ids e direcoes de `NenProtocol`, versao 1 |
| interfaces | `NenTechnique`, `NenAbility` |

Descongelar exige: um ADR novo apontando este, um degrau em
`NenProfileMigrator`, e um plano para os mundos existentes.

Dois portoes cobram: `NenCategoryTest` (nomes e ordem) e
`ProtocoloCongeladoTest` (ids, direcoes e versao).

## Custo assumido

- **Nomes escolhidos cedo, com pouca informacao.** `undetermined` para o neutro
  e `nenfoundation` para um mod que talvez cresca alem de Nen sao escolhas
  feitas antes de o projeto existir. Aceito: um nome imperfeito custa menos que
  um save invalido.
- **O nome do pack e o do mod divergem.** O projeto se chama Dark Continent
  Awakening; o mod, Nen Foundation. E confuso na primeira leitura, e esta
  escrito no README.
- **Refatoracao de pacote fica cara.** Mover uma classe entre pacotes internos
  continua livre; mudar o package BASE nao.

## O que NAO muda

- Nomes EXIBIDOS continuam livres. "Reforco" ou "Enhancement" na tela e decisao
  de traducao, e a chave e que esta congelada.
- Categorias novas podem ser acrescentadas depois — no FIM da lista, com id
  novo. O congelamento proibe renomear e reordenar, nao crescer.
- Ids internos que nunca chegam a save nem a datapack nao estao cobertos.
