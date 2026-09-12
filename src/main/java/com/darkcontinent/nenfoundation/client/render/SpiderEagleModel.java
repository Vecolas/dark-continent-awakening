package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Modelo da spider eagle com geometria EMPRESTADA da camada vanilla do phantom.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: o aviso e o mergulho sao coisas
 * VISIVELMENTE diferentes, porque entre um e outro existe a unica janela em que
 * o jogador ainda pode recuar e ser poupado. Aviso = asa aberta batendo largo;
 * mergulho = asa recolhida e corpo apontado para baixo. Se os dois lessem
 * parecido, o mob perderia o que ele tem de proprio -- quem recua nao morre --
 * e viraria mais um bicho que ataca de longe.</p>
 *
 * <p>Nao ha arte autoral da spider eagle e o ADR-007 proibe asset extraido da
 * obra; entre as camadas vanilla, a do phantom e o unico predador alado grande,
 * com asa de couro e cauda longa segmentada. O lado ARACNIDEO da silhueta nao
 * tem geometria vanilla para emprestar e fica DE FORA de proposito: inventar
 * perna de aranha com cubo de outra camada produziria um bicho manchado, ja que
 * os UVs bakeados sao os do phantom. Ele chega com a arte propria, junto da
 * camada e da textura.</p>
 *
 * <p>Este modelo nao decide nada: ele le {@link SpiderEagleEntity#estaAvisando()}
 * e {@link SpiderEagleEntity#faseDeAtaque()}, ambos vindos do SynchedEntityData.
 * Quem decide que houve invasao, quando o aviso vira bote e quando a ave desiste
 * e o servidor; o cliente nao inventa pista nenhuma.</p>
 *
 * <p>A instancia do modelo e UMA SO para todas as aves do mundo. Por isso TODO
 * campo tocado aqui e reescrito a cada quadro, sem ramo que deixe de escrever:
 * quem gira, desgira. Uma ave que terminasse o mergulho e deixasse a asa
 * recolhida contaminaria a proxima, que nasceria planando de asa fechada -- e
 * isso nao da erro nenhum, so some com o telegrafo.</p>
 *
 * <p>CONVENCAO DE SINAL usada aqui, a mesma dos modelos irmaos: xRot POSITIVO
 * baixa o focinho, xRot NEGATIVO levanta. Ja o sinal de zRot e yRot das asas nao
 * e afirmado -- as duas asas sao escritas espelhadas (esquerda {@code +v},
 * direita {@code -v}), exatamente como o PhantomModel vanilla faz, entao o
 * movimento e simetrico com qualquer um dos dois sinais. O que NAO se resolve
 * fora da tela e se a asa recolhida dobra para cima ou para baixo.</p>
 */
public final class SpiderEagleModel extends EntityModel<SpiderEagleEntity> {
    private static final float RADIANOS_POR_GRAU = (float) (Math.PI / 180.0);

    /** Rotacao com que a camada do phantom nasce no corpo: quase nivelado. */
    private static final float CORPO_NEUTRO = -0.1F;
    /**
     * WINDUP e a SUBIDA antes do mergulho: focinho para cima (xRot negativo).
     * E o segundo aviso, e o ultimo -- depois dele a ave ja esta descendo.
     */
    private static final float CORPO_EM_SUBIDA = -0.55F;
    /**
     * ACTIVE e a DESCIDA: corpo apontado para baixo (~52 graus). E este angulo
     * que faz o mergulho parecer um mergulho, e nao um voo rasante.
     */
    private static final float CORPO_EM_MERGULHO = 0.9F;

    /** Rotacao com que a camada do phantom nasce na cabeca. */
    private static final float CABECA_NEUTRA = 0.2F;
    /**
     * Na subida a cabeca desce quase tanto quanto o corpo sobe: somadas, a ave
     * continua OLHANDO para o intruso enquanto ganha altura. Quem esta sendo
     * mirado precisa ver que esta sendo mirado.
     */
    private static final float CABECA_NA_SUBIDA = 0.55F;
    /** Na descida a cabeca acompanha o corpo, so que menos: ela lidera o bote. */
    private static final float CABECA_EM_MERGULHO = 0.55F;

    /**
     * Graus por tick da batida de asa, o mesmo do phantom vanilla. E CONSTANTE
     * entre os estados de proposito: so a amplitude muda. Mudar a frequencia
     * junto faria a fase saltar no quadro em que o aviso comeca, e um salto de
     * fase na asa aparece como um tranco, nao como uma mudanca de humor.
     */
    private static final float VELOCIDADE_DA_BATIDA = 7.448451F;
    /**
     * Duas aves proximas nao batem asa em uniformidade de esquadrilha. O phantom
     * vanilla resolve isso com um offset proprio da entidade; aqui o id serve ao
     * mesmo fim e nao carrega informacao nenhuma do servidor.
     */
    private static final float DESSINCRONIA_POR_ENTIDADE = 7.0F;

    /** Batida de patrulha, a mesma amplitude do phantom vanilla (graus). */
    private static final float BATIDA_DE_CRUZEIRO = 16.0F;
    /**
     * Batida de AVISO (graus): quase tres vezes mais larga que a de patrulha.
     * Este numero e a leitura do mob, e nao um botao de balanceamento -- e a
     * unica coisa que diz ao jogador "da meia volta" enquanto dar meia volta
     * ainda resolve.
     */
    private static final float BATIDA_DE_AVISO = 45.0F;
    /** No mergulho a asa para de bater e trava recolhida (graus). */
    private static final float ASAS_RECOLHIDAS = 22.0F;
    /** Varredura das asas para tras durante o mergulho, em radianos. */
    private static final float ASAS_VARRIDAS = 0.45F;

    /** Amplitude do meneio de cauda do phantom vanilla, em graus. */
    private static final float MENEIO_DA_CAUDA = 5.0F;
    /** No mergulho a cauda para de menear e vira leme, alinhada com o corpo. */
    private static final float CAUDA_NO_MERGULHO = 0.0F;

    /** Raiz bakeada da camada; e ela que vai para o buffer. */
    private final ModelPart raiz;
    private final ModelPart corpo;
    private final ModelPart cabeca;
    private final ModelPart asaEsquerdaBase;
    private final ModelPart asaEsquerdaPonta;
    private final ModelPart asaDireitaBase;
    private final ModelPart asaDireitaPonta;
    private final ModelPart caudaBase;
    private final ModelPart caudaPonta;

    public SpiderEagleModel(ModelPart raiz) {
        this.raiz = raiz;
        // A camada do phantom pendura TUDO num filho chamado "body" -- asas,
        // cauda e cabeca. A raiz bakeada e so o no vazio acima dele.
        this.corpo = raiz.getChild("body");
        this.cabeca = corpo.getChild("head");
        this.asaEsquerdaBase = corpo.getChild("left_wing_base");
        this.asaEsquerdaPonta = asaEsquerdaBase.getChild("left_wing_tip");
        this.asaDireitaBase = corpo.getChild("right_wing_base");
        this.asaDireitaPonta = asaDireitaBase.getChild("right_wing_tip");
        this.caudaBase = corpo.getChild("tail_base");
        this.caudaPonta = caudaBase.getChild("tail_tip");
    }

    @Override
    public void setupAnim(SpiderEagleEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        boolean avisando = entity.estaAvisando();
        AttackPhase fase = entity.faseDeAtaque();
        boolean subindo = fase == AttackPhase.WINDUP;
        boolean descendo = fase == AttackPhase.ACTIVE;
        boolean mergulhando = subindo || descendo;

        float faseDaBatida = (ageInTicks + entity.getId() * DESSINCRONIA_POR_ENTIDADE)
                * VELOCIDADE_DA_BATIDA * RADIANOS_POR_GRAU;

        // O mergulho GANHA do aviso: a ave que ja se lancou nao esta mais
        // avisando, e mostrar as duas coisas ao mesmo tempo diria ao jogador que
        // ele ainda tem a janela de recuo quando ela ja fechou.
        float amplitude = (avisando ? BATIDA_DE_AVISO : BATIDA_DE_CRUZEIRO) * RADIANOS_POR_GRAU;
        float asaZ = mergulhando
                ? ASAS_RECOLHIDAS * RADIANOS_POR_GRAU
                : Mth.cos(faseDaBatida) * amplitude;
        float asaY = mergulhando ? ASAS_VARRIDAS : 0.0F;

        // Espelhadas, como no PhantomModel vanilla: a direita recebe o oposto da
        // esquerda. Nenhuma das quatro pecas fica sem escrita em nenhum ramo.
        this.asaEsquerdaBase.zRot = asaZ;
        this.asaEsquerdaPonta.zRot = asaZ;
        this.asaDireitaBase.zRot = -asaZ;
        this.asaDireitaPonta.zRot = -asaZ;
        this.asaEsquerdaBase.yRot = asaY;
        this.asaDireitaBase.yRot = -asaY;
        this.asaEsquerdaPonta.yRot = 0.0F;
        this.asaDireitaPonta.yRot = 0.0F;

        float cauda = mergulhando
                ? CAUDA_NO_MERGULHO
                : -(MENEIO_DA_CAUDA + Mth.cos(faseDaBatida * 2.0F) * MENEIO_DA_CAUDA)
                        * RADIANOS_POR_GRAU;
        this.caudaBase.xRot = cauda;
        this.caudaPonta.xRot = cauda;

        this.corpo.xRot = descendo ? CORPO_EM_MERGULHO : subindo ? CORPO_EM_SUBIDA : CORPO_NEUTRO;

        this.cabeca.xRot = descendo
                ? CABECA_EM_MERGULHO
                : subindo ? CABECA_NA_SUBIDA : CABECA_NEUTRA;
        // Fora do mergulho a cabeca acompanha o olhar da entidade -- inclusive
        // durante o aviso, porque o intruso precisa ver que o aviso e PARA ELE.
        // Ja lancada, a ave olha para onde vai, e nao para onde o alvo estava.
        this.cabeca.yRot = mergulhando ? 0.0F : netHeadYaw * RADIANOS_POR_GRAU;
        this.cabeca.zRot = 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        this.raiz.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
