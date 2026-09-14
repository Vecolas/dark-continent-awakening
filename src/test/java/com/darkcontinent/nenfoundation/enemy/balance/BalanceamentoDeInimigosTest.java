package com.darkcontinent.nenfoundation.enemy.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.content.EnemyCatalog;
import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao de BALANCEAMENTO POR PAPEL (EN15 / issue #150).
 *
 * <p>O erro que ele pega nao levanta excecao: um ELITE que morre em dois
 * segundos e um LOW que leva um minuto passam por todos os outros portoes -- os
 * numeros sao validos, os atributos sao finitos, o mob nasce e morre. O que eles
 * quebram e o ROTULO, e um rotulo em que ninguem confia e pior do que nenhum.</p>
 *
 * <p><b>A conta e o LIMITE SUPERIOR de eficiencia:</b> dois lados parados
 * trocando golpes, sem desvio, telegrafo, terreno, cura nem erro de mira. Um
 * combate real sempre dura mais. Se ja o limite superior estiver fora da faixa,
 * o real esta pior -- e e por isso que a reprovacao aqui vale mesmo sendo um
 * modelo simplificado.</p>
 */
class BalanceamentoDeInimigosTest {

    /**
     * Cadencia de ataque assumida, em golpes por segundo.
     *
     * <p>Ela e um NUMERO DE TESTE e nao do jogo: os mobs tem recarga propria, e
     * lê-la aqui exigiria que este portao conhecesse quatro arquivos de perfil
     * diferentes. Assumir uma cadencia comum para todos torna a comparacao ENTRE
     * eles justa, que e o que a faixa mede. O custo esta declarado: um mob com
     * recarga muito mais longa parece mais perigoso aqui do que e em jogo.</p>
     */
    private static final double CADENCIA_ASSUMIDA = 0.6D;

    private static PrevisaoDeCombate prever(EnemyDefinition definicao) {
        ThreatTier papel = definicao.metadata().threatTier();
        return PrevisaoDeCombate.de(FaixaDeTempo.loadoutDe(papel), definicao.attributes(),
                CADENCIA_ASSUMIDA);
    }

    // --------------------------------------------------------------- portao

    @Test
    @DisplayName("PORTAO: o tempo para matar de cada bicho cabe na faixa do papel dele")
    void todoBichoCabeNaFaixaDoPapel() {
        Map<ThreatTier, FaixaDeTempo> faixas = FaixaDeTempo.porPapel();
        List<String> foraDaFaixa = new ArrayList<>();

        List<String> excecoesQueVoltaram = new ArrayList<>();

        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            ThreatTier papel = entrada.getValue().metadata().threatTier();
            FaixaDeTempo faixa = faixas.get(papel);
            double segundos = prever(entrada.getValue()).segundosParaMatar();
            boolean dentro = faixa.contem(segundos);
            boolean declarado = ExcecoesDeBalanceamento.declarado(entrada.getKey());

            if (!dentro && !declarado) {
                foraDaFaixa.add(String.format("%s (%s): %.1fs, faixa %.1f..%.1f",
                        entrada.getKey(), papel, segundos,
                        faixa.minimoEmSegundos(), faixa.maximoEmSegundos()));
            }
            if (dentro && declarado) {
                excecoesQueVoltaram.add(String.format("%s (%s): %.1fs ja cabe em %.1f..%.1f",
                        entrada.getKey(), papel, segundos,
                        faixa.minimoEmSegundos(), faixa.maximoEmSegundos()));
            }
        }

        assertTrue(foraDaFaixa.isEmpty(),
                "Inimigos fora da faixa do proprio papel e SEM excecao declarada:\n  "
                        + String.join("\n  ", foraDaFaixa)
                        + "\n\nIsto nao e erro de codigo: e o rotulo de ameaca mentindo. O"
                        + " jogador aprende que ThreatTier nao quer dizer nada, e para de"
                        + " confiar nele. Conserte o ATRIBUTO, conserte o PAPEL, ou DECLARE a"
                        + " excecao em ExcecoesDeBalanceamento com o motivo -- nunca afrouxe a"
                        + " faixa, que e o contrato de todos os outros.");

        // O OUTRO LADO DA MORDIDA. Sem ele, a lista de excecoes passaria a cobrir
        // em silencio o dia em que a excecao deixasse de existir -- e um ELITE que
        // virou saco de pancada por engano ficaria coberto por ela.
        assertTrue(excecoesQueVoltaram.isEmpty(),
                "Excecoes declaradas que JA cabem na faixa:\n  "
                        + String.join("\n  ", excecoesQueVoltaram)
                        + "\n\nTire a linha de ExcecoesDeBalanceamento no mesmo PR que devolveu"
                        + " o bicho para dentro da faixa.");
    }

    @Test
    @DisplayName("toda excecao de balanceamento tem MOTIVO, e o motivo diz o que se perde")
    void excecaoSemMotivoReprova() {
        assertFalse(ExcecoesDeBalanceamento.todos().isEmpty(),
                "Nenhuma excecao declarada: se a lista esvaziou de verdade, este caso deixou de"
                        + " medir alguma coisa e tem de ser REMOVIDO -- nao mantido verde.");
        ExcecoesDeBalanceamento.todos().forEach((id, motivo) -> {
            assertTrue(EnemyCatalog.publicados().containsKey(id),
                    "A excecao '" + id + "' nao corresponde a inimigo publicado nenhum: ela"
                            + " cobre um bicho que nao existe, e cobriria em silencio outro que"
                            + " ganhasse esse id depois.");
            assertTrue(motivo != null && motivo.length() > 80,
                    "A excecao '" + id + "' tem motivo curto demais. Motivo de uma linha vira"
                            + " carimbo: a proxima pessoa le, nao entende, e mantem.");
        });
    }

    @Test
    @DisplayName("PORTAO: a tabela publicada e a que a conta produz AGORA")
    void aTabelaPublicadaNaoEnvelhece() {
        // Uma tabela de balanceamento escrita a mao envelhece no primeiro numero
        // girado, e envelhece em SILENCIO: o documento continua parecendo a
        // verdade. Aqui ele e DERIVADO -- e quando divergir, este caso regrava o
        // arquivo e reprova, para que a diferenca apareca no diff em vez de
        // apodrecer.
        // Normalizado nos DOIS lados. A primeira versao usava %n no String.format,
        // que resolve para o separador da PLATAFORMA: no Windows a tabela saia com
        // CRLF e o arquivo era lido normalizado, a comparacao nunca casava, e o
        // portao reprovava para sempre sem que nada estivesse errado -- uma regua
        // que grita todo dia e uma regua que a proxima pessoa desliga.
        String esperado = normalizar(montarTabela());
        java.nio.file.Path destino =
                com.darkcontinent.nenfoundation.Repo.raiz().resolve(TABELA);
        String atual = java.nio.file.Files.exists(destino)
                ? leia(destino) : "(o arquivo nao existe)";
        if (!esperado.equals(atual)) {
            escreva(destino, esperado);
            org.junit.jupiter.api.Assertions.fail(
                    "A tabela de balanceamento em " + TABELA + " estava desatualizada e foi"
                            + " REGRAVADA agora. Confira o diff e commite junto com a mudanca de"
                            + " numero que a causou -- um documento de balanceamento que nao"
                            + " acompanha os atributos nao e documentacao, e folclore.");
        }
    }

    private static final String TABELA = "docs/inimigos/balanceamento.md";

    private static String montarTabela() {
        Map<ThreatTier, FaixaDeTempo> faixas = FaixaDeTempo.porPapel();
        StringBuilder texto = new StringBuilder();
        texto.append("# Balanceamento dos inimigos, por papel\n\n")
                .append("> **Este arquivo e GERADO.** Ele sai de `BalanceamentoDeInimigosTest`,")
                .append(" que o regrava e reprova quando ele diverge dos atributos. Nao edite a")
                .append(" mao: edite o perfil do bicho, rode `./gradlew test` e commite o diff.\n\n")
                .append("A conta e o **limite superior de eficiencia** -- dois lados parados")
                .append(" trocando golpes, sem desvio, telegrafo, terreno, cura nem erro de")
                .append(" mira. Um combate real sempre dura mais; se ja o limite superior")
                .append(" estiver fora da faixa, o real esta pior.\n\n")
                .append("Cada papel e medido contra o loadout que **encontra** aquele papel:")
                .append(" pedra sem armadura ate HUNTER, ferro completo em DANGEROUS e ELITE,")
                .append(" diamante completo de SQUADRON para cima. Medir um oficial de Chimera")
                .append(" contra a espada de pedra diria que ele e impossivel, e nenhuma das")
                .append(" duas leituras seria util.\n\n")
                .append("| Inimigo | Papel | Vida | Dano | Armadura | Para matar | Para morrer |")
                .append(" Faixa do papel |\n")
                .append("| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |\n");

        EnemyCatalog.publicados().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entrada -> {
                    EnemyAttributes a = entrada.getValue().attributes();
                    ThreatTier papel = entrada.getValue().metadata().threatTier();
                    PrevisaoDeCombate previsao = prever(entrada.getValue());
                    FaixaDeTempo faixa = faixas.get(papel);
                    String marca = ExcecoesDeBalanceamento.declarado(entrada.getKey())
                            ? " *(excecao declarada)*" : "";
                    texto.append(String.format(java.util.Locale.ROOT,
                            "| `%s` | %s | %.0f | %.0f | %.0f | %.1fs | %.1fs | %.1f..%.1f%s |\n",
                            entrada.getKey(), papel, a.maxHealth(), a.attackDamage(), a.armor(),
                            previsao.segundosParaMatar(), previsao.segundosParaMorrer(),
                            faixa.minimoEmSegundos(), faixa.maximoEmSegundos(), marca));
                });

        texto.append("\n## Excecoes declaradas\n\n");
        ExcecoesDeBalanceamento.todos().forEach((id, motivo) ->
                texto.append("- **`").append(id).append("`** -- ").append(motivo).append("\n"));
        texto.append("\n## O que esta tabela NAO diz\n\n")
                .append("Ela nao mede desvio, telegrafo, terreno, cura, pocao, encantamento,")
                .append(" critico, knockback nem a chance de errar o golpe. Tambem nao mede")
                .append(" GRUPO: quatro lobos nao sao quatro vezes um lobo. E ela assume uma")
                .append(" cadencia comum para todos os mobs, o que torna a comparacao entre")
                .append(" eles justa e faz um mob de recarga longa parecer mais perigoso aqui")
                .append(" do que e em jogo.\n");
        return texto.toString();
    }

    /** Um separador de linha so, dos dois lados da comparacao. */
    private static String normalizar(String texto) {
        return texto.replace("\r\n", "\n");
    }

    private static String leia(java.nio.file.Path arquivo) {
        try {
            return normalizar(java.nio.file.Files.readString(arquivo,
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException erro) {
            throw new java.io.UncheckedIOException(erro);
        }
    }

    private static void escreva(java.nio.file.Path arquivo, String texto) {
        try {
            java.nio.file.Files.createDirectories(arquivo.getParent());
            java.nio.file.Files.writeString(arquivo, texto,
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException erro) {
            throw new java.io.UncheckedIOException(erro);
        }
    }

    @Test
    @DisplayName("PORTAO: nenhum papel foi resolvido enchendo a barra de vida")
    void dificuldadeNaoEBarraDeVida() {
        // A issue #150 proibe com todas as letras: nao ajustar HP para mascarar
        // gimmick quebrada. A leitura possivel sem jogar e a RAZAO entre vida e
        // dano -- um mob com muita vida e pouco dano nao e dificil, e demorado.
        List<String> sacoDePancada = new ArrayList<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            EnemyAttributes attrs = entrada.getValue().attributes();
            if (attrs.attackDamage() <= 0.0F) continue;  // o Puffball nao bate, de proposito
            double razao = attrs.maxHealth() / attrs.attackDamage();
            if (razao > 20.0D) {
                sacoDePancada.add(String.format("%s: %.0f de vida para %.0f de dano (%.1fx)",
                        entrada.getKey(), attrs.maxHealth(), attrs.attackDamage(), razao));
            }
        }
        assertTrue(sacoDePancada.isEmpty(),
                "Inimigos com vida desproporcional ao dano:\n  " + String.join("\n  ", sacoDePancada)
                        + "\n\nMuita vida e pouco dano nao e dificil: e demorado. O combate fica"
                        + " sem tensao e sem risco, e o jogador conclui que o encontro nao"
                        + " valia a pena -- sem que nada no build reclame.");
    }

    @Test
    @DisplayName("mobs que NAO ganham por combate sao poucos, e cada um e uma decisao")
    void quemNaoVenceNoCombateEDeclarado() {
        List<String> perdemATroca = new ArrayList<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            if (prever(entrada.getValue()).razaoDeTroca() > 1.0D) perdemATroca.add(entrada.getKey());
        }
        // NAO ha assercao de lista vazia aqui: razao acima de 1 e legitima para
        // quem ensina outro caminho (o Master of the Swamp vence pela captura). O
        // que este caso cobra e que sejam POUCOS -- se metade do bestiario vencer
        // a troca, o jogo deixou de ser sobre combate sem ninguem ter decidido.
        int total = EnemyCatalog.publicados().size();
        assertTrue(perdemATroca.size() * 2 <= total,
                "Mais da metade dos inimigos vence a troca de golpes contra o loadout de"
                        + " referencia: " + perdemATroca + ". Cada um sozinho pode ser uma"
                        + " decisao; todos juntos sao um jogo que ninguem escolheu.");
    }

    // ---------------------------------------------------------------- conta

    @Test
    @DisplayName("a armadura do MOB entra na conta -- senao a ficha dele some")
    void armaduraDoMobContaNaConta() {
        EnemyAttributes semCouro = new EnemyAttributes(90, 0.26F, 12, 0, 28, 0.8F);
        EnemyAttributes comCouro = new EnemyAttributes(90, 0.26F, 12, 9, 28, 0.8F);
        LoadoutDeReferencia loadout = LoadoutDeReferencia.preparado();

        double semArmadura = PrevisaoDeCombate.de(loadout, semCouro, 0.6D).segundosParaMatar();
        double comArmadura = PrevisaoDeCombate.de(loadout, comCouro, 0.6D).segundosParaMatar();
        assertTrue(comArmadura > semArmadura * 1.25D,
                "A armadura do mob quase nao mudou o tempo: medir dano bruto contra vida faria"
                        + " o King White Stag Beetle pontuar igual a um bicho sem couro, e a"
                        + " armadura, que e a ficha dele, nao apareceria na conta.");
    }

    @Test
    @DisplayName("a armadura do JOGADOR usa a formula do vanilla, e nao uma aproximacao")
    void armaduraDoJogadorUsaAFormulaDoVanilla() {
        // Numeros conferidos contra a formula do jogo: ferro completo (15) contra
        // um golpe de 12 corta para 25 - max(3, 15 - 6) = 25 - 9 -> 16/25 do dano.
        LoadoutDeReferencia ferro = LoadoutDeReferencia.preparado();
        assertEquals(12.0D * (1.0D - 9.0D / 25.0D), ferro.danoRecebido(12.0D), 1.0e-9D);

        // Sem armadura, o dano chega inteiro.
        assertEquals(12.0D, LoadoutDeReferencia.inicial().danoRecebido(12.0D), 1.0e-9D);

        // Tenacidade muda o resultado: diamante (20 de armadura, 8 de tenacidade)
        // segura mais um golpe pesado do que ferro segura um leve.
        assertTrue(LoadoutDeReferencia.veterano().danoRecebido(20.0D)
                < ferro.danoRecebido(20.0D),
                "A tenacidade sumiu da conta: uma aproximacao de 4 por cento por ponto erra"
                        + " por dezenas de por cento com tenacidade, e o erro aparece como um"
                        + " mob que mede bem na planilha e mata rapido demais em jogo.");
    }

    @Test
    @DisplayName("mob de dano zero nao vira infinito na tabela")
    void danoZeroNaoContaminaARelacao() {
        EnemyAttributes inofensivo = new EnemyAttributes(18, 0.0F, 0, 0, 12, 1.0F);
        PrevisaoDeCombate previsao = PrevisaoDeCombate.de(
                LoadoutDeReferencia.inicial(), inofensivo, 0.6D);
        assertEquals(PrevisaoDeCombate.SEM_AMEACA, previsao.segundosParaMorrer());
        assertTrue(Double.isFinite(previsao.razaoDeTroca()),
                "Infinito contamina medias, ordenacoes e qualquer relatorio, e o sintoma e uma"
                        + " tabela cheia de valores que ninguem le.");
    }

    @Test
    @DisplayName("faixa e loadout invalidos reprovam")
    void entradasInvalidasReprovam() {
        assertThrows(IllegalArgumentException.class, () -> new FaixaDeTempo(10.0D, 5.0D));
        assertThrows(IllegalArgumentException.class, () -> new FaixaDeTempo(0.0D, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new LoadoutDeReferencia(0.0D, 1.6D, 20.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new LoadoutDeReferencia(5.0D, 1.6D, 20.0D, 25.0D, 0.0D));
    }

    @Test
    @DisplayName("todo papel usado pelo catalogo tem faixa e loadout declarados")
    void todoPapelUsadoTemFaixa() {
        Map<ThreatTier, FaixaDeTempo> faixas = FaixaDeTempo.porPapel();
        for (EnemyDefinition definicao : EnemyCatalog.publicados().values()) {
            ThreatTier papel = definicao.metadata().threatTier();
            assertTrue(faixas.containsKey(papel),
                    "O papel " + papel + " esta em uso e nao tem faixa: o portao passaria a"
                            + " NAO medir esse bicho, e ficaria verde varrendo menos.");
            assertFalse(FaixaDeTempo.loadoutDe(papel) == null);
        }
        assertEquals(ThreatTier.values().length, faixas.size(),
                "Um ThreatTier novo sem faixa nao reprova nada ate alguem usa-lo, e nesse dia"
                        + " o bicho novo entra sem medida.");
    }
}
