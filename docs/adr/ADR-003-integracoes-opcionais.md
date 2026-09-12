# ADR-003 — Toda integracao com mod externo e opcional

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

O plano prevê FTB Quests, KubeJS, LootJS, In Control, Patchouli, Jade, Curios,
JEI/EMI, GeckoLib e uma pilha de performance. Todos ativos em 1.21.1/NeoForge.

Nenhum deles e mantido por nos. Qualquer um pode parar de ser atualizado, mudar
de API ou sumir. A pesquisa de viabilidade e explicita sobre isso: o valor de
separar as camadas e que, se a camada de Nen cair, quests, estruturas, loot e
identidade sobrevivem — e vice-versa.

## Decisao

O JAR do Nen Foundation inicia e funciona em cliente e em servidor dedicado com
NeoForge e mais nada instalado.

1. Nenhuma dependencia opcional aparece como `required` em
   `neoforge.mods.toml`.
2. `integration/*` pode depender das APIs externas. O nucleo **nao** pode
   depender de `integration/*`. A seta aponta so para um lado.
3. Classe de integracao e carregada sob verificacao de mod presente.
4. Quest nunca escreve NBT de Nen. Ela chama a API, e o nucleo decide.
5. O nucleo guarda **marcos proprios** (`nenfoundation:despertou`), nunca ids de
   quest do FTB. Trocar o questbook nao pode apagar progresso.
6. Script nenhum valida aura, dano, cooldown ou seguranca de rede.

O [ADR-012](ADR-012-geckolib-obrigatorio.md) cria uma excecao estreita para
GeckoLib: ele deixa de ser integracao opcional do pack e passa a ser biblioteca
obrigatoria do pipeline de entidades animadas. As demais integracoes continuam
sob todas as regras deste ADR.

## Custo assumido

- **Duplicacao aparente.** O nucleo tem marcos de progressao e o FTB Quests tem
  o proprio estado de quest. Parecem a mesma coisa e nao sao: um e o estado do
  jogador, o outro e o estado da campanha. Quando eles discordarem, o nucleo
  ganha.
- **Integracao mais cara de escrever.** Verificacao de mod presente, carregamento
  isolado e uma camada de traducao custam mais que importar a classe direto.
- **Menos poder para o pack.** Um pack que quisesse reescrever regra de aura em
  KubeJS nao consegue. E deliberado.

## O que NAO muda

- O modpack continua sendo a camada que entrega a experiencia. Esta decisao nao
  diz que o pack e secundario — diz que o nucleo nao depende dele para existir.
- Epic Fight tem regime proprio e mais estrito. Ver ADR-006.
