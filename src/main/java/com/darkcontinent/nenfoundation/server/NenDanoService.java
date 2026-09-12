package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.combat.DefesaDeNen;
import com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.ProtegeComAura;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * O UNICO lugar onde a aura reduz dano.
 *
 * <p>O {@code package-info} de {@code nen.combat} abre com a regra que este
 * arquivo existe para cumprir: <i>existe UMA ordem documentada de aplicacao de
 * modificadores, e ela mora num lugar so</i>. Multiplicador aplicado em dois
 * handlers multiplica duas vezes, e o numero final e plausivel demais para
 * alguem notar sem medir.
 *
 * <p>NO EVENTO DE ENTRADA, e nao depois da armadura. No canone a aura e a
 * camada mais externa -- o golpe encontra a aura antes de encontrar qualquer
 * outra coisa. Reduzir aqui tambem faz a armadura trabalhar sobre o que sobrou,
 * que e a ordem correta e nao um detalhe.
 *
 * <p>VALE PARA TODO DANO, e nao so para dano de Nen. No canone a aura protege o
 * corpo: Gon aguenta golpes fisicos com Ken. E nao existe dano de Nen ainda --
 * restringir a ele faria esta camada nascer sem consumidor nenhum, que e
 * exatamente o falso verde que este projeto ja cometeu uma vez.
 *
 * <p>SO JOGADOR. Mob nao tem perfil de Nen, e chamar {@code estadoDe} para cada
 * criatura ferida no mundo criaria sessao para quem nao tem.
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenDanoService {

    private NenDanoService() {
    }

    @SubscribeEvent
    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) {
            return;
        }
        float dano = evento.getAmount();
        if (!(dano > 0.0F)) {
            return;
        }

        RuntimeNenState estado;
        try {
            estado = NenRuntimeService.estadoDe(jogador);
        } catch (IllegalStateException semSessao) {
            // Jogador sem runtime nao tem aura para segurar nada. Acontece
            // entre entrar na lista e a sessao comecar, e nao e erro.
            return;
        }

        FaixaDoCorpo faixa = faixaAtingida(jogador, evento.getSource());
        float reducao = DefesaDeNen.reducao(
                protecaoDe(estado.tecnicasAtivas()),
                estado.alocacao(),
                faixa,
                NenConfig.tetoDeReducaoDeDano());

        if (reducao <= 0.0F) {
            return;
        }
        evento.setAmount(DefesaDeNen.danoDepoisDaAura(dano, reducao));
    }

    /**
     * A protecao das tecnicas ativas.
     *
     * <p>VALE A MAIOR, e nao a soma. Somar faria duas tecnicas modestas darem
     * uma protecao que nenhuma das duas promete -- o mesmo motivo pelo qual o
     * teto de Output e o maior e nao a soma.
     *
     * <p>E ZERO VENCE QUANDO E EXPLICITO: Zetsu implementa a interface
     * devolvendo zero, e isso nao e "nao mexe" -- e "apago a protecao". Como
     * ele exclui todas as outras, o maior acaba sendo o zero dele de qualquer
     * jeito; a regra nao depende disso, mas hoje ela se apoia nesse fato, e
     * esta escrito para quando alguem criar a tecnica que combine com Zetsu.
     *
     * <p>PUBLICO PARA O PORTAO. A regra "vale a maior" e invisivel em jogo --
     * Zetsu exclui todas as outras protetoras, entao nunca ha duas ativas, e a
     * mutacao que troca {@code max} por {@code +=} passou por todos os 92
     * gametests. O portao usa tecnicas falsas que CONVIVEM.
     */
    public static double protecaoDe(Set<ResourceLocation> ativas) {
        double maior = 0.0D;
        for (ResourceLocation id : ativas) {
            NenTechnique tecnica = NenTechniqueService.registro().porId(id).orElse(null);
            if (tecnica instanceof ProtegeComAura protetora) {
                maior = Math.max(maior, protetora.protecaoBase());
            }
        }
        return maior;
    }

    /**
     * Onde o golpe acertou, pela altura de onde ele veio.
     *
     * <p>SEM POSICAO DE ORIGEM, o padrao e o TRONCO. Dano de fome, de veneno e
     * de queda nao vem de lugar nenhum -- e o tronco e a faixa que representa
     * "o corpo", que e onde esse tipo de dano acontece.
     */
    static FaixaDoCorpo faixaAtingida(ServerPlayer jogador, DamageSource fonte) {
        if (fonte == null || fonte.getSourcePosition() == null) {
            return FaixaDoCorpo.TRONCO;
        }
        double altura = jogador.getBbHeight();
        if (!(altura > 0.0D)) {
            return FaixaDoCorpo.TRONCO;
        }
        double relativa = (fonte.getSourcePosition().y - jogador.getY()) / altura;
        return FaixaDoCorpo.porAltura(relativa);
    }
}
