package com.darkcontinent.nenfoundation.enemy.balance;

import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import java.util.Objects;

/**
 * Quanto tempo o combate dura, dos DOIS lados -- calculado, e nao cronometrado.
 *
 * <p><b>Por que uma conta, e nao uma medicao.</b> Cronometrar em jogo e a prova
 * final e custa uma sessao por bicho; com vinte e quatro, ninguem refaz a
 * medicao depois de girar um numero. A conta roda no build, vale para os vinte e
 * quatro de uma vez, e reprova o outlier antes de alguem entrar no jogo. Ela nao
 * substitui a medicao: ela impede que a medicao seja gasta com o caso obvio.</p>
 *
 * <p><b>O que ela NAO modela, e esta declarado:</b> desvio, telegrafo, terreno,
 * cura, pocao, encantamento, critico, knockback e a chance de o jogador errar.
 * Ela e o LIMITE SUPERIOR de eficiencia -- dois lados parados trocando golpes.
 * Um combate real sempre dura mais; se ja o limite superior estiver fora da
 * faixa, o real esta pior.</p>
 */
public record PrevisaoDeCombate(double segundosParaMatar, double segundosParaMorrer) {

    /**
     * Tempo atribuido a um mob que nao causa dano nenhum.
     *
     * <p>Nao e infinito de proposito: infinito contamina medias, ordenacoes e
     * qualquer relatorio, e o sintoma e uma tabela cheia de valores nao
     * numericos que ninguem le. Uma hora e claramente "nunca" e continua sendo
     * um numero.</p>
     */
    public static final double SEM_AMEACA = 3600.0D;

    public PrevisaoDeCombate {
        if (!Double.isFinite(segundosParaMatar) || segundosParaMatar <= 0) {
            throw new IllegalArgumentException("tempo para matar invalido: " + segundosParaMatar);
        }
        if (!Double.isFinite(segundosParaMorrer) || segundosParaMorrer <= 0) {
            throw new IllegalArgumentException("tempo para morrer invalido: " + segundosParaMorrer);
        }
    }

    /**
     * Preve o combate entre um loadout e um inimigo.
     *
     * @param golpesPorSegundoDoMob cadencia do mob; ela sai da RECARGA do ataque
     *        dele, e nao de um chute -- um mob com recarga de 60 ticks bate 0.33
     *        vez por segundo, e tratar todos como um golpe por segundo erra por
     *        tres vezes, sempre para o lado de fazer o mob parecer mais perigoso
     */
    public static PrevisaoDeCombate de(LoadoutDeReferencia loadout, EnemyAttributes mob,
            double golpesPorSegundoDoMob) {
        Objects.requireNonNull(loadout, "loadout ausente");
        Objects.requireNonNull(mob, "atributos do mob ausentes");
        if (!Double.isFinite(golpesPorSegundoDoMob) || golpesPorSegundoDoMob <= 0) {
            throw new IllegalArgumentException("cadencia do mob invalida");
        }

        // O mob TAMBEM tem armadura, e ela usa a mesma formula do vanilla. Medir
        // dano bruto contra vida faria o King White Stag Beetle, de armadura 9,
        // pontuar igual a um bicho sem couro nenhum -- e a armadura, que e a
        // ficha dele, nao apareceria em lugar nenhum da conta.
        double danoNoMob = reduzir(loadout.danoPorGolpe(), mob.armor(), 0.0D);
        double dpsNoMob = danoNoMob * loadout.golpesPorSegundo();

        double danoNoJogador = loadout.danoRecebido(mob.attackDamage());
        double dpsNoJogador = danoNoJogador * golpesPorSegundoDoMob;

        // Mob de dano zero nao mata nunca; dividir por zero devolveria infinito, e
        // infinito quebra toda comparacao a jusante.
        double paraMorrer = dpsNoJogador <= 0.0D ? SEM_AMEACA : loadout.vida() / dpsNoJogador;
        return new PrevisaoDeCombate(mob.maxHealth() / dpsNoMob, paraMorrer);
    }

    /** A formula do vanilla, isolada -- ela aparece nos dois lados da conta. */
    private static double reduzir(double danoBruto, double armadura, double tenacidade) {
        double reducao = Math.min(20.0D,
                Math.max(armadura / 5.0D, armadura - danoBruto / (2.0D + tenacidade / 4.0D)));
        return danoBruto * (1.0D - reducao / 25.0D);
    }

    /**
     * A razao entre os dois tempos: acima de 1 o jogador perde a troca.
     *
     * <p>E o numero que diz se o encontro e sobre MATAR ou sobre outra coisa. Um
     * mob cuja razao passa de 1 nao esta necessariamente errado -- o Master of
     * the Swamp e assim de proposito, porque o caminho dele e a captura. O que
     * seria errado e isso acontecer sem ninguem ter decidido.</p>
     */
    public double razaoDeTroca() { return segundosParaMatar / segundosParaMorrer; }
}
