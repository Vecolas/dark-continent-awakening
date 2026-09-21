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
        // SO OS ESTADOS QUE LIBERAM AURA, e isso e a regra e nao uma excecao.
        //
        // O item 6 do ADR-010 dizia "nenhum estado sustentado se paga". A
        // decisao do responsavel em 2026-09-12 estreitou isso para os estados
        // que LIBERAM aura, e o motivo e o canone:
        //
        //   Zetsu nao gasta nada e recupera -- o preco dele e ficar sem defesa;
        //   Ten retem e recupera devagar -- o preco e nao liberar acima do teto;
        //   Ren libera -- e esse paga em aura.
        //
        // Um estado que nao libera nada nao tem como "se pagar em aura": ele
        // nao gasta aura para existir. Cobrar dele o saldo negativo era exigir
        // que o descanso cansasse.
        //
        // O PONTO CEGO DO #127 FECHOU PELA METADE, e a metade que sobrou tem
        // nome. Este comentario dizia "enquanto #127 nao existir (...) os dois
        // sao mais baratos do que deveriam ser". O #127 fechou: a camada de
        // dano existe em nen/combat/, e com Zetsu o golpe doi o mesmo que sem
        // aura nenhuma.
        //
        // Entao o PRECO existe. O que ninguem verificou e se ele MORDE: nenhuma
        // sessao de jogo exercitou a vulnerabilidade, e "existe em codigo" nao
        // e "custa alguma coisa para quem joga". Quem responde isso e o gate do
        // M4 (#91), com dois clientes -- e ele nunca rodou.
        //
        // Ate o #91, o preco de Zetsu e regra provada e comportamento suposto.
        List<String> achados = problemas(NenConfig.auraRegeneracaoPorSegundo(), List.of(
                new Estado("Ren", 1.0D, NenConfig.renCustoPorSegundo())));

        for (String achado : achados) {
            LOG.error("[balanceamento] {}", achado);
        }
    }
}
