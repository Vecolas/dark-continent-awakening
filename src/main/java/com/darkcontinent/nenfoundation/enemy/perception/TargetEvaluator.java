package com.darkcontinent.nenfoundation.enemy.perception;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Escolhe UM alvo entre candidatos ja medidos, respeitando faccao.
 *
 * <p><b>Por que a faccao entra aqui e nao no MobType vanilla:</b> {@code MobType}
 * responde "isto e morto-vivo?", e a pergunta deste mod e outra -- "esta criatura
 * e presa, aliada ou inimiga DESTA aqui?". A resposta muda por par, e nao por
 * especie. Decidir por {@code instanceof Player} espalha a regra por cada mob e
 * garante que o decimo esqueca um caso; decidir por {@link FactionRelations}
 * deixa a regra num lugar so e permite CUSTOM sem {@code switch} novo.</p>
 *
 * <p><b>Relacao ausente e NEUTRAL, e isso e declarado.</b> Um par que ninguem
 * escreveu nao vira hostil por acidente -- fauna nao ataca o mundo inteiro so
 * porque o autor esqueceu uma linha. O preco esta assumido: um inimigo que
 * DEVERIA ser hostil e nao foi declarado fica passivo, e o sintoma e um mob
 * pacifico demais, nao um massacre. Dos dois erros, este e o barato.</p>
 */
public final class TargetEvaluator {
    private final FactionRelations relacoes;
    private final EnemyFaction faccaoDoMob;
    private final double alcanceMaximo;
    private final boolean territorial;

    public TargetEvaluator(FactionRelations relacoes, EnemyFaction faccaoDoMob,
            double alcanceMaximo, boolean territorial) {
        this.relacoes = Objects.requireNonNull(relacoes, "relacoes ausentes");
        this.faccaoDoMob = Objects.requireNonNull(faccaoDoMob, "faccao do mob ausente");
        if (!Double.isFinite(alcanceMaximo) || alcanceMaximo <= 0.0D) {
            throw new IllegalArgumentException("alcance de avaliacao invalido");
        }
        this.alcanceMaximo = alcanceMaximo;
        this.territorial = territorial;
    }

    /** Relacao desta criatura com a faccao do candidato. */
    public FactionRelation relacaoCom(EnemyFaction outra) {
        return relacoes.relation(faccaoDoMob, outra);
    }

    /**
     * Um candidato so e alvo quando a relacao autoriza E a geometria permite.
     *
     * <p>Territorio nao TORNA hostil: ele so autoriza o mob territorial a
     * responder a quem entrou. Confundir os dois faria a spider eagle caçar
     * pelo canyon inteiro -- e o encontro que ela ensina e justamente "saia da
     * area e nada acontece".</p>
     */
    public boolean elegivel(TargetCandidate candidato) {
        Objects.requireNonNull(candidato, "candidato ausente");
        if (!candidato.mesmaDimensao() || candidato.distancia() > alcanceMaximo) return false;
        if (candidato.jaFeriuOMob()) return true;
        FactionRelation relacao = relacaoCom(candidato.faccao());
        if (relacao == FactionRelation.ALLY || relacao == FactionRelation.NEUTRAL) {
            // Neutro so vira alvo quando invade territorio de quem tem territorio.
            return territorial && candidato.dentroDoTerritorio();
        }
        return true;
    }

    /**
     * Melhor alvo entre os elegiveis, por PRIORIDADE e depois por distancia.
     *
     * <p>Quem feriu o mob vem primeiro, depois presa e hostil, depois o resto.
     * Ordenar so por distancia faria o bicho largar quem o esta atacando para
     * perseguir um passante mais perto -- sem erro, e com cara de IA burra.</p>
     */
    public Optional<TargetCandidate> melhor(Collection<TargetCandidate> candidatos) {
        Objects.requireNonNull(candidatos, "candidatos ausentes");
        return candidatos.stream()
                .filter(Objects::nonNull)
                .filter(this::elegivel)
                .min(Comparator.<TargetCandidate>comparingInt(this::prioridade)
                        .thenComparingDouble(TargetCandidate::distancia));
    }

    /** Menor e melhor. Nao e balanceamento: e ordem de decisao. */
    private int prioridade(TargetCandidate candidato) {
        if (candidato.jaFeriuOMob()) return 0;
        return switch (relacaoCom(candidato.faccao())) {
            case HOSTILE -> 1;
            case PREY -> 2;
            case NEUTRAL -> 3;
            case ALLY -> 4;
        };
    }
}
