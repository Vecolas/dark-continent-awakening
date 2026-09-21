package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da traducao de conteudo: bloco e item registrados tem nome nos DOIS idiomas.
 *
 * <p>POR QUE ELE EXISTE. Um bloco sem chave de traducao passa em TUDO que este
 * repositorio sabe medir: ele registra, tem blockstate, tem modelo, entra na
 * tag, tem loot e aparece no criativo. O build fica verde. O unico sinal e
 * alguem abrir o inventario e ler {@code block.nenfoundation.world_tree_bark}
 * escrito na tooltip -- e o jogo nao trata isso como erro, trata como o nome.
 *
 * <p>Ele nasceu depois de uma varredura achar <b>vinte blocos e cinco itens</b>
 * da World Tree nessa situacao, todos eles registrados, modelados e em uso.
 * Nenhum portao tinha visto, porque nenhum portao olhava para ca.
 *
 * <p><b>MORDE DOS DOIS LADOS.</b> Id registrado sem chave reprova, e chave de
 * nome que nao corresponde a id registrado nenhum tambem reprova. Sem a segunda
 * metade, um rename deixaria a chave velha apodrecendo no arquivo e o bloco
 * novo sem nome -- as duas coisas em silencio.
 *
 * <p><b>ELE DESCOBRE OS HELPERS, e nao os lista.</b> Uma lista escrita a mao de
 * nomes de metodo envelhece calada: no primeiro dia deste portao ela ja estava
 * errada -- {@code NenItems.note(...)} registrava seis itens que a varredura nao
 * via, e o que denunciou isso foi a metade ORFA do portao, nao a lista. Entao a
 * varredura primeiro procura as ASSINATURAS ({@code private static ... X(String
 * ...)}) nos arquivos que registram, e so depois procura as chamadas.
 *
 * <p><b>PONTO CEGO DECLARADO.</b> Ele le a FONTE como TEXTO. Um registro que nao
 * passe por um {@code private static} com {@code String} na primeira posicao --
 * um laco sobre uma lista de ids, um id montado por concatenacao -- continua
 * invisivel. O que resta de defesa sao duas coisas: o piso de
 * {@link #PISO_DE_REGISTROS}, que denuncia a regra casando NADA, e a metade orfa,
 * que denuncia o conteudo traduzido cuja id sumiu da varredura. Conteudo que
 * escape das DUAS -- sem casar a regra e sem traducao -- e invisivel. A
 * limitacao esta escrita em {@code docs/testing/o-que-nao-provamos.md}.
 *
 * <p>E ele NAO julga a traducao. Um nome errado, um portugues torto ou a palavra
 * "TODO" escrita no valor passam. O que ele prova e que a chave EXISTE nos dois
 * arquivos -- nada sobre o que ela diz.
 */
class TraducaoDeConteudoTest {

    private static final String JAVA = "src/main/java/com/darkcontinent/nenfoundation";
    private static final String EN = "src/main/resources/assets/nenfoundation/lang/en_us.json";
    private static final String PT = "src/main/resources/assets/nenfoundation/lang/pt_br.json";

    /**
     * Piso da varredura. Ele nao e um numero de design: e a contagem que existia
     * quando o portao foi escrito, e serve para UMA coisa -- denunciar o dia em
     * que o padrao de registro mudar e a expressao regular passar a casar nada.
     * Subir este numero e de quem adiciona conteudo; descer exige dizer qual
     * conteudo saiu.
     */
    private static final int PISO_DE_REGISTROS = 50;

    /** Quem registra direto. O {@code \\(} logo depois do nome impede casar prefixo. */
    private static final String CHAMADA_BLOCO = "BLOCKS\\.register(?:SimpleBlock|Block)?";
    private static final String CHAMADA_ITEM = "ITEMS\\.register(?:SimpleItem|Item)?";

    /**
     * Assinatura de helper de registro: {@code private static <o que for> nome(String ...}.
     *
     * <p>O {@code private static} nao e enfeite. Sem ele, qualquer metodo do
     * arquivo que receba uma String entraria na lista -- e a varredura passaria a
     * colher o primeiro argumento de coisas que nao registram nada.
     */
    private static final Pattern HELPER = Pattern.compile(
            "\\bprivate\\s+static\\s+[\\w.<>?,\\[\\]\\s]+?\\s(\\w+)\\(\\s*String\\s");

    /** Chave de NOME: exatamente tres segmentos. {@code .status} e {@code .ready} sao outra coisa. */
    private static final Pattern CHAVE_DE_NOME = Pattern.compile(
            "^\\s*\"((?:block|item)\\.nenfoundation\\.[a-z0-9_]+)\"\\s*:", Pattern.MULTILINE);

    @Test
    @DisplayName("todo bloco e item registrado tem nome em en_us e em pt_br")
    void conteudoRegistradoTemNome() {
        Set<String> blocos = varrer(CHAMADA_BLOCO);
        Set<String> itens = varrer(CHAMADA_ITEM);
        int total = blocos.size() + itens.size();
        assertTrue(total >= PISO_DE_REGISTROS,
                "A varredura achou so " + total + " registros, abaixo do piso de "
                        + PISO_DE_REGISTROS + ". Isso quase nunca significa que o conteudo"
                        + " encolheu; significa que o padrao de registro mudou e esta regua"
                        + " parou de medir. Conserte a expressao regular antes de mexer no piso.");

        Set<String> en = chaves(EN);
        Set<String> pt = chaves(PT);

        Set<String> semNome = new TreeSet<>();
        for (String id : blocos) {
            if (!en.contains("block.nenfoundation." + id)) {
                semNome.add("en_us: block." + id);
            }
            if (!pt.contains("block.nenfoundation." + id)) {
                semNome.add("pt_br: block." + id);
            }
        }
        for (String id : itens) {
            // BlockItem herda o nome do bloco: qualquer um dos dois prefixos serve.
            if (!en.contains("item.nenfoundation." + id)
                    && !en.contains("block.nenfoundation." + id)) {
                semNome.add("en_us: item." + id);
            }
            if (!pt.contains("item.nenfoundation." + id)
                    && !pt.contains("block.nenfoundation." + id)) {
                semNome.add("pt_br: item." + id);
            }
        }
        assertTrue(semNome.isEmpty(),
                "Conteudo registrado sem chave de traducao. No jogo o nome vira a propria"
                        + " chave, e nada acusa isso: " + semNome);
    }

    @Test
    @DisplayName("chave de nome orfa reprova -- um rename nao deixa lixo para tras")
    void naoHaChaveDeNomeOrfa() {
        Set<String> registrados = new LinkedHashSet<>();
        for (String id : varrer(CHAMADA_BLOCO)) {
            registrados.add("block.nenfoundation." + id);
        }
        for (String id : varrer(CHAMADA_ITEM)) {
            registrados.add("item.nenfoundation." + id);
            registrados.add("block.nenfoundation." + id);
        }

        Set<String> orfas = new TreeSet<>();
        for (String chave : chavesDeNome(EN)) {
            if (!registrados.contains(chave)) {
                orfas.add(chave);
            }
        }
        assertTrue(orfas.isEmpty(),
                "Chave de nome sem conteudo registrado correspondente. Ou o id foi"
                        + " renomeado e a chave velha ficou, ou o conteudo saiu e o texto nao: "
                        + orfas);
    }

    @Test
    @DisplayName("en_us e pt_br cobrem exatamente o mesmo conjunto de chaves")
    void osDoisIdiomasCobremOMesmo() {
        Set<String> en = new TreeSet<>(chaves(EN));
        Set<String> pt = new TreeSet<>(chaves(PT));
        Set<String> soEn = new TreeSet<>(en);
        soEn.removeAll(pt);
        Set<String> soPt = new TreeSet<>(pt);
        soPt.removeAll(en);
        assertEquals(Set.<String>of(), soEn,
                "Chaves so em en_us -- o jogador em pt_br le a chave crua.");
        assertEquals(Set.<String>of(), soPt,
                "Chaves so em pt_br -- o jogador em en_us le a chave crua.");
    }

    /**
     * Ids registrados, arquivo por arquivo.
     *
     * <p>DUAS PASSAGENS POR ARQUIVO, e a ordem importa. A primeira acha as
     * assinaturas dos helpers daquele arquivo; a segunda acha as chamadas, tanto
     * as diretas quanto as dos helpers recem-descobertos. Feito numa passagem so,
     * um helper declarado DEPOIS de ser usado -- que e o normal em Java -- nao
     * seria visto.
     */
    private static Set<String> varrer(String chamadaDireta) {
        Set<String> ids = new LinkedHashSet<>();
        List<Path> arquivos = Repo.varrer(JAVA, ".java");
        assertTrue(arquivos.size() > 100,
                "A varredura da fonte achou so " + arquivos.size() + " arquivos Java.");
        for (Path arquivo : arquivos) {
            String fonte = ler(arquivo);
            if (!Pattern.compile("\\b" + chamadaDireta + "\\(").matcher(fonte).find()) {
                continue;
            }
            StringBuilder alternativas = new StringBuilder(chamadaDireta);
            Matcher helper = HELPER.matcher(fonte);
            while (helper.find()) {
                alternativas.append('|').append(Pattern.quote(helper.group(1)));
            }
            Matcher m = Pattern.compile("\\b(?:" + alternativas + ")\\(\\s*\"([a-z0-9_]+)\"")
                    .matcher(fonte);
            while (m.find()) {
                ids.add(m.group(1));
            }
        }
        return ids;
    }

    private static Set<String> chaves(String arquivo) {
        Set<String> chaves = new LinkedHashSet<>();
        Matcher m = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:", Pattern.MULTILINE)
                .matcher(Repo.texto(arquivo));
        while (m.find()) {
            chaves.add(m.group(1));
        }
        assertTrue(chaves.size() > 100, "O arquivo " + arquivo + " rendeu so " + chaves.size()
                + " chaves. Verde com zero leitura e o falso verde mais barato que existe.");
        return chaves;
    }

    private static Set<String> chavesDeNome(String arquivo) {
        Set<String> chaves = new LinkedHashSet<>();
        Matcher m = CHAVE_DE_NOME.matcher(Repo.texto(arquivo));
        while (m.find()) {
            chaves.add(m.group(1));
        }
        return chaves;
    }

    private static String ler(Path arquivo) {
        return Repo.texto(Repo.raiz().relativize(arquivo).toString().replace('\\', '/'));
    }
}
