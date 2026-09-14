package com.darkcontinent.nenfoundation.enemy.ai.squad;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Os bandos vivos de um servidor -- e a faxina que impede bando fantasma.
 *
 * <p><b>O vazamento que ele fecha nao da erro.</b> Um bando cujo ultimo membro
 * morreu continua existindo no mapa, com alvo, moral e id. Ele nao consome tick
 * e nao aparece em lugar nenhum -- so ocupa memoria e, pior, pode receber um
 * membro novo mais tarde e "ressuscitar" com o alvo de um combate que acabou ha
 * uma hora. Por isso {@link #removerDissolvidos()} existe e e chamada no mesmo
 * ritmo da coordenacao, e nao "quando der".</p>
 *
 * <p><b>Um membro pertence a UM bando.</b> Sem essa regra, um lobo poderia ser
 * flanker de uma matilha e frontliner de outra ao mesmo tempo, recebendo duas
 * ordens por tick -- e a que chegasse por ultimo venceria, o que faz o bicho
 * parecer indeciso sem nenhuma causa visivel.</p>
 */
public final class SquadRegistry {

    private final Map<UUID, Squad> bandos = new LinkedHashMap<>();
    /** Membro -> bando. E o indice que torna "um membro, um bando" cobravel. */
    private final Map<UUID, UUID> bandoDoMembro = new LinkedHashMap<>();

    public Optional<Squad> bando(UUID squadId) {
        return Optional.ofNullable(bandos.get(Objects.requireNonNull(squadId, "id ausente")));
    }

    public Optional<Squad> bandoDe(UUID membro) {
        UUID id = bandoDoMembro.get(Objects.requireNonNull(membro, "membro ausente"));
        return id == null ? Optional.empty() : bando(id);
    }

    public int quantidade() { return bandos.size(); }

    /** Cria um bando novo com o lider dado. */
    public Squad criar(UUID squadId, SquadRules regras, UUID lider) {
        if (bandos.containsKey(squadId)) {
            throw new IllegalStateException("squad duplicado: " + squadId + ". Dois bandos com o"
                    + " mesmo id competiriam pelos mesmos membros, e o segundo roubaria em"
                    + " silencio os do primeiro.");
        }
        exigirLivre(lider);
        Squad bando = new Squad(squadId, regras, lider);
        bandos.put(squadId, bando);
        bandoDoMembro.put(lider, squadId);
        return bando;
    }

    /** Entra num bando existente; recusa quem ja pertence a outro. */
    public boolean entrar(UUID squadId, UUID membro, SquadRole papel) {
        Squad bando = bandos.get(Objects.requireNonNull(squadId, "id ausente"));
        if (bando == null) return false;
        exigirLivre(membro);
        if (!bando.entrar(membro, papel)) return false;
        bandoDoMembro.put(membro, squadId);
        return true;
    }

    /**
     * Sai do bando por qualquer motivo -- morte, unload, dimensao, fuga.
     *
     * <p>UM ponto de saida. Espalhar a remocao pelos eventos que a disparam e
     * como o indice {@code bandoDoMembro} fica com entrada morta: o mob morreu, o
     * bando o esqueceu, e o indice continua dizendo que ele pertence a alguem --
     * entao ele nunca mais entra em bando nenhum.</p>
     */
    public void sair(UUID membro) {
        UUID squadId = bandoDoMembro.remove(Objects.requireNonNull(membro, "membro ausente"));
        if (squadId == null) return;
        Squad bando = bandos.get(squadId);
        if (bando != null) bando.sair(membro);
    }

    /**
     * Faxina: tira do mapa os bandos sem ninguem.
     *
     * @return quantos foram removidos
     */
    public int removerDissolvidos() {
        int antes = bandos.size();
        bandos.values().removeIf(Squad::dissolvido);
        // O indice tambem: uma entrada apontando para bando removido faria o
        // membro ser recusado em todo bando futuro, para sempre.
        bandoDoMembro.values().removeIf(id -> !bandos.containsKey(id));
        return antes - bandos.size();
    }

    /** Apaga tudo -- fim de servidor, troca de mundo, reset administrativo. */
    public void limpar() {
        bandos.clear();
        bandoDoMembro.clear();
    }

    private void exigirLivre(UUID membro) {
        UUID atual = bandoDoMembro.get(Objects.requireNonNull(membro, "membro ausente"));
        if (atual != null) {
            throw new IllegalStateException(membro + " ja pertence ao squad " + atual
                    + ". Pertencer a dois bandos daria duas ordens por tick ao mesmo bicho, e a"
                    + " ultima venceria -- o que aparece como um mob indeciso, sem causa visivel.");
        }
    }
}
