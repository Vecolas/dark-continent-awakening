package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * QUAL dos dois ataques o Scorpion Leader comeca agora -- e por que nao comeca.
 *
 * <p>Este mob tem dois golpes que custam coisas diferentes ao jogador: a pinca e
 * curta e nao envenena; o ferrao avisa por 28 ticks e cobra um preco que so
 * aparece depois. Um {@code boolean podeAtacar} colapsaria a escolha entre eles
 * -- e a escolha E o bicho.</p>
 *
 * <p>As tres recusas estao separadas pela mesma razao que as duas acoes: elas
 * pedem coisas diferentes de quem chama. Cambaleando a navegacao tem de parar;
 * segurando a guarda ela continua encarando o alvo sem avancar; aguardando ela
 * volta a se aproximar. Colapsadas em "nao ataca", a formiga ficaria parada no
 * lugar em todas as tres, e o jogador leria isso como um bicho travado.</p>
 */
public enum DecisaoDeFerrao {
    /** Nada a fazer agora: sem alvo visivel, fora de alcance ou em recarga. */
    AGUARDAR,
    /**
     * Golpe de pinca: curto, sem veneno.
     *
     * <p>Ele e o ataque de enchimento, e essa e a funcao dele. Se o ferrao
     * saisse sempre que a formiga estivesse perto, o jogador nunca veria um golpe
     * comum e nao teria com o que COMPARAR o telegrafo longo -- e sem comparacao
     * nao ha leitura, so um bicho que as vezes envenena.</p>
     */
    GOLPEAR_COM_PINCA,
    /** Ferroada: telegrafo longo, alcance maior, veneno. */
    ARMAR_O_FERRAO,
    /**
     * A postura mandou segurar.
     *
     * <p>E aqui que a INTENCAO de Nen chega, ja traduzida para um efeito do
     * inimigo: a formiga fecha a guarda e para de comecar golpes. Ela nao calcula
     * aura, nao paga custo e nao ativa tecnica -- o Nen Foundation e a unica
     * autoridade sobre Nen, e o que atravessa esta fronteira e uma decisao de
     * POSTURA, que e coisa de quem luta e nao de quem tem aura.</p>
     */
    SEGURAR_A_GUARDA,
    /**
     * Cambaleando: nenhum ataque comeca, e a navegacao tem de parar.
     *
     * <p>Separado de {@link #AGUARDAR} porque so este exige parar o caminho. Sem
     * a distincao, a formiga interrompida continuaria andando para o alvo durante
     * o cambaleio, e interromper deixaria de parecer que fez alguma coisa.</p>
     */
    CAMBALEANDO
}
