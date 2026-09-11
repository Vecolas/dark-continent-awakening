package com.darkcontinent.nenfoundation.nen.category;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O UNICO lugar que responde quao bem alguem se da com uma categoria.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A PERGUNTA E SOBRE O PERFIL, e nao sobre uma categoria solta. Quem
 * consulta escreve {@code NenAffinity.effectiveness(perfil, TRANSMUTATION)}, e
 * nao {@code ...(perfil.category(), TRANSMUTATION)}. A diferenca parece
 * cosmetica hoje, porque so a categoria primaria entra na conta -- e e o
 * ponto: no dia em que afinidade secundaria (<i>leaning</i>) existir, ela entra
 * AQUI DENTRO, e nenhum codigo de habilidade muda. Com a categoria solta na
 * assinatura, esse dia obrigaria a editar todo chamador.
 *
 * <p>2. TRES PERGUNTAS, TRES METODOS. Ver {@link Afinidade}.
 *
 * <p>3. NAO HA MATRIZ EM JAVA. Os numeros chegam por datapack, e este servico
 * comeca VAZIO. Um default embutido "para o caso de o arquivo faltar" seria a
 * mesma verdade em duas fontes, e a de Java venceria em silencio no dia em que
 * o JSON quebrasse.
 *
 * <p>4. SEM MATRIZ, A RESPOSTA E ZERO -- E BARULHENTA. Zero calado seria o pior
 * resultado possivel: toda habilidade renderia nada, nada daria erro, e a
 * investigacao comecaria pelo balanceamento em vez de pelo datapack. O aviso
 * sai UMA vez por carregamento, e nao por consulta, porque consulta acontece em
 * tick.
 *
 * <p>PONTO CEGO DECLARADO: <b>nao existe consumidor de afinidade ainda.</b> O
 * primeiro chega no M4, com as tecnicas. Ate la, este servico e exercitado
 * apenas por teste -- ele esta certo, mas nao esta em uso.
 */
public final class NenAffinity {

    private static final Logger LOG = LoggerFactory.getLogger(NenAffinity.class);

    /**
     * A matriz em uso. {@code volatile} porque a recarga de datapack e
     * preparada fora da thread do servidor e aplicada nela.
     */
    private static volatile MatrizDeAfinidade matriz;

    /** Para o aviso de "sem matriz" sair uma vez por carregamento, e nao por tick. */
    private static final AtomicBoolean JA_AVISOU = new AtomicBoolean(false);

    private NenAffinity() {
    }

    // ------------------------------------------------------- as tres perguntas

    /** Velocidade de ganho de proficiencia deste jogador nesta categoria. */
    public static double learningRate(PersistentNenData perfil, NenCategory alvo) {
        return consultar(perfil, alvo).learningRate();
    }

    /** Teto de proficiencia que este jogador alcanca nesta categoria. */
    public static double maxProficiency(PersistentNenData perfil, NenCategory alvo) {
        return consultar(perfil, alvo).maxProficiency();
    }

    /** Quanto uma tecnica desta categoria rende para este jogador. */
    public static double effectiveness(PersistentNenData perfil, NenCategory alvo) {
        return consultar(perfil, alvo).effectiveness();
    }

    /**
     * As tres de uma vez.
     *
     * <p>Existe para quem precisa das tres: pedir uma por vez faria tres
     * leituras do {@code volatile} e tres buscas na matriz dentro do mesmo
     * calculo.
     *
     * <p>A afinidade sai da categoria ATRIBUIDA, e nao da revelada. Um jogador
     * que ainda nao fez a Water Divination ja tem a afinidade dele -- ele so
     * nao sabe qual e. Usar {@code categoriaVisivel()} aqui faria a
     * revelacao MUDAR o poder do jogador, o que transformaria um evento de
     * narrativa num buff.
     */
    public static Afinidade consultar(PersistentNenData perfil, NenCategory alvo) {
        Objects.requireNonNull(perfil, "perfil");
        return entre(perfil.category(), alvo);
    }

    /**
     * A consulta crua entre duas categorias, sem perfil.
     *
     * <p>Para diagnostico, comando e teste. <b>Codigo de gameplay usa a versao
     * com perfil</b>: e ela que vai ganhar a afinidade secundaria sem quebrar
     * chamador.
     */
    public static Afinidade entre(NenCategory origem, NenCategory alvo) {
        MatrizDeAfinidade atual = matriz;
        if (atual == null) {
            avisarUmaVez();
            return Afinidade.NENHUMA;
        }
        return atual.entre(origem, alvo);
    }

    // ------------------------------------------------------------- carga

    /**
     * Troca a matriz em uso. <b>So o carregador de datapack chama isto.</b>
     *
     * <p>Nao ha estado intermediario: ou a matriz nova entra inteira, ou a
     * antiga continua. Aplicar celula a celula deixaria uma janela em que
     * metade das perguntas responde pelos numeros novos e metade pelos velhos
     * -- e a janela cairia bem no meio de um tick.
     *
     * <p>Isto NAO toca perfil de jogador. A matriz e sobre categorias, nao
     * sobre pessoas; recarregar datapack com gente online nao pode mexer no
     * progresso de ninguem, e a forma de garantir isso e nao ter por onde.
     */
    public static void instalar(MatrizDeAfinidade nova) {
        matriz = Objects.requireNonNull(nova, "matriz");
        JA_AVISOU.set(false);
    }

    /**
     * Esquece a matriz. Existe para o teste poder exercitar o estado "sem
     * dado", que e exatamente o estado que produz zero silencioso se ninguem
     * reclamar.
     */
    public static void limpar() {
        matriz = null;
        JA_AVISOU.set(false);
    }

    /** Se ha matriz carregada. Para diagnostico e para o portao. */
    public static boolean carregada() {
        return matriz != null;
    }

    /** A matriz em uso, para diagnostico. Vazio quando nada foi carregado. */
    public static Optional<MatrizDeAfinidade> matriz() {
        return Optional.ofNullable(matriz);
    }

    private static void avisarUmaVez() {
        if (JA_AVISOU.compareAndSet(false, true)) {
            LOG.error("Nenhuma matriz de afinidade carregada: toda consulta vai"
                    + " responder zero. Confira se"
                    + " data/nenfoundation/nen_afinidade/matriz.json existe e e"
                    + " valido; o carregador registra o motivo exato na recarga.");
        }
    }
}
