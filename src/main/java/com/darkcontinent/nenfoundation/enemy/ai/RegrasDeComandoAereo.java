package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.Objects;

/**
 * A regra de ALTITUDE, COMANDO e MERGULHO da comandante alada -- pura, sem mundo.
 *
 * <p><b>O ciclo inteiro do bicho cabe em quatro passos, e a ordem deles E a
 * regra:</b> ela sobe, comanda do alto, mergulha, e sobe de novo. Quebrar
 * qualquer elo nao levanta excecao -- cada quebra apaga uma parte diferente do
 * encontro:</p>
 *
 * <ul>
 *   <li>sem a exigencia de {@code altitudeDeComando}, ela comanda do chao, e
 *       derruba-la deixa de ser tatica: vira um detalhe visual;</li>
 *   <li>sem {@code alturaDeAbandonoDoMergulho}, o mergulho nao termina -- ela
 *       desce e FICA embaixo, que e um mob voador pousado em cima do alvo;</li>
 *   <li>sem {@code ticksDeSubidaAposMergulho}, o mergulho nao custa nada: ela
 *       desce, machuca e volta a comandar no tick seguinte. O jogador apanha e
 *       nao ganha janela nenhuma por ter sobrevivido ao golpe.</li>
 * </ul>
 *
 * <p><b>Este record nao conhece entidade, nivel nem Minecraft.</b> E o que
 * permite provar em teste unitario que uma comandante no chao NAO comanda, que o
 * mergulho termina, e que a janela de subida existe -- coisas que so um gametest
 * veria se elas morassem na entidade, e que gametest nenhum deste repositorio
 * roda hoje.</p>
 *
 * @param altitudeDeComando blocos sobre o chao a partir dos quais ela enxerga o
 *        campo e pode ordenar. Abaixo disso ela nao comanda, e o silencio e o
 *        premio de quem a trouxe para baixo
 * @param altitudeParaMergulhar altura minima para COMECAR um mergulho. Nunca
 *        menor que {@code altitudeDeComando}: o mergulho sai de cima, e um
 *        mergulho que comeca baixo e um pulo
 * @param alturaDeAbandonoDoMergulho altura em que o mergulho ACABA e a subida
 *        comeca. Maior que zero de proposito: ela raspa o chao, nao pousa
 * @param ticksDeSubidaAposMergulho ticks em que ela nao comanda depois do
 *        mergulho -- a janela do jogador, medida em ticks e nao em "quando der"
 * @param alcanceDeMergulho distancia horizontal (centro a centro) em que ela se
 *        compromete com o mergulho
 */
public record RegrasDeComandoAereo(double altitudeDeComando, double altitudeParaMergulhar,
        double alturaDeAbandonoDoMergulho, int ticksDeSubidaAposMergulho,
        double alcanceDeMergulho) {

    public RegrasDeComandoAereo {
        if (!finito(altitudeDeComando) || !finito(altitudeParaMergulhar)
                || !finito(alturaDeAbandonoDoMergulho) || !finito(alcanceDeMergulho)) {
            throw new IllegalArgumentException("altura ou alcance invalido em regras de comando"
                    + " aereo: altitudeDeComando=" + altitudeDeComando
                    + " altitudeParaMergulhar=" + altitudeParaMergulhar
                    + " alturaDeAbandonoDoMergulho=" + alturaDeAbandonoDoMergulho
                    + " alcanceDeMergulho=" + alcanceDeMergulho);
        }
        if (alturaDeAbandonoDoMergulho >= altitudeDeComando) {
            throw new IllegalArgumentException("o mergulho abandona em "
                    + alturaDeAbandonoDoMergulho + " blocos e a altitude de comando e "
                    + altitudeDeComando + ": o mergulho terminaria na altura em que ela ja"
                    + " comanda, ou seja, ela nunca desceria de verdade. O ataque continua"
                    + " saindo, o dano continua certo, e o que some e o mergulho -- o unico"
                    + " momento em que o jogador consegue alcanca-la");
        }
        if (altitudeParaMergulhar < altitudeDeComando) {
            throw new IllegalArgumentException("ela mergulharia a partir de "
                    + altitudeParaMergulhar + " blocos, abaixo da altitude de comando ("
                    + altitudeDeComando + "): o mergulho passaria a sair de meia altura e a"
                    + " leitura do golpe -- 'ela largou o comando e esta vindo' -- deixaria de"
                    + " existir");
        }
        if (ticksDeSubidaAposMergulho < 1) {
            throw new IllegalArgumentException("subida de " + ticksDeSubidaAposMergulho
                    + " ticks depois do mergulho: sem janela, ela desce, machuca e volta a"
                    + " comandar no mesmo tick. O mergulho vira lucro puro, e sobreviver ao golpe"
                    + " deixa de valer alguma coisa");
        }
        if (alcanceDeMergulho <= 0.0D) {
            throw new IllegalArgumentException("alcance de mergulho " + alcanceDeMergulho
                    + ": zero ou negativo faz ela nunca se comprometer, e o bicho vira um mob"
                    + " voador decorativo que nunca ataca");
        }
    }

    private static boolean finito(double valor) {
        return Double.isFinite(valor) && valor > 0.0D;
    }

    /**
     * A postura deste tick.
     *
     * <p>A ordem das perguntas e a personalidade dela, e inverter qualquer par
     * produz outro bicho sem produzir erro nenhum:</p>
     *
     * <ol>
     *   <li><b>cambaleando</b> vence tudo. Interromper uma comandante tem de
     *       CALAR as ordens, senao acertar nela nao muda nada no bando e o jogador
     *       aprende a ignora-la;</li>
     *   <li><b>a janela de subida</b> vem antes de qualquer coisa sobre o alvo.
     *       Checada depois, um alvo colado faria ela emendar mergulho em mergulho
     *       e a janela nunca chegaria;</li>
     *   <li><b>o mergulho em curso</b> continua ate raspar o chao. Cortado no
     *       meio por "ja da para comandar de novo", o golpe sairia sem que a
     *       comandante tivesse descido, e o jogador levaria dano de quem, na tela,
     *       ainda esta no alto;</li>
     *   <li><b>altitude</b> antes de alvo. Baixa, ela sobe -- e nao comanda. E
     *       esta a regra que o jogador aprende a forcar;</li>
     *   <li><b>mergulhar ou comandar</b>, nesta ordem, e so com alvo.</li>
     * </ol>
     */
    public PosturaDeComando decidir(SituacaoDeComandoAereo s) {
        Objects.requireNonNull(s, "situacao de comando aereo ausente");
        if (s.cambaleando()) return PosturaDeComando.RECOLHER;
        if (s.ticksDesdeOMergulho() < ticksDeSubidaAposMergulho) return PosturaDeComando.RECOLHER;
        if (s.mergulhando()) {
            return s.alturaSobreOChao() > alturaDeAbandonoDoMergulho
                    ? PosturaDeComando.MERGULHAR : PosturaDeComando.RECOLHER;
        }
        if (s.alturaSobreOChao() < altitudeDeComando) return PosturaDeComando.SUBIR;
        if (s.temAlvo() && s.alturaSobreOChao() >= altitudeParaMergulhar
                && s.distanciaAoAlvo() <= alcanceDeMergulho) {
            return PosturaDeComando.MERGULHAR;
        }
        return PosturaDeComando.COMANDAR;
    }

    /**
     * O mergulho ja acabou nesta altura?
     *
     * <p>Existe separada porque quem MEDE o fim do mergulho e o mesmo lado que
     * zera {@code ticksDesdeOMergulho}, e as duas coisas precisam concordar. Com a
     * comparacao escrita duas vezes -- uma aqui, outra no consumidor -- bastaria
     * um {@code >} virar {@code >=} num dos lados para a comandante entrar em
     * RECOLHER sem nunca ter o contador zerado, e ficar subindo para sempre.</p>
     */
    public boolean mergulhoTerminou(double alturaSobreOChao) {
        return alturaSobreOChao <= alturaDeAbandonoDoMergulho;
    }
}
