package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Transforma o estado visual em particulas em volta do jogador.
 *
 * <p>PARTICULA VANILLA, COLORIDA POR PARAMETRO. {@link DustParticleOptions}
 * aceita a cor como argumento, entao o efeito muda de cor por tecnica sem
 * nenhum sprite novo -- e sprite novo exigiria arte autoral (ADR-007), que nao
 * existe. E aparece em primeira <b>e</b> terceira pessoa, ao contrario de um
 * efeito preso ao renderer do jogador.
 *
 * <p>A CONTAGEM E SORTEADA, e nao arredondada. Com intensidade baixa, Ten pede
 * algo como 0,3 particula por tick; arredondar daria zero para sempre, e Ten
 * ficaria invisivel. Sortear faz sair uma particula a cada tres ticks, em
 * media -- que e o efeito discreto que Ten deve ter.
 *
 * <p>NADA AQUI DECIDE REGRA. Ele le um snapshot imutavel e desenha. Nao toca em
 * aura, custo, tecnica nem servidor.
 */
public final class EmissorDeParticulasDeAura {

    /**
     * Teto duro de particulas por tick, por jogador.
     *
     * <p>CONSTANTE NO CODIGO de proposito: e uma trava de seguranca contra
     * numero absurdo vindo da config, e nao um botao de tuning. Quem quiser
     * menos particulas mexe na densidade, que e config; o teto existe para que
     * uma densidade errada nao trave o cliente.
     */
    private static final int TETO_POR_TICK = 12;

    /**
     * Quanto da densidade configurada sobra depois que a shell existe.
     *
     * <p>A PARTICULA FOI REBAIXADA A ACABAMENTO (AV3, issue #186). Ate o AV0 ela
     * ERA a aura -- era o unico efeito que desenhava. Agora a shell e os
     * filamentos sustentam a identidade sozinhos, e a nuvem de poeira que
     * bastava antes passaria a competir com eles.
     *
     * <p>O NUMERO NAO E UM BOTAO: quem quer menos particula mexe em
     * {@code vfx.densidadeDeParticulas}, que e config. Este fator existe para
     * que a densidade 1.0 -- o padrao, escolhido quando a particula era tudo --
     * passe a significar "faisca ocasional" em vez de "nuvem".
     *
     * <p>Com Ten em intensidade cheia isto da algo perto de meia particula por
     * tick; com Ren, cerca de tres. A direcao de arte pede 0-4 em Ten e 10-24
     * em Ren considerando TODAS as fontes, e o resto vem dos filamentos.
     */
    private static final double FATOR_DE_ACABAMENTO = 0.25D;

    /** Altura e largura da nuvem, em blocos, relativas ao corpo. */
    private static final double RAIO_HORIZONTAL = 0.45D;
    private static final double ALTURA = 1.9D;

    private EmissorDeParticulasDeAura() {
    }

    /**
     * Quantas particulas este tick deve emitir.
     *
     * <p>SEPARADO DO DESENHO para poder ser provado sem o jogo: o sorteio entra
     * como {@code float} de fora, e nao como chamada a um gerador escondido.
     *
     * <p>O PERFIL ENTRA POR PARAMETRO, e nao e buscado aqui dentro. Buscar
     * {@code AuraPerfis.de(...)} nesta funcao a amarraria ao gerenciador de
     * recursos do Minecraft -- e a unica parte do emissor que da para provar sem
     * tela deixaria de rodar em JUnit. Quem chama ja tem o perfil na mao.
     *
     * @param perfil  os numeros de arte do modo, vindos de {@code nen_vfx/*.json}
     * @param sorteio um numero de 0 (inclusive) a 1 (exclusive)
     */
    public static int quantasEmitir(AuraPerfilVisual perfil, AuraVisualState estado,
            double densidade, float sorteio) {
        if (perfil == null || estado == null || !estado.enabled() || densidade <= 0.0D) {
            return 0;
        }
        double bruto = perfil.densidadeDeParticula() * estado.intensity() * densidade
                * FATOR_DE_ACABAMENTO * TETO_POR_TICK;
        int inteiras = (int) bruto;
        double resto = bruto - inteiras;
        // O RESTO VIRA CHANCE. Sem isto, toda intensidade abaixo de 1/TETO
        // some, e a tecnica mais discreta -- Ten -- nunca aparece.
        if (sorteio < resto) {
            inteiras++;
        }
        return Math.min(TETO_POR_TICK, inteiras);
    }

    /** Emite as particulas deste tick em volta do jogador. */
    public static void emitir(ClientLevel nivel, Player jogador, AuraVisualState estado,
            double densidade) {
        if (nivel == null || jogador == null || estado == null || !estado.enabled()) {
            return;
        }
        // O MESMO PERFIL DO MESMO MODO que a shell desenha, e nao uma copia:
        // `AuraPerfis.de` ja aplica a sobreposicao de tuning, entao mexer no
        // slider muda a shell E a faisca juntas. Duas fontes aqui produziriam
        // uma captura em que a poeira nao acompanha o ajuste, sem erro nenhum.
        AuraPerfilVisual perfil = AuraPerfis.de(estado.mode());
        RandomSource aleatorio = nivel.getRandom();
        int quantas = quantasEmitir(perfil, estado, densidade, aleatorio.nextFloat());
        if (quantas == 0) {
            return;
        }
        MedidorDeVfx.particulas(quantas);

        DustParticleOptions poeira = new DustParticleOptions(
                corComo(estado.primaryColor()), tamanhoDe(perfil, estado));

        for (int i = 0; i < quantas; i++) {
            double angulo = aleatorio.nextDouble() * Math.PI * 2.0D;
            double raio = RAIO_HORIZONTAL * (0.75D + aleatorio.nextDouble() * 0.35D);
            double x = jogador.getX() + Math.cos(angulo) * raio;
            double z = jogador.getZ() + Math.sin(angulo) * raio;
            // A ALTURA SAI DA ALOCACAO, e nao de um sorteio uniforme. E o que
            // faz Gyo na cabeca parecer Gyo na cabeca: a aura adensa onde o
            // servidor disse que ela esta.
            double y = jogador.getY()
                    + alturaSorteada(estado.distribution(), aleatorio.nextFloat())
                    + (aleatorio.nextDouble() - 0.5D) * 0.25D;

            // VELOCIDADE PARA CIMA, E CURTA. Aura sobe junto do corpo; deriva
            // lateral com rastro longo vira fumaca, que a issue #100 proibe com
            // todas as letras.
            nivel.addParticle(poeira, x, y, z, 0.0D, 0.02D + aleatorio.nextDouble() * 0.03D, 0.0D);
        }
    }

    /**
     * Sorteia uma altura no corpo, com peso pela alocacao.
     *
     * <p>SORTEIO POR PESO, e nao a regiao mais concentrada. Pegar so o maior
     * faria a aura sumir do resto do corpo de uma vez -- e a alocacao nunca e
     * tudo num lugar so, nem em Ko. O peso preserva a proporcao: concentrar 45%
     * na cabeca poe quase metade das particulas la, e o resto continua
     * aparecendo.
     *
     * <p>PURO E TESTAVEL: o sorteio entra como argumento, e nao como chamada a
     * um gerador escondido.
     *
     * @param sorteio de 0 (inclusive) a 1 (exclusive)
     * @return altura em blocos a partir dos pes
     */
    static double alturaSorteada(AuraDistribution distribuicao, float sorteio) {
        float acumulado = 0.0F;
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            acumulado += distribuicao.intensidade(regiao) / totalDe(distribuicao);
            if (sorteio < acumulado) {
                return alturaDe(regiao);
            }
        }
        return alturaDe(AuraBodyRegion.TORSO);
    }

    private static float totalDe(AuraDistribution distribuicao) {
        float total = 0.0F;
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            total += distribuicao.intensidade(regiao);
        }
        // TOTAL ZERO ACONTECE: e a distribuicao de Zetsu. Dividir por ele daria
        // NaN, e NaN numa coordenada de particula nao lanca -- ela so nao
        // aparece, em lugar nenhum, para sempre.
        return total > 0.0F ? total : 1.0F;
    }

    /** Onde cada regiao fica, em blocos a partir dos pes de um jogador de pe. */
    private static double alturaDe(AuraBodyRegion regiao) {
        return switch (regiao) {
            case HEAD -> 1.60D;
            case TORSO -> 1.10D;
            case LEFT_ARM, RIGHT_ARM -> 1.20D;
            case LEFT_LEG, RIGHT_LEG -> 0.45D;
        };
    }

    /** Um ARGB de inteiro para o vetor de cor que a poeira vanilla espera. */
    static Vector3f corComo(int argb) {
        return new Vector3f(
                ((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F);
    }

    /**
     * Particula maior quando a aura esta mais forte, dentro do que o vanilla aceita.
     *
     * <p>O PISO E O VAO SAO ESTRUTURA, e nao botao de arte: abaixo de 0,6 a
     * poeira vanilla some em qualquer luz, e acima de 1,5 ela vira mancha. O que
     * a sessao de arte gira e {@code tamanho_de_particula}, no perfil.
     */
    static float tamanhoDe(AuraPerfilVisual perfil, AuraVisualState estado) {
        return 0.6F + 0.9F * estado.intensity() * perfil.tamanhoDeParticula();
    }
}
