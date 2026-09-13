package com.darkcontinent.nenfoundation.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;

/**
 * Blockout estrutural V2. Gera apenas blocos; um template NBT ou worldgen pode
 * consumir a mesma lista quando a revisão visual for aprovada.
 */
public final class PostoAvancadoBlockout {
    private PostoAvancadoBlockout() { }

    public static List<Placement> gerar() {
        PostoAvancadoLayout layout = PostoAvancadoLayout.referencia();
        List<Placement> blocos = new ArrayList<>();
        for (PostoAvancadoLayout.Modulo modulo : layout.modulos()) {
            if (modulo.id().equals("patio_operacional")) {
                piso(blocos, modulo, Material.PATIO_GRAVEL);
                patioEquipamento(blocos, modulo);
            } else if (modulo.id().equals("torre_scaffold")) {
                torre(blocos, modulo, layout.towerHeight());
            } else if (modulo.id().equals("portao_logistico")) {
                portao(blocos, modulo);
            } else {
                modulo(blocos, modulo, layout.buildingHeight());
                interiores(blocos, modulo);
                infraestrutura(blocos, modulo);
                if (modulo.id().equals("laboratorio")) {
                    // O cache fica sob o piso: e uma descoberta opcional, nao a
                    // recompensa que o posto anuncia para quem entra pela frente.
                    adicionar(blocos, modulo.x() + 2, 1, modulo.z() + 2,
                            Material.SECRET_CACHE);
                }
                if (modulo.id().equals("comando")) {
                    simboloAssociacao(blocos, modulo);
                }
            }
        }
        cercamento(blocos, layout.footprint());
        cabosEAntenas(blocos);
        return List.copyOf(blocos);
    }

    /**
     * Aplica o blockout usando o resolvedor de materiais da camada de worldgen.
     * O gerador continua independente de registries, mas pode ser ligado a
     * {@code BlockState} em runtime sem duplicar a geometria.
     */
    public static <T> void aplicar(Function<Material, T> resolvedor,
            BiConsumer<BlockPos, T> consumidor) {
        Objects.requireNonNull(resolvedor, "resolvedor do blockout ausente");
        Objects.requireNonNull(consumidor, "consumidor do blockout ausente");
        for (Placement placement : gerar()) {
            T valor = Objects.requireNonNull(resolvedor.apply(placement.material()),
                    "material sem valor: " + placement.material());
            consumidor.accept(placement.posicao(), valor);
        }
    }

    private static void modulo(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo, int altura) {
        piso(blocos, modulo, Material.FOUNDATION);
        for (int y = 1; y <= altura - 2; y++) {
            borda(blocos, modulo, y, Material.CONCRETE);
        }
        pisoEm(blocos, modulo, altura - 1, Material.METAL);
        for (int x = modulo.x(); x < modulo.x() + modulo.largura(); x++) {
            adicionar(blocos, x, altura - 2, modulo.z(), Material.TRIM);
            adicionar(blocos, x, altura - 2, modulo.z() + modulo.profundidade() - 1, Material.TRIM);
        }
        janelas(blocos, modulo);
        toldo(blocos, modulo);
    }

    private static void janelas(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        int y = 2;
        for (int x = modulo.x() + 2; x < modulo.x() + modulo.largura() - 1; x += 3) {
            adicionar(blocos, x, y, modulo.z(), Material.WINDOW);
            adicionar(blocos, x, y + 1, modulo.z(), Material.WINDOW);
            adicionar(blocos, x, y, modulo.z() + modulo.profundidade() - 1, Material.WINDOW);
            adicionar(blocos, x, y + 1, modulo.z() + modulo.profundidade() - 1, Material.WINDOW);
        }
    }

    private static void interiores(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        int centroX = modulo.x() + modulo.largura() / 2;
        int centroZ = modulo.z() + modulo.profundidade() / 2;
        for (int x = modulo.x() + 2; x < modulo.x() + modulo.largura() - 2; x += 3) {
            adicionar(blocos, x, 1, centroZ, Material.WOOD);
        }
        adicionar(blocos, centroX, 1, centroZ, Material.EQUIPMENT);
        adicionar(blocos, centroX, 2, centroZ, Material.LIGHT);
        for (int z = modulo.z() + 2; z < modulo.z() + modulo.profundidade() - 1; z += 3) {
            adicionar(blocos, modulo.x() + 1, 1, z, Material.EQUIPMENT);
            adicionar(blocos, modulo.x() + modulo.largura() - 2, 1, z, Material.CRATE);
        }
        adicionar(blocos, centroX, 1, modulo.z() + modulo.profundidade() - 2, Material.SIGN);
    }

    private static void infraestrutura(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        int y = 4;
        int centroX = modulo.x() + modulo.largura() / 2;
        adicionar(blocos, centroX, y, modulo.z() - 1, Material.LIGHT);
        adicionar(blocos, modulo.x() + 1, 1, modulo.z() - 1, Material.CRATE);
        adicionar(blocos, modulo.x() + modulo.largura() - 2, 1,
                modulo.z() + modulo.profundidade(), Material.CRATE);
        for (int x = modulo.x() + 1; x < modulo.x() + modulo.largura() - 1; x += 2) {
            adicionar(blocos, x, y, modulo.z() + modulo.profundidade() - 1, Material.LIGHT);
        }
    }

    /** Emblema discreto em pixels: dois X e um losango entre eles. */
    private static void simboloAssociacao(List<Placement> blocos,
            PostoAvancadoLayout.Modulo modulo) {
        int centro = modulo.x() + modulo.largura() / 2;
        int frente = modulo.z();
        int y = 3;
        int[][] pixels = {
            {-4, 0}, {-2, 0}, {-3, 1}, {-4, 2}, {-2, 2},
            {0, 0}, {0, 1}, {0, 2},
            {2, 0}, {4, 0}, {3, 1}, {2, 2}, {4, 2}
        };
        for (int[] pixel : pixels) {
            adicionar(blocos, centro + pixel[0], y + pixel[1], frente,
                    Material.EMBLEM_RED);
        }
    }

    private static void portao(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        piso(blocos, modulo, Material.HARDSTAND);
        int esquerda = modulo.x();
        int direita = modulo.x() + modulo.largura() - 1;
        int frente = modulo.z() + modulo.profundidade() - 1;
        for (int y = 1; y <= 4; y++) {
            adicionar(blocos, esquerda, y, frente, Material.METAL);
            adicionar(blocos, direita, y, frente, Material.METAL);
        }
        for (int x = esquerda; x <= direita; x++) {
            adicionar(blocos, x, 4, frente, Material.METAL);
        }
        for (int x = esquerda + 1; x < direita; x++) {
            adicionar(blocos, x, 1, frente, Material.GATE);
            adicionar(blocos, x, 2, frente, Material.GATE);
        }
        adicionar(blocos, esquerda, 4, frente, Material.LIGHT);
        adicionar(blocos, direita, 4, frente, Material.LIGHT);
        adicionar(blocos, 0, 3, frente, Material.SIGN);
        toldo(blocos, modulo, frente);
    }

    private static void patioEquipamento(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        for (int x = modulo.x() + 1; x <= modulo.x() + 4; x++) {
            for (int z = modulo.z() + 1; z <= modulo.z() + 3; z++) {
                adicionar(blocos, x, 1, z, Material.HARDSTAND);
            }
        }
        for (int x = modulo.x() + modulo.largura() - 5; x < modulo.x() + modulo.largura() - 1; x++) {
            adicionar(blocos, x, 1, modulo.z() + modulo.profundidade() - 2, Material.EQUIPMENT);
        }
        for (int x = modulo.x() + 2; x < modulo.x() + modulo.largura() - 2; x += 3) {
            adicionar(blocos, x, 2, modulo.z() + 4, Material.LIGHT);
        }
    }

    private static void piso(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo, Material material) {
        pisoEm(blocos, modulo, 0, material);
    }

    private static void pisoEm(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo, int y,
            Material material) {
        for (int x = modulo.x(); x < modulo.x() + modulo.largura(); x++) {
            for (int z = modulo.z(); z < modulo.z() + modulo.profundidade(); z++) {
                adicionar(blocos, x, y, z, material);
            }
        }
    }

    private static void borda(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo, int y,
            Material material) {
        for (int x = modulo.x(); x < modulo.x() + modulo.largura(); x++) {
            if (!ehEntrada(modulo, x, y)) {
                adicionar(blocos, x, y, modulo.z(), material);
            }
            adicionar(blocos, x, y, modulo.z() + modulo.profundidade() - 1, material);
        }
        for (int z = modulo.z() + 1; z < modulo.z() + modulo.profundidade() - 1; z++) {
            adicionar(blocos, modulo.x(), y, z, material);
            adicionar(blocos, modulo.x() + modulo.largura() - 1, y, z, material);
        }
    }

    private static boolean ehEntrada(PostoAvancadoLayout.Modulo modulo, int x, int y) {
        if (y > 3 || modulo.profundidade() < 3) {
            return false;
        }
        int centro = modulo.x() + modulo.largura() / 2;
        return x >= centro - 1 && x <= centro + 1;
    }

    private static void toldo(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        int z = Math.min(18, modulo.z() + modulo.profundidade());
        toldo(blocos, modulo, z);
    }

    private static void toldo(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo, int z) {
        for (int x = modulo.x() + 1; x < modulo.x() + modulo.largura() - 1; x++) {
            adicionar(blocos, x, 3, z, Material.CANVAS);
            // Iron bars pertencem somente ao cercamento e ao portao. O suporte
            // do toldo e madeira para nao espalhar a linguagem da cerca pelos
            // modulos internos.
            adicionar(blocos, x, 2, z, Material.WOOD);
        }
    }

    private static void torre(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo, int altura) {
        int x1 = modulo.x();
        int x2 = modulo.x() + modulo.largura() - 1;
        int z1 = modulo.z();
        int z2 = modulo.z() + modulo.profundidade() - 1;
        for (int y = 0; y < altura; y++) {
            adicionar(blocos, x1, y, z1, Material.METAL);
            adicionar(blocos, x1, y, z2, Material.METAL);
            adicionar(blocos, x2, y, z1, Material.METAL);
            adicionar(blocos, x2, y, z2, Material.METAL);
        }
        for (int y : new int[] { 4, 8, 12 }) {
            pisoEm(blocos, new PostoAvancadoLayout.Modulo("plataforma", x1, z1,
                    modulo.largura(), modulo.profundidade()), y, Material.GRATING);
            for (int x = x1; x <= x2; x++) {
                adicionar(blocos, x, y + 1, z1, Material.METAL);
                adicionar(blocos, x, y + 1, z2, Material.METAL);
            }
        }
        pisoEm(blocos, new PostoAvancadoLayout.Modulo("topo", x1, z1,
                modulo.largura(), modulo.profundidade()), altura - 1,
                Material.CANVAS);
    }

    private static void cercamento(List<Placement> blocos, int lado) {
        int metade = lado / 2;
        for (int x = -metade; x <= metade; x++) {
            if (Math.abs(x) > 4) {
                poste(blocos, x, -metade);
                poste(blocos, x, metade);
            }
        }
        for (int z = -metade + 1; z < metade; z++) {
            poste(blocos, -metade, z);
            poste(blocos, metade, z);
        }
        for (int x = -metade + 2; x < metade - 1; x += 3) {
            painelHorizontal(blocos, x, -metade);
            painelHorizontal(blocos, x, metade);
        }
        for (int z = -metade + 2; z < metade - 1; z += 3) {
            painelVertical(blocos, -metade, z);
            painelVertical(blocos, metade, z);
        }
        // Coroa continua de smooth stone sobre a malha, sem ocupar a abertura
        // frontal reservada ao portao.
        for (int x = -metade; x <= metade; x++) {
            if (Math.abs(x) > 4) {
                adicionar(blocos, x, 4, -metade, Material.FOUNDATION);
                adicionar(blocos, x, 4, metade, Material.FOUNDATION);
            }
        }
        for (int z = -metade + 1; z < metade; z++) {
            adicionar(blocos, -metade, 4, z, Material.FOUNDATION);
            adicionar(blocos, metade, 4, z, Material.FOUNDATION);
        }
    }

    private static void painelHorizontal(List<Placement> blocos, int x, int z) {
        for (int i = 1; i <= 2; i++) {
            adicionar(blocos, x, i, z, Material.FRAME);
        }
    }

    private static void painelVertical(List<Placement> blocos, int x, int z) {
        for (int i = 1; i <= 2; i++) {
            adicionar(blocos, x, i, z, Material.FRAME);
        }
    }

    private static void poste(List<Placement> blocos, int x, int z) {
        // A base ancora a cerca no terreno; acima dela o perimetro e iron bars.
        adicionar(blocos, x, 0, z, Material.FOUNDATION);
        for (int y = 1; y <= 3; y++) {
            adicionar(blocos, x, y, z, Material.FRAME);
        }
    }

    private static void cabosEAntenas(List<Placement> blocos) {
        for (int z = -10; z <= 2; z++) {
            adicionar(blocos, 4, 10, z, Material.CABLE);
        }
        for (int x = 4; x <= 13; x++) {
            adicionar(blocos, x, 10, 2, Material.CABLE);
        }
        for (int y = 11; y <= 13; y++) {
            adicionar(blocos, 0, y, -14, Material.ANTENNA);
        }
    }

    private static void adicionar(List<Placement> blocos, int x, int y, int z, Material material) {
        BlockPos posicao = new BlockPos(x, y, z);
        Placement placement = new Placement(posicao, material);
        for (int indice = 0; indice < blocos.size(); indice++) {
            if (blocos.get(indice).posicao().equals(posicao)) {
                // A camada mais específica (janela, guarnição ou equipamento) vence
                // a parede/base emitida antes dela, sem deixar duas verdades para o
                // consumidor que transforma o blockout em BlockState.
                blocos.set(indice, placement);
                return;
            }
        }
        blocos.add(placement);
    }

    public enum Material {
        FOUNDATION, CONCRETE, METAL, CANVAS, PATIO_GRAVEL, WINDOW, WOOD,
        GRATING, HARDSTAND, EQUIPMENT, LIGHT, TRIM, FRAME, CRATE, SIGN, GATE,
        CABLE, ANTENNA, SECRET_CACHE, EMBLEM_RED
    }

    public record Placement(BlockPos posicao, Material material) {
        public Placement {
            posicao = posicao.immutable();
            if (material == null) throw new NullPointerException("material do blockout ausente");
        }
    }
}
