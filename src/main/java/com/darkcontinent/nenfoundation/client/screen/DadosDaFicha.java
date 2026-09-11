package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Projecao somente-leitura dos desbloqueios recebidos. Nunca consulta registros
 * de servidor: um id concedido por debug tambem precisa aparecer na ficha.
 * A lista ordenada e refeita quando o snapshot muda, inclusive no logout.
 */
public final class DadosDaFicha {
    private final NenClientCache cache;
    private SnapshotDePerfilS2C ultimoPerfil;
    private List<Entrada> entradas = List.of();

    public DadosDaFicha(NenClientCache cache) {
        this.cache = cache;
    }

    public Optional<SnapshotDePerfilS2C> perfil() {
        return this.cache.snapshot();
    }

    public List<Entrada> entradas() {
        SnapshotDePerfilS2C atual = perfil().orElse(null);
        if (atual != this.ultimoPerfil) {
            List<Entrada> novas = new ArrayList<>();
            if (atual != null) {
                atual.tecnicasDesbloqueadas().forEach(id -> novas.add(new Entrada(Tipo.TECNICA, id)));
                atual.habilidadesDesbloqueadas().forEach(id -> novas.add(new Entrada(Tipo.HABILIDADE, id)));
                atual.marcos().forEach(id -> novas.add(new Entrada(Tipo.MARCO, id)));
            }
            novas.sort(Comparator.comparing(Entrada::tipo).thenComparing(e -> e.id().toString()));
            this.entradas = List.copyOf(novas);
            this.ultimoPerfil = atual;
        }
        return this.entradas;
    }

    public enum Tipo {
        TECNICA("nenfoundation.ficha.tecnica"),
        HABILIDADE("nenfoundation.ficha.habilidade"),
        MARCO("nenfoundation.ficha.marco");

        private final String chave;

        Tipo(String chave) { this.chave = chave; }
        public String chave() { return this.chave; }
    }

    public record Entrada(Tipo tipo, ResourceLocation id) { }
}
