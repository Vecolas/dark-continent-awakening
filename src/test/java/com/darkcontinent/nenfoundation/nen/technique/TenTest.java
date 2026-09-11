package com.darkcontinent.nenfoundation.nen.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.nen.aura.AuraFormulas;
import com.darkcontinent.nenfoundation.nen.aura.ParametrosDeAura;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao de Ten, e das duas condicoes que o ADR-010 impos.
 *
 * <p>As duas nao sao observacoes de balanceamento: elas sao itens 5 e 6 da
 * decisao aprovada, e por isso viram teste em vez de comentario.
 */
class TenTest {

    private static final String CONFIG =
            "src/main/java/com/darkcontinent/nenfoundation/config/NenConfig.java";

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    /** Parametros de teste, com valores distintos para um nao passar pelo outro. */
    private static ParametrosDeAura parametros(double regenPorSegundo, double teto) {
        return new ParametrosDeAura() {
            @Override public double maximaBase() { return 100.0D; }
            @Override public double regeneracaoPorSegundo() { return regenPorSegundo; }
            @Override public double outputBase() { return 10.0D; }
            @Override public double multiplicadorMaximoDeRegeneracao() { return teto; }
        };
    }

    private static Ten ten(double custoPorSegundo, double multiplicador) {
        return new Ten(() -> custoPorSegundo, () -> multiplicador);
    }

    // --------------------------------------------------- o que Ten declara

    @Test
    @DisplayName("Ten converte o custo por segundo em custo por tick")
    void custoPorTick() {
        assertEquals(3.0D / TICKS_POR_SEGUNDO, ten(3.0D, 1.0D).custoPorTick(), 1.0E-9D);
    }

    @Test
    @DisplayName("Ten le os numeros NA HORA, e nao os congela na construcao")
    void numerosSaoLidosNaHora() {
        // Congelar o custo faria Ten ignorar toda recarga de config posterior,
        // em silencio. E o erro numero 1 da lista do CLAUDE.md.
        double[] custo = {4.0D};
        Ten ten = new Ten(() -> custo[0], () -> 1.0D);

        assertEquals(4.0D / TICKS_POR_SEGUNDO, ten.custoPorTick(), 1.0E-9D);
        custo[0] = 20.0D;
        assertEquals(20.0D / TICKS_POR_SEGUNDO, ten.custoPorTick(), 1.0E-9D,
                "Ten guardou o custo da construcao; uma recarga de config nao"
                        + " seria vista.");
    }

    @Test
    @DisplayName("Ten nao declara incompatibilidade com tecnica que nao existe")
    void semExclusaoOrfa() {
        // Ten DEVERIA excluir Zetsu, e vai excluir. Mas declarar isso antes de
        // Zetsu existir reprova o selamento do registro -- de proposito: a
        // exclusao entra dos dois lados de uma vez, ou nao entra.
        assertTrue(ten(1.0D, 1.0D).incompativeisCom().isEmpty(),
                "Ten declarou exclusao com uma tecnica que ainda nao existe; o"
                        + " registro nao vai selar.");
    }

    // ------------------------------------- item 5 do ADR-010: o teto morde

    @Test
    @DisplayName("o teto limita o multiplicador, e limita ANTES de multiplicar")
    void tetoLimita() {
        ParametrosDeAura p = parametros(2.0D, 3.0D);

        assertEquals(3.0D, AuraFormulas.limitarMultiplicador(p, 10.0D),
                "um multiplicador acima do teto tem de ser cortado nele");
        assertEquals(2.0D, AuraFormulas.limitarMultiplicador(p, 2.0D),
                "abaixo do teto nada muda");

        // 2.0/s a 20 ticks = 0.1 por tick. Com teto 3.0, o maximo e 0.3.
        assertEquals(0.3D, AuraFormulas.regeneracaoPorTick(p, 99.0D), 1.0E-9D,
                "Sem o teto, um produto absurdo de multiplicadores viraria"
                        + " regeneracao absurda -- e o que impediria isso seria a"
                        + " exclusao entre tecnicas, que e regra de OUTRO lugar.");
    }

    @Test
    @DisplayName("multiplicador negativo vira zero, e nao drena aura")
    void negativoViraZero() {
        ParametrosDeAura p = parametros(2.0D, 3.0D);

        assertEquals(0.0D, AuraFormulas.limitarMultiplicador(p, -5.0D),
                "Multiplicador negativo faria a 'regeneracao' DRENAR aura por um"
                        + " caminho que ninguem chamaria de dreno, e o sintoma"
                        + " seria aura sumindo sem gasto registrado.");
        assertEquals(0.0D, AuraFormulas.regeneracaoPorTick(p, -5.0D));
    }

    @Test
    @DisplayName("multiplicador nao-finito e recusado")
    void naoFinitoERecusado() {
        ParametrosDeAura p = parametros(2.0D, 3.0D);

        for (double invalido : List.of(Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY)) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> AuraFormulas.limitarMultiplicador(p, invalido),
                    "aceitou " + invalido);
        }
    }

    @Test
    @DisplayName("sem tecnica ativa, a regeneracao e exatamente a de antes")
    void neutroNaoMuda() {
        ParametrosDeAura p = parametros(2.0D, 3.0D);

        assertEquals(AuraFormulas.regeneracaoPorTick(p),
                AuraFormulas.regeneracaoPorTick(p, 1.0D), 1.0E-12D,
                "O ADR-010 diz que sem tecnica ativa nada muda. Se este teste"
                        + " falhar, a decisao foi implementada mais larga do que"
                        + " foi aprovada.");
    }

    // --------------------------- item 6 do ADR-010: o saldo e NEGATIVO

    @Test
    @DisplayName("com os numeros DISTRIBUIDOS, Ten drena: ele nao vira estado permanente")
    void tenTemSaldoNegativo() {
        // ESTE E O PORTAO DA CONDICAO QUE O RESPONSAVEL IMPOS.
        //
        // Se a regeneracao ganha superar o custo de manutencao, Ten passa a se
        // pagar e vira o estado obviamente sempre-ligado -- e o jogo perde a
        // escolha. Isso NAO da erro: da um jogo pior, devagar.
        //
        // Os numeros saem da config DISTRIBUIDA, lidos do fonte, e nao de
        // constantes repetidas aqui: constante repetida no teste vira a segunda
        // fonte da mesma verdade, e o teste passaria a aprovar a si mesmo.
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");
        double multTen = valorDeConfig("tecnica.ten.multiplicadorDeRegeneracao");
        double teto = valorDeConfig("aura.multiplicadorMaximoDeRegeneracao");

        double multiplicadorEfetivo = Math.min(multTen, teto);
        double ganhoPorSegundo = regenBase * multiplicadorEfetivo - regenBase;
        double saldoPorSegundo = ganhoPorSegundo - custoTen;

        assertTrue(saldoPorSegundo < 0.0D,
                "Com os numeros distribuidos, Ten se paga: ganho de "
                        + ganhoPorSegundo + "/s contra custo de " + custoTen
                        + "/s, saldo " + saldoPorSegundo + "/s. Pelo item 6 do"
                        + " ADR-010 o saldo de um estado sustentado precisa ser"
                        + " NEGATIVO -- senao Ten vira o estado permanente obvio.");
    }

    @Test
    @DisplayName("a regeneracao de Ten reduz o custo, mas nao chega a paga-lo")
    void tenReduzMasNaoPaga() {
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");
        double multTen = valorDeConfig("tecnica.ten.multiplicadorDeRegeneracao");

        double ganho = regenBase * multTen - regenBase;

        assertTrue(ganho > 0.0D,
                "Ten nao melhora a regeneracao em nada. A retencao e o unico"
                        + " efeito que ele tem hoje; sem ela, Ten cobra aura e"
                        + " nao faz nada -- que era justamente o desenho recusado.");
        assertTrue(ganho < custoTen,
                "O ganho (" + ganho + "/s) chegou ao custo (" + custoTen + "/s).");
    }

    /**
     * Le um numero da config DISTRIBUIDA, do fonte.
     *
     * <p>POR QUE DO FONTE: repetir o valor aqui criaria a segunda fonte da
     * mesma verdade. Alguem giraria o botao na config, o teste continuaria
     * medindo o numero antigo, e aprovaria um Ten que se paga.
     */
    private static double valorDeConfig(String chave) {
        String fonte = Repo.texto(CONFIG);
        Matcher m = Pattern.compile(
                        "defineInRange\\(\"" + Pattern.quote(chave) + "\",\\s*([0-9.]+)D")
                .matcher(fonte);
        assertTrue(m.find(), "chave nao encontrada no NenConfig: " + chave);
        return Double.parseDouble(m.group(1));
    }
}
