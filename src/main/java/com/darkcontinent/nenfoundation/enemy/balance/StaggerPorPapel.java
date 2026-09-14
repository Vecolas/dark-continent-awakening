package com.darkcontinent.nenfoundation.enemy.balance;

import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerRules;
import java.util.Objects;

/**
 * Perfil de interrupcao DERIVADO do papel, em vez de escrito a mao.
 *
 * <p><b>Por que isto existe.</b> Os quatro numeros de {@code StaggerRules} sao
 * faceis de escrever e quase impossiveis de conferir de cabeca. O que decide se o
 * mob cambaleia nao e o limiar: e a relacao entre o ganho de um golpe
 * ({@code dano - resistencia}) e a perda entre dois golpes
 * ({@code decaimento * ticks}). Se a perda empata com o ganho, o acumulado sobe e
 * desce para sempre e a interrupcao <b>nunca acontece</b> -- com todos os testes
 * passando, porque o teste unitario alimenta o acumulador direto e so prova que a
 * maquina funciona.</p>
 *
 * <p>Quatorze dos dezessete perfis do repositorio nasceram assim. Nenhum deu
 * erro, e nenhum ia dar.</p>
 *
 * <p><b>O que muda aqui.</b> Quem escreve um perfil deixa de escolher quatro
 * numeros soltos e passa a declarar as duas coisas que sao decisao de design:
 * <b>quantos acertos seguidos</b> devem interromper aquele bicho, e <b>quanto
 * tempo</b> ele fica aberto depois. Limiar, resistencia e decaimento saem da
 * conta, contra a arma que de fato encontra aquele papel
 * ({@link FaixaDeTempo#loadoutDe}). Um perfil morto deixa de ser possivel de
 * escrever, em vez de ser possivel de escrever e dificil de notar.</p>
 *
 * <p><b>O que isto NAO faz.</b> Nao promete que a interrupcao acontece no ritmo
 * certo, nem que ela e legivel na tela, nem que o numero de acertos e o valor
 * divertido -- so que ela e ALCANCAVEL. A conta tambem ignora a armadura do
 * proprio mob, que em jogo reduz o dano antes de ele virar stagger: o resultado e
 * otimista de proposito, e a consequencia esta declarada em
 * o-que-nao-provamos.md.</p>
 */
public final class StaggerPorPapel {

    /**
     * Quanto da barra o esquecimento come entre dois acertos do ritmo de
     * referencia.
     *
     * <p>Acima de 1.0 o perfil e matematicamente morto. O valor e baixo de
     * proposito: o decaimento existe para punir quem bate uma vez e sai, nao para
     * anular quem mantem a pressao. A margem ate 1.0 e o espaco que sobra para a
     * armadura do mob, que esta conta nao desconta.</p>
     */
    private static final float FRACAO_ESQUECIDA = 0.35F;

    /** Quanto do golpe de referencia a carapaca do papel absorve antes da conta. */
    private static float resistenciaRelativa(ThreatTier papel) {
        return switch (papel) {
            case PASSIVE, LOW -> 0.00F;
            case HUNTER -> 0.10F;
            case DANGEROUS -> 0.20F;
            case ELITE -> 0.25F;
            case SQUADRON -> 0.25F;
            case BOSS, APEX -> 0.30F;
        };
    }

    private StaggerPorPapel() { }

    /**
     * O perfil de quem deve cair em {@code golpesParaInterromper} acertos
     * seguidos.
     *
     * @param papel de qual arma de referencia a conta parte
     * @param golpesParaInterromper acertos SEGUIDOS, sem errar um unico, ate abrir
     * @param ticksDeStagger quanto tempo o mob fica aberto depois de abrir
     */
    public static StaggerRules de(ThreatTier papel, int golpesParaInterromper, int ticksDeStagger) {
        Objects.requireNonNull(papel, "papel ausente");
        if (golpesParaInterromper < 1 || golpesParaInterromper > 12) {
            throw new IllegalArgumentException("golpesParaInterromper fora de faixa ("
                    + golpesParaInterromper + "): abaixo de 1 o mob cambaleia com qualquer"
                    + " encostao, e acima de 12 a exigencia de nao errar NENHUM golpe torna a"
                    + " interrupcao teorica -- que e o mesmo defeito que esta classe existe para"
                    + " impedir, so que escrito de outro jeito.");
        }
        if (ticksDeStagger < 1) {
            throw new IllegalArgumentException("janela de cambaleio nao positiva ("
                    + ticksDeStagger + "): a interrupcao dispararia e fecharia no mesmo tick,"
                    + " sem janela para ninguem aproveitar.");
        }

        LoadoutDeReferencia arma = FaixaDeTempo.loadoutDe(papel);
        float dano = (float) arma.danoPorGolpe();
        float ticksEntreGolpes = (float) (20.0D / arma.golpesPorSegundo());

        float resistencia = arredondar(dano * resistenciaRelativa(papel));
        float ganho = dano - resistencia;
        float perda = ganho * FRACAO_ESQUECIDA;
        float decaimento = perda / ticksEntreGolpes;

        // O primeiro golpe entra inteiro; cada golpe seguinte paga o esquecimento
        // do intervalo antes de somar. Sem este desconto o limiar sairia alto
        // demais e o mob levaria mais acertos do que o perfil promete.
        float limiar = limiarPara(golpesParaInterromper, ganho, decaimento, ticksEntreGolpes);

        return new StaggerRules(limiar, resistencia, decaimento, ticksDeStagger);
    }

    /**
     * O limiar que o numero de golpes prometido realmente alcanca.
     *
     * <p>Duas margens, e as duas ja custaram uma reprovacao. A primeira: o intervalo
     * de referencia e 12,5 ticks e o jogo so tem tick INTEIRO, entao o espacamento
     * real e 13 e o esquecimento cobrado e maior do que a media sugere -- a conta
     * usa o intervalo arredondado PARA CIMA. A segunda: o limiar e arredondado para
     * BAIXO, porque arredondar para cima pode custar um golpe inteiro a mais, e um
     * perfil que promete dois acertos e entrega tres e uma promessa quebrada que
     * nada mais reprova.</p>
     */
    private static float limiarPara(int golpes, float ganho, float decaimento,
            float ticksEntreGolpes) {
        float perdaReal = decaimento * (float) Math.ceil(ticksEntreGolpes);
        float bruto = ganho + (golpes - 1) * (ganho - perdaReal);
        return (float) Math.floor(bruto * 2.0F) / 2.0F;
    }

    /** Meio ponto: o perfil continua legivel para quem for ajustar a mao depois. */
    private static float arredondar(float valor) {
        return Math.round(valor * 2.0F) / 2.0F;
    }

    /**
     * O menor multiplicador de ponto fraco em uso no projeto.
     *
     * <p>Ele existe para que a regua de alcancabilidade consiga medir um perfil de
     * ponto fraco sem conhecer o mob: se o perfil fecha com o MENOR multiplicador,
     * ele fecha com qualquer um. Baixar esta constante afrouxa a regua para todos
     * de uma vez, e por isso ela mora aqui e nao no teste.</p>
     */
    public static final float MULTIPLICADOR_MINIMO_DE_PONTO_FRACO = 3.0F;

    /**
     * O perfil de quem <b>so</b> cambaleia pelo ponto fraco.
     *
     * <p>Aqui a absorcao do golpe comum e a MECANICA, e nao um efeito colateral: a
     * resistencia e posta exatamente no dano da arma de referencia, entao um acerto
     * fora do ponto fraco soma zero e devolve {@code ABSORVIDO}. O Cyclops diz
     * "pelo flanco voce nao existe" e o besouro diz "vire-o, o ventre e a unica
     * coisa que vale acertar" -- as duas frases so sao verdade se bater no lugar
     * errado nao adiantar NADA.</p>
     *
     * <p><b>Por que nao deixar a aritmetica fazer isso sozinha.</b> Antes daqui o
     * efeito era obtido de lado: resistencia baixa, decaimento alto, e o golpe
     * comum morria porque o esquecimento comia mais do que ele somava. Funcionava,
     * e era indistinguivel de um perfil quebrado -- tanto que oito perfis
     * quebrados conviveram com estes dois sem que ninguem separasse os casos. A
     * absorcao declarada e a mesma regra dita em voz alta, e agora um perfil que
     * absorve sem ter ponto fraco reprova.</p>
     *
     * @param multiplicador quanto o ponto fraco multiplica o golpe
     */
    public static StaggerRules porPontoFraco(ThreatTier papel, float multiplicador,
            int golpesParaInterromper, int ticksDeStagger) {
        Objects.requireNonNull(papel, "papel ausente");
        if (!Float.isFinite(multiplicador) || multiplicador < MULTIPLICADOR_MINIMO_DE_PONTO_FRACO) {
            throw new IllegalArgumentException("multiplicador de ponto fraco abaixo do minimo ("
                    + multiplicador + " < " + MULTIPLICADOR_MINIMO_DE_PONTO_FRACO + "): com a"
                    + " resistencia posta no dano inteiro da arma, um multiplicador pequeno deixa"
                    + " sobrar quase nada por acerto e o ponto fraco vira decorativo -- o mob"
                    + " ficaria sem NENHUM caminho de interrupcao, que e o defeito que esta"
                    + " classe existe para impedir.");
        }
        if (golpesParaInterromper < 1 || golpesParaInterromper > 12) {
            throw new IllegalArgumentException("golpesParaInterromper fora de faixa ("
                    + golpesParaInterromper + ")");
        }
        if (ticksDeStagger < 1) {
            throw new IllegalArgumentException("janela de cambaleio nao positiva");
        }

        LoadoutDeReferencia arma = FaixaDeTempo.loadoutDe(papel);
        float dano = (float) arma.danoPorGolpe();
        float ticksEntreGolpes = (float) (20.0D / arma.golpesPorSegundo());

        // A resistencia E o dano comum: o acerto fora do ponto fraco soma zero.
        float resistencia = dano;
        float ganho = dano * multiplicador - resistencia;
        float perda = ganho * FRACAO_ESQUECIDA;
        float decaimento = perda / ticksEntreGolpes;
        float limiar = limiarPara(golpesParaInterromper, ganho, decaimento, ticksEntreGolpes);

        return new StaggerRules(limiar, resistencia, decaimento, ticksDeStagger);
    }
}
