package com.darkcontinent.nenfoundation.enemy.perception;

import java.util.Optional;
import java.util.UUID;

/**
 * Regras do relatorio do Radio Rat, sem mundo e sem entidade.
 *
 * <p><b>A decisao que este record carrega:</b> o rato nao entrega um ALVO, ele
 * entrega um SOM. A diferenca decide o bicho inteiro. Entregar o alvo seria
 * escrever na memoria do vizinho por fora -- uma segunda autoridade sobre a
 * escolha de alvo, que discordaria do {@code TargetEvaluator} em silencio e
 * produziria mobs perseguindo quem a faccao deles nem considera inimigo. Como
 * som, o aviso passa pelo {@link PerceptionController#ouvir} de quem recebe, e
 * portanto pelo alcance de audicao DELE: um vizinho longe demais do jogador
 * ouve o grito e nao aprende nada, que e a resposta certa.</p>
 *
 * <p><b>Por que as tres condicoes moram juntas.</b> "Tem alvo", "nao esta
 * cambaleando" e "o grito ja pode comecar" espalhadas por tres {@code if} da
 * Goal viram tres divergencias possiveis, e nenhuma delas da erro: dao um rato
 * que grita cambaleando (e a interrupcao deixa de valer alguma coisa), ou que
 * grita de graca toda vez que o alvo pisca na memoria.</p>
 *
 * <p><b>E por que existe {@code ticksDeObservacao}.</b> Sem ele o grito sai no
 * mesmo tick em que o rato percebe alguem, e a resposta que este bicho ensina --
 * matar o mensageiro antes do relatorio -- deixa de existir como janela. O
 * telegrafo do ataque ja da uma janela; esta da a primeira, a que separa "ele me
 * viu" de "ele contou". Zero aqui nao da erro: da um encounter que nao tem
 * resposta, e ninguem consegue dizer por que.</p>
 *
 * <p>Os numeros sao injetados por {@code RadioRatTuning}; este record nao conhece
 * nenhum deles.</p>
 *
 * @param ticksDeObservacao ticks com alvo antes de o primeiro grito poder comecar
 * @param raioDoRelatorio ate onde do RATO um vizinho e alcancado pelo grito
 * @param intensidade 0..1 do {@link HearingEvent}; e o que decide, do lado de
 *        quem ouve, quao longe do alvo ainda da para aproveitar o aviso
 */
public record RadioRatReportRules(int ticksDeObservacao, double raioDoRelatorio,
        double intensidade) {

    public RadioRatReportRules {
        if (ticksDeObservacao < 1) {
            throw new IllegalArgumentException("ticksDeObservacao (" + ticksDeObservacao + ") tem"
                    + " de ser pelo menos 1: com zero o rato grita no mesmo tick em que ve alguem,"
                    + " e matar o mensageiro antes do relatorio deixa de ser possivel -- sem erro"
                    + " nenhum, so um encounter sem resposta");
        }
        if (!Double.isFinite(raioDoRelatorio) || raioDoRelatorio <= 0.0D) {
            throw new IllegalArgumentException("raio de relatorio invalido: " + raioDoRelatorio);
        }
        if (!Double.isFinite(intensidade) || intensidade <= 0.0D || intensidade > 1.0D) {
            throw new IllegalArgumentException("intensidade (" + intensidade + ") fora de (0, 1]:"
                    + " o HearingEvent recusaria o valor em runtime, no meio de um tick de"
                    + " servidor, e nao aqui onde o numero foi escrito");
        }
    }

    /**
     * O grito pode COMECAR agora.
     *
     * <p>Note o que nao esta aqui: linha de visao. O rato denuncia o que ele
     * lembra, nao so o que ele ve -- e por isso que se esconder atras de uma
     * pedra depois de ser visto nao apaga o relatorio. Exigir visao aqui daria um
     * bicho que perde o assunto toda vez que o alvo passa atras de um tronco.</p>
     *
     * @param temAlvo o servidor tem alvo vivo resolvido a partir da memoria
     * @param cambaleando o rato esta interrompido; interromper tem de CORTAR o aviso
     * @param gritoLiberado o {@code AttackController} autoriza comecar (recarga zerada)
     * @param ticksComAlvo ha quantos ticks seguidos existe alvo
     */
    public boolean denuncia(boolean temAlvo, boolean cambaleando, boolean gritoLiberado,
            int ticksComAlvo) {
        return temAlvo && !cambaleando && gritoLiberado && ticksComAlvo >= ticksDeObservacao;
    }

    /**
     * O vizinho esta dentro do raio do grito.
     *
     * <p>Distancia nao finita ou negativa nao alcanca ninguem: um NaN vindo de
     * uma entidade em estado estranho nao pode virar um aviso gratuito, e
     * comparacao com NaN ja e falsa nas duas pontas -- o {@code isFinite}
     * explicito existe para a regra dizer isso, e nao para depender do acidente.
     * </p>
     */
    public boolean alcanca(double distanciaAteOVizinho) {
        if (!Double.isFinite(distanciaAteOVizinho) || distanciaAteOVizinho < 0.0D) return false;
        return distanciaAteOVizinho <= raioDoRelatorio;
    }

    /**
     * O relatorio que UM vizinho recebe, medido a partir DELE.
     *
     * <p>A distancia que viaja no evento e a do VIZINHO ao alvo, e nao a do rato
     * ao alvo. Mandar a distancia do rato encheria a memoria de ameaca do vizinho
     * com um numero que nao descreve a situacao dele: o mob acharia o alvo perto
     * estando longe, e a unica coisa que denunciaria isso seria alguem medindo.
     * </p>
     *
     * <p>Vazio, e nao excecao, quando a distancia nao e medivel: isto roda dentro
     * de um tick de servidor, e derrubar o tick por causa de um vizinho em estado
     * estranho seria trocar um aviso perdido por um mob quebrado. A recusa tem
     * motivo e o motivo e este comentario -- o chamador so pula aquele vizinho.
     * </p>
     */
    public Optional<HearingEvent> relatorio(UUID alvo, double distanciaDoVizinhoAoAlvo) {
        if (alvo == null) return Optional.empty();
        if (!Double.isFinite(distanciaDoVizinhoAoAlvo) || distanciaDoVizinhoAoAlvo < 0.0D) {
            return Optional.empty();
        }
        return Optional.of(new HearingEvent(alvo, distanciaDoVizinhoAoAlvo, intensidade));
    }
}
