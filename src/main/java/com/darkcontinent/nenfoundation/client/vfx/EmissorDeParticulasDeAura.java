package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.darkcontinent.nenfoundation.registry.AuraSparkParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Transforma o estado visual em faiscas ocasionais na superficie do jogador.
 *
 * <p>ELA NAO E A AURA. A shell e as ribbons sustentam a leitura; esta classe
 * acrescenta pequenas faiscas autorais perto da origem dos filamentos. Zerar a
 * densidade do jogador remove apenas esse acabamento.
 *
 * <p>A CONTAGEM E SORTEADA, e nao arredondada. Ten pede 0,4 faisca por segundo,
 * ou 0,02 por tick; arredondar daria zero para sempre. O resto fracionario vira
 * chance e preserva essa media sem transformar acabamento em nuvem.
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
    private static final double TICKS_POR_SEGUNDO = 20.0D;

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
        // A TAXA E POR SEGUNDO NO PERFIL. Nao a confundir com o teto: uma e
        // tuning de arte; o outro so impede que um resource pack hostil ou
        // quebrado congele o cliente.
        double particulasPorTick = perfil.taxaDeFaiscas() / TICKS_POR_SEGUNDO;
        double bruto = particulasPorTick * estado.intensity() * densidade;
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
        AuraPerfilVisual perfil = AuraPerfis.de(estado.mode());
        RandomSource aleatorio = nivel.getRandom();
        int quantas = quantasEmitir(perfil, estado, densidade, aleatorio.nextFloat());
        if (quantas == 0) {
            return;
        }
        MedidorDeVfx.particulas(quantas);

        float tamanho = tamanhoDe(perfil, estado);
        AuraSparkParticleOptions faisca = new AuraSparkParticleOptions(
                jogador.getId(), estado.primaryColor(), tamanho, maximoAtivas(estado.mode()));
        boolean slim = jogador instanceof AbstractClientPlayer cliente
                && cliente.getSkin().model() == PlayerSkin.Model.SLIM;

        for (int i = 0; i < quantas; i++) {
            AuraAnchor ancora = ancoraSorteada(estado.distribution(), aleatorio.nextFloat(),
                    aleatorio.nextInt());
            PontoDaAncora local = pontoLocalDa(ancora, slim);
            double yaw = Math.toRadians(-jogador.yBodyRot);
            double rotX = local.x() * Math.cos(yaw) - local.z() * Math.sin(yaw);
            double rotZ = local.x() * Math.sin(yaw) + local.z() * Math.cos(yaw);

            nivel.addParticle(faisca,
                    jogador.getX() + rotX,
                    jogador.getY() + local.y(),
                    jogador.getZ() + rotZ,
                    0.0D, 0.015D + aleatorio.nextDouble() * 0.015D, 0.0D);
        }
    }

    /**
     * Sorteia uma ancora no corpo, com peso pela alocacao.
     *
     * <p>SORTEIO POR PESO, e nao a regiao mais concentrada. Pegar so o maior
     * faria a aura sumir do resto do corpo de uma vez -- e a alocacao nunca e
     * tudo num lugar so, nem em Ko. O peso preserva a proporcao.
     *
     * <p>PURO E TESTAVEL: os dois sorteios entram como valores, sem precisar
     * fingir uma implementacao inteira de {@link RandomSource} no teste.
     */
    static AuraAnchor ancoraSorteada(AuraDistribution distribuicao, float sorteio,
            int sorteioDaAncora) {
        float acumulado = 0.0F;
        AuraBodyRegion regiaoEscolhida = AuraBodyRegion.TORSO;
        float total = totalDe(distribuicao);

        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            acumulado += distribuicao.intensidade(regiao) / total;
            if (sorteio < acumulado) {
                regiaoEscolhida = regiao;
                break;
            }
        }
        int quantidade = 0;
        for (AuraAnchor a : AuraAnchor.values()) {
            if (a.regiao() == regiaoEscolhida) {
                quantidade++;
            }
        }
        int indice = Math.floorMod(sorteioDaAncora, quantidade);
        for (AuraAnchor a : AuraAnchor.values()) {
            if (a.regiao() == regiaoEscolhida && indice-- == 0) {
                return a;
            }
        }
        return AuraAnchor.CHEST_LEFT;
    }

    private static float totalDe(AuraDistribution distribuicao) {
        float total = 0.0F;
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            total += distribuicao.intensidade(regiao);
        }
        return total > 0.0F ? total : 1.0F;
    }

    /**
     * Origem aproximada da mesma ancora usada pela ribbon, em blocos locais.
     *
     * <p>O tick nao possui a matriz final de {@code ModelPart}; por isso a
     * faisca acompanha a guinada do corpo e nasce na superficie contratada da
     * parte, mas nao tenta reconstruir oscilacao de braco/perna. Fazer essa
     * reconstrucao aqui duplicaria o renderer e divergiria de animacoes de
     * outros mods. A vida curta limita essa aproximacao a poucos quadros.
     */
    static PontoDaAncora pontoLocalDa(AuraAnchor ancora, boolean slim) {
        double psi = ancora.psiInicial();
        double xModelo = pivoX(ancora.regiao()) + ancora.centroX(slim)
                + Math.cos(psi) * ancora.raioX(slim);
        double yModelo = pivoY(ancora.regiao()) + ancora.alturaBase();
        double zModelo = Math.sin(psi) * ancora.raioZ();
        return new PontoDaAncora(xModelo / 16.0D, 1.501D - yModelo / 16.0D,
                zModelo / 16.0D);
    }

    private static double pivoX(AuraBodyRegion regiao) {
        return switch (regiao) {
            case LEFT_ARM -> 5.0D;
            case RIGHT_ARM -> -5.0D;
            case LEFT_LEG -> 1.9D;
            case RIGHT_LEG -> -1.9D;
            case HEAD, TORSO -> 0.0D;
        };
    }

    private static double pivoY(AuraBodyRegion regiao) {
        return switch (regiao) {
            case LEFT_ARM, RIGHT_ARM -> 2.0D;
            case LEFT_LEG, RIGHT_LEG -> 12.0D;
            case HEAD, TORSO -> 0.0D;
        };
    }

    static int maximoAtivas(AuraVisualMode modo) {
        return modo == AuraVisualMode.TEN ? 4 : 24;
    }

    record PontoDaAncora(double x, double y, double z) {
    }

    /**
     * Particula maior quando a aura esta mais forte, dentro do que o vanilla aceita.
     *
     * <p>O PISO E O VAO SAO ESTRUTURA, e nao botao de arte: abaixo de 0,6 a
     * faisca some em qualquer luz, e acima de 1,5 ela vira mancha. O que
     * a sessao de arte gira e {@code tamanho_de_particula}, no perfil.
     */
    static float tamanhoDe(AuraPerfilVisual perfil, AuraVisualState estado) {
        return 0.6F + 0.9F * estado.intensity() * perfil.tamanhoDeParticula();
    }
}
