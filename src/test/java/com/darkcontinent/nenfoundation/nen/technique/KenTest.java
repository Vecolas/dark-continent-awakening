package com.darkcontinent.nenfoundation.nen.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.aura.PresencaDeAura;
import com.darkcontinent.nenfoundation.Repo;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ken: a versao sustentavel de Ren, e o que a separa dele. */
class KenTest {

    private static final String CONFIG =
            "src/main/java/com/darkcontinent/nenfoundation/config/NenConfig.java";

    private static Ken ken() {
        return new Ken(() -> 6.0D, () -> 0.8F);
    }

    @Test
    @DisplayName("Ken NAO redistribui aura, e isso e a decisao de desenho")
    void kenNaoEhDistribuicao() {
        // A TENTACAO ERA OBVIA: "Ken distribui alto em todas as regioes" parece
        // pedir RedistribuiAura. Mas a alocacao soma 1.0 e diz ONDE a aura
        // esta, nao QUANTA -- "alto em todas" e literalmente a alocacao
        // uniforme, que e o repouso.
        //
        // Ken implementando redistribuicao pediria exatamente o que ja existe
        // sem tecnica nenhuma, e a tecnica nao faria nada. Este teste impede
        // que alguem "conserte" isso mais tarde sem refazer a conta.
        // POR REFLEXAO, e nao por `instanceof`: o compilador RECUSA
        // `ken() instanceof RedistribuiAura` porque Ken e final e nao
        // implementa a interface. A recusa e uma prova mais forte que qualquer
        // teste -- so que ela some no dia em que alguem acrescentar a
        // interface, e e ai que este teste precisa existir.
        assertFalse(RedistribuiAura.class.isAssignableFrom(Ken.class),
                "Ken virou tecnica de distribuicao. Alocacao diz ONDE, e Ken e"
                        + " MAGNITUDE -- pedir a alocacao uniforme e pedir o"
                        + " repouso, e a tecnica nao faria nada.");
        assertTrue(ModificaTetoDeOutput.class.isAssignableFrom(Ken.class),
                "Ken nao mexe no teto de Output; sem isso ele nao libera nada.");
    }

    @Test
    @DisplayName("Ken troca pico por duracao: teto e custo abaixo dos de Ren")
    void kenTrocaPicoPorDuracao() {
        double tetoKen = valorDeConfig("tecnica.ken.tetoDeOutput");
        double tetoRen = valorDeConfig("tecnica.ren.tetoDeOutput");
        double custoKen = valorDeConfig("tecnica.ken.custoPorSegundo");
        double custoRen = valorDeConfig("tecnica.ren.custoPorSegundo");
        double repouso = valorDeConfig("aura.tetoDeOutputEmRepouso");
        double custoTen = valorDeConfig("tecnica.ten.custoPorSegundo");

        assertTrue(tetoKen < tetoRen,
                "o teto de Ken (" + tetoKen + ") alcancou o de Ren (" + tetoRen
                        + "). Ren e o pico; Ken e a versao que se aguenta. Iguais,"
                        + " Ken vira Ren com outro nome.");
        assertTrue(tetoKen > repouso,
                "o teto de Ken nao passa do repouso (" + repouso + "), entao ele"
                        + " nao libera nada e a tecnica cobra por nada.");
        assertTrue(custoKen < custoRen,
                "Ken custa tanto quanto Ren (" + custoKen + " vs " + custoRen
                        + "), e ai nao ha o que treinar para durar.");
        assertTrue(custoKen > custoTen,
                "Ken ficou mais barato que Ten (" + custoKen + " vs " + custoTen
                        + "). Ele e a defesa pesada, nao o estado de repouso.");
    }

    @Test
    @DisplayName("Ken LIBERA aura, entao o saldo dele e negativo (ADR-013)")
    void kenPagaEmAura() {
        double regen = valorDeConfig("aura.regeneracaoPorSegundo");
        double custo = valorDeConfig("tecnica.ken.custoPorSegundo");
        // Ken nao multiplica regeneracao -- como Ren, ele so libera.
        assertTrue(regen - custo < 0.0D,
                "Ken se paga: saldo de " + (regen - custo) + "/s. Pelo ADR-013,"
                        + " quem LIBERA aura tem saldo negativo, senao vira o"
                        + " estado sempre-ligado e a escolha some.");
    }

    @Test
    @DisplayName("Ken substitui Ten e Ren, e a exclusao vale dos dois lados")
    void kenSubstituiTenERen() {
        Ten ten = new Ten(() -> 1.0D, () -> 1.0D);
        Ren ren = new Ren(() -> 1.0D, () -> 1.0D);

        assertTrue(ken().incompativeisCom().contains(Ten.ID));
        assertTrue(ken().incompativeisCom().contains(Ren.ID));
        assertTrue(ten.incompativeisCom().contains(Ken.ID),
                "Ten nao recusa Ken. Exclusao pela metade passa numa ordem e"
                        + " falha na outra -- e o registro nem sela.");
        assertTrue(ren.incompativeisCom().contains(Ken.ID), "Ren nao recusa Ken");
        assertTrue(ken().incompativeisCom().contains(Zetsu.ID),
                "Ken libera aura e nao pode conviver com Zetsu.");
    }

    @Test
    @DisplayName("Ken CONVIVE com Gyo e com Shu, de proposito")
    void kenConviveComAsQueConcentram() {
        // Concentrar dentro de um Ken e exatamente o que Ryu vira quando
        // chegar. Excluir as duas aqui fecharia a porta do caminho que o
        // proprio canone descreve.
        assertFalse(ken().incompativeisCom().contains(Gyo.ID),
                "Ken passou a excluir Gyo, e com isso Ryu fica impossivel.");
        assertFalse(ken().incompativeisCom().contains(Shu.ID),
                "Ken passou a excluir Shu.");
    }

    @Test
    @DisplayName("quem esta em Ken e percebido como KEN, e nao como REN")
    void kenTemPresencaPropria() {
        assertEquals(SinalDeAura.KEN, PresencaDeAura.percebida(true, Set.of(Ken.ID)),
                "Ken nao tem presenca propria; para quem olha, ele seria"
                        + " indistinguivel de Ren -- e e o envelope maior dos"
                        + " dois.");
        assertEquals(SinalDeAura.KEN,
                PresencaDeAura.percebida(true, Set.of(Ken.ID, Ren.ID)),
                "com os dois no conjunto, venceu o menor. Hoje eles se excluem,"
                        + " entao esta regra so existe em teste -- e e por isso"
                        + " que ela precisa de teste.");
    }

    @Test
    @DisplayName("Zetsu continua apagando Ken")
    void zetsuApagaKen() {
        assertEquals(SinalDeAura.NENHUM,
                PresencaDeAura.percebida(true, Set.of(Ken.ID, Zetsu.ID)),
                "Zetsu deixou de suprimir Ken. O maior envelope de aura do jogo"
                        + " estaria sendo anunciado por quem esta tentando sumir.");
    }

    @Test
    @DisplayName("KEN entrou no FIM do enum, e a ordem e contrato")
    void kenEhOUltimoOrdinal() {
        // O sinal viaja como ordinal. Inserir no meio reescreveria o
        // significado de TEN e de REN para todo cliente ja conectado, sem erro
        // nenhum em lugar nenhum.
        SinalDeAura[] v = SinalDeAura.values();
        assertEquals(SinalDeAura.KEN, v[v.length - 1],
                "KEN saiu do fim do enum; os ordinais anteriores mudaram de"
                        + " significado.");
        assertEquals(0, SinalDeAura.NENHUM.ordinal(),
                "NENHUM deixou de ser o ordinal zero, que e o valor seguro.");
    }

    private static double valorDeConfig(String chave) {
        String fonte = Repo.texto(CONFIG);
        Matcher m = Pattern.compile(
                        "defineInRange\\(\"" + Pattern.quote(chave) + "\",\\s*([0-9.]+)D")
                .matcher(fonte);
        assertTrue(m.find(), "chave nao encontrada no NenConfig: " + chave);
        return Double.parseDouble(m.group(1));
    }
}
