package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.SingleEyeRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;
import java.util.Map;

/**
 * Os numeros do Cyclops que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link GreedIslandProfiles}.</b> Aquele arquivo e hostil a merge: ele guarda
 * os perfis das sete criaturas da ilha, e neste momento varias frentes escrevem
 * o comportamento de criaturas diferentes ao mesmo tempo. Todas elas precisariam
 * acrescentar metodos no MESMO arquivo, e o resultado de um merge assim nao e um
 * conflito barulhento -- e uma resolucao apressada em que o metodo de alguem
 * some. Metodo que some nao da erro de compilacao quando o chamador some junto:
 * da um mob que perdeu o ponto fraco e continua nascendo, atacando e passando em
 * todo portao.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code GreedIslandProfiles} ficam
 * a ficha publicada (atributos, spawn, card, condicao de captura), a recarga e o
 * stagger -- tudo que ja estava escrito e que outros sistemas leem. Aqui ficam
 * os numeros NOVOS, os que este comportamento inaugurou: a forma do telegrafo, a
 * geometria do olho, o alcance do porrete.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code GreedIslandProfiles.cyclops().attributes().attackDamage()}, porque o
 * porrete e o unico ataque do bicho. Repetir o 14 aqui criaria duas fontes para
 * a mesma verdade, e girar o atributo numa sessao de balanceamento mudaria a
 * barra de vida do jogador sem mudar este arquivo.</p>
 */
public final class CyclopsTuning {

    private CyclopsTuning() { }

    // ------------------------------------------------------------------ olho
    //
    // O OLHO E A FICHA INTEIRA. As tres constantes abaixo se leem JUNTAS, e e por
    // isso que elas viram um SingleEyeRules: e ele que impede o arco do olho de
    // ficar mais largo que o arco da visao.

    /**
     * Meia-abertura do campo de visao, em graus. METADE de um mob comum.
     *
     * <p>O boneco de treino usa 75 e a maioria da fauna fica perto disso. Sessenta
     * graus deixam um flanco cego de verdade: o jogador que circula sai do cone
     * antes de chegar as costas, e e essa margem que faz "circular" ser uma
     * tatica e nao um truque de esquina. Subir este numero para 75 nao quebraria
     * nada, nao reprovaria portao nenhum, e apagaria o mob.</p>
     */
    public static final double MEIA_ABERTURA_DA_VISAO_EM_GRAUS = 60.0D;

    /**
     * Meia-abertura em que o olho fica exposto, em graus.
     *
     * <p>Mais estreita que o campo de visao de proposito: o critico nao pode ser
     * a recompensa de simplesmente estar visivel. Trinta e cinco graus obrigam o
     * jogador a encarar o bicho -- o lugar mais perigoso do encontro -- para
     * cobrar o multiplicador, e e essa troca que o mob ensina.</p>
     */
    public static final double MEIA_ABERTURA_DO_OLHO_EM_GRAUS = 35.0D;

    /**
     * Altura relativa a partir da qual o impacto conta como olho.
     *
     * <p>0.84 de 4.2 blocos e 3.53 blocos do chao. O olho DESENHADO comeca em
     * 3.62 (y=58 px de 67.2) -- o limiar fica de proposito um pouco ABAIXO dele,
     * e o gerador de geometria cobra exatamente essa folga. A razao e que o
     * servidor mede onde o traco ENTRA na caixa de colisao, e nao o pixel que o
     * jogador mirou: exigir o pixel exato faria o critico quase nunca pagar, e um
     * ponto fraco que nunca paga e pior do que nenhum, porque o desenho continua
     * prometendo.</p>
     */
    public static final double ALTURA_MINIMA_DO_OLHO = 0.84D;

    /** Id da regiao vulneravel; o mesmo nome aparece no registro e no resolver. */
    public static final String REGIAO_DO_OLHO = "eye";
    /** Id da regiao comum -- tudo que nao e o olho. */
    public static final String REGIAO_COMUM = "body";

    /**
     * Multiplicador do olho.
     *
     * <p>Alto porque o alvo e pequeno, alto e so acessivel de frente. A conta que
     * importa e a do stagger, nao a da barra de vida: com
     * {@code cyclopsStagger()} (limiar 26, resistencia 3), um golpe comum de 7
     * entra com 4 e precisaria de sete acertos; o mesmo golpe no olho entra com
     * 18 e cambaleia o gigante em DOIS. E essa diferenca -- e nao o dano -- que
     * ensina o jogador a mirar.</p>
     */
    public static final float MULTIPLICADOR_DO_OLHO = 3.0F;

    // ------------------------------------------------------------- telegrafo
    //
    // A FORMA e a ficha: aviso longo, janela curta, recuperacao longa. Um gigante
    // cujo windup seja mais curto que a janela deixa de ser telegrafado e vira um
    // mob que bate sem aviso -- com o mesmo dano, o mesmo cooldown e o mesmo log
    // limpo. CyclopsTuningTest reprova essa inversao.

    /** Ticks de aviso: um segundo e meio de porrete subindo. */
    public static final int WINDUP_DO_PORRETE = 30;
    /** Ticks em que o golpe existe. Curto: o desvio precisa ser possivel. */
    public static final int JANELA_DO_PORRETE = 5;
    /** Ticks de recuperacao: a janela em que o jogador pune. */
    public static final int RECUPERACAO_DO_PORRETE = 25;
    /** Empurrao do porrete -- e uma tora, e ela joga longe. */
    public static final float EMPURRAO_DO_PORRETE = 1.1F;

    /**
     * Quantos ticks do aviso ele ainda MIRA antes de travar a direcao.
     *
     * <p>Doze dos trinta. Depois disso o golpe esta comprometido e a direcao nao
     * muda mais. Mirar o aviso inteiro transformaria o porrete em mira-laser: o
     * gigante giraria junto com quem desvia, o desvio deixaria de existir e a
     * unica defesa restante seria correr para fora do alcance -- com o telegrafo
     * de um segundo e meio virando decoracao. Nao mirar nada faria o contrario,
     * um chefe que erra sozinho. Nenhum dos dois da erro.</p>
     */
    public static final int TICKS_DE_MIRA_NO_WINDUP = 12;

    // --------------------------------------------------------------- alcance

    /**
     * Meia-largura do alvo tipico -- um jogador tem 0.6 de lado.
     *
     * <p>Nao e botao de balanceamento: e a diferenca entre as duas reguas que
     * este arquivo usa. A distancia de DECISAO e medida de centro a centro
     * ({@code distanceTo}); a caixa de golpe e testada contra a BORDA do alvo
     * ({@code AABB.intersects}). Sem esta conversao as duas parecem comparaveis e
     * nao sao.</p>
     */
    public static final double MEIA_LARGURA_DE_UM_ALVO = 0.3D;

    /**
     * Distancia (centro a centro) em que ele decide golpear.
     *
     * <p>Ela tem de ser MENOR que {@code maxZ + MEIA_LARGURA_DE_UM_ALVO}, e o
     * teste cobra isso. Maior, o gigante comeca um windup de um segundo e meio
     * contra alguem que ja esta fora do alcance da tora -- e como ele trava a
     * navegacao durante o golpe, o ataque no limite da distancia NUNCA acertaria.
     * Isso nao da erro nenhum: da um chefe que erra sozinho e parece quebrado.</p>
     *
     * <p>A folga que sobra (3.1 decide, 3.3 ainda alcanca) e pequena de proposito.
     * Ela e o que faz o desvio depender de sair de perto, e nao de um passo
     * lateral de meio bloco.</p>
     */
    public static final double ALCANCE_DO_GOLPE = 3.1D;

    /**
     * Recarga imposta a quem interrompeu o golpe.
     *
     * <p>Mais longa que a recarga normal (50): interromper um gigante no meio do
     * aviso tem de VALER. Sem isso, {@code reset()} devolveria a fase para IDLE e
     * ele poderia recomecar no tick seguinte -- e o jogador aprenderia a nao
     * interromper, que e o oposto do que o ponto fraco existe para ensinar.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 70;

    /**
     * Alcance do traco que procura o ponto de impacto na caixa do bicho.
     *
     * <p>Oito blocos, contra os seis do boneco de treino: a caixa deste aqui tem
     * 4.2 blocos de altura e o jogador mira a cabeca de baixo para cima, entao o
     * traco entra na caixa muito mais longe do olho do atacante. Curto demais, o
     * traco nao alcanca a caixa, o codigo cai na posicao de reserva e TODO acerto
     * na cabeca viraria corpo comum -- sem erro, e com o ponto fraco simplesmente
     * nao funcionando.</p>
     */
    public static final double ALCANCE_DO_TRACO = 8.0D;

    // ------------------------------------------------------------- percepcao

    /** Ticks de memoria de alvo; e tambem o prazo do {@code ThreatMemory}. */
    public static final int MEMORIA_DE_ALVO_TICKS = 120;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    public static final int TICKS_DE_AVISO = 20;
    /** Alcance de audicao em blocos, para som de intensidade 1. */
    public static final double ALCANCE_DE_AUDICAO = 14.0D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // ------------------------------------------------------------- montagem

    /** O par de arcos do olho unico, com a continencia cobrada no construtor. */
    public static SingleEyeRules olhoUnico() {
        return SingleEyeRules.deGraus(MEIA_ABERTURA_DA_VISAO_EM_GRAUS,
                MEIA_ABERTURA_DO_OLHO_EM_GRAUS, ALTURA_MINIMA_DO_OLHO);
    }

    /**
     * O cone de visao, DERIVADO das regras do olho.
     *
     * <p>Derivado, e nao escrito de novo: um {@code VisionCone.deGraus(alcance,
     * 60)} solto aqui seria a segunda fonte para a mesma abertura, e no dia em
     * que alguem estreitasse o olho sem mexer neste metodo o bicho passaria a
     * enxergar mais longe do que o proprio olho enxerga.</p>
     *
     * @param alcance normalmente {@code attributes().followRange()}
     */
    public static VisionCone coneDeVisao(double alcance) {
        return new VisionCone(alcance, olhoUnico().cossenoDoCampoDeVisao());
    }

    /**
     * A geometria do ponto fraco, DERIVADA das mesmas regras.
     *
     * <p>Os dois limiares saem de {@link #olhoUnico()} para que o resolver e o
     * cone nunca possam discordar. Escritos a mao aqui, eles discordariam na
     * primeira vez que alguem ajustasse um so -- e o sintoma seria um critico
     * pago fora do campo de visao, que e dano de graca.</p>
     */
    public static WeakPointResolver olho() {
        SingleEyeRules regras = olhoUnico();
        return new WeakPointResolver(REGIAO_DO_OLHO, REGIAO_COMUM,
                regras.alturaMinimaDoOlho(), regras.cossenoDoOlho());
    }

    /** O catalogo: so o olho vale multiplicador; o resto do gigante e corpo comum. */
    public static WeakPointRegistry pontosFracos() {
        return new WeakPointRegistry(Map.of(REGIAO_DO_OLHO,
                new WeakPoint(REGIAO_DO_OLHO, "head", MULTIPLICADOR_DO_OLHO, true)));
    }

    /**
     * O golpe de porrete.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que a tora desce, ela
     * desce. Interromper durante os 5 ticks ativos faria o gigante cancelar um
     * golpe que o jogador ja viu sair, e a leitura -- que e a unica coisa que um
     * ataque telegrafado entrega -- deixaria de valer.</p>
     */
    public static AttackDefinition porrete() {
        return new AttackDefinition("club", WINDUP_DO_PORRETE, JANELA_DO_PORRETE,
                RECUPERACAO_DO_PORRETE,
                GreedIslandProfiles.cyclops().attributes().attackDamage(),
                EMPURRAO_DO_PORRETE, true, false, true);
    }

    /**
     * Caixa do golpe, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente -- quem estivesse
     * pelas costas e que apanhava. Fase certa, cooldown certo, log limpo. Ver
     * {@code DummyEnemyEntity.CAIXA_DO_GOLPE}.</p>
     *
     * <p>O {@code maxZ} de 3.0 nao e um numero solto: o porrete DESENHADO alcanca
     * 3.19 blocos a partir do ombro, e {@code cyclops_geo.py} reprova se a caixa
     * passar do desenho. Caixa maior que a arma da um jogador que apanha de um
     * porrete que, na tela, parou antes dele.</p>
     *
     * <p>O arco e largo em X (4 blocos) porque uma tora nao acerta um ponto: ela
     * varre. E {@code minZ} de 0.6 apenas exclui o proprio corpo do gigante --
     * ninguem consegue ficar a menos de 0.6 do centro dele, porque a hitbox tem
     * 0.9 de meia-largura.</p>
     */
    public static AttackHitbox caixaDoPorrete() {
        return new AttackHitbox(-2.0D, 0.0D, 0.6D, 2.0D, 3.2D, 3.0D);
    }
}
