/**
 * As oito cidades de Greed Island: identidade congelada, arranjo gerado.
 *
 * <p>DECISAO da secao 47: nenhuma cidade e uma NBT gigante. O que fica
 * congelado e a IDENTIDADE -- ancora, pegada, papel, distritos e landmark --,
 * e o arranjo interno sai do pipeline
 * {@code CityAnchor -> DistrictGraph -> RoadSkeleton -> Parcels -> jigsaw}.
 *
 * <p>Uma estrutura monolitica do tamanho de Limeiro (1.200x1.500) teria
 * dezenas de megabytes, seria impossivel de revisar em diff, e seria colada
 * por cima do relevo em vez de adaptada a ele.
 *
 * <p>A POSICAO TEM FONTE UNICA: {@code GreedIslandConstants.CIDADES}. Duas
 * listas de coordenada divergem no dia em que alguem move uma cidade e esquece
 * a outra, e o sintoma e a estrada chegando num campo vazio ao lado.
 *
 * <p>Fases G6-G8. Owner: conjunto.
 */
package com.darkcontinent.nenfoundation.enemy.greedisland.city;
