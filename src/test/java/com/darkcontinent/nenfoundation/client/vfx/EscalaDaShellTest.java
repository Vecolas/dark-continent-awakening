package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da escala da shell de aura: {@code PoseStack.scale} e PROIBIDO.
 *
 * <p>POR QUE ELE EXISTE. A regra estava escrita em quatro arquivos -- o
 * {@code package-info} de {@code vfx.model}, o {@code AuraGeometryLadder}, o
 * {@code AuraPlayerModel} e o {@code AuraRenderRegistro} --, sempre em javadoc,
 * e cobrada em lugar nenhum. Quatro comentarios nao reprovam nada: quem
 * escrevesse {@code pilha.scale(1.08F)} num domingo teria build verde, e a
 * regra viraria folclore no dia em que as quatro frases envelhecessem.
 *
 * <p><b>E o defeito que ela evita nao da erro.</b> Escalar a pilha multiplica a
 * matriz inteira, ossos e juntas incluidos: a aura fica visivelmente maior e
 * <b>descola nas articulacoes</b> quando o corpo anima. Parado, a captura passa.
 * Correndo, agachando e nadando -- as tres capturas centrais do AV0 (#169) -- a
 * manga descola do braco. O jeito certo e {@link
 * net.minecraft.client.model.geom.builders.CubeDeformation} CUBO A CUBO, que
 * engorda a geometria sem tocar na hierarquia de ossos.
 *
 * <p><b>ELE MIRA O TIPO, E NAO O NOME.</b> {@code .scale(} sozinho tambem casa
 * {@code Vec3.scale} e {@code Vector3f.scale}, que sao aritmetica legitima e
 * aparecem no caminho de desenho. Entao a varredura primeiro descobre quais
 * IDENTIFICADORES daquele arquivo sao {@code PoseStack} e so depois procura a
 * chamada neles.
 *
 * <p><b>E ELE SE TESTA.</b> {@link #detectorPegaAChamadaProibida()} alimenta o
 * detector com uma fonte sintetica que DEVE reprovar, e {@link
 * #detectorIgnoraMencaoEmComentario()} com uma que NAO deve. Sem esses dois, um
 * erro na expressao regular deixaria o portao verde para sempre, afirmando estar
 * medindo -- e este repositorio ja teve um portao exatamente nesse estado
 * (ver {@code PlaceholderDeclaradoTest}).
 *
 * <p><b>PONTO CEGO DECLARADO.</b> Ele le TEXTO, e cobre o pacote
 * {@code client/vfx}. Uma escala aplicada fora dele, por reflexao, por um
 * {@code Matrix4f} montado a mao ou dentro de um shader nao aparece aqui. A
 * pergunta "a shell descola?" continua sendo respondida por olho humano nas
 * capturas {@code ten_correndo}, {@code ten_agachado} e {@code ten_nadando};
 * este portao so garante que a causa MAIS BARATA de descolamento nao volte
 * calada.
 */
class EscalaDaShellTest {

    private static final String VFX = "src/main/java/com/darkcontinent/nenfoundation/client/vfx";

    /** Piso da varredura: abaixo disto a regua nao esta lendo o pacote. */
    private static final int PISO_DE_ARQUIVOS = 20;

    /** Declaracao de um identificador {@code PoseStack}: parametro, local ou campo. */
    private static final Pattern DECLARACAO_DE_PILHA =
            Pattern.compile("\\bPoseStack\\s+(\\w+)\\b");

    /**
     * Metodo conhecido que DEVOLVE um {@code PoseStack}.
     *
     * <p>Sem esta metade, {@code evento.getPoseStack().scale(1.1F)} passaria:
     * nao ha identificador nenhum entre o tipo e a chamada.
     */
    private static final Pattern PILHA_ENCADEADA =
            Pattern.compile("getPoseStack\\(\\)\\s*\\.\\s*scale\\s*\\(");

    @Test
    @DisplayName("nenhuma chamada de PoseStack.scale existe na shell de aura")
    void aShellNaoEscalaAPilha() {
        List<Path> arquivos = Repo.varrer(VFX, ".java");
        assertTrue(arquivos.size() >= PISO_DE_ARQUIVOS,
                "A varredura achou so " + arquivos.size() + " arquivos em " + VFX
                        + ", abaixo do piso de " + PISO_DE_ARQUIVOS + ". Varredura vazia nao e"
                        + " aprovacao: conserte o caminho antes de mexer no piso.");

        Set<String> violacoes = new TreeSet<>();
        int arquivosComPilha = 0;
        for (Path arquivo : arquivos) {
            String fonte = semComentarios(ler(arquivo));
            if (DECLARACAO_DE_PILHA.matcher(fonte).find() || fonte.contains("getPoseStack()")) {
                arquivosComPilha++;
            }
            for (String achado : escalasDePilha(fonte)) {
                violacoes.add(arquivo.getFileName() + ": " + achado);
            }
        }

        assertTrue(arquivosComPilha > 0,
                "Nenhum arquivo de " + VFX + " menciona PoseStack. Ou o pacote mudou de lugar,"
                        + " ou o detector parou de reconhecer a declaracao -- nos dois casos este"
                        + " portao esta verde sem medir nada.");

        assertTrue(violacoes.isEmpty(),
                "PoseStack.scale na shell de aura. Isso multiplica a matriz inteira, ossos"
                        + " incluidos: a aura cresce e DESCOLA NAS ARTICULACOES quando o corpo"
                        + " anima -- parado ninguem ve. Engorde a geometria com CubeDeformation,"
                        + " cubo a cubo. Achados: " + violacoes);
    }

    @Test
    @DisplayName("o detector PEGA a chamada proibida -- a regua morde")
    void detectorPegaAChamadaProibida() {
        String comIdentificador = """
                void desenharRegiao(PoseStack pilha, VertexConsumer vertices) {
                    pilha.pushPose();
                    pilha.scale(1.08F, 1.08F, 1.08F);
                    pilha.popPose();
                }
                """;
        assertEquals(List.of("pilha.scale("), escalasDePilha(semComentarios(comIdentificador)),
                "O detector nao viu uma escala aplicada num PoseStack declarado como parametro.");

        String encadeada = """
                void aoRenderizar(RenderLevelStageEvent evento) {
                    evento.getPoseStack().scale(1.1F, 1.1F, 1.1F);
                }
                """;
        assertFalse(escalasDePilha(semComentarios(encadeada)).isEmpty(),
                "O detector nao viu uma escala encadeada em getPoseStack() -- e nesse caso nao ha"
                        + " identificador nenhum entre o tipo e a chamada.");

        String outroNome = """
                void desenhar(PoseStack matrizes) {
                    matrizes.scale(2.0F, 2.0F, 2.0F);
                }
                """;
        assertFalse(escalasDePilha(semComentarios(outroNome)).isEmpty(),
                "O detector depende do NOME da variavel. Renomear pilha para matrizes nao pode"
                        + " apagar a regra.");
    }

    @Test
    @DisplayName("o detector IGNORA mencao em comentario -- senao os quatro javadocs reprovariam")
    void detectorIgnoraMencaoEmComentario() {
        String soComentario = """
                /**
                 * NAO use {@code poseStack.scale}: a escala acontece por CubeDeformation,
                 * cubo a cubo. Um pilha.scale(1.1F) aqui descolaria a aura das juntas.
                 */
                void desenharRegiao(PoseStack pilha, VertexConsumer vertices) {
                    // pilha.scale(1.08F) -- proibido, ver o javadoc acima
                    pilha.pushPose();
                }
                """;
        assertEquals(List.of(), escalasDePilha(semComentarios(soComentario)),
                "O detector confundiu javadoc com chamada. Os quatro arquivos que PROIBEM a"
                        + " escala citam o nome dela, e um portao que reprovasse por isso obrigaria"
                        + " a apagar justamente a documentacao da regra.");
    }

    @Test
    @DisplayName("Vec3.scale continua permitido -- e aritmetica, nao transformacao de pilha")
    void aritmeticaDeVetorNaoEViolacao() {
        String vetor = """
                void desenhar(PoseStack pilha) {
                    Vec3 direcao = olhar.scale(0.5D);
                    Vector3f normal = base.scale(forca);
                    pilha.pushPose();
                }
                """;
        assertEquals(List.of(), escalasDePilha(semComentarios(vetor)),
                "O detector reprovou Vec3.scale/Vector3f.scale. Isso e aritmetica legitima no"
                        + " caminho de desenho, e proibi-la empurraria o calculo para um lugar pior.");
    }

    // ------------------------------------------------------------- detector

    /**
     * Chamadas de {@code scale} aplicadas a um {@code PoseStack} desta fonte.
     *
     * <p>Duas passagens, e a ordem importa: primeiro descobre os nomes que SAO
     * {@code PoseStack} naquele arquivo, depois procura a chamada neles. Feito
     * ao contrario, ou casaria {@code Vec3.scale}, ou dependeria de a variavel
     * chamar-se {@code poseStack}.
     */
    private static List<String> escalasDePilha(String fonteSemComentarios) {
        Set<String> nomes = new LinkedHashSet<>();
        Matcher declaracao = DECLARACAO_DE_PILHA.matcher(fonteSemComentarios);
        while (declaracao.find()) {
            nomes.add(declaracao.group(1));
        }

        List<String> achados = new ArrayList<>();
        for (String nome : nomes) {
            Matcher chamada = Pattern
                    .compile("\\b" + Pattern.quote(nome) + "\\s*\\.\\s*scale\\s*\\(")
                    .matcher(fonteSemComentarios);
            if (chamada.find()) {
                achados.add(nome + ".scale(");
            }
        }
        if (PILHA_ENCADEADA.matcher(fonteSemComentarios).find()) {
            achados.add("getPoseStack().scale(");
        }
        return List.copyOf(achados);
    }

    /**
     * Remove comentario de bloco e de linha.
     *
     * <p>A regra e citada por nome nos javadocs que a proibem; sem esta limpeza
     * o portao reprovaria a propria documentacao dela.
     */
    private static String semComentarios(String fonte) {
        return fonte.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }

    private static String ler(Path arquivo) {
        return Repo.texto(Repo.raiz().relativize(arquivo).toString().replace('\\', '/'));
    }
}
