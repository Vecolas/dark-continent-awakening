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
            }
        }
        cercamento(blocos, layout.footprint());
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
    }

    private static void portao(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        piso(blocos, modulo, Material.HARDSTAND);
        int esquerda = modulo.x();
        int direita = modulo.x() + modulo.largura() - 1;
        for (int y = 1; y <= 4; y++) {
            adicionar(blocos, esquerda, y, modulo.z(), Material.METAL);
            adicionar(blocos, direita, y, modulo.z(), Material.METAL);
        }
        for (int x = esquerda; x <= direita; x++) {
            adicionar(blocos, x, 4, modulo.z(), Material.METAL);
        }
        toldo(blocos, modulo);
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
            adicionar(blocos, x, y, modulo.z(), material);
            adicionar(blocos, x, y, modulo.z() + modulo.profundidade() - 1, material);
        }
        for (int z = modulo.z() + 1; z < modulo.z() + modulo.profundidade() - 1; z++) {
            adicionar(blocos, modulo.x(), y, z, material);
            adicionar(blocos, modulo.x() + modulo.largura() - 1, y, z, material);
        }
    }

    private static void toldo(List<Placement> blocos, PostoAvancadoLayout.Modulo modulo) {
        int z = Math.min(18, modulo.z() + modulo.profundidade());
        for (int x = modulo.x() + 1; x < modulo.x() + modulo.largura() - 1; x++) {
            adicionar(blocos, x, 3, z, Material.CANVAS);
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
    }

    private static void poste(List<Placement> blocos, int x, int z) {
        adicionar(blocos, x, 0, z, Material.FOUNDATION);
        adicionar(blocos, x, 1, z, Material.METAL);
        adicionar(blocos, x, 2, z, Material.METAL);
    }

    private static void adicionar(List<Placement> blocos, int x, int y, int z, Material material) {
        blocos.add(new Placement(new BlockPos(x, y, z), material));
    }

    public enum Material {
        FOUNDATION, CONCRETE, METAL, CANVAS, PATIO_GRAVEL, WINDOW, WOOD,
        GRATING, HARDSTAND, EQUIPMENT, LIGHT
    }

    public record Placement(BlockPos posicao, Material material) {
        public Placement {
            posicao = posicao.immutable();
            if (material == null) throw new NullPointerException("material do blockout ausente");
        }
    }
}
