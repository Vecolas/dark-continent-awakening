package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeEstouro;

/**
 * Os numeros do Hyper Puffball que nao existiam quando o perfil dele nasceu.
 *
 * <p><b>Por que um arquivo proprio, e nao mais constantes em
 * {@code GreedIslandProfiles}.</b> Aquele arquivo e a fila unica das sete
 * criaturas da ilha: toda frente que trabalha num bicho de Greed Island precisa
 * toca-lo, e varias frentes trabalham ao mesmo tempo. Um arquivo que todo mundo
 * edita no mesmo bloco de linhas e hostil a merge -- e o conflito de merge e o
 * caso BOM. O caso ruim e o merge que resolve limpo e leva junto o numero da
 * outra pessoa, porque ninguem le duzentas linhas de diff de constantes. Aqui o
 * arquivo tem um dono so, e um conflito aqui e sempre uma disputa real.</p>
 *
 * <p><b>Por que estes numeros nao ficam na entidade.</b> Limiar de vida,
 * distancia de gatilho, raio e dano sao exatamente o que alguem gira numa sessao
 * de balanceamento. Deixados no consumidor, eles viram o erro 7 da lista do
 * CLAUDE.md ao contrario: quem for balancear procura por config, nao acha, e
 * conclui que o bicho nao tem botao nenhum.</p>
 *
 * <p><b>O que NAO esta aqui, de proposito:</b> o limiar de caminhada da animacao
 * e o tempo de morte do vanilla. Aquele e ponto de troca entre dois clipes, este
 * e do jogo -- nenhum dos dois e balanceamento, e os dois moram junto do
 * comentario que os explica.</p>
 */
public final class HyperPuffballTuning {

    private HyperPuffballTuning() { }

    /**
     * Fracao da vida maxima em que ou abaixo da qual a casca cede.
     *
     * <p>0.35 de 18 de vida sao 6.3: duas pancadas de espada de ferro ainda
     * deixam o fungo inteiro, a terceira o estoura. Isso e proposital -- o bicho
     * tem de sobreviver ao primeiro golpe para que o jogador chegue a aprender
     * alguma coisa com o segundo.</p>
     */
    public static final float FRACAO_DE_VIDA_DO_GATILHO = 0.35F;

    /**
     * Distancia maxima do ATACANTE para o dano contar como toque.
     *
     * <p>3.5 blocos ficam logo acima do alcance de ataque de um jogador (3.0) e
     * muito abaixo de qualquer tiro util. E esta folga que faz a licao do bicho
     * existir: quem mata de longe nunca ve o estouro.</p>
     */
    public static final double DISTANCIA_DO_GATILHO = 3.5D;

    /**
     * Distancia em que o fungo comeca a estufar.
     *
     * <p>Maior que {@link #DISTANCIA_DO_GATILHO} por obrigacao -- o construtor de
     * {@link RegrasDeEstouro} reprova o contrario. Os 2 blocos de sobra sao o
     * tempo de leitura: o jogador ve a bola inchar antes de estar perto o
     * bastante para armar o estouro.</p>
     */
    public static final double DISTANCIA_DO_AVISO = 5.5D;

    /** Alcance do dano em area, do centro da entidade. Pequeno: recuar e a resposta. */
    public static final double RAIO_DO_ESTOURO = 2.5D;

    /**
     * Dano do estouro.
     *
     * <p><b>Ele NAO sai do ATTACK_DAMAGE do perfil, que e zero.</b> Zero e a ficha
     * correta: o fungo nunca desfere golpe, e nao existe goal de corpo a corpo
     * nele. Ler o atributo aqui daria um estouro de dano zero -- e isso nao
     * levanta erro nenhum: da um bicho cuja unica ameaca nao faz nada, com
     * particula, animacao e morte funcionando perfeitamente.</p>
     *
     * <p>6.0 sao tres coracoes em quem esta sem armadura: o suficiente para doer
     * e ser lembrado, longe de matar quem chegou com a vida cheia.</p>
     */
    public static final float DANO_DO_ESTOURO = 6.0F;

    /** Ticks de telegrafo do estufo -- copiados em hyper_puffball_animacoes.py. */
    public static final int AVISO_WINDUP_TICKS = 12;
    /** Ticks em que o estufo esta aberto. Ele NAO consulta caixa de golpe. */
    public static final int AVISO_ACTIVE_TICKS = 4;
    /** Ticks de volta ao repouso: a janela em que encostar ainda e seguro. */
    public static final int AVISO_RECOVERY_TICKS = 10;

    /**
     * Recarga imposta a quem foi interrompido no meio do estufo.
     *
     * <p>Sem ela, {@code reset()} devolveria a fase para IDLE e o fungo poderia
     * recomecar o aviso no mesmo tick em que cambaleou -- um bicho que reage mais
     * depressa por ter apanhado.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO_TICKS = 30;

    /**
     * As regras do estouro, montadas a partir das constantes acima.
     *
     * <p>Um metodo, e nao um campo estatico ja construido: o record e imutavel,
     * mas um campo publico convidaria alguem a guardar a referencia num consumidor
     * e a ler dela para sempre. Chamar aqui todo uso mantem UMA fonte.</p>
     */
    public static RegrasDeEstouro regrasDeEstouro() {
        return new RegrasDeEstouro(FRACAO_DE_VIDA_DO_GATILHO, DISTANCIA_DO_GATILHO,
                DISTANCIA_DO_AVISO, RAIO_DO_ESTOURO, DANO_DO_ESTOURO);
    }

    /**
     * O estufo de aviso: um ataque de dano ZERO.
     *
     * <p>O zero e declarado aqui e nao herdado por acidente. Ele e a segunda
     * tranca: a primeira e a entidade nunca chamar {@code tryHit}, e se um dia
     * alguem chamar, o golpe continua nao machucando ninguem.</p>
     *
     * <p>As tres janelas sao interrompiveis: bater no fungo enquanto ele estufa
     * tem de cortar o estufo, senao o jogador ve o bicho continuar o telegrafo
     * como se o golpe nao tivesse chegado.</p>
     */
    public static AttackDefinition aviso() {
        return new AttackDefinition("hyper_puffball_aviso", AVISO_WINDUP_TICKS,
                AVISO_ACTIVE_TICKS, AVISO_RECOVERY_TICKS, 0.0F, 0.0F, true, true, true);
    }
}
