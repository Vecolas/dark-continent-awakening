package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Traduz geometria de impacto em id de regiao atingida.
 *
 * <p><b>Por que isto mora no servidor, com todas as letras:</b> nunca se aceita
 * o cliente dizendo "acertei a testa". O cliente manda apenas a intencao de
 * atacar; quem mede altura do impacto e angulo de ataque e o servidor, com a
 * posicao que ELE tem da entidade. Se a regiao viesse do cliente, qualquer
 * pacote forjado transformaria todo golpe em ponto fraco — e o sintoma nao
 * seria erro nenhum, seria um mob que morre rapido demais e ninguem consegue
 * explicar.</p>
 *
 * <p>Por isso o record nao toca em mundo nem em entidade: ele recebe dois
 * numeros ja medidos e devolve um id. Assim a regra e testavel sozinha e o
 * unico ponto que pode chama-la e o lado autoritativo.</p>
 */
public record WeakPointResolver(String regiaoVulneravel, String regiaoPadrao,
        double alturaMinima, double cossenoMinimo) {
    public WeakPointResolver {
        if (regiaoVulneravel == null || regiaoVulneravel.isBlank()
                || regiaoPadrao == null || regiaoPadrao.isBlank()
                || regiaoVulneravel.equals(regiaoPadrao)
                || !Double.isFinite(alturaMinima) || !Double.isFinite(cossenoMinimo)
                || alturaMinima < 0.0D || alturaMinima > 1.0D
                || cossenoMinimo < -1.0D || cossenoMinimo > 1.0D) {
            throw new IllegalArgumentException("resolucao de ponto fraco invalida");
        }
    }

    /**
     * @param alturaRelativa altura do impacto normalizada pela altura da caixa, em [0,1]
     * @param cossenoDeFrente produto escalar entre o olhar horizontal do alvo e a
     *        direcao horizontal ate o atacante; 1 e de frente, -1 e pelas costas
     * @return o id da regiao vulneravel quando as duas condicoes batem; a regiao padrao no resto
     */
    public String resolver(double alturaRelativa, double cossenoDeFrente) {
        if (!Double.isFinite(alturaRelativa) || !Double.isFinite(cossenoDeFrente)) {
            throw new IllegalArgumentException("geometria de impacto invalida");
        }
        return alturaRelativa >= alturaMinima && cossenoDeFrente >= cossenoMinimo
                ? regiaoVulneravel
                : regiaoPadrao;
    }
}
