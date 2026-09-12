package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import com.darkcontinent.nenfoundation.nen.combat.AtaqueDeNen;
import com.darkcontinent.nenfoundation.nen.combat.DefesaDeNen;
import com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.ProtegeComAura;
import com.darkcontinent.nenfoundation.nen.technique.ReforcaGolpe;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * O UNICO lugar onde a aura mexe em dano -- nos dois sentidos.
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

    /**
     * A ORDEM, escrita uma vez e num lugar so: <b>primeiro o ataque, depois a
     * defesa.</b>
     *
     * <p>E HOJE ELA NAO MUDA NUMERO NENHUM, e isso precisa estar escrito. Os
     * dois lados sao multiplicativos -- {@code x (1 + reforco)} e
     * {@code x (1 - reducao)} -- e multiplicacao comuta: inverter as duas
     * chamadas da exatamente o mesmo dano. Eu escrevi aqui, na primeira versao,
     * que invertida "um Ko atravessaria um Ken", alimentei a mutacao que
     * inverte as chamadas, e ela passou pelos 120 gametests. A justificativa
     * estava errada; a ordem continua, e por outro motivo.
     *
     * <p>O MOTIVO REAL: o primeiro modificador que NAO for multiplicativo --
     * um dano fixo somado, um piso, um dano que ignora aura -- deixa de comutar
     * no dia em que entrar, e ai a ordem passa a decidir o numero em silencio.
     * Ter o lugar definido antes disso e o que impede a pergunta "onde isto
     * entra?" de ser respondida por acaso, no meio de outra tarefa.
     *
     * <p>UM `setAmount` SO, no fim. Os dois lados sao calculados sobre valores
     * locais e o evento e escrito uma vez. Isto e o erro numero 5 do CLAUDE.md
     * tratado na raiz: nao basta haver um handler se dentro dele o dano for
     * remexido em dois lugares que podem se cruzar depois.
     */
    @SubscribeEvent
    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        float dano = evento.getAmount();
        if (!(dano > 0.0F)) {
            return;
        }

        float depoisDoAtaque = comReforcoDeQuemBateu(dano, evento.getSource());
        float depoisDaDefesa = comDefesaDeQuemApanhou(depoisDoAtaque, evento);

        if (depoisDaDefesa != dano) {
            evento.setAmount(depoisDaDefesa);
        }
    }

    /**
     * O lado ofensivo.
     *
     * <p>QUEM BATE PODE NAO SER QUEM APANHA TER PERFIL: um jogador em Ren
     * batendo num zumbi tem de bater mais forte, e o zumbi nao tem aura
     * nenhuma. Por isso este lado olha para o ATACANTE e nao para a vitima --
     * e por isso o handler nao pode mais desistir cedo quando a vitima nao e
     * jogador, como ele fazia enquanto so existia defesa.
     *
     * <p>O BRACO DOMINANTE E A REGIAO QUE CONTA. Quem bate, bate com o braco.
     * Um jogador concentrado na cabeca -- que e o padrao de Gyo -- perde golpe,
     * e essa e a troca que a concentracao sempre prometeu sem nunca cobrar.
     */
    private static float comReforcoDeQuemBateu(float dano, DamageSource fonte) {
        if (fonte == null) {
            return dano;
        }
        Entity atacante = fonte.getEntity();
        if (!(atacante instanceof ServerPlayer jogador)) {
            return dano;
        }
        RuntimeNenState estado = estadoOuNulo(jogador);
        if (estado == null) {
            return dano;
        }
        RegiaoDoCorpo braco = NenGyoService.focoDe(jogador).bracoPrincipal();
        float reforco = AtaqueDeNen.reforco(
                reforcoDe(estado.tecnicasAtivas()),
                estado.alocacao(),
                braco,
                NenConfig.tetoDeReforcoDeDano());

        return reforco <= 0.0F ? dano : AtaqueDeNen.danoDepoisDoReforco(dano, reforco);
    }

    /** O lado defensivo, que ja existia. */
    private static float comDefesaDeQuemApanhou(float dano,
            LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) {
            return dano;
        }
        RuntimeNenState estado = estadoOuNulo(jogador);
        if (estado == null) {
            return dano;
        }
        FaixaDoCorpo faixa = faixaAtingida(jogador, evento.getSource());
        float reducao = DefesaDeNen.reducao(
                protecaoDe(estado.tecnicasAtivas()),
                estado.alocacao(),
                faixa,
                NenConfig.tetoDeReducaoDeDano());

        return reducao <= 0.0F ? dano : DefesaDeNen.danoDepoisDaAura(dano, reducao);
    }

    /**
     * O runtime do jogador, ou nulo.
     *
     * <p>Jogador sem sessao acontece entre entrar na lista e a sessao comecar,
     * e nao e erro -- ele so nao tem aura para segurar nem para somar.
     */
    private static RuntimeNenState estadoOuNulo(ServerPlayer jogador) {
        try {
            return NenRuntimeService.estadoDe(jogador);
        } catch (IllegalStateException semSessao) {
            return null;
        }
    }

    /**
     * O reforco das tecnicas ativas.
     *
     * <p>VALE A MAIOR, e nao a soma, pelo mesmo motivo da protecao: somar faria
     * duas tecnicas modestas darem um golpe que nenhuma das duas promete.
     */
    public static double reforcoDe(Set<ResourceLocation> ativas) {
        double maior = 0.0D;
        for (ResourceLocation id : ativas) {
            NenTechnique tecnica = NenTechniqueService.registro().porId(id).orElse(null);
            if (tecnica instanceof ReforcaGolpe reforcadora) {
                maior = Math.max(maior, reforcadora.reforcoBase());
            }
        }
        return maior;
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
