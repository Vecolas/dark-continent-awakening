package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Modelo GeckoLib AUTORAL do kiriko (ADR-017: mob vanilla e andaime).
 *
 * <p>ELE ESTENDE O {@code GeoModel} ABSTRATO, e nao o
 * {@code DefaultedEntityGeoModel} que a maioria dos mobs usa. O motivo e o
 * mesmo do man-faced ape e o unico que justifica a excecao: este mob tem DUAS
 * SILHUETAS. Disfarcado ele e gente; revelado ele e o Magical Beast. O
 * Defaulted resolve os tres caminhos a partir de um id fixo, e um id fixo so
 * descreve um corpo.</p>
 *
 * <p>SAO DOIS IDS ({@code kiriko} e {@code kiriko_disfarce}), e nao um modelo
 * com metade dos ossos escondida. Esconder osso cria uma segunda maneira de
 * mudar a silhueta -- uma no renderer, outra na animacao -- e as duas divergem
 * na primeira correcao, em silencio.</p>
 *
 * <p>OS TRES RECURSOS TROCAM JUNTOS, pela MESMA pergunta
 * ({@link #vestindoCorpoHumano}). Trocar o modelo sem trocar a animacao daria um
 * clipe procurando osso que nao existe -- nenhuma excecao, nenhum log, so um
 * membro parado; trocar o modelo sem trocar a textura daria a silhueta humana
 * pintada com o atlas do bicho, que le como erro de arte.</p>
 *
 * <h2>Por que a pergunta NAO e so {@code estaDisfarcado()}</h2>
 *
 * <p>E a licao que o macaco ensinou, e aqui ela custa mais caro. O clipe
 * {@code animation.kiriko_disfarce.transform} mora no arquivo do DISFARCE e
 * move ossos que so o corpo humano tem ({@code hat}, {@code arm_left}...). A
 * transformacao E o corpo humano se desfazendo: ela precisa ser desenhada COM o
 * corpo humano em cena, do primeiro ao ultimo tick.</p>
 *
 * <p>Lida so por {@code estaDisfarcado()}, a troca aconteceria no tick em que o
 * servidor desliga o disfarce -- ou seja, no tick em que o clipe COMECA. O
 * jogador veria o kiriko ja revelado e a unica cena que o mob tem para
 * contar nunca apareceria, sem erro no log. Por isso a pergunta e a UNIAO:
 * enquanto {@code estaTransformando()} estiver ligado, o corpo desenhado
 * continua sendo o humano.</p>
 *
 * <p>QUEM MANDA E O SERVIDOR. As duas leituras vem de {@code SynchedEntityData};
 * o cliente nao decide disfarce, nao julga ninguem e nao antecipa revelacao.
 * Este arquivo tambem nao le {@code foiReprovado()}: aprovacao e reprovacao
 * levam AO MESMO corpo verdadeiro -- o que muda entre elas e o clipe
 * ({@code approve} ou {@code strike}), e clipe e decisao da entidade.</p>
 */
public final class KirikoGeoModel extends GeoModel<KirikoEntity> {
    private static final ResourceLocation GEO_HUMANO =
            NenFoundation.id("geo/entity/kiriko_disfarce.geo.json");
    private static final ResourceLocation GEO_VERDADEIRO =
            NenFoundation.id("geo/entity/kiriko.geo.json");

    private static final ResourceLocation TEXTURA_HUMANA =
            NenFoundation.id("textures/entity/kiriko_disfarce/humano.png");
    private static final ResourceLocation TEXTURA_VERDADEIRA =
            NenFoundation.id("textures/entity/kiriko/verdadeiro.png");

    private static final ResourceLocation ANIMACAO_HUMANA =
            NenFoundation.id("animations/entity/kiriko_disfarce.animation.json");
    private static final ResourceLocation ANIMACAO_VERDADEIRA =
            NenFoundation.id("animations/entity/kiriko.animation.json");

    /**
     * A pergunta unica que troca os TRES recursos ao mesmo tempo.
     *
     * <p>Verdadeira enquanto o kiriko estiver disfarcado E durante a
     * transformacao inteira. A segunda metade e a que nao se deduz olhando o
     * codigo em jogo: sem ela o corpo humano sai de cena no primeiro tick do
     * clipe que existe para mostra-lo saindo de cena.</p>
     *
     * <p>Nao existe estado do CLIENTE aqui dentro. Ela e uma funcao pura das
     * duas flags sincronizadas, e e por isso que ela pode ser a unica fonte:
     * qualquer um dos tres {@code get...Resource} que respondesse sozinho
     * viraria uma quarta resposta possivel para a mesma pergunta.</p>
     */
    private static boolean vestindoCorpoHumano(KirikoEntity kiriko) {
        return kiriko.estaDisfarcado() || kiriko.estaTransformando();
    }

    @Override
    public ResourceLocation getModelResource(KirikoEntity kiriko) {
        return vestindoCorpoHumano(kiriko) ? GEO_HUMANO : GEO_VERDADEIRO;
    }

    @Override
    public ResourceLocation getTextureResource(KirikoEntity kiriko) {
        return vestindoCorpoHumano(kiriko) ? TEXTURA_HUMANA : TEXTURA_VERDADEIRA;
    }

    @Override
    public ResourceLocation getAnimationResource(KirikoEntity kiriko) {
        return vestindoCorpoHumano(kiriko) ? ANIMACAO_HUMANA : ANIMACAO_VERDADEIRA;
    }
}
