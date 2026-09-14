package com.darkcontinent.nenfoundation.enemy.ai.squad;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * UM bando: quem manda, quem esta dentro, qual e o alvo e quanta moral resta.
 *
 * <p><b>Ele guarda UUID, nunca entidade.</b> Um bando sobrevive ao descarregamento
 * do chunk e ao restart; nenhuma referencia de entidade sobrevive a nenhum dos
 * dois. Guardar a entidade tambem impediria o objeto de morrer -- um bando de
 * oito membros seguraria oito mobs mortos vivos na memoria, sem erro algum.</p>
 *
 * <p><b>A morte do lider PROMOVE, e nao dissolve.</b> Dissolver seria mais
 * simples e erraria o comportamento que a issue #143 pede: o jogador que mata o
 * chefe tem de ver a matilha MUDAR, nao sumir. Promover no mesmo tick e o que
 * torna a morte do lider uma decisao tatica em vez de um botao de vitoria.</p>
 *
 * <p><b>O alvo e COMPARTILHADO, e e isso que separa bando de multidao.</b> Oito
 * bichos perseguindo oito alvos diferentes sao oito bichos; oito perseguindo um
 * alvo sao um bando. O alvo mora aqui e nao em cada membro justamente para que
 * eles nao possam divergir -- e a divergencia nao daria erro, daria um cerco que
 * nunca fecha.</p>
 */
public final class Squad {

    /** Moral cheia. Um bando nasce inteiro. */
    public static final int MORAL_MAXIMA = 100;

    private final UUID id;
    private final SquadRules regras;
    private final Map<UUID, SquadRole> membros = new LinkedHashMap<>();

    private UUID lider;
    private UUID alvo;
    private int moral = MORAL_MAXIMA;

    public Squad(UUID id, SquadRules regras, UUID lider) {
        this.id = Objects.requireNonNull(id, "id de squad ausente");
        this.regras = Objects.requireNonNull(regras, "regras de squad ausentes");
        this.lider = Objects.requireNonNull(lider, "squad sem lider: um bando sem ninguem no"
                + " comando nao coordena nada, e a primeira ordem procuraria um lider nulo");
        membros.put(lider, SquadRole.LEADER);
    }

    public UUID id() { return id; }
    public SquadRules regras() { return regras; }
    public Optional<UUID> lider() { return Optional.ofNullable(lider); }
    public Optional<UUID> alvo() { return Optional.ofNullable(alvo); }
    public int moral() { return moral; }
    public int tamanho() { return membros.size(); }
    public Map<UUID, SquadRole> membros() { return Map.copyOf(membros); }
    public Set<UUID> ids() { return Set.copyOf(membros.keySet()); }

    public SquadRole papelDe(UUID membro) {
        return membros.get(Objects.requireNonNull(membro, "membro ausente"));
    }

    public boolean contem(UUID membro) { return membros.containsKey(membro); }

    /**
     * Entra no bando, respeitando o teto.
     *
     * @return false quando o bando esta cheio -- e a recusa importa: sem teto, o
     *         reforco chama reforco e a matilha cresce ate o chunk inteiro
     */
    public boolean entrar(UUID membro, SquadRole papel) {
        Objects.requireNonNull(membro, "membro ausente");
        Objects.requireNonNull(papel, "papel ausente");
        if (papel == SquadRole.LEADER) {
            throw new IllegalArgumentException("nao se entra como LEADER: o lider e promovido"
                    + " por " + Squad.class.getSimpleName() + ", e dois lideres dariam duas"
                    + " ordens diferentes ao mesmo bando");
        }
        if (membros.containsKey(membro)) return false;
        if (membros.size() >= regras.maximoDeMembros()) return false;
        membros.put(membro, papel);
        return true;
    }

    /**
     * Sai do bando -- por morte, unload, dimensao ou fuga.
     *
     * <p>Sair o LIDER promove o proximo na ordem de entrada. A ordem importa: um
     * sorteio faria o mesmo bando promover gente diferente em dois servidores com
     * o mesmo save, e a divergencia so apareceria numa investigacao de
     * dessincronia.</p>
     *
     * @return o novo lider, se houve promocao
     */
    public Optional<UUID> sair(UUID membro) {
        Objects.requireNonNull(membro, "membro ausente");
        if (membros.remove(membro) == null) return Optional.empty();
        if (!membro.equals(lider)) return Optional.empty();

        lider = membros.keySet().stream().findFirst().orElse(null);
        if (lider != null) {
            membros.put(lider, SquadRole.LEADER);
            // Perder o chefe custa moral, e custa DE UMA VEZ. Sem isso a matilha
            // continuaria avancando como se nada tivesse acontecido, e matar o
            // lider deixaria de ser uma decisao tatica.
            moral(moral - CUSTO_MORAL_DA_MORTE_DO_LIDER);
        }
        return Optional.ofNullable(lider);
    }

    /** Quanto a morte do lider tira da moral do bando. */
    private static final int CUSTO_MORAL_DA_MORTE_DO_LIDER = 40;

    /** O alvo do BANDO. Trocar aqui troca para todos, que e o ponto. */
    public void alvo(UUID novoAlvo) { this.alvo = novoAlvo; }

    public void moral(int valor) {
        this.moral = Math.max(0, Math.min(MORAL_MAXIMA, valor));
    }

    /** Machucar um membro abala o bando inteiro, mas pouco. */
    public void abalar(int quanto) {
        if (quanto < 0) throw new IllegalArgumentException("abalo negativo");
        moral(moral - quanto);
    }

    /** O bando quebrou? Abaixo da moral minima ele recua JUNTO. */
    public boolean desmoralizado() { return moral < regras.moralMinima(); }

    /** O bando acabou quando nao ha mais ninguem -- nem lider, nem membro. */
    public boolean dissolvido() { return membros.isEmpty(); }

    /**
     * A leitura que cada membro consome para decidir a propria ordem.
     *
     * <p>Ela e montada AQUI e nao em cada membro: montada por membro, o mesmo
     * bando produziria leituras diferentes no mesmo tick, e dois lobos decidiriam
     * recuar e avancar ao mesmo tempo.</p>
     */
    public SquadInput leituraPara(UUID membro, boolean alvoVisivel, boolean alvoRecuando,
            boolean membroFerido) {
        Objects.requireNonNull(membro, "membro ausente");
        return new SquadInput(alvo, lider != null, alvoVisivel,
                alvoRecuando || desmoralizado(), membroFerido);
    }

    /** Carga do save: substitui o bando inteiro. */
    public void restaurar(UUID lider, Map<UUID, SquadRole> membros, UUID alvo, int moral) {
        Objects.requireNonNull(membros, "membros ausentes");
        this.membros.clear();
        this.membros.putAll(new LinkedHashMap<>(membros));
        this.lider = lider;
        if (lider != null) this.membros.put(lider, SquadRole.LEADER);
        this.alvo = alvo;
        moral(moral);
        Set<UUID> lideres = new LinkedHashSet<>();
        this.membros.forEach((id, papel) -> {
            if (papel == SquadRole.LEADER) lideres.add(id);
        });
        if (lideres.size() > 1) {
            throw new IllegalArgumentException("save com " + lideres.size() + " lideres no mesmo"
                    + " squad: dois lideres dao duas ordens ao mesmo bando, e o sintoma e um"
                    + " cerco que se contradiz sem nenhum erro no log");
        }
    }
}
