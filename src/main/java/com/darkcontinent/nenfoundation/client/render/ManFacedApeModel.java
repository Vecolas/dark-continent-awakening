package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Modelo do man-faced ape: DUAS silhuetas emprestadas do vanilla, e o servidor
 * escolhe qual delas o jogador ve.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: o disfarce e uma TROCA DE MALHA, nao um
 * truque de textura. Disfarcado o bicho usa a geometria da camada do aldeao
 * (silhueta humana, que e o "conjunto de aparencias humanas pre-feitas" do
 * plano); revelado, a da camada do piglin (bicho humanoide). Ha arte autoral
 * nenhuma e o ADR-007 proibe asset extraido da obra, entao as duas silhuetas
 * sao vanilla e trocam JUNTO com a textura, no renderer.</p>
 *
 * <p>Este modelo nao decide nada: ele le {@link ManFacedApeEntity#estaDisfarcado()}
 * e {@link ManFacedApeEntity#faseDeAtaque()}, ambos vindos do SynchedEntityData.
 * A inconsistencia observavel que ensina o mob -- ele so avanca quando ninguem
 * esta olhando -- e decidida no servidor; o cliente nao inventa pista nenhuma.</p>
 *
 * <p>AS DUAS RAIZES NUNCA APARECEM JUNTAS. Elas sao bakeadas de camadas
 * diferentes e cada uma so tem UV valido contra a SUA textura; o renderer manda
 * uma imagem por quadro. Duas visiveis ao mesmo tempo nao daria erro nenhum --
 * daria um aldeao pintado com o atlas do piglin.</p>
 *
 * <p>A instancia do modelo e UMA SO para todos os macacos do mundo. Por isso
 * TODO campo tocado aqui e reescrito a cada quadro, nos dois esqueletos, sem
 * ramo que deixe de escrever: quem esconde, mostra. Um macaco revelado que
 * deixasse a raiz do disfarce ligada faria o proximo macaco disfarcado nascer
 * com duas cabecas.</p>
 */
public final class ManFacedApeModel extends EntityModel<ManFacedApeEntity> {
    private static final float RADIANOS_POR_GRAU = (float) (Math.PI / 180.0);

    /** Rotacao com que a camada do aldeao nasce: bracos cruzados na frente. */
    private static final float ALDEAO_BRACOS_EM_REPOUSO = -0.75F;

    /**
     * Bracos acima da linha dos ombros (-120 graus). E o unico quadro em que o
     * jogador ve QUE A COISA MUDOU, e ele dura os ticks de windup do golpe.
     * Nao e botao de balanceamento: e a leitura do reveal.
     */
    private static final float BRACOS_ERGUIDOS = -2.0944F;

    /** Cabeca jogada para tras no reveal; xRot negativo levanta o focinho. */
    private static final float CABECA_DE_REVELACAO = -0.4F;

    // ------------------------------------------------------------- disfarce
    /** Raiz bakeada de ModelLayers.VILLAGER. Desenhada so enquanto disfarcado. */
    private final ModelPart raizDoDisfarce;
    private final ModelPart cabecaDeAldeao;
    /** O aldeao tem UMA peca de bracos, e nao dois bracos separados. */
    private final ModelPart bracosDeAldeao;
    private final ModelPart pernaDireitaDeAldeao;
    private final ModelPart pernaEsquerdaDeAldeao;

    // ------------------------------------------------------------- revelado
    /** Raiz bakeada de ModelLayers.PIGLIN. Desenhada so depois do reveal. */
    private final ModelPart raizRevelada;
    private final ModelPart cabecaDePiglin;
    private final ModelPart bracoDireitoDePiglin;
    private final ModelPart bracoEsquerdoDePiglin;
    private final ModelPart pernaDireitaDePiglin;
    private final ModelPart pernaEsquerdaDePiglin;

    /*
     * A camada do piglin herda a malha do PlayerModel, e com ela SETE pecas que
     * o renderer vanilla nunca desenha (ele itera headParts/bodyParts; nos
     * desenhamos a raiz inteira). Duas delas -- "ear" e "cloak" -- tem cubos na
     * origem e cairiam como uma placa atravessada no peito do macaco. As outras
     * cinco sao as camadas externas de pele, e as regioes delas em piglin.png
     * estao 100% transparentes (medido: 0 pixel opaco em jacket, mangas e
     * calcas), entao desenha-las so custaria quad.
     *
     * Guardadas como campo porque visibilidade e estado: quem esconde, mostra,
     * e esconder no construtor deixaria a limpeza longe do lugar que a le.
     */
    private final ModelPart orelhaDeJogador;
    private final ModelPart capa;
    private final ModelPart casaco;
    private final ModelPart mangaDireita;
    private final ModelPart mangaEsquerda;
    private final ModelPart calcaDireita;
    private final ModelPart calcaEsquerda;

    public ManFacedApeModel(ModelPart raizDoDisfarce, ModelPart raizRevelada) {
        this.raizDoDisfarce = raizDoDisfarce;
        this.cabecaDeAldeao = raizDoDisfarce.getChild("head");
        this.bracosDeAldeao = raizDoDisfarce.getChild("arms");
        this.pernaDireitaDeAldeao = raizDoDisfarce.getChild("right_leg");
        this.pernaEsquerdaDeAldeao = raizDoDisfarce.getChild("left_leg");

        this.raizRevelada = raizRevelada;
        this.cabecaDePiglin = raizRevelada.getChild("head");
        this.bracoDireitoDePiglin = raizRevelada.getChild("right_arm");
        this.bracoEsquerdoDePiglin = raizRevelada.getChild("left_arm");
        this.pernaDireitaDePiglin = raizRevelada.getChild("right_leg");
        this.pernaEsquerdaDePiglin = raizRevelada.getChild("left_leg");

        this.orelhaDeJogador = raizRevelada.getChild("ear");
        this.capa = raizRevelada.getChild("cloak");
        this.casaco = raizRevelada.getChild("jacket");
        this.mangaDireita = raizRevelada.getChild("right_sleeve");
        this.mangaEsquerda = raizRevelada.getChild("left_sleeve");
        this.calcaDireita = raizRevelada.getChild("right_pants");
        this.calcaEsquerda = raizRevelada.getChild("left_pants");
    }

    @Override
    public void setupAnim(ManFacedApeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        boolean disfarcado = entity.estaDisfarcado();
        // WINDUP e o reveal: o unico aviso que o jogador recebe antes do golpe,
        // e o instante em que o bando inteiro sai do disfarce junto.
        boolean revelando = entity.faseDeAtaque() == AttackPhase.WINDUP;

        float balanco = limbSwing * 0.6662F;
        float cabecaYRot = netHeadYaw * RADIANOS_POR_GRAU;
        float cabecaXRot = revelando ? CABECA_DE_REVELACAO : headPitch * RADIANOS_POR_GRAU;

        // Os DOIS esqueletos sao posados a cada quadro, e nao so o que vai ser
        // desenhado. Posar apenas o ramo visivel economizaria nada e deixaria a
        // pose velha de outro macaco esperando no esqueleto de folga.
        posarDisfarce(balanco, limbSwingAmount, cabecaYRot, cabecaXRot, revelando);
        posarRevelado(balanco, limbSwingAmount, cabecaYRot, cabecaXRot, revelando);
        apagarPecasQueONossoRenderNaoQuer();

        // A escolha da silhueta e do SERVIDOR. As duas raizes sao escritas aqui,
        // sempre, e sao mutuamente exclusivas por construcao.
        this.raizDoDisfarce.visible = disfarcado;
        this.raizRevelada.visible = !disfarcado;
    }

    /** Pose do aldeao: a caminhada e a mesma do VillagerModel vanilla. */
    private void posarDisfarce(float balanco, float amplitude, float cabecaYRot, float cabecaXRot,
                               boolean revelando) {
        this.cabecaDeAldeao.yRot = cabecaYRot;
        this.cabecaDeAldeao.xRot = cabecaXRot;
        // zRot e o balanco de aldeao irritado; aqui ele nunca existe, e escrever
        // zero sempre impede que um quadro futuro o deixe torto para sempre.
        this.cabecaDeAldeao.zRot = 0.0F;

        this.bracosDeAldeao.xRot = revelando ? BRACOS_ERGUIDOS : ALDEAO_BRACOS_EM_REPOUSO;

        this.pernaDireitaDeAldeao.xRot = Mth.cos(balanco) * 1.4F * amplitude * 0.5F;
        this.pernaEsquerdaDeAldeao.xRot = Mth.cos(balanco + (float) Math.PI) * 1.4F * amplitude * 0.5F;
        this.pernaDireitaDeAldeao.yRot = 0.0F;
        this.pernaEsquerdaDeAldeao.yRot = 0.0F;
    }

    /** Pose do bicho: a caminhada e a mesma do HumanoidModel vanilla. */
    private void posarRevelado(float balanco, float amplitude, float cabecaYRot, float cabecaXRot,
                               boolean revelando) {
        this.cabecaDePiglin.yRot = cabecaYRot;
        this.cabecaDePiglin.xRot = cabecaXRot;
        this.cabecaDePiglin.zRot = 0.0F;

        this.bracoDireitoDePiglin.xRot = revelando
                ? BRACOS_ERGUIDOS
                : Mth.cos(balanco + (float) Math.PI) * 2.0F * amplitude * 0.5F;
        this.bracoEsquerdoDePiglin.xRot = revelando
                ? BRACOS_ERGUIDOS
                : Mth.cos(balanco) * 2.0F * amplitude * 0.5F;
        this.bracoDireitoDePiglin.yRot = 0.0F;
        this.bracoEsquerdoDePiglin.yRot = 0.0F;
        this.bracoDireitoDePiglin.zRot = 0.0F;
        this.bracoEsquerdoDePiglin.zRot = 0.0F;

        this.pernaDireitaDePiglin.xRot = Mth.cos(balanco) * 1.4F * amplitude;
        this.pernaEsquerdaDePiglin.xRot = Mth.cos(balanco + (float) Math.PI) * 1.4F * amplitude;
        this.pernaDireitaDePiglin.yRot = 0.0F;
        this.pernaEsquerdaDePiglin.yRot = 0.0F;
    }

    /**
     * Pecas da malha de jogador que a camada do piglin carrega e que ninguem
     * quer aqui. Reescritas TODO quadro pelo mesmo motivo das rotacoes: uma
     * delas religada por outro caminho ficaria ligada para sempre.
     */
    private void apagarPecasQueONossoRenderNaoQuer() {
        this.orelhaDeJogador.visible = false;
        this.capa.visible = false;
        this.casaco.visible = false;
        this.mangaDireita.visible = false;
        this.mangaEsquerda.visible = false;
        this.calcaDireita.visible = false;
        this.calcaEsquerda.visible = false;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        // As duas chamadas sao deliberadas: ModelPart.render sai na primeira
        // linha quando visible e falso, e e o campo visible -- escrito no
        // setupAnim, a partir do estado do servidor -- que e a UNICA fonte da
        // escolha. Decidir aqui de novo seria a segunda fonte para a mesma
        // verdade, e as duas divergiriam no dia em que uma delas mudasse.
        this.raizDoDisfarce.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.raizRevelada.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
