package com.darkcontinent.nenfoundation.enemy.chimera.nen;

import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraNenStatus;
import java.util.Objects;

/**
 * Decide QUANDO uma formiga deveria usar Nen -- e a resposta quase sempre e "nao".
 *
 * <p><b>O que ele nao faz, e e o mais importante:</b> nao calcula aura, nao
 * aplica custo, nao ativa tecnica e nao conhece {@code AuraPool}. Ele recebe
 * fatos ja medidos e devolve uma {@link TacticalNenIntent}. O Nen Foundation e a
 * unica autoridade sobre Nen (CLAUDE.md); um controlador de inimigo que
 * calculasse a propria aura seria a segunda, e duas autoridades sobre a mesma
 * mecanica divergem sem dar erro -- o sintoma e desbalanceamento que ninguem
 * consegue explicar.</p>
 *
 * <p><b>A ordem das decisoes e a personalidade da formiga.</b> Fugir vem antes de
 * atacar; perceber vem antes de elevar; e manter Ten e o piso de quem ja
 * despertou. Inverter qualquer par nao produz erro: produz uma formiga que gasta
 * aura atacando enquanto morre, ou que foge sem nunca ter lutado.</p>
 *
 * <p><b>Nen caro exige TREINO, e nao so despertar.</b> Ken so aparece para
 * {@link ChimeraNenStatus#TRAINED}. Sem essa trava, a primeira formiga desperta
 * da colonia usaria a defesa mais cara do jogo -- e a progressao que a colonia
 * inteira existe para contar deixaria de ser observavel.</p>
 */
public final class TacticalNenController {

    /**
     * Fracao de aura abaixo da qual a formiga para de gastar.
     *
     * <p>NAO e botao de balanceamento e por isso nao vai para config: e a
     * diferenca entre "economiza" e "fica sem". Uma formiga que gasta ate zero
     * perde o Ten junto com o Ren e morre do golpe seguinte -- o que o jogador le
     * como um bicho que desistiu, sem entender por que.</p>
     */
    public static final double RESERVA_MINIMA = 0.2D;

    /**
     * Fracao de vida abaixo da qual a formiga prefere SUMIR a lutar.
     *
     * <p>Zetsu recolhe a aura inteira: ela fica indefesa e, em troca, deixa de
     * ser sentida. E uma escolha de quem ja perdeu a briga, e por isso o limiar e
     * baixo -- alto demais faria toda formiga ferida desaparecer, e o combate
     * viraria uma perseguicao a fantasmas.</p>
     */
    public static final double VIDA_PARA_FUGIR = 0.2D;

    private final ChimeraNenStatus estagio;
    private int ticksNaIntencao;
    private TacticalNenIntent atual = TacticalNenIntent.NENHUMA;

    public TacticalNenController(ChimeraNenStatus estagio) {
        this.estagio = Objects.requireNonNull(estagio, "estagio de Nen ausente");
    }

    public ChimeraNenStatus estagio() { return estagio; }
    public TacticalNenIntent atual() { return atual; }
    public int ticksNaIntencao() { return ticksNaIntencao; }

    /**
     * Decide a intencao deste tick.
     *
     * @param situacao fatos medidos por quem tem o mundo
     * @return a intencao; {@link TacticalNenIntent#NENHUMA} e resposta legitima
     */
    public TacticalNenIntent decidir(TacticalNenSituation situacao) {
        Objects.requireNonNull(situacao, "situacao ausente");
        TacticalNenIntent proxima = escolher(situacao);
        if (proxima != atual) {
            atual = proxima;
            ticksNaIntencao = 0;
        } else {
            ticksNaIntencao++;
        }
        return atual;
    }

    private TacticalNenIntent escolher(TacticalNenSituation s) {
        // Formiga dormente nao tem o que decidir. Este ramo vem PRIMEIRO para que
        // nenhuma regra abaixo possa, por descuido, dar Nen a quem nao tem.
        if (!estagio.desperto()) return TacticalNenIntent.NENHUMA;

        // Sem aura utilizavel, manter qualquer coisa e gastar o que nao ha.
        if (s.fracaoDeAura() <= RESERVA_MINIMA) return TacticalNenIntent.NENHUMA;

        // FUGIR vem antes de atacar. Uma formiga que ataca enquanto morre nao da
        // erro; da um bicho que parece nao ter instinto nenhum.
        if (s.fracaoDeVida() <= VIDA_PARA_FUGIR && s.podeFugir()) {
            return TacticalNenIntent.ENTRAR_EM_ZETSU;
        }

        if (s.alvoEscondido() && estagio.desperto()) return TacticalNenIntent.USAR_GYO;

        if (s.emCombate()) {
            // Ken so para quem treinou. Sem esta trava a primeira formiga desperta
            // usaria a defesa mais cara do jogo, e a progressao da colonia deixaria
            // de ser observavel.
            if (estagio == ChimeraNenStatus.TRAINED && s.sobPressao()) {
                return TacticalNenIntent.MANTER_KEN;
            }
            if (s.alvoAoAlcance()) return TacticalNenIntent.ELEVAR_REN;
        }

        // O piso de quem despertou: defesa passiva, sempre.
        return TacticalNenIntent.MANTER_TEN;
    }

    /** Limpeza: morte, unload, troca de dimensao. */
    public void limpar() {
        atual = TacticalNenIntent.NENHUMA;
        ticksNaIntencao = 0;
    }
}
