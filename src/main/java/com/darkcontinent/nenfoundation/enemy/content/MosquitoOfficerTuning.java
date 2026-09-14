package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDaPicada;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeDreno;

/**
 * Os numeros do Mosquito Officer que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo e hostil a merge: ele guarda os
 * perfis das nove formigas quimera, e neste momento varias frentes escrevem o
 * comportamento de formigas diferentes ao mesmo tempo. Todas elas precisariam
 * acrescentar metodos no MESMO arquivo, e o resultado de um merge assim nao e um
 * conflito barulhento -- e uma resolucao apressada em que o metodo de alguem
 * some. Metodo que some nao da erro de compilacao quando o chamador some junto:
 * da um mob que perdeu o dreno e continua nascendo, picando e passando em todo
 * portao.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada (atributos, spawn, faccao), a recarga e o stagger -- tudo que
 * ja estava escrito e que outros sistemas leem. Aqui ficam os numeros NOVOS, os
 * que este comportamento inaugurou: a forma do telegrafo, a geometria da picada,
 * a fracao e o teto do dreno, e o angulo do flanco.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code ChimeraProfiles.mosquitoOfficer().attributes().attackDamage()}, porque a
 * picada e o unico ataque do bicho. Repetir o 8 aqui criaria duas fontes para a
 * mesma verdade, e girar o atributo numa sessao de balanceamento mudaria a barra
 * de vida do jogador sem mudar este arquivo -- e sem mudar o dreno, que e uma
 * FRACAO desse dano.</p>
 */
public final class MosquitoOfficerTuning {

    private MosquitoOfficerTuning() { }

    // ----------------------------------------------------------------- dreno
    //
    // O DRENO E A FICHA INTEIRA. Os tres numeros abaixo se leem JUNTOS, e e por
    // isso que eles viram um RegrasDeDreno: e ele que impede a cura minima de
    // engolir o teto, e e ele que guarda a ordem em que as recusas acontecem.

    /**
     * Quanto do dano APLICADO volta como vida.
     *
     * <p>Metade. Com o dano 8 do perfil, cada picada devolve 4 -- um quinze avos
     * da vida dela por acerto, e ela acerta a cada 76 ticks (36 de golpe mais 40
     * de recarga). Subir para 1.0 nao quebraria nada e nao reprovaria portao
     * nenhum: faria cada picada valer 8, e o teto abaixo seria gasto em tres
     * acertos em vez de seis -- o que troca "ela se sustenta durante a briga" por
     * "ela enche a vida e o teto vira enfeite".</p>
     */
    public static final float FRACAO_DRENADA = 0.5F;

    /**
     * Teto de vida que UM encontro pode devolver.
     *
     * <p><b>E o numero que separa "tensa" de "impossivel", e por isso ele nao e
     * um detalhe de balanceamento -- e a mecanica.</b> Vinte e dois e 40% dos 55
     * de vida dela: o jogador que erra a leitura por cinco picadas ve a barra
     * voltar quase a metade, e entao ela PARA de voltar. Sem teto, um combate
     * longo a leva a vida praticamente infinita e o encontro deixa de ter fim,
     * sem uma linha de log -- um mob de 55 de vida que nao morre.</p>
     *
     * <p>O teto e do ENCONTRO e nao da vida: ele zera quando ela perde o alvo,
     * porque o que ele limita e uma luta, e nao uma carreira.</p>
     */
    public static final float TETO_DE_DRENO_POR_ENCONTRO = 22.0F;

    /**
     * Piso abaixo do qual a picada nao devolve nada.
     *
     * <p>Meio ponto de vida. Nao e botao de balanceamento: e o que impede um
     * respingo de dano -- resistencia alta, armadura pesada, dano residual -- de
     * gastar teto sem mover um pixel da barra. Sem ele, o teto acabaria sem que
     * nada visivel tivesse acontecido.</p>
     */
    public static final float CURA_MINIMA_DO_DRENO = 0.5F;

    // ------------------------------------------------------------- telegrafo
    //
    // A FORMA e a ficha: aviso curto, janela curtissima, recuperacao longa. Este
    // oficial e RAPIDO (velocidade 0.4, a maior da familia) e FRACO (armadura 2),
    // e o telegrafo tem de refletir os dois: ele avisa menos que um gigante,
    // porque avisar muito num bicho veloz o tornaria inofensivo, e ele se expoe
    // mais depois, porque e na recuperacao que a armadura 2 e cobrada.

    /** Ticks de aviso: sete decimos de segundo com a agulha avancando. */
    public static final int WINDUP_DA_PICADA = 14;
    /** Ticks em que a picada existe. Quatro: ela fura, nao varre. */
    public static final int JANELA_DA_PICADA = 4;
    /** Ticks de recuperacao: a janela em que o jogador pune, e a mais longa das tres. */
    public static final int RECUPERACAO_DA_PICADA = 18;

    /**
     * Empurrao da picada: ZERO, e o zero e a decisao.
     *
     * <p>Uma agulha nao arremessa ninguem, mas o motivo mecanico e outro e e mais
     * forte: empurrar a vitima a tiraria do alcance de 0.85 blocos da propria
     * agulha, e o oficial teria de refazer a aproximacao inteira depois de cada
     * acerto. O dreno em cadeia -- picar, curar, picar de novo -- deixaria de
     * existir, e a ficha do bicho viraria um mob rapido que bate uma vez e some.
     * Isso nao daria erro nenhum: daria um encontro sem a pressao que ele existe
     * para criar.</p>
     */
    public static final float EMPURRAO_DA_PICADA = 0.0F;

    /**
     * Quantos ticks do aviso ela ainda MIRA antes de travar a direcao.
     *
     * <p>Seis dos catorze. Depois disso a picada esta comprometida e a direcao
     * nao muda mais. Mirar o aviso inteiro transformaria a agulha em mira-laser
     * num bicho que ja e o mais rapido da familia: o desvio deixaria de existir e
     * a unica defesa restante seria matar antes. Nao mirar nada faria o oposto,
     * um oficial que erra sozinho. Nenhum dos dois da erro.</p>
     */
    public static final int TICKS_DE_MIRA_NO_WINDUP = 6;

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
     * Distancia (centro a centro) em que ela decide picar.
     *
     * <p>Ela tem de ser MENOR que {@code maxZ + MEIA_LARGURA_DE_UM_ALVO}, e o
     * teste cobra isso. Maior, o oficial comeca um aviso de catorze ticks contra
     * alguem que ja esta fora do alcance da agulha, e a picada NUNCA acertaria --
     * um chefe que erra sozinho e parece quebrado, sem nenhum erro no log.</p>
     *
     * <p>A folga e apertadissima de proposito (0.85 decide, 0.9 ainda alcanca) e
     * ela e a ficha do bicho: esta agulha tem 0.625 blocos, contra os 3.0 do
     * porrete do Cyclops. Ela precisa ENCOSTAR. E isso que torna cada picada uma
     * aproximacao completa, e e por isso que o dreno pode ser generoso.</p>
     */
    public static final double ALCANCE_DA_PICADA = 0.85D;

    /**
     * Recarga imposta a quem interrompeu a picada.
     *
     * <p>Mais longa que a recarga normal (40): interromper um bicho que se cura
     * com o que tira tem de VALER. Sem isso, {@code reset()} devolveria a fase
     * para IDLE e ela poderia recomecar no tick seguinte -- e o jogador
     * aprenderia a nao interromper, que e o oposto do que uma armadura 2 existe
     * para ensinar.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 60;

    // ---------------------------------------------------------------- flanco

    /**
     * Meia-abertura, em graus, do arco em que o alvo a ENXERGA.
     *
     * <p>Sessenta e cinco graus para cada lado do olhar do jogador. Dentro desse
     * arco ela recusa a picada e volta para o anel de espera; fora dele, ataca. O
     * numero e grande de proposito -- quase a metade do circulo e area proibida
     * -- porque a defesa que este bicho ensina e simples e tem de ser confiavel:
     * mantenha o zumbido na tela. Estreita-lo para trinta nao daria erro nenhum:
     * daria um oficial que ataca de quase toda parte, e o jogador nunca
     * descobriria que havia uma regra para aprender.</p>
     */
    public static final double ARCO_FRONTAL_DO_ALVO_EM_GRAUS = 65.0D;

    /**
     * Raio do anel onde ela circula enquanto o alvo a encara.
     *
     * <p>Tem de ser MAIOR que {@link #ALCANCE_DA_PICADA}, e o construtor de
     * {@link RegrasDaPicada} cobra isso: um anel dentro do alcance deixaria o
     * oficial parado exatamente onde ele ja podia picar, oscilando entre recuar e
     * atacar no mesmo ponto -- o que o jogador le como bicho travado.</p>
     *
     * <p>Quatro blocos e meio tambem e a distancia em que o zumbido ainda se ouve
     * e a silhueta ainda se le: o anel e o lugar onde o jogador DEVE conseguir
     * acompanha-la com a camera.</p>
     */
    public static final double RAIO_DO_CONTORNO = 4.5D;

    /**
     * Altura, em blocos acima do alvo, em que ela paira ao se reposicionar.
     *
     * <p>Limite de LEITURA, e nao botao de balanceamento: acima disso ela sai do
     * campo de visao de quem olha para a frente, e a defesa do encontro --
     * mante-la na tela -- passaria a exigir olhar para cima o tempo todo. Abaixo,
     * ela se confunde com um mob terrestre e a silhueta de asa deixa de ser lida
     * contra o ceu.</p>
     */
    public static final double ALTURA_DE_ESPERA = 1.6D;

    // ------------------------------------------------------------- percepcao

    /** Ticks de memoria de alvo; e tambem o prazo do {@code ThreatMemory}. */
    public static final int MEMORIA_DE_ALVO_TICKS = 140;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    public static final int TICKS_DE_AVISO = 20;
    /** Meia-abertura do campo de visao dela, em graus. */
    public static final double ABERTURA_DA_VISAO = 80.0D;
    /** Alcance de audicao em blocos, para som de intensidade 1. */
    public static final double ALCANCE_DE_AUDICAO = 16.0D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // -------------------------------------------------------------- montagem

    /** Os tres numeros do dreno, com a continencia cobrada no construtor. */
    public static RegrasDeDreno regrasDeDreno() {
        return new RegrasDeDreno(FRACAO_DRENADA, TETO_DE_DRENO_POR_ENCONTRO,
                CURA_MINIMA_DO_DRENO);
    }

    /**
     * As regras do flanco, com o cosseno DERIVADO do angulo.
     *
     * <p>Derivado, e nao escrito de novo: um cosseno literal aqui seria a segunda
     * fonte para a mesma abertura, e no dia em que alguem estreitasse o angulo
     * sem mexer neste metodo o oficial passaria a atacar num arco diferente
     * daquele que a documentacao promete -- sem erro nenhum.</p>
     */
    public static RegrasDaPicada regrasDaPicada() {
        return new RegrasDaPicada(Math.cos(Math.toRadians(ARCO_FRONTAL_DO_ALVO_EM_GRAUS)),
                RAIO_DO_CONTORNO, ALCANCE_DA_PICADA);
    }

    /**
     * A picada.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que a agulha desce, ela
     * desce. Interromper durante os 4 ticks ativos faria o oficial cancelar um
     * golpe que o jogador ja viu sair, e a leitura -- que e a unica coisa que um
     * ataque telegrafado entrega -- deixaria de valer. A recuperacao TAMBEM e
     * interrompivel, e essa e a janela de punicao dela.</p>
     */
    public static AttackDefinition picada() {
        return new AttackDefinition("picada", WINDUP_DA_PICADA, JANELA_DA_PICADA,
                RECUPERACAO_DA_PICADA,
                ChimeraProfiles.mosquitoOfficer().attributes().attackDamage(),
                EMPURRAO_DA_PICADA, true, false, true);
    }

    /**
     * Caixa da picada, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente.</p>
     *
     * <p><b>Ela e ESTREITA, e a estreiteza e a ficha.</b> Setenta centimetros de
     * arco, contra os quatro blocos do porrete do Cyclops. Uma tora varre; uma
     * agulha fura. Alargar este arco nao daria erro nenhum: daria um oficial que
     * acerta sem precisar mirar, e o unico preco que ele paga pela cura que
     * recebe -- ter de encostar exatamente -- desapareceria.</p>
     *
     * <p>O {@code maxZ} de 0.6 nao e um numero solto: a probocide DESENHADA
     * alcanca 0.625 blocos a partir do eixo, e {@code mosquito_officer_geo.py}
     * reprova se a caixa passar do desenho. A faixa de altura (0.2 a 1.3) e
     * cobrada pelo mesmo gerador contra a altura em que a agulha esta desenhada:
     * uma caixa que nao cobrisse a agulha daria um bicho que encosta e nao acerta
     * nada.</p>
     */
    public static AttackHitbox caixaDaPicada() {
        return new AttackHitbox(-0.35D, 0.2D, 0.0D, 0.35D, 1.3D, 0.6D);
    }
}
