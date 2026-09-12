package com.darkcontinent.nenfoundation.client.vfx.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * A silhueta da aura: o modelo do jogador, inflado.
 *
 * <p>ELE HERDA DE {@link PlayerModel} de proposito. A alternativa -- escrever a
 * geometria a mao -- divergiria do modelo real no dia em que o Minecraft
 * mudasse um cubo, e a aura passaria a vestir um corpo que nao existe mais.
 *
 * <p>A INFLACAO E POR CUBO, feita por {@link PlayerModel#createMesh} com uma
 * {@link CubeDeformation} positiva. NAO use {@code poseStack.scale}: a escala
 * acontece em torno do origin de cada parte, entao os bracos se afastam do
 * corpo, a espessura fica diferente entre membros, e a pelicula descola
 * justamente NAS ARTICULACOES -- que e onde a referencia depende de aderencia.
 *
 * <p>SAO DOIS MODELOS, e isso nao e detalhe: {@code default} e {@code slim}.
 * Usar o modelo padrao num jogador com skin Alex deixa a aura do braco larga
 * demais, e o defeito so aparece quando alguem com skin slim entra no servidor.
 *
 * <p>AS CAMADAS DE OVERLAY FICAM INVISIVEIS. A shell e UMA superficie; desenhar
 * tambem chapeu, jaqueta, mangas e calcas dobraria o overdraw para redesenhar
 * a mesma casca alguns milimetros adiante. A inflacao ja passa por fora do
 * overlay da skin (ver {@link AuraGeometryProfile}).
 */
public final class AuraPlayerModel extends PlayerModel<AbstractClientPlayer> {

    public AuraPlayerModel(ModelPart raiz, boolean slim) {
        super(raiz, slim);
        // A shell e uma superficie so. As seis partes base bastam, e sao
        // exatamente as seis regioes do ADR-014.
        this.hat.visible = false;
        this.jacket.visible = false;
        this.leftSleeve.visible = false;
        this.rightSleeve.visible = false;
        this.leftPants.visible = false;
        this.rightPants.visible = false;
    }

    /**
     * A definicao de camada para um passe, ja com a deformacao aplicada.
     *
     * <p>A malha e construida UMA VEZ, no registro, e guardada pelo
     * {@code EntityModelSet}. Construir por frame seria alocar um modelo inteiro
     * a sessenta vezes por segundo, por jogador visivel.
     */
    public static LayerDefinition definicao(CubeDeformation deformacao, boolean slim) {
        return LayerDefinition.create(PlayerModel.createMesh(deformacao, slim), 64, 64);
    }
}
