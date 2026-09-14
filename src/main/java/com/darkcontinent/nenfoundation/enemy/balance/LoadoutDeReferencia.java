package com.darkcontinent.nenfoundation.enemy.balance;

/**
 * O jogador PADRAO contra o qual todo inimigo e medido.
 *
 * <p><b>Sem um loadout fixo, "esse mob e forte" nao quer dizer nada.</b> Forte
 * contra quem? Um ferro cheio e uma mao vazia dao respostas opostas, e sem
 * escolher uma delas cada pessoa balanceia contra um jogador imaginario
 * diferente -- e os numeros do repositorio param de se comparar entre si.</p>
 *
 * <p>Os tres loadouts abaixo NAO sao dificuldade: sao os tres momentos em que o
 * jogador encontra conteudo. Um mob so precisa ser justo contra o momento em que
 * ele aparece.</p>
 *
 * @param danoPorGolpe dano bruto da arma
 * @param golpesPorSegundo cadencia efetiva, ja contando o cooldown do vanilla
 * @param vida pontos de vida do jogador
 * @param armadura pontos de armadura
 * @param tenacidade armor toughness
 */
public record LoadoutDeReferencia(double danoPorGolpe, double golpesPorSegundo,
        double vida, double armadura, double tenacidade) {

    public LoadoutDeReferencia {
        if (!finito(danoPorGolpe) || danoPorGolpe <= 0
                || !finito(golpesPorSegundo) || golpesPorSegundo <= 0
                || !finito(vida) || vida <= 0
                || !finito(armadura) || armadura < 0 || armadura > 20
                || !finito(tenacidade) || tenacidade < 0) {
            throw new IllegalArgumentException("loadout de referencia invalido");
        }
    }

    private static boolean finito(double valor) { return Double.isFinite(valor); }

    /** Recem-chegado: espada de pedra, sem armadura. E com quem o exame comeca. */
    public static LoadoutDeReferencia inicial() {
        return new LoadoutDeReferencia(5.0D, 1.6D, 20.0D, 0.0D, 0.0D);
    }

    /** Preparado: ferro completo e espada de ferro. O meio do jogo. */
    public static LoadoutDeReferencia preparado() {
        return new LoadoutDeReferencia(7.0D, 1.6D, 20.0D, 15.0D, 0.0D);
    }

    /** Veterano: diamante completo e espada de diamante. Chimera e officers. */
    public static LoadoutDeReferencia veterano() {
        return new LoadoutDeReferencia(8.0D, 1.6D, 20.0D, 20.0D, 8.0D);
    }

    public double danoPorSegundo() { return danoPorGolpe * golpesPorSegundo; }

    /**
     * Dano que ESTE jogador recebe, depois da armadura.
     *
     * <p>A formula e a do vanilla, e esta escrita aqui de proposito: a conta de
     * balanceamento tem de usar EXATAMENTE a que o jogo usa. Uma aproximacao
     * ("armadura corta 4%% por ponto") erra por dezenas de por cento com
     * tenacidade, e o erro aparece como um mob que mede bem na planilha e mata
     * rapido demais em jogo.</p>
     */
    public double danoRecebido(double danoBruto) {
        if (!finito(danoBruto) || danoBruto < 0) throw new IllegalArgumentException("dano invalido");
        double reducao = Math.min(20.0D,
                Math.max(armadura / 5.0D, armadura - danoBruto / (2.0D + tenacidade / 4.0D)));
        return danoBruto * (1.0D - reducao / 25.0D);
    }
}
