package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import java.util.Optional;
import java.util.UUID;

/**
 * Quando um batedor RELATA, ate onde o aviso chega, e por quanto tempo ele vale.
 *
 * <p><b>O que foi reusado, e foi quase tudo.</b> O transporte do aviso e o mesmo
 * do Radio Rat e nao foi reescrito: {@link HearingEvent} carrega o som,
 * {@code OuvidoDeInimigo} o entrega, e quem recebe continua decidindo com o
 * proprio alcance de audicao e a propria faccao. A razao esta escrita la e vale
 * igual aqui: entregar um ALVO seria escrever na memoria do vizinho por fora --
 * uma segunda autoridade sobre a escolha de alvo, discordando do
 * {@code TargetEvaluator} em silencio. O batedor aponta; ele nao decide pelos
 * outros.</p>
 *
 * <p><b>O que NAO foi reusado, e por que.</b> {@code RadioRatReportRules} prende
 * o relatorio a JANELA ATIVA DE UM ATAQUE: o rato grita, e o grito e um golpe
 * telegrafado com windup, janela e recuperacao. Este bicho e o contrario disso.
 * Ele <b>nao luta</b>: ao ver o jogador ele relata e FOGE, e a mordida dele so
 * existe quando ele esta encurralado. Amarrar o relatorio ao ataque faria o
 * batedor ter de ATACAR para avisar -- ou seja, ter de fazer exatamente aquilo
 * que a ficha dele diz que ele evita. O relatorio aqui e periodico, e o preco
 * dele e {@link #intervaloEntreRelatorios()}.</p>
 *
 * <p>A segunda diferenca e o DESTINO. O rato so produz som. O batedor tambem
 * abastece a colonia ({@code ChimeraColony.relatar}), e relatorio de colonia tem
 * PRAZO -- sem prazo a colonia lembraria para sempre de quem passou por ali uma
 * vez, e o alerta nunca baixaria. Por isso {@link #duracaoNaColonia()} mora aqui,
 * junto das outras condicoes: espalhada, ela viraria um literal no meio do tick
 * da entidade e ninguem acharia o numero de novo.</p>
 *
 * <p><b>Divida declarada:</b> {@link #alcanca} e {@link #aviso} sao, linha a
 * linha, o que {@code RadioRatReportRules} ja faz. Elas nao foram extraidas para
 * um lugar comum porque a extracao mexeria no bicho de outra frente enquanto as
 * duas estao abertas no mesmo worktree, e conflito de merge resolvido as pressas
 * apaga linha sem dar erro de compilacao. O gatilho de troca fica escrito: <b>na
 * terceira criatura que precisar avisar alguem, a extracao passa a valer mais que
 * o risco.</b></p>
 *
 * @param ticksDeObservacao ticks seguidos com alvo antes do PRIMEIRO relatorio
 * @param intervaloEntreRelatorios ticks minimos entre dois relatorios
 * @param raioDoAviso ate onde DO BATEDOR um vizinho e alcancado pelo aviso
 * @param intensidade 0..1 do {@link HearingEvent}; e o que decide, do lado de quem
 *        ouve, quao longe do alvo o aviso ainda serve
 * @param duracaoNaColonia por quantos ticks a colonia guarda a ameaca relatada
 */
public record RegrasDeRelatorioDeBatedor(int ticksDeObservacao, int intervaloEntreRelatorios,
        double raioDoAviso, double intensidade, int duracaoNaColonia) {

    public RegrasDeRelatorioDeBatedor {
        if (ticksDeObservacao < 1) {
            throw new IllegalArgumentException("ticksDeObservacao (" + ticksDeObservacao + ") tem"
                    + " de ser pelo menos 1: com zero o batedor relata no mesmo tick em que ve"
                    + " alguem, e a janela que separa 'ele me viu' de 'ele contou' deixa de"
                    + " existir -- sem erro nenhum, so um encontro sem resposta");
        }
        if (intervaloEntreRelatorios < 1) {
            throw new IllegalArgumentException("intervalo (" + intervaloEntreRelatorios + ") tem"
                    + " de ser pelo menos 1: relatorio por tick e uma varredura de mundo por tick"
                    + " disfarcada de gameplay, e o sintoma nao e erro -- e TPS caindo devagar"
                    + " numa colonia com muitos batedores");
        }
        if (!Double.isFinite(raioDoAviso) || raioDoAviso <= 0.0D) {
            throw new IllegalArgumentException("raio de aviso invalido: " + raioDoAviso);
        }
        if (!Double.isFinite(intensidade) || intensidade <= 0.0D || intensidade > 1.0D) {
            throw new IllegalArgumentException("intensidade (" + intensidade + ") fora de (0, 1]:"
                    + " o HearingEvent recusaria o valor em runtime, no meio de um tick de"
                    + " servidor, e nao aqui onde o numero foi escrito");
        }
        if (duracaoNaColonia < 1) {
            throw new IllegalArgumentException("duracao na colonia (" + duracaoNaColonia + ") tem"
                    + " de ser pelo menos 1: ChimeraColony.relatar recusa duracao zero, e recusar"
                    + " aqui e recusar onde o numero foi escrito");
        }
    }

    /**
     * O batedor RELATA agora.
     *
     * <p>As quatro condicoes moram juntas de proposito. Espalhadas por quatro
     * {@code if} do tick da entidade, cada uma vira uma divergencia possivel, e
     * nenhuma delas da erro: dao um batedor que relata cambaleando (e interromper
     * deixa de valer alguma coisa), ou que relata todo tick que o alvo pisca na
     * memoria (e a varredura de vizinhos vira custo por tick).</p>
     *
     * <p>Note o que NAO esta aqui: linha de visao. O batedor denuncia o que ele
     * LEMBRA, e nao so o que ve -- por isso se esconder atras de uma pedra depois
     * de ter sido visto nao apaga o relatorio. Exigir visao daria um bicho que
     * perde o assunto toda vez que o alvo passa atras de um tronco.</p>
     *
     * @param temAlvo o servidor tem alvo vivo resolvido a partir da memoria
     * @param cambaleando o batedor esta interrompido; interromper tem de CORTAR o aviso
     * @param ticksComAlvo ha quantos ticks seguidos existe alvo
     * @param ticksDesdeOUltimo ticks desde o ultimo relatorio que de fato saiu
     */
    public boolean relata(boolean temAlvo, boolean cambaleando, int ticksComAlvo,
            int ticksDesdeOUltimo) {
        return temAlvo && !cambaleando && ticksComAlvo >= ticksDeObservacao
                && ticksDesdeOUltimo >= intervaloEntreRelatorios;
    }

    /**
     * O vizinho esta dentro do raio do aviso.
     *
     * <p>Distancia nao finita ou negativa nao alcanca ninguem: um NaN vindo de uma
     * entidade em estado estranho nao pode virar um aviso gratuito. Comparacao com
     * NaN ja e falsa nas duas pontas -- o {@code isFinite} explicito existe para a
     * regra DIZER isso, e nao para depender do acidente.</p>
     */
    public boolean alcanca(double distanciaAteOVizinho) {
        if (!Double.isFinite(distanciaAteOVizinho) || distanciaAteOVizinho < 0.0D) return false;
        return distanciaAteOVizinho <= raioDoAviso;
    }

    /**
     * O aviso que UM vizinho recebe, medido a partir DELE.
     *
     * <p>A distancia que viaja no evento e a do VIZINHO ao alvo, e nao a do
     * batedor ao alvo. Mandar a do batedor encheria a memoria de ameaca do vizinho
     * com um numero que nao descreve a situacao dele: o mob acharia o alvo perto
     * estando longe, e a unica coisa que denunciaria isso seria alguem medindo.</p>
     *
     * <p>Vazio, e nao excecao, quando a distancia nao e medivel: isto roda dentro
     * de um tick de servidor, e derrubar o tick por causa de um vizinho em estado
     * estranho seria trocar um aviso perdido por um mob quebrado. A recusa tem
     * motivo, e o motivo e este comentario -- o chamador so pula aquele vizinho.</p>
     */
    public Optional<HearingEvent> aviso(UUID alvo, double distanciaDoVizinhoAoAlvo) {
        if (alvo == null) return Optional.empty();
        if (!Double.isFinite(distanciaDoVizinhoAoAlvo) || distanciaDoVizinhoAoAlvo < 0.0D) {
            return Optional.empty();
        }
        return Optional.of(new HearingEvent(alvo, distanciaDoVizinhoAoAlvo, intensidade));
    }
}
