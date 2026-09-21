package com.darkcontinent.nenfoundation.nen.technique;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.darkcontinent.nenfoundation.Repo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le os numeros de balanceamento do FONTE do {@code NenConfig}.
 *
 * <p><b>POR QUE DO FONTE, e nao de uma constante aqui.</b> Repetir o valor no
 * teste criaria a segunda fonte da mesma verdade: alguem giraria o botao na
 * config, o teste continuaria medindo o numero antigo, e aprovaria um estado que
 * se paga. Ler o fonte e o que faz o teste falar do projeto de HOJE.
 *
 * <p><b>ELE ESTAVA DUPLICADO EM DOIS ARQUIVOS.</b> {@code TenTest} e
 * {@code KenTest} carregavam copias do mesmo regex. Duas copias de um leitor
 * fragil divergem na primeira vez que alguem conserta so uma -- e o sintoma
 * seria um teste medindo o numero certo e o outro medindo o antigo, os dois
 * verdes.
 *
 * <p><b>O QUE ESTE LEITOR NAO LE, e isso e limite declarado.</b> O padrao casa
 * {@code [0-9.]+} seguido de {@code D}. Ficam de fora:
 *
 * <ul>
 *   <li>separador de milhar -- {@code 1_000.0D};
 *   <li>notacao cientifica -- {@code 1.0e-1D};
 *   <li>inteiro sem sufixo -- {@code 20} (e o caso de
 *       {@code tecnica.ko.duracaoEmTicks}, que por isso nao passa por aqui);
 *   <li>negativo -- o sinal fica fora do grupo.
 * </ul>
 *
 * <p>Quem escrever um default nessas formas recebe "chave nao encontrada", e nao
 * um numero errado em silencio. A recusa e barulhenta de proposito.
 */
final class ValoresDeBalanceamento {

    private static final String CONFIG =
            "src/main/java/com/darkcontinent/nenfoundation/config/NenConfig.java";

    private ValoresDeBalanceamento() { }

    /** O default de uma chave {@code double} declarada com {@code defineInRange}. */
    static double de(String chave) {
        Matcher m = Pattern.compile(
                        "defineInRange\\(\"" + Pattern.quote(chave) + "\",\\s*([0-9.]+)D")
                .matcher(Repo.texto(CONFIG));
        assertTrue(m.find(), "chave nao encontrada no NenConfig: " + chave
                + ". Ou ela sumiu, ou o default foi escrito numa forma que este leitor"
                + " nao casa -- separador de milhar, notacao cientifica ou inteiro sem D."
                + " Ver o javadoc de ValoresDeBalanceamento.");
        return Double.parseDouble(m.group(1));
    }

    /**
     * Quantos segundos um estado sustentado dura, partindo da reserva cheia.
     *
     * <p>E a conta que o ADR-018 usa para desenhar a escada: a duracao e
     * escolhida, e o custo e consequencia. Um estado que se paga devolve
     * infinito -- e isso nao e erro, e o desenho de Ten e Zetsu.
     */
    static double duracaoEmSegundos(String tecnica, double multiplicadorDeRegeneracao) {
        double reserva = de("aura.maximaBase");
        double regen = de("aura.regeneracaoPorSegundo")
                * Math.min(multiplicadorDeRegeneracao, de("aura.multiplicadorMaximoDeRegeneracao"));
        double dreno = de("tecnica." + tecnica + ".custoPorSegundo") - regen;
        if (dreno <= 0.0D) {
            return Double.POSITIVE_INFINITY;
        }
        if (reserva <= 0.0D) {
            fail("aura.maximaBase e zero: nenhum estado dura nada, e a escada inteira"
                    + " deixa de significar o que o ADR-018 diz que ela significa.");
        }
        return reserva / dreno;
    }

    /** Duracao de um estado que nao mexe na regeneracao, que e o caso de todos os que liberam. */
    static double duracaoEmSegundos(String tecnica) {
        return duracaoEmSegundos(tecnica, 1.0D);
    }
}
