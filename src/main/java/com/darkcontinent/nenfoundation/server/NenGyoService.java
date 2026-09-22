package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.aura.FocoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Onde cada jogador esta concentrando a aura.
 *
 * <p>POR QUE ISTO NAO MORA NA TECNICA. A implementacao de {@code Gyo} e uma so,
 * compartilhada por todos os jogadores do servidor -- e o erro numero 2 da
 * lista do CLAUDE.md e exatamente guardar estado de jogador num campo dela.
 * Dois jogadores concentrando em membros diferentes escreveriam no mesmo campo,
 * sem erro nenhum: um deles veria a aura no lugar errado, e ninguem saberia
 * dizer por que.
 *
 * <p>POR QUE NAO MORA NO RUNTIME. A alocacao no {@code RuntimeNenState} e
 * DERIVADA -- ela e o resultado. Isto aqui e a ESCOLHA, a entrada do calculo.
 * Misturar as duas faria o recalculo ler o proprio resultado.
 *
 * <p>MEMORIA DE SESSAO, e nao progresso: a escolha morre com o logout, como
 * qualquer estado de combate (ADR-002).
 *
 * <p><b>O CONTROLE CHEGOU EM 2026-09-22.</b> Ate ali este javadoc declarava o
 * proprio buraco: <i>"o jogador ainda nao tem como escolher a regiao. Nao ha
 * payload nem interface para isso, entao todo mundo concentra no padrao."</i>
 * Agora ha: {@code EscolherFocoC2S} e a tecla {@code G}, que percorre as seis
 * regioes. O padrao continua sendo a CABECA, para quem nunca apertar.
 */
public final class NenGyoService {

    /**
     * A regiao padrao e a CABECA, e a escolha nao e arbitraria.
     *
     * <p>Gyo nos olhos e a aplicacao famosa da tecnica -- a que percebe aura
     * sutil e revela o que In esconde. Enquanto nao houver como escolher, o
     * padrao e o uso mais reconhecivel, e nao o primeiro do enum.
     */
    private static final RegiaoDoCorpo PADRAO = RegiaoDoCorpo.CABECA;

    private static final Map<UUID, RegiaoDoCorpo> ESCOLHIDA = new ConcurrentHashMap<>();

    private NenGyoService() {
    }

    /** Onde este jogador concentra. Nunca nula. */
    public static RegiaoDoCorpo regiaoDe(ServerPlayer jogador) {
        return regiaoDe(jogador.getUUID());
    }

    /**
     * Idem, por UUID.
     *
     * <p>EXISTE PORQUE O INICIO DE SESSAO NAO TEM {@code ServerPlayer} -- ele
     * roda por UUID. Sem esta sobrecarga, o recalculo do primeiro tick
     * precisaria de um jogador que ainda nao existe, e a saida facil seria um
     * ThreadLocal escondendo o argumento que falta.
     */
    public static RegiaoDoCorpo regiaoDe(UUID jogadorId) {
        return ESCOLHIDA.getOrDefault(jogadorId, PADRAO);
    }

    /**
     * O foco completo deste jogador: a regiao escolhida e o braco dominante.
     *
     * <p>O BRACO SAI DO JOGADOR, e nao de uma escolha: quem joga canhoto tem a
     * mao dominante a esquerda, e Shu tem de concentrar la. Assumir o direito
     * funcionaria para a maioria e poria a aura no braco errado de algumas
     * pessoas -- um defeito que nao da erro nenhum.
     */
    public static FocoDeAura focoDe(ServerPlayer jogador) {
        return new FocoDeAura(regiaoDe(jogador), bracoDominanteDe(jogador));
    }

    /**
     * Idem, por UUID: o inicio de sessao nao tem {@code ServerPlayer}.
     *
     * <p>Cai no braco padrao, e isso basta: no primeiro recalculo nao ha
     * tecnica ativa nenhuma, entao o braco nao e consultado por ninguem.
     */
    public static FocoDeAura focoDe(UUID jogadorId) {
        return new FocoDeAura(regiaoDe(jogadorId), FocoDeAura.padrao().bracoPrincipal());
    }

    private static RegiaoDoCorpo bracoDominanteDe(ServerPlayer jogador) {
        return jogador.getMainArm() == HumanoidArm.LEFT
                ? RegiaoDoCorpo.BRACO_ESQUERDO
                : RegiaoDoCorpo.BRACO_DIREITO;
    }

    /** Troca a escolha deste jogador. */
    public static void escolher(ServerPlayer jogador, RegiaoDoCorpo regiao) {
        if (regiao == null) {
            throw new IllegalArgumentException("regiao obrigatoria");
        }
        ESCOLHIDA.put(jogador.getUUID(), regiao);
    }

    /** Esquece o jogador. Chamado quando ele sai. */
    public static void esquecer(ServerPlayer jogador) {
        ESCOLHIDA.remove(jogador.getUUID());
    }

    /** Apaga tudo. Encerramento do servidor e testes. */
    public static void limparTudo() {
        ESCOLHIDA.clear();
    }

}
