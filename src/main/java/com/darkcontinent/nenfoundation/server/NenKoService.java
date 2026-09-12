package com.darkcontinent.nenfoundation.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import net.minecraft.server.level.ServerPlayer;

/**
 * Quanto tempo falta do Ko de cada jogador.
 *
 * <p>KO E UM GOLPE, e nao um estado. No canone ele concentra quase toda a aura
 * num ponto para UM ataque -- e e isso que o separa de "Gyo com um numero
 * maior". Gyo se sustenta; Ko e um compromisso curto do qual nao da para
 * desistir no meio.
 *
 * <p>POR QUE O TEMPO MORA AQUI, e nao na tecnica: a implementacao de {@code Ko}
 * e uma so, compartilhada por todos os jogadores do servidor. Um contador num
 * campo dela seria o erro numero 2 da lista do CLAUDE.md -- dois jogadores
 * usando Ko ao mesmo tempo dividiriam o mesmo relogio, e um deles veria a
 * tecnica cair sozinha sem motivo.
 *
 * <p>ISTO E O SEGUNDO MAPA AD-HOC de estado por jogador neste pacote, depois do
 * {@code NenGyoService}. Dois ainda cabem; o TERCEIRO e sinal de que falta uma
 * casa comum para "escolha de jogador que nao e progresso e nao e derivado".
 * Esta escrito para que a terceira vez seja uma decisao, e nao mais uma
 * repeticao.
 *
 * <p>A limpeza mora nos MESMOS pontos de saida que ja limpam o Gyo -- um lugar
 * so, como o erro numero 3 da lista exige.
 */
public final class NenKoService implements Ko.RelogioDeKo {

    /** A instancia unica; Ko recebe esta, e nao um campo estatico escondido. */
    public static final NenKoService INSTANCIA = new NenKoService();

    private static final Map<UUID, Integer> RESTANTE = new ConcurrentHashMap<>();

    private NenKoService() {
    }


    @Override
    public void iniciar(ServerPlayer jogador, int ticks) {
        if (ticks <= 0) {
            RESTANTE.remove(jogador.getUUID());
            return;
        }
        RESTANTE.put(jogador.getUUID(), ticks);
    }

    /**
     * Passa um tick e diz se o prazo acabou.
     *
     * <p>DEVOLVE {@code true} TAMBEM QUANDO NAO HA CONTAGEM. Um Ko ativo sem
     * prazo registrado e estado quebrado -- e o padrao seguro e encerrar, e nao
     * deixar ligado para sempre uma tecnica que devia durar um golpe.
     */
    @Override
    public boolean passarTickEVerSeAcabou(ServerPlayer jogador) {
        Integer restante = RESTANTE.get(jogador.getUUID());
        if (restante == null) {
            return true;
        }
        int proximo = restante - 1;
        if (proximo <= 0) {
            RESTANTE.remove(jogador.getUUID());
            return true;
        }
        RESTANTE.put(jogador.getUUID(), proximo);
        return false;
    }

    /** Quantos ticks faltam, para o dump e para os testes. Zero quando nao ha Ko. */
    public static int restanteDe(ServerPlayer jogador) {
        return RESTANTE.getOrDefault(jogador.getUUID(), 0);
    }

    @Override
    public void limpar(ServerPlayer jogador) {
        RESTANTE.remove(jogador.getUUID());
    }

    /** Esquece o jogador. Chamado quando ele sai do servidor. */
    public static void esquecer(ServerPlayer jogador) {
        RESTANTE.remove(jogador.getUUID());
    }

    /** Apaga tudo. Encerramento do servidor e testes. */
    public static void limparTudo() {
        RESTANTE.clear();
    }
}
