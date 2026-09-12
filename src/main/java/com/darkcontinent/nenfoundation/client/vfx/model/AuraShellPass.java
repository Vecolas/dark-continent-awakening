package com.darkcontinent.nenfoundation.client.vfx.model;

/**
 * As tres camadas da shell, da mais colada a mais distante.
 *
 * <p>TRES, E NAO UMA. Uma camada translucida sozinha e lida como plastico,
 * vidro ou armadura holografica -- a referencia exige profundidade, e
 * profundidade vem de camadas com PAPEIS diferentes, nao de uma camada mais
 * opaca.
 *
 * <p>O NUMERO DE PASSES E CONSTANTE DE DESIGN, e nao configuracao. Quem quiser
 * uma aura mais fraca mexe no alpha, que e dado; mexer na quantidade de passes
 * muda o que o efeito E.
 *
 * <p>No AV0 os tres desenham com o mesmo material, porque o shader proprio so
 * chega no AV1. A geometria nasce separada agora justamente para que o AV1
 * troque material sem mexer em modelo.
 */
public enum AuraShellPass {

    /** Filme interno: presenca continua no corpo. */
    INTERNA,

    /** A borda. No AV1 ela ganha Fresnel, e vira o contorno das referencias. */
    BORDA,

    /** Halo externo, muito fraco: separa a aura do fundo e alimenta o bloom. */
    EXTERNA
}
