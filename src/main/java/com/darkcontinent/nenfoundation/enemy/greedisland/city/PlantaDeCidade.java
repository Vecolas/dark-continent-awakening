package com.darkcontinent.nenfoundation.enemy.greedisland.city;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import java.util.ArrayList;
import java.util.List;

/**
 * O arranjo interno de uma cidade, calculado e nao desenhado. Secao 47.
 *
 * <p>O pipeline do documento e {@code CityAnchor -> DistrictGraph ->
 * RoadSkeleton -> Parcels -> jigsaw}, e esta classe e os tres do meio. Ela
 * responde, para qualquer bloco: isto e rua, lote, praca ou campo?
 *
 * <p><b>DETERMINISTICA, e sem seed de mundo.</b> Masadora precisa ser a mesma
 * Masadora em todo servidor -- inclusive por dentro. Um jogador que aprendeu
 * onde fica o Spell Card Hall tem de achar o mesmo predio em outro servidor,
 * senao o conhecimento que a ilha existe para construir nao vale nada fora
 * dali.
 *
 * <p><b>O NUCLEO FICA NO CENTRO; O RESTO ORBITA.</b> Os distritos sao
 * distribuidos em anel a partir do segundo, com o raio crescendo conforme a
 * lista avanca -- e a lista e ordenada de proposito em
 * {@link RegistroDeCidades}. Sem ordem, o mercado externo de Masadora teria a
 * mesma chance de cair no meio que o Spell Card Hall.
 *
 * <p>SEM MINECRAFT: o portao precisa varrer a planta de oito cidades para
 * conferir que nenhuma rua fica sem lote e nenhum lote fica sem rua.
 */
public final class PlantaDeCidade {

    /** Meia largura da avenida que corta a cidade. */
    private static final int MEIA_AVENIDA = 4;

    /** Meia largura das ruas de distrito. */
    private static final int MEIA_RUA = 2;

    /** O lado do quarteirao: de quanto em quanto nasce uma rua. */
    private static final int QUARTEIRAO = 24;

    /** Quanto a praca do nucleo ocupa. */
    private static final int PRACA = 22;

    private PlantaDeCidade() {
    }

    /** O que existe num bloco da cidade. */
    public enum Uso {
        /** Fora da pegada. */
        FORA,
        /** A praca central, aberta. */
        PRACA,
        /** A avenida principal. */
        AVENIDA,
        /** Rua de quarteirao. */
        RUA,
        /** Lote construivel. */
        LOTE,
        /** Area aberta dentro da cidade: parque, campo, patio. */
        ABERTO
    }

    /**
     * O uso de um bloco.
     *
     * <p>A ORDEM DAS PERGUNTAS E A REGRA: praca, depois avenida, depois rua,
     * depois lote. Invertida, um lote cairia em cima da avenida e a cidade
     * nasceria sem por onde andar.
     */
    public static Uso usoEm(DefinicaoDeCidade cidade, int x, int z) {
        int dx = x - cidade.ancora().x();
        int dz = z - cidade.ancora().z();
        if (Math.abs(dx) > cidade.larguraX() / 2 || Math.abs(dz) > cidade.larguraZ() / 2) {
            return Uso.FORA;
        }
        if (Math.abs(dx) <= PRACA && Math.abs(dz) <= PRACA) {
            return Uso.PRACA;
        }
        if (Math.abs(dz) <= MEIA_AVENIDA || Math.abs(dx) <= MEIA_AVENIDA) {
            return Uso.AVENIDA;
        }
        if (Math.floorMod(dx, QUARTEIRAO) <= MEIA_RUA
                || Math.floorMod(dz, QUARTEIRAO) <= MEIA_RUA) {
            return Uso.RUA;
        }
        // O ARRABALDE FICA ABERTO. A secao 46 diz que a cidade nao precisa
        // estar cheia de predios: praca, parque, campo e muralha fazem parte.
        // Sem isso, uma pegada de 1.200x1.500 vira um tabuleiro de casas.
        double fracaoDoRaio = Math.max(Math.abs(dx) / (cidade.larguraX() / 2.0D),
                Math.abs(dz) / (cidade.larguraZ() / 2.0D));
        if (fracaoDoRaio > 0.72D) {
            return Uso.ABERTO;
        }
        return Uso.LOTE;
    }

    /**
     * Qual distrito cobre este bloco.
     *
     * <p>O NUCLEO NO CENTRO, os outros em anel. O indice devolvido e o da
     * lista de {@link DefinicaoDeCidade#distritos()}.
     */
    public static int distritoEm(DefinicaoDeCidade cidade, int x, int z) {
        List<Ponto> centros = centrosDeDistrito(cidade);
        int melhor = 0;
        double menor = Double.MAX_VALUE;
        for (int i = 0; i < centros.size(); i++) {
            double d = Math.hypot(x - centros.get(i).x(), z - centros.get(i).z());
            if (d < menor) {
                menor = d;
                melhor = i;
            }
        }
        return melhor;
    }

    /**
     * Onde cada distrito fica.
     *
     * <p>O ANEL USA O ANGULO DOURADO, e nao uma divisao igual do circulo: com
     * divisao igual, cidades de contagens diferentes ficam com o mesmo desenho
     * girado, e as oito passam a parecer a mesma planta. O angulo dourado
     * distribui sem repetir padrao.
     */
    public static List<Ponto> centrosDeDistrito(DefinicaoDeCidade cidade) {
        List<Ponto> centros = new ArrayList<>();
        centros.add(cidade.ancora());

        int quantos = cidade.distritos().size() - 1;
        double raioBase = Math.min(cidade.larguraX(), cidade.larguraZ()) * 0.30D;
        for (int i = 0; i < quantos; i++) {
            double angulo = i * 2.399963D;
            double raio = raioBase * (1.0D + 0.45D * (i / (double) Math.max(1, quantos)));
            centros.add(new Ponto(
                    cidade.ancora().x() + (int) Math.round(Math.cos(angulo) * raio),
                    cidade.ancora().z() + (int) Math.round(Math.sin(angulo) * raio)));
        }
        return centros;
    }

    /**
     * A altura em que a cidade assenta.
     *
     * <p><b>UMA COTA SO PARA A CIDADE INTEIRA</b>, e ela e a MEDIA do terreno
     * sob a pegada. Seguir o relevo bloco a bloco faria rua em degrau e casa
     * meio enterrada; uma cota unica exige aterro, e a secao 48 pede adaptacao
     * suave -- que e o que a zona de aproximacao resolve.
     */
    /**
     * As cotas ja calculadas, uma por cidade.
     *
     * <p><b>ESTA FALTA MATOU O SERVIDOR.</b> {@code cotaDe} varre a pegada
     * inteira amostrando elevacao -- quase mil e oitocentas amostras em
     * Limeiro --, e a feature a chamava POR BLOCO: 256 vezes por chunk. Com a
     * elevacao custando centenas de contas cada, deu um tick de 60 segundos e
     * o watchdog derrubou o servidor.
     *
     * <p>A cota nao muda: ela sai de constantes. Recalcular era refazer, a
     * cada bloco, uma conta cujo resultado ja se sabia.
     *
     * <p>O TESTE DE PERFORMANCE NAO PEGOU porque media {@code alturaEm}, e
     * nao {@code cotaDe}. Medir a funcao barata e concluir que o sistema e
     * barato e a forma mais comum de falso verde em performance.
     */
    private static final java.util.Map<String, Integer> COTAS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** A cota da cidade, calculada uma vez. */
    public static int cotaDe(DefinicaoDeCidade cidade) {
        return COTAS.computeIfAbsent(cidade.id(), id -> calcularCota(cidade));
    }

    private static int calcularCota(DefinicaoDeCidade cidade) {
        long soma = 0;
        int n = 0;
        for (int dx = -cidade.larguraX() / 2; dx <= cidade.larguraX() / 2; dx += 32) {
            for (int dz = -cidade.larguraZ() / 2; dz <= cidade.larguraZ() / 2; dz += 32) {
                soma += com.darkcontinent.nenfoundation.enemy.greedisland.layout
                        .GreedIslandElevationField.alturaEm(
                                cidade.ancora().x() + dx, cidade.ancora().z() + dz);
                n++;
            }
        }
        return (int) Math.round(soma / (double) n);
    }

    /**
     * A altura da construcao de um lote.
     *
     * <p>DERIVADA DA POSICAO, e nao sorteada: sorteio com {@code Random} daria
     * cidades diferentes em servidores diferentes, e a regra de ouro do
     * documento e que Masadora seja a mesma em todo lugar.
     */
    public static int alturaDoPredio(DefinicaoDeCidade cidade, int x, int z) {
        int base = switch (cidade.papel()) {
            case CAPITAL -> 9;
            case CARTAS, JOGO -> 8;
            case PORTO, SOCIAL, HUB_INICIAL -> 6;
            case HUB_COMERCIAL -> 5;
            case ENTRADA -> 4;
        };
        int variacao = Math.floorMod(x * 31 + z * 17, 4);
        // MAIS ALTO PERTO DO NUCLEO. Uma cidade de altura uniforme le como
        // conjunto habitacional; a silhueta que sobe para o centro e o que faz
        // ela parecer ter crescido em volta de alguma coisa.
        double perto = 1.0D - Math.min(1.0D, Math.hypot(x - cidade.ancora().x(),
                z - cidade.ancora().z()) / cidade.raio());
        return base + variacao + (int) Math.round(perto * 4);
    }
}
