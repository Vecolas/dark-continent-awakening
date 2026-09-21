package com.darkcontinent.nenfoundation.client.vfx.model;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * De que OSSO cada ancora nasce, neste corpo.
 *
 * <p><b>O NOME DO OSSO E DECLARADO PELO INIMIGO, e nao constante no renderer.</b>
 * Um mapa fixo dentro do renderer funcionaria para o primeiro mob e estaria
 * errado para o segundo -- e o defeito nao apareceria como erro: a aura nasceria
 * no lugar errado, ou nao nasceria. Cada corpo diz quais ossos ele tem, e o
 * renderer nao precisa saber que existe mais de um tipo de corpo.
 *
 * <p><b>ANCORA SEM OSSO SIMPLESMENTE NAO NASCE.</b> Nao e erro, e nao deveria
 * ser: um quadrupede nao tem braco, e exigir que ele declare
 * {@code SHOULDER_LEFT} seria obrigar todo corpo a fingir ser humanoide. O que e
 * erro -- e vira recusa com motivo, UMA vez por modelo -- e declarar um osso que
 * o modelo nao tem.
 *
 * <p>PURA E SEM MINECRAFT: a resolucao inteira se prova sem subir o jogo, e ela
 * e a parte que erra. O que precisa de tela e desenhar.
 *
 * <p>SEM MAPEAMENTO DE REGIAO POR ANATOMIA, e isso e limite declarado: as seis
 * regioes do [ADR-014](docs/adr/ADR-014-alocacao-de-aura-por-regiao.md) sao de
 * um corpo humanoide, e um quadrupede nao tem como responder a "braco
 * esquerdo". O padrao aqui e DISTRIBUICAO UNIFORME, e o mapeamento vira campo
 * da definicao quando houver o primeiro caso que precise dele.
 */
public final class AuraOssosDoInimigo {

    /** Nenhum osso declarado: a shell desenha, os filamentos nao. */
    public static final AuraOssosDoInimigo NENHUM =
            new AuraOssosDoInimigo(new EnumMap<>(AuraAnchor.class),
                    new EnumMap<>(AuraBodyRegion.class));

    private final Map<AuraAnchor, String> porAncora;
    private final Map<AuraBodyRegion, String> porRegiao;

    private AuraOssosDoInimigo(Map<AuraAnchor, String> porAncora,
            Map<AuraBodyRegion, String> porRegiao) {
        this.porAncora = porAncora;
        this.porRegiao = porRegiao;
    }

    /** Comeca a declarar os ossos de um corpo. */
    public static Construtor declarar() {
        return new Construtor();
    }

    /**
     * Os nomes de osso do esqueleto humanoide padrao do Blockbench.
     *
     * <p>ELE E UMA CONVENIENCIA, E NAO UM PADRAO IMPLICITO. Um corpo que nao o
     * use declara o proprio; o que nao pode e o renderer ASSUMIR estes nomes e
     * desenhar em lugar nenhum quando eles nao existirem.
     */
    public static AuraOssosDoInimigo humanoidePadrao() {
        return declarar()
                .regiao(AuraBodyRegion.HEAD, "head")
                .regiao(AuraBodyRegion.TORSO, "body")
                .regiao(AuraBodyRegion.LEFT_ARM, "left_arm")
                .regiao(AuraBodyRegion.RIGHT_ARM, "right_arm")
                .regiao(AuraBodyRegion.LEFT_LEG, "left_leg")
                .regiao(AuraBodyRegion.RIGHT_LEG, "right_leg")
                .ancora(AuraAnchor.HEAD_TOP, "head")
                .ancora(AuraAnchor.HEAD_LEFT, "head")
                .ancora(AuraAnchor.HEAD_RIGHT, "head")
                .ancora(AuraAnchor.CHEST_LEFT, "body")
                .ancora(AuraAnchor.CHEST_RIGHT, "body")
                .ancora(AuraAnchor.BACK_CENTER, "body")
                .ancora(AuraAnchor.HIP_LEFT, "body")
                .ancora(AuraAnchor.HIP_RIGHT, "body")
                .ancora(AuraAnchor.SHOULDER_LEFT, "left_arm")
                .ancora(AuraAnchor.FOREARM_LEFT, "left_arm")
                .ancora(AuraAnchor.HAND_LEFT, "left_arm")
                .ancora(AuraAnchor.SHOULDER_RIGHT, "right_arm")
                .ancora(AuraAnchor.FOREARM_RIGHT, "right_arm")
                .ancora(AuraAnchor.HAND_RIGHT, "right_arm")
                .ancora(AuraAnchor.THIGH_LEFT, "left_leg")
                .ancora(AuraAnchor.CALF_LEFT, "left_leg")
                .ancora(AuraAnchor.FOOT_LEFT, "left_leg")
                .ancora(AuraAnchor.THIGH_RIGHT, "right_leg")
                .ancora(AuraAnchor.CALF_RIGHT, "right_leg")
                .ancora(AuraAnchor.FOOT_RIGHT, "right_leg")
                .montar();
    }

    /** O osso desta ancora, ou {@code null} se este corpo nao a tem. */
    @Nullable
    public String ossoDe(AuraAnchor ancora) {
        return ancora == null ? null : this.porAncora.get(ancora);
    }

    /** O osso desta regiao, ou {@code null} se este corpo nao a tem. */
    @Nullable
    public String ossoDe(AuraBodyRegion regiao) {
        return regiao == null ? null : this.porRegiao.get(regiao);
    }

    /** Se ha alguma ancora declarada. Sem nenhuma, os filamentos nao nascem. */
    public boolean temAncoras() {
        return !this.porAncora.isEmpty();
    }

    /** Quantas regioes este corpo declara. */
    public int regioesDeclaradas() {
        return this.porRegiao.size();
    }

    /** Montagem, para que a declaracao de um inimigo fique legivel. */
    public static final class Construtor {

        private final Map<AuraAnchor, String> porAncora = new EnumMap<>(AuraAnchor.class);
        private final Map<AuraBodyRegion, String> porRegiao = new EnumMap<>(AuraBodyRegion.class);

        private Construtor() {
        }

        /** Declara o osso de uma ancora. */
        public Construtor ancora(AuraAnchor ancora, String osso) {
            exigir(ancora, osso);
            this.porAncora.put(ancora, osso);
            return this;
        }

        /** Declara o osso de uma regiao. */
        public Construtor regiao(AuraBodyRegion regiao, String osso) {
            exigir(regiao, osso);
            this.porRegiao.put(regiao, osso);
            return this;
        }

        /**
         * Fecha a declaracao.
         *
         * <p>NAO EXIGE AS SEIS REGIOES, ao contrario da linha do tempo de
         * transicao. A diferenca e de natureza: la, um componente omitido
         * desenha zero e o zero por esquecimento e indistinguivel do zero de
         * proposito. Aqui, uma regiao ausente e uma AFIRMACAO sobre o corpo --
         * um quadrupede nao tem braco, e obriga-lo a declarar um seria pior que
         * deixar de fora.
         */
        public AuraOssosDoInimigo montar() {
            return new AuraOssosDoInimigo(new EnumMap<>(this.porAncora),
                    new EnumMap<>(this.porRegiao));
        }

        private static void exigir(Object chave, String osso) {
            if (chave == null) {
                throw new NullPointerException("ancora ou regiao obrigatoria");
            }
            if (osso == null || osso.isBlank()) {
                throw new IllegalArgumentException("nome de osso vazio para " + chave
                        + "; declarar vazio e pior que nao declarar -- a busca falharia"
                        + " em runtime em vez de aqui");
            }
        }
    }
}
