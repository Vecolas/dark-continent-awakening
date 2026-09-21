package com.darkcontinent.nenfoundation.client.vfx.model;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

/**
 * Como a aura encontra um CORPO, qualquer que seja o corpo.
 *
 * <p><b>POR QUE ELE EXISTE, JA QUE NENHUM INIMIGO TEM NEN HOJE.</b> Desenhar o
 * renderer so para o corpo de jogador e a decisao que obriga a reescreve-lo
 * depois. O [ADR-012](docs/adr/ADR-012-geckolib-obrigatorio.md) ja diz que a
 * aura de mob customizado usa os BONES do GeckoLib, e nao o modelo de jogador --
 * e "o modelo de jogador em cima de um quadrupede" nao e um bug que se conserta
 * com ajuste: e um renderer inteiro no lugar errado.
 *
 * <p>DUAS RESPONSABILIDADES, E NENHUMA A MAIS:
 *
 * <ol>
 *   <li>devolver as SUPERFICIES inflaveis, para a shell;</li>
 *   <li>resolver uma {@link AuraAnchor} para uma transformacao, para os
 *       filamentos.</li>
 * </ol>
 *
 * <p>NAO E RESPONSABILIDADE DELE decidir cor, intensidade, perfil, visibilidade
 * ou qualquer coisa de gameplay. Ele responde ONDE, e nunca QUANTO.
 *
 * <p><b>OSSO AUSENTE E RECUSA COM MOTIVO</b>, e nao silencio nem log por quadro.
 * Um modelo sem o bone pedido perde os filamentos e mantem a shell -- e o log
 * sai UMA VEZ por modelo. Um log por quadro a sessenta hertz enche o arquivo em
 * minutos e esconde justamente a linha que importa; o silencio deixa a pessoa
 * procurando por que a aura daquele mob nao tem filamento.
 *
 * <p>CLIENT-ONLY, e nada em {@code nen/}, {@code api/}, {@code network/} ou
 * {@code server/} importa daqui -- o portao {@code PacotesDeclaradosTest} cobra.
 */
public interface AuraModelAdapter {

    /**
     * Empilha a transformacao da regiao pedida e devolve {@code true}.
     *
     * <p>QUEM CHAMA E RESPONSAVEL PELO {@code popPose}, e so quando a resposta
     * for {@code true}. Empilhar dentro e desempilhar fora e a assimetria que
     * produz vazamento de pilha -- e pilha vazada nao lanca: desloca tudo o que
     * vier depois, no quadro inteiro.
     *
     * @return {@code false} quando este corpo nao tem a regiao pedida
     */
    boolean empilharRegiao(PoseStack pilha, AuraBodyRegion regiao);

    /**
     * Empilha a transformacao da ancora pedida e devolve {@code true}.
     *
     * <p>MESMA REGRA DO {@code popPose} acima.
     *
     * @return {@code false} quando o osso desta ancora nao existe neste modelo
     */
    boolean empilharAncora(PoseStack pilha, AuraAnchor ancora);

    /**
     * Desenha a superficie inflada de uma regiao.
     *
     * @param alpha ja com intensidade, regiao e fase aplicados
     */
    void desenharRegiao(PoseStack pilha, com.mojang.blaze3d.vertex.VertexConsumer vertices,
            AuraBodyRegion regiao, int luz, int argb);

    /** Se este corpo tem a regiao. Um quadrupede nao tem braco. */
    boolean temRegiao(AuraBodyRegion regiao);

    /**
     * O adaptador de uma entidade, ou {@code null} se nao houver um.
     *
     * <p>DEVOLVER {@code null} E A RESPOSTA CERTA, e nao lancar: o caminho de
     * desenho nao pode lancar, e uma entidade sem adaptador simplesmente nao
     * ganha aura. O contrario -- inventar um adaptador de jogador para qualquer
     * coisa -- desenharia um corpo humano em cima de um bicho.
     */
    @FunctionalInterface
    interface Fabrica {
        @Nullable
        AuraModelAdapter para(LivingEntity entidade);
    }
}
