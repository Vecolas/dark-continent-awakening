package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Regras do disfarce, sem mundo e sem entidade.
 *
 * <p>Carrega a decisao central do mob 03: a ameaca e o ENGANO, e a
 * inconsistencia que entrega o engano e OBSERVAVEL. Enquanto disfarcado, o
 * macaco so avanca quando ninguem esta olhando para ele. Quem o encara, trava
 * o avanco; quem desvia o olhar, e alcancado.</p>
 *
 * <p>"Quando ele pode andar", "quando ele esta sendo encarado" e "quando o
 * disfarce cai" sao UMA fonte so, testavel sozinha. Espalhadas pela Goal e
 * pelo tick da entidade, cada condicao viraria um {@code if} diferente, e a
 * divergencia entre elas nao daria erro nenhum: apareceria como um macaco que
 * as vezes anda mesmo encarado -- e ai a pista que o jogador precisa aprender
 * a ler deixa de existir.</p>
 *
 * <p>Os numeros sao injetados pelo perfil de balanceamento; este record nao
 * conhece nenhum deles.</p>
 *
 * @param distanciaDeRevelacao a que distancia do alvo o disfarce cai sozinho
 * @param cossenoDeObservacao  quao de frente o olhar do alvo precisa estar
 *                             para contar como "esta me encarando" (1 = em
 *                             cheio, 0 = de lado)
 * @param raioDoBando          ate onde a revelacao chama os outros disfarcados
 * @param ticksDeReveal        quanto dura o aviso entre revelar e atacar
 */
public record DisguiseRules(double distanciaDeRevelacao, double cossenoDeObservacao,
        double raioDoBando, int ticksDeReveal) {
    public DisguiseRules {
        if (!Double.isFinite(distanciaDeRevelacao) || !Double.isFinite(cossenoDeObservacao)
                || !Double.isFinite(raioDoBando)
                || distanciaDeRevelacao <= 0.0D
                || cossenoDeObservacao < -1.0D || cossenoDeObservacao > 1.0D
                // Bando menor que o proprio gatilho de revelacao nunca se avisa: o
                // primeiro macaco revela e os outros continuam parados na paisagem.
                // Isso nao da erro nenhum, so apaga a emboscada em bando.
                || raioDoBando < distanciaDeRevelacao
                // Reveal de zero tick e bote sem aviso -- o telegrafo e o unico tempo
                // de reacao que o jogador tem.
                || ticksDeReveal < 1) {
            throw new IllegalArgumentException("regras de disfarce invalidas");
        }
    }

    /**
     * O ALVO esta olhando para o macaco.
     *
     * <p>O cosseno e medido entre o olhar horizontal do alvo e a direcao
     * horizontal alvo-&gt;macaco, e quem mede e o SERVIDOR. Cosseno nao finito
     * (alvo em cima do macaco, vetor degenerado) reprova: um NaN nao pode virar
     * "ninguem esta olhando" por acidente de comparacao.</p>
     */
    public boolean observado(boolean alvoVisivel, double cossenoDoOlharDoAlvo) {
        if (!Double.isFinite(cossenoDoOlharDoAlvo)) return false;
        return alvoVisivel && cossenoDoOlharDoAlvo >= cossenoDeObservacao;
    }

    /**
     * A INCONSISTENCIA QUE O JOGADOR PRECISA NOTAR.
     *
     * <p>Revelado, o macaco anda sempre -- e um bicho perseguindo, sem misterio.
     * Disfarcado, ele SO anda quando nao esta sendo observado: parado enquanto
     * encarado, mais perto a cada vez que o jogador desvia o olhar. A
     * contrapartida ensinavel e simples: nao tire os olhos dele.</p>
     */
    public boolean podeAproximar(boolean disfarcado, boolean observado) {
        return !disfarcado || !observado;
    }

    /**
     * O disfarce cai por DANO ou por DISTANCIA -- e so faz sentido disfarcado.
     *
     * <p>Distancia nao finita nao revela: sem alvo nao ha de quem se aproximar,
     * e um NaN nao pode abrir a emboscada de graca.</p>
     */
    public boolean revela(boolean disfarcado, double distanciaDoAlvo, boolean sofreuDano) {
        if (!disfarcado) return false;
        if (sofreuDano) return true;
        return Double.isFinite(distanciaDoAlvo) && distanciaDoAlvo <= distanciaDeRevelacao;
    }
}
