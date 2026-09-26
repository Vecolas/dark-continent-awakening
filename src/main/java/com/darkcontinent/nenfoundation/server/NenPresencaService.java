package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.PresencaDeAura;
import com.darkcontinent.nenfoundation.network.payload.PresencaDeAuraS2C;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Conta aos outros o que da para perceber da aura de alguem.
 *
 * <p>E O UNICO LUGAR DO MOD QUE FALA DE TERCEIROS, e por isso ele decide antes
 * de enviar em vez de enviar e pedir discricao. {@link PresencaDeAura} devolve
 * {@link SinalDeAura#NENHUM} para quem esta em Zetsu -- o mesmo valor de quem
 * nunca despertou -- e e esse valor que viaja.
 *
 * <p>SO NA MUDANCA. O sinal e um enum de tres valores e muda quando o jogador
 * liga ou desliga tecnica, ou seja, raramente. Enviar por tick gastaria banda
 * para repetir o mesmo byte sessenta vezes por segundo, e nenhum portao
 * acusaria isso -- apareceria como trafego que ninguem sabe explicar.
 *
 * <p>QUEM LIGA, DESLIGA: o ultimo sinal conhecido de cada jogador vive num mapa
 * proprio, e {@link #esquecer} e chamado no mesmo ponto de saida onde a sessao
 * de runtime acaba. Sem isso o mapa cresceria por toda a vida do servidor.
 */
public final class NenPresencaService {

    /**
     * O ultimo sinal ANUNCIADO de cada jogador.
     *
     * <p>Guardado a parte do runtime de proposito: ele nao e estado de Nen, e
     * sim memoria de rede -- "o que ja foi dito" -- e misturar as duas coisas
     * faria o reinicio de sessao por morte reenviar tudo sem motivo.
     */
    private static final Map<UUID, SinalDeAura> ULTIMO_ANUNCIADO = new ConcurrentHashMap<>();

    /**
     * A ultima FORMA anunciada de cada jogador.
     *
     * <p>SEPARADA DO SINAL, e nao um record com os dois, porque ela muda por
     * outra razao: o sinal muda ao ligar tecnica, a forma muda tambem ao
     * apertar {@code G}. Com um campo so, mudar de regiao dentro do mesmo Gyo
     * nao dispararia anuncio -- e os observadores continuariam vendo a aura no
     * lugar antigo, sem erro nenhum.
     */
    private static final Map<UUID, AlocacaoDeAura> ULTIMA_FORMA = new ConcurrentHashMap<>();

    private NenPresencaService() {
    }

    /**
     * A forma da aura deste jogador agora.
     *
     * <p>VEM DO RUNTIME, que e onde o recalculo do servidor a deixou. Recalcular
     * aqui seria a segunda fonte da mesma verdade, e as duas divergiriam no dia
     * em que uma tecnica nova mudasse a conta.
     *
     * <p>QUEM ESTA EM ZETSU MANDA UNIFORME, e nao a forma real: a alocacao de
     * quem se suprime nao pode atravessar a rede, pelo mesmo motivo que o sinal
     * dele vira NENHUM.
     */
    public static AlocacaoDeAura formaDe(ServerPlayer jogador) {
        if (sinalDe(jogador) == SinalDeAura.NENHUM) {
            return AlocacaoDeAura.uniforme();
        }
        try {
            return NenRuntimeService.estadoDe(jogador).alocacao();
        } catch (IllegalStateException semSessao) {
            // Mesma guarda de `tecnicasAtivasDe`: entre a entrada na lista e o
            // inicio da sessao nao ha runtime, e isso nao e erro.
            return AlocacaoDeAura.uniforme();
        }
    }

    /** O sinal que este jogador emite agora, calculado do zero. */
    public static SinalDeAura sinalDe(ServerPlayer jogador) {
        return PresencaDeAura.percebida(
                NenProfileService.ler(jogador).awakened(),
                tecnicasAtivasDe(jogador));
    }

    /**
     * Recalcula e avisa os observadores, se mudou.
     *
     * @return {@code true} quando algo foi enviado
     */
    public static boolean anunciarSeMudou(ServerPlayer jogador) {
        SinalDeAura agora = sinalDe(jogador);
        AlocacaoDeAura forma = formaDe(jogador);
        SinalDeAura sinalAntes = ULTIMO_ANUNCIADO.put(jogador.getUUID(), agora);
        AlocacaoDeAura formaAntes = ULTIMA_FORMA.put(jogador.getUUID(), forma);
        // OS DOIS PRECISAM SER COMPARADOS. Trocar de regiao com o mesmo Gyo
        // ligado nao muda o sinal -- e sem a segunda comparacao os observadores
        // ficariam vendo a aura no lugar de onde ela saiu.
        if (agora == sinalAntes && forma.equals(formaAntes)) {
            return false;
        }
        enviarPara(jogador, agora, forma);
        return true;
    }

    /**
     * Manda o estado atual a quem acabou de comecar a enxergar este jogador.
     *
     * <p>SEM ISTO O BUG E CLASSICO E CHATO: quem chega perto de alguem que ja
     * esta em Ren nao ve nada, porque o anuncio aconteceu antes de ele estar
     * olhando. A aura so apareceria quando a outra pessoa alternasse a tecnica,
     * e o relato seria "as vezes a aura nao aparece".
     */
    public static void anunciarPara(ServerPlayer observador, ServerPlayer alvo) {
        if (!consegueReceber(observador)) {
            return;
        }
        PacketDistributor.sendToPlayer(observador,
                new PresencaDeAuraS2C(alvo.getId(), sinalDe(alvo), formaDe(alvo)));
    }

    /** Esquece o jogador. Chamado quando ele sai. */
    public static void esquecer(ServerPlayer jogador) {
        ULTIMO_ANUNCIADO.remove(jogador.getUUID());
        ULTIMA_FORMA.remove(jogador.getUUID());
    }

    /** Apaga tudo. Usado no encerramento do servidor e nos testes. */
    public static void limpar() {
        ULTIMO_ANUNCIADO.clear();
        ULTIMA_FORMA.clear();
    }

    /**
     * O envio.
     *
     * <p>LACO EXPLICITO, E NAO {@code sendToPlayersTrackingEntity}. A primeira
     * versao usava aquele metodo justamente para nao inventar uma definicao
     * propria de "perto" -- e ele reprovou nove gametests de uma vez com
     * {@code Payload nenfoundation:aura_presence may not be sent to the
     * client!}.
     *
     * <p>O motivo esta documentado ha tempos em {@code NenSyncService}: enviar
     * a uma conexao que nao negociou o canal do mod LANCA. Vale para jogador de
     * teste, FakePlayer de outro mod e conexao em encerramento. O envio em
     * massa nao deixa conferir destinatario por destinatario, entao ele nao
     * serve aqui.
     *
     * <p>A DISTANCIA CONTINUA SENDO A DO JOGO, e nao um numero meu: sai da
     * distancia de visao do servidor. Assim ela acompanha a configuracao de
     * quem hospeda em vez de discordar dela.
     */
    private static void enviarPara(ServerPlayer jogador, SinalDeAura sinal,
            AlocacaoDeAura forma) {
        PresencaDeAuraS2C payload = new PresencaDeAuraS2C(jogador.getId(), sinal, forma);
        double alcance = alcanceDeVisao(jogador);
        double alcanceAoQuadrado = alcance * alcance;

        for (ServerPlayer observador : jogador.serverLevel().players()) {
            if (observador == jogador
                    || observador.distanceToSqr(jogador) > alcanceAoQuadrado
                    || !consegueReceber(observador)) {
                continue;
            }
            PacketDistributor.sendToPlayer(observador, payload);
        }
    }

    /** Em blocos, tirado da distancia de visao configurada no servidor. */
    private static double alcanceDeVisao(ServerPlayer jogador) {
        return jogador.serverLevel().getServer().getPlayerList().getViewDistance() * 16.0D;
    }

    /**
     * Se a conexao deste observador consegue receber o payload.
     *
     * <p>Nao receber NAO dessincroniza ninguem: quem nao tem o canal tambem nao
     * tem o mod, e nao ha aura para desenhar do lado de la. Pular e a resposta
     * correta, e nao um contorno.
     */
    private static boolean consegueReceber(ServerPlayer observador) {
        return observador.connection != null
                && observador.connection.hasChannel(PresencaDeAuraS2C.TYPE);
    }

    private static java.util.Set<net.minecraft.resources.ResourceLocation> tecnicasAtivasDe(
            ServerPlayer jogador) {
        try {
            return NenRuntimeService.estadoDe(jogador).tecnicasAtivas();
        } catch (IllegalStateException semSessao) {
            // Jogador sem runtime nao emite nada. Acontece entre a entrada na
            // lista e o inicio da sessao, e nao e erro.
            return java.util.Set.of();
        }
    }
}
