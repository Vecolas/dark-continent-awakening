package com.darkcontinent.nenfoundation.enemy.telemetry;

import java.util.Objects;

/**
 * UM combate que aconteceu -- o que foi, e nunca quem estava jogando.
 *
 * <p><b>Nome, uuid e posicao ficam de fora de proposito.</b> Nenhuma pergunta de
 * balanceamento precisa deles: "quanto tempo o Cyclops dura" e "quanto tempo o
 * Cyclops dura para o Fulano" respondem coisas diferentes, e so a primeira e
 * trabalho de balanceamento. Guardar o identificador "porque pode ser util
 * depois" e como um arquivo local vira um arquivo que alguem manda por engano.</p>
 *
 * <p><b>{@code jogadores} entra, e e o unico dado de pessoa.</b> Sem ele nao da
 * para separar solo de grupo, e a media junta os dois -- o que faz todo mob
 * parecer facil demais em solo e dificil demais em grupo, ao mesmo tempo.</p>
 *
 * @param mobId qual criatura
 * @param jogadores quantos participaram; 1 e solo
 * @param ticks quanto durou
 * @param danoCausado dano total que os jogadores aplicaram
 * @param danoRecebido dano total que os jogadores levaram
 * @param mobMorreu se o combate terminou com a criatura morta
 */
public record AmostraDeCombate(String mobId, int jogadores, int ticks,
        float danoCausado, float danoRecebido, boolean mobMorreu) {

    public AmostraDeCombate {
        if (mobId == null || mobId.isBlank()) {
            throw new IllegalArgumentException("amostra sem criatura: ela entraria no relatorio"
                    + " como uma linha anonima, e uma media de 'algo' nao balanceia nada");
        }
        if (jogadores < 1) {
            throw new IllegalArgumentException("combate com " + jogadores + " jogadores nao"
                    + " aconteceu");
        }
        if (ticks < 1) throw new IllegalArgumentException("combate de duracao nao positiva");
        if (!Float.isFinite(danoCausado) || danoCausado < 0.0F
                || !Float.isFinite(danoRecebido) || danoRecebido < 0.0F) {
            throw new IllegalArgumentException("dano invalido na amostra");
        }
    }

    public double segundos() { return ticks / 20.0D; }

    /** Linha CSV, com ponto decimal independente do locale do servidor. */
    public String linha() {
        return String.join(";",
                mobId,
                Integer.toString(jogadores),
                Integer.toString(ticks),
                String.format(java.util.Locale.ROOT, "%.2f", danoCausado),
                String.format(java.util.Locale.ROOT, "%.2f", danoRecebido),
                Boolean.toString(mobMorreu));
    }

    /** O cabecalho do CSV; escrito uma vez, no topo do arquivo. */
    public static String cabecalho() {
        return "mob;jogadores;ticks;dano_causado;dano_recebido;mob_morreu";
    }

    /**
     * Le uma linha de volta.
     *
     * <p>Existe para o relatorio poder somar sessoes anteriores. Linha torta
     * REPROVA em vez de virar uma amostra com zeros: uma amostra silenciosamente
     * zerada puxa a media para baixo e ninguem descobre por que o balanceamento
     * "mudou sozinho".</p>
     */
    public static AmostraDeCombate de(String linha) {
        Objects.requireNonNull(linha, "linha ausente");
        String[] campos = linha.split(";", -1);
        if (campos.length != 6) {
            throw new IllegalArgumentException("linha de telemetria com " + campos.length
                    + " campos (esperados 6): '" + linha + "'");
        }
        return new AmostraDeCombate(campos[0], Integer.parseInt(campos[1]),
                Integer.parseInt(campos[2]), Float.parseFloat(campos[3]),
                Float.parseFloat(campos[4]), Boolean.parseBoolean(campos[5]));
    }
}
