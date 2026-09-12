package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

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
 * <p>PONTO CEGO DECLARADO: <b>o jogador ainda nao tem como escolher a
 * regiao</b>. Nao ha payload nem interface para isso, entao todo mundo
 * concentra no padrao. A tecnica funciona e a alocacao muda de verdade; o que
 * falta e o controle. Ver a issue #162.
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
