# Balanceamento dos inimigos, por papel

> **Este arquivo e GERADO.** Ele sai de `BalanceamentoDeInimigosTest`, que o regrava e reprova quando ele diverge dos atributos. Nao edite a mao: edite o perfil do bicho, rode `./gradlew test` e commite o diff.

A conta e o **limite superior de eficiencia** -- dois lados parados trocando golpes, sem desvio, telegrafo, terreno, cura nem erro de mira. Um combate real sempre dura mais; se ja o limite superior estiver fora da faixa, o real esta pior.

Cada papel e medido contra o loadout que **encontra** aquele papel: pedra sem armadura ate HUNTER, ferro completo em DANGEROUS e ELITE, diamante completo de SQUADRON para cima. Medir um oficial de Chimera contra a espada de pedra diria que ele e impossivel, e nenhuma das duas leituras seria util.

| Inimigo | Papel | Vida | Dano | Armadura | Para matar | Para morrer | Faixa do papel |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| `avian_commander` | SQUADRON | 130 | 15 | 6 | 11.0s | 6.3s | 5.0..50.0 |
| `bat_scout` | LOW | 16 | 3 | 0 | 2.0s | 11.1s | 0.4..8.0 |
| `bubble_horse` | LOW | 30 | 3 | 1 | 3.8s | 11.1s | 0.4..8.0 |
| `cheetah_leader` | SQUADRON | 95 | 14 | 3 | 7.6s | 7.0s | 5.0..50.0 |
| `crab_heavy` | DANGEROUS | 60 | 9 | 8 | 6.5s | 6.4s | 2.5..26.0 |
| `cyclops` | ELITE | 120 | 14 | 6 | 11.9s | 3.5s | 3.0..36.0 |
| `dummy_enemy` | LOW | 30 | 4 | 0 | 3.8s | 8.3s | 0.4..8.0 |
| `foxbear` | HUNTER | 44 | 6 | 2 | 5.6s | 5.6s | 1.5..16.0 |
| `frog_in_waiting` | DANGEROUS | 50 | 10 | 3 | 4.6s | 5.6s | 2.5..26.0 |
| `great_stamp` | HUNTER | 70 | 11 | 7 | 10.7s | 3.0s | 1.5..16.0 |
| `hyper_puffball` | LOW | 18 | 0 | 0 | 2.3s | 3600.0s | 0.4..8.0 |
| `king_white_stag_beetle` | DANGEROUS | 90 | 12 | 9 | 10.3s | 4.3s | 2.5..26.0 |
| `kiriko` | ELITE | 40 | 7 | 3 | 3.7s | 8.8s | 3.0..36.0 |
| `man_faced_ape` | DANGEROUS | 24 | 4 | 1 | 2.2s | 17.4s | 2.5..26.0 *(excecao declarada)* |
| `master_of_the_swamp` | DANGEROUS | 60 | 6 | 4 | 5.5s | 10.7s | 2.5..26.0 |
| `melanin_lizard` | HUNTER | 40 | 7 | 3 | 5.1s | 4.8s | 1.5..16.0 |
| `mosquito_officer` | ELITE | 55 | 8 | 2 | 5.0s | 7.4s | 3.0..36.0 |
| `multiarm_centipede` | ELITE | 110 | 13 | 7 | 11.4s | 3.9s | 3.0..36.0 |
| `radio_rat` | LOW | 10 | 2 | 0 | 1.3s | 16.7s | 0.4..8.0 |
| `scorpion_leader` | SQUADRON | 120 | 12 | 8 | 11.2s | 8.7s | 5.0..50.0 |
| `spider_eagle` | HUNTER | 28 | 6 | 2 | 3.6s | 5.6s | 1.5..16.0 |
| `spider_webber` | ELITE | 70 | 10 | 4 | 6.5s | 5.6s | 3.0..36.0 |
| `wolf_pack_hunter` | HUNTER | 26 | 6 | 2 | 3.3s | 5.6s | 1.5..16.0 |
| `wolf_runner` | HUNTER | 28 | 7 | 2 | 3.6s | 4.8s | 1.5..16.0 |

## Excecoes declaradas

- **`man_faced_ape`** -- Emboscador FRAGIL de propósito. O perfil dele diz, com todas as letras, que dar a ele um corpo que aguenta troca de golpes premiaria justamente o jogador que NAO percebeu o disfarce -- a pista observavel viraria enfeite. A ameaca dele e o bando revelando junto, e nao o couro; ele morre em pouco mais de dois segundos e continua sendo DANGEROUS porque o encontro e perigoso, e nao porque o bicho e duro.

## O que esta tabela NAO diz

Ela nao mede desvio, telegrafo, terreno, cura, pocao, encantamento, critico, knockback nem a chance de errar o golpe. Tambem nao mede GRUPO: quatro lobos nao sao quatro vezes um lobo. E ela assume uma cadencia comum para todos os mobs, o que torna a comparacao entre eles justa e faz um mob de recarga longa parecer mais perigoso aqui do que e em jogo.
