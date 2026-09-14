package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do andaime: mob vanilla emprestado se DECLARA ([ADR-017]).
 *
 * <p>POR QUE ELE EXISTE. Um inimigo com corpo emprestado spawna, anda, ataca,
 * tem loot, tem traducao e passa em mais de cem gametests. Ele parece pronto por
 * TODOS os sinais que este repositorio sabe ler. O unico que denuncia o
 * contrario e alguem abrir o jogo e reconhecer um hoglin -- e isso nenhum
 * portao ve. Entao o que da para medir e outra coisa: que o emprestimo esteja
 * ESCRITO, e que a lista de quem deve seja a fonte, e nao a lembranca de
 * alguem.
 *
 * <p>Ele varre a FONTE -- o diretorio de render -- e nao a lista. E MORDE DOS
 * DOIS LADOS: arquivo que empresta e nao esta na lista reprova, e arquivo na
 * lista que parou de emprestar tambem reprova. Sem a segunda metade, a lista
 * passaria a cobrir em silencio exatamente o dia mais importante -- aquele em
 * que a arte propria chegou e ninguem tirou o andaime do caminho.
 *
 * <p>O QUE ELE NAO PROVA, e e a maior parte: que o modelo proprio esta bom, que
 * a animacao le como a IA, que a hitbox casa com a silhueta. Nada disso tem
 * portao. A prova final e humana, e continua sendo -- ver
 * {@code docs/inimigos/mobs-customizados.md}.
 */
class PlaceholderDeclaradoTest {

    private static final String RENDER = "src/main/java/com/darkcontinent/nenfoundation/client/render";

    /**
     * Especies vanilla concretas. Duas armadilhas moram nesta linha.
     *
     * <p>O limite de palavra importa: sem ele, "Frog" casaria dentro de
     * "FrogInWaitingEntity" e o portao acusaria heranca onde ha so nome parecido.
     *
     * <p>E o limite precisa de DUAS barras. Escrito {@code "\b"} com uma barra so, o Java le
     * BACKSPACE -- e {@code "\s"} le espaco, porque virou escape valido no 15 --
     * entao o padrao compila, o teste passa e nunca casa nada. Este portao chegou a
     * ficar verde exatamente assim por alguns minutos: o pior estado possivel, uma
     * regua que afirma estar medindo.
     */
    private static final Pattern ESPECIE_VANILLA = Pattern.compile(
            "\\bextends\\s+(PolarBear|Hoglin|Frog|Villager|Piglin|Phantom|Pig|Zombie|Spider"
                    + "|IronGolem|Wolf|Sheep|Cow|Chicken|Ravager|Warden)\\b");

    /** Sinais de que o arquivo veste identidade vanilla, e nao so infraestrutura. */
    private static final List<String> EMPRESTIMOS = List.of(
            "ModelLayers.",
            "withDefaultNamespace(\"textures/entity/");

    /**
     * DIVIDA DECLARADA. Cada linha e um inimigo que ainda veste corpo vanilla.
     *
     * <p>Tirar um nome daqui e parte da mesma entrega que lhe da modelo proprio:
     * o {@code PLACEHOLDER} do arquivo e a linha desta lista saem no MESMO PR.
     * Enquanto o nome estiver aqui, o inimigo NAO conta como entregue, por mais
     * verde que o build esteja.
     *
 * <p>HOJE ELA ESTA VAZIA: os inimigos principais tem corpo proprio. Isso
     * nao aposenta o portao nem a lista -- a metade que reprova emprestimo NAO
     * DECLARADO continua sendo a que morde, e e ela que pega o proximo mob que
     * nascer vestindo vanilla. Divida visual zerada tambem nao e o mesmo que mob
     * DONE: faltam sons proprios, e isso esta escrito em
     * {@code docs/inimigos/mobs-customizados.md}.
     */
    private static final Set<String> VESTINDO_VANILLA_AINDA = Set.of(
            // VAZIA, e vazia e o estado CORRETO -- nao um esquecimento.
            //
            // GreatStampModel.java e GreatStampRenderer.java sairam daqui no PR que deu ao
            // great stamp modelo, esqueleto, animacoes, textura e renderer proprios
            // (ADR-017). FrogInWaitingModel.java e FrogInWaitingRenderer.java sairam no PR
            // seguinte, pelo mesmo motivo, e ManFacedApeModel.java e ManFacedApeRenderer.java
            // no PR depois desse -- este foi o primeiro mob com DUAS silhuetas proprias, uma
            // por id. SpiderEagleModel.java e SpiderEagleRenderer.java sairam no PR seguinte,
            // que fechou a fila da migracao: a ave era a primeira VOADORA, e o emprestimo
            // dela mentia sobre ENVERGADURA num mob cuja unica resposta ensinada e recuar.
            //
            // FoxbearModel.java e FoxbearRenderer.java sairam no PR desta linha. O foxbear
            // era de OUTRA FRENTE e por isso ficou por ultimo; ele vestia a geometria do
            // urso-polar com uma textura autoral pintada na UV emprestada -- o disfarce mais
            // convincente da lista, porque a cor ja era nossa. Com ele, a divida VISUAL
            // fecha.
            //
            // Nao reponha nome nenhum: a lista e a divida, e divida paga que continua escrita
            // manda a proxima pessoa refazer o trabalho. Lista vazia tambem nao afrouxa o
            // portao -- ele varre a FONTE, e qualquer renderer novo que volte a emprestar
            // reprova exatamente aqui.
            );

    @Test
    @DisplayName("quem veste corpo vanilla se declara PLACEHOLDER e consta na divida")
    void emprestimoDeIdentidadeSeDeclara() {
        List<Path> fontes = Repo.varrer(RENDER, ".java");
        assertFalse(fontes.isEmpty(),
                "Nenhum arquivo encontrado em " + RENDER + ". Varredura vazia nao e aprovacao:"
                        + " um portao que percorre zero arquivos imprime verde sem verificar nada.");

        Set<String> vestindo = new LinkedHashSet<>();
        Set<String> semMarcador = new LinkedHashSet<>();
        for (Path fonte : fontes) {
            String nome = fonte.getFileName().toString();
            String texto = Repo.texto(RENDER + "/" + nome);
            boolean empresta = EMPRESTIMOS.stream().anyMatch(texto::contains);
            if (!empresta) {
                continue;
            }
            vestindo.add(nome);
            if (!texto.contains("PLACEHOLDER")) {
                semMarcador.add(nome);
            }
        }

        assertTrue(semMarcador.isEmpty(),
                "Estes arquivos vestem identidade vanilla e nao se declaram: " + semMarcador
                        + ". Escreva o marcador PLACEHOLDER dizendo O QUE foi emprestado e O QUE"
                        + " vai substituir. Emprestimo que nao se declara e o que faz um mob"
                        + " emprestado atravessar uma revisao inteira parecendo pronto.");

        Set<String> naoDeclarados = new LinkedHashSet<>(vestindo);
        naoDeclarados.removeAll(VESTINDO_VANILLA_AINDA);
        assertTrue(naoDeclarados.isEmpty(),
                "Inimigo novo vestindo corpo vanilla e fora da lista de divida: " + naoDeclarados
                        + ". Acrescente o nome em VESTINDO_VANILLA_AINDA -- a divida que so existe"
                        + " na cabeca de quem escreveu nao existe.");

        Set<String> dividaQuitada = new LinkedHashSet<>(VESTINDO_VANILLA_AINDA);
        dividaQuitada.removeAll(vestindo);
        assertTrue(dividaQuitada.isEmpty(),
                "Estes arquivos pararam de vestir corpo vanilla e continuam na lista de divida: "
                        + dividaQuitada + ". Tire-os da lista e apague o PLACEHOLDER deles."
                        + " Deixar a linha e pior do que nunca te-la escrito: ela passa a afirmar"
                        + " uma divida que ja foi paga, e a proxima pessoa refaz o trabalho.");
    }

    @Test
    @DisplayName("som vanilla emprestado se declara PLACEHOLDER")
    void somVanillaEmprestadoSeDeclara() {
        // A diretriz PERMITE som vanilla como placeholder (secao 10) -- e permite de
        // propriedade, porque identidade sonora propria esta bloqueada por FERRAMENTA
        // nesta maquina: o Minecraft so toca .ogg vorbis e nao ha encoder aqui. O que
        // ela nao permite e o emprestimo passar despercebido.
        //
        // Som e o emprestimo mais facil de esquecer de todos: ele nao aparece em
        // revisao de diff visual, nao tem textura para alguem estranhar, e some no
        // meio do barulho do jogo. Sem esta regra, o dia em que houver encoder
        // ninguem vai saber QUAIS sons precisam trocar.
        Set<String> semMarcador = new LinkedHashSet<>();
        for (Path fonte : Repo.varrer("src/main/java/com/darkcontinent/nenfoundation/enemy", ".java")) {
            String caminho = Repo.raiz().relativize(fonte).toString().replace('\\', '/');
            String texto = Repo.texto(caminho);
            if (texto.contains("SoundEvents.") && !texto.contains("PLACEHOLDER")) {
                semMarcador.add(caminho);
            }
        }
        assertTrue(semMarcador.isEmpty(),
                "Estes arquivos usam som VANILLA e nao se declaram: " + semMarcador
                        + ". Escreva PLACEHOLDER dizendo qual som foi emprestado e o que vai"
                        + " substituir. A secao 10 da diretriz permite o emprestimo durante o"
                        + " desenvolvimento; o que ela nao permite e ele virar definitivo por"
                        + " ninguem lembrar que estava la.");
    }

    @Test
    @DisplayName("nenhuma entidade do mod herda de especie vanilla concreta")
    void nenhumaEntidadeHerdaDeEspecieVanilla() {
        Set<String> violacoes = new LinkedHashSet<>();
        for (Path fonte : Repo.varrer("src/main/java/com/darkcontinent/nenfoundation/enemy", ".java")) {
            String caminho = Repo.raiz().relativize(fonte).toString().replace('\\', '/');
            for (String linha : Repo.texto(caminho).split("\\R")) {
                String t = linha.strip();
                // SO DECLARACAO DE CLASSE. A primeira versao deste portao procurava a
                // substring "extends Frog" em qualquer linha e reprovava o construtor
                // EntityType<? extends FrogInWaitingEntity> -- um curinga generico, que nao
                // herda nada. Regua que mede a coisa errada custa mais caro que regua
                // nenhuma: ela ensina a ignorar o vermelho.
                if (!t.contains("class ")) {
                    continue;
                }
                // Generico e infraestrutura e pode ficar; especie e identidade e nao pode.
                Matcher especie = ESPECIE_VANILLA.matcher(t);
                if (especie.find()) {
                    violacoes.add(caminho + " -> extends " + especie.group(1));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
                "Entidade herdando de ESPECIE vanilla: " + violacoes + ". Heranca carrega o que nao"
                        + " se ve -- goals escondidos, sons, regras de reproducao, atributos e"
                        + " suposicoes internas da especie. A base e BaseHxHMob; o que vier de"
                        + " vanilla vem de classe generica (Animal, PathfinderMob). Ver ADR-017.");
    }
}
