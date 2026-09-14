package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Regras da guarda de ninho, sem mundo e sem entidade.
 *
 * <p>DECISAO CENTRAL QUE ESTE ARQUIVO CARREGA: a spider eagle e perigo de
 * LUGAR, nao de caminho. Ela nao caca ninguem. Tudo que ela decide -- avisar,
 * botar e DESISTIR -- e medido a partir do NINHO, nunca a partir do corpo dela.
 * Medir a partir do corpo transformaria a coleira numa perseguicao: bastaria a
 * ave se afastar um pouco para o raio inteiro andar junto com ela.</p>
 *
 * <p>{@link #desiste} E A REGRA QUE PERMITE ESCAPAR. E ela, e so ela, que faz
 * "quem recua e poupado" ser verdade: o intruso que sai do raio de aviso e
 * largado, e a ave volta para o ninho. Sem essa regra o mob vira mais um
 * perseguidor, e o encounter que o plano quer -- roubar os ovos sem matar a
 * mae, agora ancorada no bloco de ninho persistente -- deixa de ser possivel.</p>
 *
 * <p>"Quando ela avisa", "quando ela bota" e "quando ela desiste" sao UMA
 * fonte so, testavel sozinha. Espalhadas pela Goal e pelo tick da entidade,
 * cada condicao viraria um {@code if} diferente, e a divergencia entre elas
 * nao daria erro nenhum: apareceria como uma ave que as vezes persegue e as
 * vezes nao.</p>
 *
 * <p>Os numeros sao injetados pelo perfil de balanceamento; este record nao
 * conhece nenhum deles.</p>
 *
 * @param raioDeAviso   ate onde do ninho um intruso ja e avisado
 * @param raioDeBote    ate onde do ninho o mergulho pode comecar
 * @param raioDeColeira ate onde do ninho a ave pode se afastar perseguindo
 * @param ticksDeAviso  quantos ticks de aviso sao obrigatorios antes do bote
 */
public record NestGuardRules(double raioDeAviso, double raioDeBote, double raioDeColeira,
        int ticksDeAviso) {
    public NestGuardRules {
        if (!Double.isFinite(raioDeAviso) || !Double.isFinite(raioDeBote)
                || !Double.isFinite(raioDeColeira)
                || raioDeBote <= 0.0D
                // A ORDEM raioDeBote < raioDeAviso <= raioDeColeira NAO E DECORACAO.
                //
                // Com o bote maior que o aviso, a ave ataca antes de avisar, e o
                // telegrafo -- o unico tempo de reacao que o jogador tem -- some. Com a
                // coleira menor que o aviso, ela desiste antes mesmo de alcancar quem
                // acabou de avisar, e o mergulho nunca acontece. Nenhuma das duas da
                // erro: as duas dao "a ave se comporta estranho" e ninguem sabe dizer
                // por que.
                || raioDeAviso <= raioDeBote
                || raioDeColeira < raioDeAviso
                // Aviso de zero tick e bote sem telegrafo.
                || ticksDeAviso < 1) {
            throw new IllegalArgumentException("regras de guarda de ninho invalidas");
        }
    }

    /**
     * O intruso esta perto o bastante do NINHO para ser avisado.
     *
     * <p>Distancia nao finita nao avisa: um NaN vindo de um alvo em estado
     * estranho nao pode acender o aviso de graca, e comparacao com NaN ja e
     * falsa nas duas pontas.</p>
     */
    public boolean avisa(double distanciaDoIntrusoAoNinho) {
        if (!Double.isFinite(distanciaDoIntrusoAoNinho)) return false;
        return distanciaDoIntrusoAoNinho <= raioDeAviso;
    }

    /**
     * O bote so acontece DENTRO do raio de bote E depois do aviso completo.
     *
     * <p>O aviso e OBRIGATORIO, e as duas condicoes vivem juntas aqui de
     * proposito: separadas, a que sobrasse em algum caminho produziria um
     * mergulho sem telegrafo -- morte sem aviso, que e exatamente o que o plano
     * proibe.</p>
     */
    public boolean bote(double distanciaDoIntrusoAoNinho, int ticksAvisando) {
        if (!Double.isFinite(distanciaDoIntrusoAoNinho)) return false;
        return distanciaDoIntrusoAoNinho <= raioDeBote && ticksAvisando >= ticksDeAviso;
    }

    /**
     * A REGRA QUE PERMITE ESCAPAR.
     *
     * <p>Duas razoes largam o alvo, e as duas devolvem a ave ao ninho: ela
     * passou da coleira (foi longe demais do ninho) ou o intruso saiu do raio
     * de aviso (recuou). A segunda e a promessa do mob inteiro -- QUEM RECUA E
     * POUPADO -- e e o que permite, agora que existe um bloco de ninho,
     * roubar os ovos sem matar a mae.</p>
     *
     * <p>Distancia nao finita DESISTE, e nao o contrario: o lado seguro de uma
     * medida invalida e a ave voltar para casa, nunca ficar presa perseguindo
     * um alvo que a medicao nao sabe mais onde esta.</p>
     */
    public boolean desiste(double distanciaDaAveAoNinho, double distanciaDoIntrusoAoNinho) {
        if (!Double.isFinite(distanciaDaAveAoNinho) || !Double.isFinite(distanciaDoIntrusoAoNinho)) {
            return true;
        }
        return distanciaDaAveAoNinho > raioDeColeira || distanciaDoIntrusoAoNinho > raioDeAviso;
    }
}
