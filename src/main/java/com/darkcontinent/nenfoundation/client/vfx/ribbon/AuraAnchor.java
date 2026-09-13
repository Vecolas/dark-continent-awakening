package com.darkcontinent.nenfoundation.client.vfx.ribbon;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;

/**
 * Onde cada filamento NASCE no corpo.
 *
 * <p>ESTE E O COMPONENTE QUE MAIS APROXIMA O RESULTADO DA REFERENCIA. Como a
 * ancora pertence a um {@code ModelPart}, ela herda a transformacao dele -- e o
 * filamento acompanha corrida, pulo, ataque e agachamento em vez de ficar para
 * tras como fumaca. Uma particula solta no mundo nao faz isso.
 *
 * <p>OS NUMEROS SAO EM UNIDADE DE MODELO, e a conversao para bloco ({@code /16})
 * acontece uma vez so, em {@link AuraCurve}. Misturar as duas unidades produz
 * uma ribbon dezesseis vezes maior ou menor, sem erro nenhum.
 *
 * <p>EIXO Y PARA BAIXO. Na pilha que chega a layer, {@code +Y} aponta para o
 * chao -- consequencia do {@code scale(-1,-1,1)} de
 * {@code LivingEntityRenderer}. Por isso o topo da cabeca e {@code y = -8} e a
 * subida do filamento SUBTRAI. Com o sinal trocado, a ribbon entra no chao, e o
 * chao nao reclama.
 *
 * <p>A REGIAO E A PARTE COINCIDEM nas vinte ancoras, entao um campo so serve
 * para as duas coisas. Se um dia uma ancora precisar nascer numa parte e pesar
 * por outra regiao -- uma ribbon de ombro contando como tronco, por exemplo --,
 * este campo se divide em dois. Hoje dividir seria inventar diferenca.
 *
 * <p>O BRACO SLIM MUDA SEIS ANCORAS, e so em {@code cx} e {@code rx}: ele tem
 * tres unidades de largura em vez de quatro. O pivo (2.0 contra 2.5) e do
 * {@code ModelPart} pai e ja vem aplicado pela transformacao dele -- somar aqui
 * seria contar duas vezes.
 */
public enum AuraAnchor {

    HEAD_TOP(AuraBodyRegion.HEAD, Familia.AXIAL, 0.0F, -8.0F, 0, 0.0F, 0.0F),
    HEAD_LEFT(AuraBodyRegion.HEAD, Familia.RADIAL, 0.0F, -5.0F, 0, 4.0F, 4.0F),
    HEAD_RIGHT(AuraBodyRegion.HEAD, Familia.RADIAL, 180.0F, -5.0F, 0, 4.0F, 4.0F),

    CHEST_LEFT(AuraBodyRegion.TORSO, Familia.RADIAL, 320.0F, 3.0F, 0, 4.0F, 2.0F),
    CHEST_RIGHT(AuraBodyRegion.TORSO, Familia.RADIAL, 220.0F, 3.0F, 0, 4.0F, 2.0F),
    BACK_CENTER(AuraBodyRegion.TORSO, Familia.RADIAL, 90.0F, 4.0F, 0, 4.0F, 2.0F),
    HIP_LEFT(AuraBodyRegion.TORSO, Familia.RADIAL, 0.0F, 11.0F, 0, 4.0F, 2.0F),
    HIP_RIGHT(AuraBodyRegion.TORSO, Familia.RADIAL, 180.0F, 11.0F, 0, 4.0F, 2.0F),

    SHOULDER_LEFT(AuraBodyRegion.LEFT_ARM, Familia.RADIAL, 0.0F, -1.0F, +1, 0.0F, 2.0F),
    SHOULDER_RIGHT(AuraBodyRegion.RIGHT_ARM, Familia.RADIAL, 180.0F, -1.0F, -1, 0.0F, 2.0F),
    FOREARM_LEFT(AuraBodyRegion.LEFT_ARM, Familia.RADIAL, 300.0F, 5.5F, +1, 0.0F, 2.0F),
    FOREARM_RIGHT(AuraBodyRegion.RIGHT_ARM, Familia.RADIAL, 240.0F, 5.5F, -1, 0.0F, 2.0F),
    HAND_LEFT(AuraBodyRegion.LEFT_ARM, Familia.RADIAL, 0.0F, 9.5F, +1, 0.0F, 2.0F),
    HAND_RIGHT(AuraBodyRegion.RIGHT_ARM, Familia.RADIAL, 180.0F, 9.5F, -1, 0.0F, 2.0F),

    THIGH_LEFT(AuraBodyRegion.LEFT_LEG, Familia.RADIAL, 0.0F, 3.0F, 0, 2.0F, 2.0F),
    THIGH_RIGHT(AuraBodyRegion.RIGHT_LEG, Familia.RADIAL, 180.0F, 3.0F, 0, 2.0F, 2.0F),
    CALF_LEFT(AuraBodyRegion.LEFT_LEG, Familia.RADIAL, 330.0F, 8.5F, 0, 2.0F, 2.0F),
    CALF_RIGHT(AuraBodyRegion.RIGHT_LEG, Familia.RADIAL, 210.0F, 8.5F, 0, 2.0F, 2.0F),
    FOOT_LEFT(AuraBodyRegion.LEFT_LEG, Familia.RADIAL, 0.0F, 11.5F, 0, 2.0F, 2.0F),
    FOOT_RIGHT(AuraBodyRegion.RIGHT_LEG, Familia.RADIAL, 180.0F, 11.5F, 0, 2.0F, 2.0F);

    /** Como o filamento sai do corpo. */
    public enum Familia {
        /** Enrola em volta de um eixo -- braco, perna, tronco. */
        RADIAL,
        /**
         * Sobe reto, com deriva leve.
         *
         * <p>So o topo da cabeca. Nao ha eixo para enrolar la, e enrolar mesmo
         * assim desenha um chifre.
         */
        AXIAL
    }

    private final AuraBodyRegion regiao;
    private final Familia familia;
    private final float psiInicialGraus;
    private final float alturaBase;
    /** 0 quando nao e braco; +1 esquerdo, -1 direito. Decide o sinal de {@code cx}. */
    private final int bracoSinal;
    private final float raioX;
    private final float raioZ;

    AuraAnchor(AuraBodyRegion regiao, Familia familia, float psiInicialGraus, float alturaBase,
            int bracoSinal, float raioX, float raioZ) {
        this.regiao = regiao;
        this.familia = familia;
        this.psiInicialGraus = psiInicialGraus;
        this.alturaBase = alturaBase;
        this.bracoSinal = bracoSinal;
        this.raioX = raioX;
        this.raioZ = raioZ;
    }

    /** A regiao cujo peso este filamento herda -- e tambem a parte em que ele nasce. */
    public AuraBodyRegion regiao() {
        return this.regiao;
    }

    public Familia familia() {
        return this.familia;
    }

    /** Angulo de partida em volta do eixo, em radianos. */
    public float psiInicial() {
        return this.psiInicialGraus * ((float) Math.PI / 180.0F);
    }

    /** Altura de partida, em unidade de modelo. Lembre: Y cresce para BAIXO. */
    public float alturaBase() {
        return this.alturaBase;
    }

    /** Centro do eixo em X, em unidade de modelo. Depende do modelo de braco. */
    public float centroX(boolean slim) {
        return this.bracoSinal * (slim ? 0.5F : 1.0F);
    }

    /** Semi-eixo em X. O braco slim e uma unidade mais estreito. */
    public float raioX(boolean slim) {
        return this.bracoSinal == 0 ? this.raioX : (slim ? 1.5F : 2.0F);
    }

    public float raioZ() {
        return this.raioZ;
    }
}
