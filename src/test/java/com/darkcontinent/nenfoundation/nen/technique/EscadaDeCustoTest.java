package com.darkcontinent.nenfoundation.nen.technique;

import static com.darkcontinent.nenfoundation.nen.technique.ValoresDeBalanceamento.de;
import static com.darkcontinent.nenfoundation.nen.technique.ValoresDeBalanceamento.duracaoEmSegundos;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A escada inteira de custo, e nao so os tres degraus que tinham portao.
 *
 * <p><b>ESTE ARQUIVO EXISTE PORQUE UM PORTAO REPROVOU UMA DECISAO.</b> Em
 * 2026-09-21, baixar so o custo de Ren para 4.0 fez {@code KenTest} reprovar:
 * Ken custava 6.0, e a tecnica que se treina para DURAR teria ficado mais cara
 * que o pico. A escada e relativa a Ren, e mexer numa ponta sem olhar as outras
 * quebra o desenho.
 *
 * <p>A investigacao mostrou que a escada tinha rede so no meio. {@code ten <
 * ken < ren} e {@code ren > 2*ten} estavam gateados; <b>{@code shu < gyo},
 * {@code ko} maior de todas, as fracoes de concentracao, as protecoes e os
 * reforcos existiam so como prosa nos {@code .comment(...)}</b> -- e valiam por
 * coincidencia. Este arquivo transforma a prosa em regua.
 *
 * <p><b>A ESCADA E DESENHADA EM SEGUNDOS</b> (ADR-018). Ninguem tem intuicao
 * sobre "4,4 de aura por segundo"; o relato que abriu o #160 fala em segundos,
 * e os gates de captura tambem. O custo e consequencia de
 * {@code custo = reserva/duracao + regeneracao}, e e por isso que
 * {@link #aEscadaCabeNaReserva} mede SEGUNDOS: e a unica assercao daqui que
 * enxerga o acoplamento com a reserva e a regeneracao. As outras medem ordem
 * entre custos, e ordem de custo nao ve reserva nenhuma.
 *
 * <p><b>O QUE ESTE ARQUIVO NAO PROVA.</b> Ele prova a ORDEM e as faixas -- que
 * a escada e coerente consigo mesma. Ele nao diz se Ren dura tempo BOM: isso e
 * cronometro na mao numa sessao de jogo, e e a unica pergunta que o #160
 * realmente fez. {@code m4-tecnicas.md} ja dizia isso: "este gate prova
 * comportamento, nao se Ren custa caro demais".
 */
class EscadaDeCustoTest {

    private static final String[] QUEM_LIBERA = {"ren", "ken", "gyo", "shu", "ko"};

    private static double custo(String tecnica) {
        return de("tecnica." + tecnica + ".custoPorSegundo");
    }

    // ------------------------------------------------- a escada de custo

    @Test
    @DisplayName("concentrar custa menos que abrir a torneira: shu < gyo < ren")
    void concentrarCustaMenosQueLiberar() {
        double shu = custo("shu");
        double gyo = custo("gyo");
        double ren = custo("ren");

        assertTrue(shu < gyo,
                "Shu (" + shu + "/s) ficou mais cara que Gyo (" + gyo + "/s). Shu ESTENDE a"
                        + " camada ate o objeto na mao; Gyo concentra quase metade da aura numa"
                        + " regiao do corpo. Cobrir o que esta na mao custa menos que"
                        + " redistribuir o corpo inteiro -- e se Shu custar mais, ninguem a usa.");
        assertTrue(gyo < ren,
                "Gyo (" + gyo + "/s) alcancou Ren (" + ren + "/s). Gyo e aplicacao de Ren:"
                        + " concentrar o que ja esta liberado. Custar o mesmo que abrir a"
                        + " torneira toda tira a razao de existir dela.");
    }

    @Test
    @DisplayName("Ko e o mais caro de todos, sem empate")
    void koEOMaisCaro() {
        double ko = custo("ko");
        for (String outra : new String[] {"zetsu", "ten", "shu", "gyo", "ken", "ren"}) {
            assertTrue(ko > custo(outra),
                    "Ko (" + ko + "/s) nao e mais caro que " + outra + " (" + custo(outra)
                            + "/s). Ko poe quase toda a aura num ponto por um segundo -- ele e"
                            + " o golpe mais caro que existe, e o que torna errar catastrofico."
                            + " Empatar com qualquer outra apaga essa leitura.");
        }
    }

    @Test
    @DisplayName("todo estado que LIBERA aura drena de verdade")
    void quemLiberaDrena() {
        double regen = de("aura.regeneracaoPorSegundo");
        for (String tecnica : QUEM_LIBERA) {
            assertTrue(custo(tecnica) > regen,
                    tecnica + " custa " + custo(tecnica) + "/s contra regeneracao " + regen
                            + "/s, entao ela se paga. Pelo ADR-013 quem LIBERA aura tem saldo"
                            + " negativo -- senao vira o estado sempre-ligado e a escolha some."
                            + " Ten e Zetsu estao fora desta lista de proposito: eles retem e"
                            + " fecham, nao liberam.");
        }
    }

    @Test
    @DisplayName("a escada cabe na reserva: Ren entre 20 e 40 segundos")
    void aEscadaCabeNaReserva() {
        // POR QUE BANDA ABSOLUTA, E NAO A ORDEM DAS DURACOES. A primeira versao
        // deste teste afirmava "Ren < Ken < Gyo < Shu" em duracao -- e ela NAO
        // PODE REPROVAR SOZINHA. Nenhum dos que liberam mexe na regeneracao,
        // entao duracao = reserva/(custo - regen) e estritamente decrescente no
        // custo: a ordem das duracoes e a ordem dos custos escrita ao contrario,
        // e concentrarCustaMenosQueLiberar ja a cobre. Era segunda fonte da
        // mesma verdade, e a sabotagem provou: mudar o custo de Ken nao fazia o
        // teste de duracao cair antes do de custo.
        //
        // O que SO a duracao ve e o acoplamento com a RESERVA e a REGENERACAO.
        // Dobrar aura.maximaBase, ou zerar a regeneracao, nao mexe em nenhuma
        // ordem de custo -- e muda a escada inteira. E essa a faixa fixada pelo
        // ADR-018, e e esta a unica assercao do arquivo que a enxerga.
        double ren = duracaoEmSegundos("ren");

        assertTrue(ren >= 20.0D && ren <= 40.0D,
                "Ren dura " + Math.round(ren) + " s, fora da faixa de 20 a 40 que o ADR-018"
                        + " fixou. A decisao do #160 foi ~30 s com reserva 100: onze segundos"
                        + " era um golpe e nao uma luta, e um minuto de graca no primeiro dia"
                        + " esvaziaria a progressao do M6. Se a reserva, a regeneracao ou o"
                        + " custo mudou de proposito, a escada inteira precisa ser refeita em"
                        + " segundos -- e este numero atualizado junto do ADR.");

        // E a que mais dura entre as que liberam nao pode virar estado
        // permanente por acidente: acima de uns tres minutos ninguem percebe
        // mais que esta gastando, e a escolha some -- que e o item 6 do ADR-010
        // aparecendo pelo outro lado.
        double shu = duracaoEmSegundos("shu");
        assertTrue(shu <= 180.0D,
                "Shu dura " + Math.round(shu) + " s. Acima de tres minutos um estado que"
                        + " LIBERA aura vira permanente na pratica, e o jogador para de"
                        + " escolher -- o mesmo defeito que o saldo negativo existe para"
                        + " impedir, chegando pela reserva em vez de pelo saldo.");
    }

    @Test
    @DisplayName("Ten e Zetsu nao duram: eles se pagam, e isso e o ADR-013")
    void tenEZetsuSePagam() {
        double ten = duracaoEmSegundos("ten", de("tecnica.ten.multiplicadorDeRegeneracao"));
        double zetsu = duracaoEmSegundos("zetsu", de("tecnica.zetsu.multiplicadorDeRegeneracao"));

        assertTrue(Double.isInfinite(ten),
                "Ten passou a ter duracao finita (" + ten + " s). Pelo ADR-013 ele RETEM em vez"
                        + " de liberar, e o preco dele e nao poder liberar acima do teto de"
                        + " repouso -- nao a reserva. Ten com saldo negativo reabre uma decisao"
                        + " que foi fechada em 2026-09-12.");
        assertTrue(Double.isInfinite(zetsu),
                "Zetsu passou a ter duracao finita (" + zetsu + " s). Ele e O estado de"
                        + " descanso: o preco dele e ficar sem defesa de Nen nenhuma. Se ele"
                        + " drenar, ninguem descansa em lugar nenhum.");
    }

    // ------------------------------------------------- concentracao

    @Test
    @DisplayName("a escada de concentracao acompanha a de custo")
    void aEscadaDeConcentracao() {
        double shu = de("tecnica.shu.fracaoConcentrada");
        double gyo = de("tecnica.gyo.fracaoConcentrada");
        double ko = de("tecnica.ko.fracaoConcentrada");

        assertTrue(shu < gyo,
                "Shu concentra " + shu + " e Gyo concentra " + gyo + ". Shu estende a camada ate"
                        + " o item; Gyo puxa o corpo inteiro para uma regiao.");
        assertTrue(gyo < ko,
                "Gyo concentra " + gyo + " e Ko concentra " + ko + ". Ko e quase TODA a aura num"
                        + " ponto -- e o resto do corpo ficando com quase nada e o que torna"
                        + " errar o golpe catastrofico.");
        assertTrue(ko < 1.0D,
                "Ko concentra " + ko + ", que nao deixa nada para as outras cinco regioes."
                        + " 'Quase toda' nao e 'toda': zero nas outras faz a defesa desaparecer"
                        + " por completo, e a conta de FaixaDoCorpo passa a dividir por nada.");
    }

    // ------------------------------------------------- reforco e protecao

    @Test
    @DisplayName("a escada de reforco: Ten quase nada, Ren a referencia")
    void aEscadaDeReforco() {
        double ten = de("tecnica.ten.reforcoBase");
        double ken = de("tecnica.ken.reforcoBase");
        double ren = de("tecnica.ren.reforcoBase");

        assertTrue(ten < ken,
                "Ten reforca " + ten + " e Ken reforca " + ken + ". Ten e o estado OCIOSO: se"
                        + " ele reforcar bem, ninguem precisa de mais nada.");
        assertTrue(ken < ren,
                "Ken reforca " + ken + " e Ren reforca " + ren + ". Quem cobre o corpo inteiro"
                        + " nao concentra em nada -- se Ken reforcar tanto quanto Ren, Ren vira"
                        + " inutil, e a troca 'pico por duracao' deixa de existir.");
    }

    @Test
    @DisplayName("Ken protege claramente mais que Ten, senao vira um Ten caro")
    void aEscadaDeProtecao() {
        double ten = de("tecnica.ten.protecaoBase");
        double ken = de("tecnica.ken.protecaoBase");
        assertTrue(ten < ken,
                "Ten protege " + ten + " e Ken protege " + ken + ". Ken e a principal defesa"
                        + " geral contra Nen; sem uma diferenca clara ele vira um Ten caro.");
    }

    @Test
    @DisplayName("o teto de combate corta a protecao de Ken, e isso precisa estar visivel")
    void aProtecaoDeKenEstaAcimaDoTeto() {
        double ken = de("tecnica.ken.protecaoBase");
        double teto = de("combate.tetoDeReducaoDeDano");

        assertTrue(ken > teto,
                "tecnica.ken.protecaoBase (" + ken + ") caiu abaixo de"
                        + " combate.tetoDeReducaoDeDano (" + teto + "). Se isso foi de proposito,"
                        + " atualize este teste; se nao, alguem baixou a defesa pesada sem"
                        + " perceber.");

        // BOTAO PARCIALMENTE MORTO, E DECLARADO. Com alocacao UNIFORME, qualquer
        // valor de Ken entre o teto e 1.0 produz exatamente a mesma reducao --
        // DefesaDeNen corta em `combate.tetoDeReducaoDeDano` antes. O numero so
        // volta a diferenciar quando a alocacao fica desigual (Ken+Gyo, Ken+Ko),
        // porque ai a media da faixa muda.
        //
        // Isto esta aqui, e nao num comentario da config, porque e o erro numero
        // 7 do CLAUDE.md noutra forma: quem girar ken.protecaoBase numa sessao de
        // balanceamento e testar parado nao vai ver NADA mudar, e vai concluir
        // que o botao esta quebrado. Quem quiser mexer de verdade na defesa
        // uniforme gira o TETO, e isso e decisao propria.
    }

    // ------------------------------------------------- tetos e folgas

    @Test
    @DisplayName("o multiplicador de Zetsu cabe no teto global")
    void oMultiplicadorDeZetsuCabeNoTeto() {
        double zetsu = de("tecnica.zetsu.multiplicadorDeRegeneracao");
        double teto = de("aura.multiplicadorMaximoDeRegeneracao");

        assertTrue(zetsu <= teto,
                "Zetsu multiplica " + zetsu + " e o teto global e " + teto + ". O excedente e"
                        + " engolido em silencio por AuraFormulas.limitarMultiplicador -- o"
                        + " numero na config diria uma coisa e o jogo faria outra.");

        // SEM FOLGA NENHUMA HOJE: os dois sao 3.0. Quem quiser tornar Zetsu
        // melhor tem de subir o TETO junto, senao gira um botao morto. Nao e bug
        // -- Ten e Zetsu se excluem, entao o produto nunca passa de um
        // multiplicador -- mas e a armadilha que esta linha existe para nomear.
    }
}
