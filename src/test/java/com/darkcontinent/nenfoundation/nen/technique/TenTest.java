package com.darkcontinent.nenfoundation.nen.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        return new Ten(() -> custoPorSegundo, () -> multiplicador, () -> 0.0D, () -> 0.0D);
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
        Ten ten = new Ten(() -> custo[0], () -> 1.0D, () -> 0.0D, () -> 0.0D);

        assertEquals(4.0D / TICKS_POR_SEGUNDO, ten.custoPorTick(), 1.0E-9D);
        custo[0] = 20.0D;
        assertEquals(20.0D / TICKS_POR_SEGUNDO, ten.custoPorTick(), 1.0E-9D,
                "Ten guardou o custo da construcao; uma recarga de config nao"
                        + " seria vista.");
    }

    @Test
    @DisplayName("Ten, Ren e Zetsu se excluem DOS DOIS LADOS")
    void exclusaoComZetsuESimetrica() {
        // ESTE TESTE ERA O OPOSTO ATE A ISSUE #88.
        //
        // Enquanto Zetsu nao existia, ele exigia que Ten NAO declarasse
        // exclusao -- porque o selamento do registro recusa apontar para
        // tecnica inexistente. Era ponto cego declarado nos javadocs de Ten e
        // de Ren, e a exclusao so podia entrar quando as duas pontas
        // existissem. Agora entrou, e inteira.
        Ten ten = ten(1.0D, 1.0D);
        Ren ren = new Ren(() -> 1.0D, () -> 1.0D, () -> 0.0D);
        Zetsu zetsu = new Zetsu(() -> 1.0D, () -> 1.0D, () -> 0.0D);

        assertTrue(ten.incompativeisCom().contains(Zetsu.ID), "Ten nao recusa Zetsu");
        assertTrue(ren.incompativeisCom().contains(Zetsu.ID), "Ren nao recusa Zetsu");
        assertTrue(zetsu.incompativeisCom().contains(Ten.ID),
                "Zetsu nao recusa Ten -- exclusao pela metade nao da erro, ela"
                        + " deixa a combinacao ilegal funcionar numa das ordens.");
        assertTrue(zetsu.incompativeisCom().contains(Ren.ID), "Zetsu nao recusa Ren");

        // E Ten e Ren CONVIVEM: no canone Ren se apoia em Ten.
        assertFalse(ten.incompativeisCom().contains(Ren.ID),
                "Ten passou a excluir Ren; eles convivem de proposito.");
    }

    @Test
    @DisplayName("Zetsu fecha o Output, e Ren o abre")
    void zetsuFechaOQueRenAbre() {
        double tetoDeZetsu = valorDeConfig("tecnica.zetsu.tetoDeOutput");
        double tetoDeRen = valorDeConfig("tecnica.ren.tetoDeOutput");
        double repouso = valorDeConfig("aura.tetoDeOutputEmRepouso");

        assertTrue(tetoDeZetsu < repouso,
                "Zetsu nao fecha nada: teto " + tetoDeZetsu + " contra repouso "
                        + repouso + ". Sem isso ele vira invisibilidade de graca.");
        assertTrue(tetoDeRen > repouso, "Ren precisa abrir o que Zetsu fecha");
    }

    @Test
    @DisplayName("Zetsu e o estado mais barato, e ainda assim custa")
    void zetsuEBaratoMasNaoGratuito() {
        double custoZetsu = valorDeConfig("tecnica.zetsu.custoPorSegundo");
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double multZetsu = valorDeConfig("tecnica.zetsu.multiplicadorDeRegeneracao");

        assertTrue(custoZetsu < custoTen,
                "Zetsu devia ser mais barato que Ten: " + custoZetsu + "/s contra "
                        + custoTen + "/s. E o estado de descanso.");
        assertTrue(custoZetsu > 0.0D,
                "Zetsu de graca contraria o item 6 do ADR-010: usar Nen gasta.");
        assertTrue(multZetsu > 1.0D,
                "Zetsu nao recupera melhor; o canone e explicito em dizer que sim.");

        // CONTRADICAO ABERTA, E DECLARADA. Esta linha exige que Zetsu tenha
        // saldo POSITIVO -- ele recupera mais do que gasta -- e o item 6 do
        // ADR-010 diz que nenhum estado sustentado se paga. As duas regras nao
        // cabem juntas, e quem escreveu as duas fui eu.
        //
        // O desenho que resolveria: Zetsu e o estado de descanso, entao ele
        // RECUPERA de proposito, e o preco dele nao e aura -- e ficar sem
        // defesa de Nen. Essa defesa nao existe (#127), e por isso hoje Zetsu e
        // um buff permanente sem desvantagem nenhuma.
        //
        // Qual regra cede e decisao conjunta, e esta na issue de balanceamento.
        // Ate la esta assercao fica, dizendo o que o projeto faz HOJE em vez de
        // fingir coerencia que ele nao tem.
        double saldo = regenBase * multZetsu - custoZetsu;
        assertTrue(saldo > 0.0D,
                "Zetsu drena mais do que recupera, e ai ninguem descansa nele.");
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

    // ------------------- item 6 do ADR-010: quem LIBERA aura e que paga

    /**
     * A ESCADA DE RECUPERACAO, e ela vale mais que qualquer sinal isolado.
     *
     * <p>ESTE TESTE SUBSTITUIU DOIS QUE DIZIAM O CONTRARIO. Eles exigiam que
     * Ten tivesse saldo NEGATIVO, pela leitura de que "nenhum estado sustentado
     * se paga" valia para todos. O responsavel decidiu em 2026-09-12 que a
     * regra vale para os estados que <b>liberam</b> aura -- e o canone e claro:
     *
     * <ul>
     *   <li>Zetsu nao gasta nada e recupera muito; o preco e ficar sem defesa;
     *   <li>Ten retem e recupera pouco; o preco e nao liberar acima do repouso;
     *   <li>Ren libera, e e esse que paga em aura.
     * </ul>
     *
     * <p>Guardar a ORDEM, e nao cada sinal separado, e o que impede a proxima
     * sessao de balanceamento de achatar os tres em coisas parecidas: girar um
     * botao sem olhar os outros quebra a escada, e a escada e o que da
     * significado a escolha entre os estados.
     */
    @Test
    @DisplayName("Zetsu recupera mais que parado, que recupera mais que Ten, que recupera mais que Ren")
    void aEscadaDeRecuperacao() {
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double teto = valorDeConfig("aura.multiplicadorMaximoDeRegeneracao");

        double parado = regenBase;
        double comTen = saldo(regenBase, teto, "ten");
        double comZetsu = saldo(regenBase, teto, "zetsu");
        double comRen = regenBase - valorDeConfig("tecnica.ren.custoPorSegundo");

        assertTrue(comZetsu > parado,
                "Zetsu (" + comZetsu + "/s) nao recupera mais que ficar parado ("
                        + parado + "/s). Ele e O estado de descanso do canone; se"
                        + " parado for melhor, ninguem usa Zetsu para nada.");
        assertTrue(parado > comTen,
                "Ten (" + comTen + "/s) recupera tanto quanto ou mais que ficar"
                        + " parado (" + parado + "/s). Ten protege, e a protecao"
                        + " tem de custar alguma coisa -- se ele so melhora tudo,"
                        + " vira o estado sempre-ligado sem escolha nenhuma.");
        assertTrue(comTen > 0.0D,
                "Ten (" + comTen + "/s) drena a reserva. A decisao de 2026-09-12"
                        + " e que ele RECUPERA pouco mantendo a protecao.");
        assertTrue(comRen < 0.0D,
                "Ren (" + comRen + "/s) nao gasta aura. Ele e o unico dos tres que"
                        + " LIBERA, e por isso e o unico a quem o item 6 do"
                        + " ADR-010 se aplica.");
    }

    /**
     * A protecao de Ten custa, e da para dizer quanto.
     *
     * <p>Se um dia Ten recuperar igual a ficar parado, a escolha some sem
     * quebrar nada: o jogador liga Ten e nunca mais desliga, porque nao ha
     * motivo. O numero exato e de balanceamento; existir uma diferenca, nao.
     */
    @Test
    @DisplayName("a protecao de Ten tem um preco visivel na recuperacao")
    void aProtecaoDeTenCusta() {
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double teto = valorDeConfig("aura.multiplicadorMaximoDeRegeneracao");

        double perdido = regenBase - saldo(regenBase, teto, "ten");
        assertTrue(perdido > 0.0D,
                "Ten nao abre mao de nada na recuperacao; a protecao sai de graca.");
    }

    @Test
    @DisplayName("o multiplicador de Ten existe: sem ele a manutencao nao retem nada")
    void tenRetemAlgumaCoisa() {
        double multTen = valorDeConfig("tecnica.ten.multiplicadorDeRegeneracao");
        assertTrue(multTen > 1.0D,
                "Ten nao melhora a regeneracao em nada. A retencao e o efeito"
                        + " dele; sem ela, Ten cobra aura e nao faz nada -- que"
                        + " era justamente o desenho recusado.");

        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");
        assertTrue(custoTen > 0.0D,
                "Ten ficou de graca. Manter aura em volta do corpo custa, mesmo"
                        + " que menos do que a retencao devolve.");
        assertTrue(custoTen < regenBase * multTen,
                "o custo de Ten (" + custoTen + "/s) alcancou a regeneracao que"
                        + " ele proporciona (" + (regenBase * multTen) + "/s), e"
                        + " ai ele drena em vez de recuperar.");
    }

    /** O saldo por segundo de um estado, com o teto do multiplicador aplicado. */
    private static double saldo(double regenBase, double teto, String tecnica) {
        double mult = Math.min(valorDeConfig(
                "tecnica." + tecnica + ".multiplicadorDeRegeneracao"), teto);
        return regenBase * mult - valorDeConfig("tecnica." + tecnica + ".custoPorSegundo");
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

    // ------------------------------------------------------------ Ren (#87)

    @Test
    @DisplayName("Ren levanta o teto de Output acima do repouso")
    void renLevantaOTeto() {
        double repouso = valorDeConfig("aura.tetoDeOutputEmRepouso");
        double tetoDeRen = valorDeConfig("tecnica.ren.tetoDeOutput");

        assertTrue(tetoDeRen > repouso,
                "Ren nao levanta nada: teto de repouso " + repouso + " contra teto"
                        + " de Ren " + tetoDeRen + ". Com os dois iguais, Ren deixa"
                        + " de ter efeito observavel e vira so um dreno de aura.");
    }

    @Test
    @DisplayName("em repouso o jogador NAO alcanca o Output maximo")
    void repousoNaoDeixaAlcancarOTopo() {
        // E isto que da a Ren o que levantar. Com o repouso em 1.0, o teto nunca
        // morde -- que era exatamente o ponto cego declarado no PR #80.
        double repouso = valorDeConfig("aura.tetoDeOutputEmRepouso");
        assertTrue(repouso < 1.0D,
                "O teto de repouso e " + repouso + ": o jogador ja alcanca tudo sem"
                        + " tecnica nenhuma, e Ren nao tem o que levantar.");
        assertTrue(repouso > 0.0D,
                "O teto de repouso e zero: sem tecnica o jogador nao libera nada.");
    }

    @Test
    @DisplayName("Ren drena MUITO mais que Ten, e drena de fato")
    void renDrenaMaisQueTen() {
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double custoRen = valorDeConfig("tecnica.ren.custoPorSegundo");
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");

        assertTrue(custoRen - regenBase > 0.0D,
                "Ren nao drena: custo " + custoRen + "/s contra regeneracao "
                        + regenBase + "/s.");
        assertTrue(custoRen > custoTen * 2.0D,
                "Ren custa " + custoRen + "/s contra " + custoTen + "/s de Ten. A"
                        + " PROPORCAO entre os dois e o que faz Ren ser estado de"
                        + " combate e Ten estado de repouso -- nao o javadoc.");
    }

    @Test
    @DisplayName("Ten e Ren juntos custam os dois")
    void tenERenSomamCusto() {
        // Eles convivem de proposito: no canone Ren se apoia em Ten. Quem liga
        // os dois paga os dois, e e assim que "o limite simultaneo e a Aura"
        // aparece em numero.
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");
        double custoRen = valorDeConfig("tecnica.ren.custoPorSegundo");
        double regenBase = valorDeConfig("aura.regeneracaoPorSegundo");
        double multTen = valorDeConfig("tecnica.ten.multiplicadorDeRegeneracao");

        double drenoJuntos = (custoTen + custoRen) - regenBase * multTen;
        double drenoSoRen = custoRen - regenBase;

        assertTrue(drenoJuntos > drenoSoRen,
                "Ligar Ten junto de Ren nao custou mais caro: " + drenoJuntos
                        + "/s contra " + drenoSoRen + "/s so com Ren.");
    }
}
