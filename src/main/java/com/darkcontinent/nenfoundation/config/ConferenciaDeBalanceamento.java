package com.darkcontinent.nenfoundation.config;

import com.darkcontinent.nenfoundation.nen.aura.SaldoSustentado;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;

/**
 * Confere os numeros que o jogo REALMENTE carregou.
 *
 * <p>POR QUE ISTO EXISTE, e e o motivo inteiro. O portao que garante o item 6
 * do ADR-010 -- nenhum estado sustentado se paga -- le o <b>padrao escrito no
 * codigo-fonte</b>, por regex sobre {@code NenConfig.java}. Ele nunca viu o
 * arquivo que o servidor abriu.
 *
 * <p>E os dois divergem com facilidade: o NeoForge <b>nao sobrescreve</b> valor
 * que ja existe no toml quando o padrao do codigo muda. Um mundo criado com um
 * custo antigo mantem aquele custo para sempre, em silencio.
 *
 * <p>Aconteceu de verdade: a instancia de teste rodava com
 * {@code tecnica.ten.custoPorSegundo = 1.5} enquanto o codigo ja pedia
 * {@code 3.0}. Com regeneracao 1.0 e multiplicador 2.0, o saldo era
 * <b>+0.5 por segundo</b> -- Ten passou a se pagar, virando buff permanente de
 * graca. Os 321 testes continuaram verdes, porque nenhum deles le o toml.
 *
 * <p>ELE AVISA, E NAO RECUSA. Numero de balanceamento errado nao e motivo para
 * um servidor nao subir: quem esta girando botao precisa poder testar valores
 * ruins de proposito. Mas o aviso vai para o log em ERROR, com o numero, a
 * conta e o que fazer -- e nao um "config invalida" que ninguem sabe consertar.
 */
public final class ConferenciaDeBalanceamento {

    private static final Logger LOG = LogUtils.getLogger();

    private ConferenciaDeBalanceamento() {
    }

    /** Um estado sustentado, com os numeros que o jogo carregou. */
    public record Estado(String nome, double multiplicadorDeRegeneracao, double custoPorSegundo) {
    }

    /**
     * Os problemas encontrados, em texto pronto para o log.
     *
     * <p>DEVOLVE A LISTA em vez de logar direto para poder ser testada sem
     * capturar log -- e para o chamador decidir a severidade.
     */
    public static List<String> problemas(double regeneracaoBase, List<Estado> estados) {
        List<String> achados = new ArrayList<>();
        for (Estado estado : estados) {
            if (!SaldoSustentado.sePaga(regeneracaoBase, estado.multiplicadorDeRegeneracao(),
                    estado.custoPorSegundo())) {
                continue;
            }
            double saldo = SaldoSustentado.porSegundo(regeneracaoBase,
                    estado.multiplicadorDeRegeneracao(), estado.custoPorSegundo());
            double minimo = SaldoSustentado.custoMinimo(regeneracaoBase,
                    estado.multiplicadorDeRegeneracao());
            achados.add(String.format(Locale.ROOT,
                    "%s SE PAGA: saldo de %+.2f de aura por segundo com ela ligada"
                            + " (regeneracao %.2f x multiplicador %.2f - custo %.2f)."
                            + " O item 6 do ADR-010 exige saldo NEGATIVO: nenhum estado"
                            + " sustentado se paga, senao vira buff permanente de graca."
                            + " Suba tecnica.%s.custoPorSegundo acima de %.2f.",
                    estado.nome(), saldo, regeneracaoBase,
                    estado.multiplicadorDeRegeneracao(), estado.custoPorSegundo(),
                    estado.nome().toLowerCase(Locale.ROOT), minimo));
        }
        return achados;
    }

    /** Le a config carregada e escreve no log o que estiver errado. */
    public static void conferirConfigCarregada() {
        // ZETSU FICA DE FORA, e isto NAO e um esquecimento -- e uma
        // contradicao aberta do projeto, escrita aqui para nao sumir.
        //
        // Com os numeros de hoje Zetsu tem saldo de +1.8 por segundo: ele SE
        // PAGA, e pelo item 6 do ADR-010 isso seria proibido. So que Zetsu e o
        // estado de DESCANSO -- no canone e assim que se recupera -- e o preco
        // dele nao e aura: e ficar sem defesa de Nen.
        //
        // Essa defesa NAO EXISTE ainda (#127), entao hoje Zetsu e de fato um
        // buff permanente sem desvantagem. Incluir Zetsu nesta conferencia
        // encheria o log de um aviso que ninguem pode resolver, e aviso que
        // aparece sempre deixa de ser lido -- levando junto o proximo defeito
        // de verdade.
        //
        // Qual das duas regras cede e decisao conjunta (ADR-010 tem tabela de
        // governanca). Esta na issue de balanceamento; nao invente aqui.
        List<String> achados = problemas(NenConfig.auraRegeneracaoPorSegundo(), List.of(
                new Estado("Ten", NenConfig.tenMultiplicadorDeRegeneracao(),
                        NenConfig.tenCustoPorSegundo())));

        for (String achado : achados) {
            LOG.error("[balanceamento] {}", achado);
        }
    }
}
