package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A trava contra recompensa paga duas vezes.
 *
 * <p><b>A chave e {@code encounterId + rewardId}, e a escolha e o sistema
 * inteiro.</b> Travar so por {@code rewardId} impediria o jogador de ganhar a
 * mesma recompensa em OUTRO encontro -- o card do Cyclops so cairia uma vez na
 * vida. Travar so por {@code encounterId} impediria um encontro de pagar duas
 * recompensas DIFERENTES. E o par que responde a pergunta certa: <em>este
 * episodio ja pagou esta recompensa?</em></p>
 *
 * <p><b>Por que isto e persistente e nao um {@code Set} de runtime.</b> A corrida
 * que ele impede e justamente a que sobrevive ao restart: dois jogadores matam o
 * alvo no mesmo tick, o servidor cai antes de salvar, e ao voltar o encontro
 * paga de novo. Em memoria, a trava morre com o processo e o dupe volta em todo
 * restart -- sem erro, e com o item na mao de quem o recebeu.</p>
 *
 * <p><b>Ela e monotona.</b> Uma trava so e removida por reset administrativo
 * explicito, nunca por expiracao. Expirar automaticamente e a mesma coisa que
 * nao travar, so que mais tarde.</p>
 */
public final class RewardLedger {

    private final Set<String> travas = new LinkedHashSet<>();

    /**
     * A chave de transacao. Formato estavel porque ela vai para o SAVE.
     *
     * <p>Mudar este formato invalida travas existentes em silencio: as antigas
     * deixam de casar, e todo encontro ja concluido volta a poder pagar. Se um
     * dia precisar mudar, a mudanca exige degrau de migracao, como qualquer
     * schema de save.</p>
     */
    public static String chave(UUID encounterId, String rewardId) {
        Objects.requireNonNull(encounterId, "encontro ausente");
        if (rewardId == null || rewardId.isBlank()) {
            throw new IllegalArgumentException("recompensa sem id: a trava viraria uma chave"
                    + " generica que casa com tudo, e o primeiro pagamento bloquearia todos"
                    + " os outros deste encontro");
        }
        return encounterId + "/" + rewardId;
    }

    /**
     * Tenta travar. {@code true} significa <b>pague agora</b>; {@code false}
     * significa <b>ja foi pago, nao pague de novo</b>.
     *
     * <p>E uma operacao so, e nao "consultar depois gravar". Entre a consulta e a
     * gravacao cabe o outro jogador -- e essa e exatamente a corrida que a issue
     * #142 descreve. Uma unica chamada nao tem esse meio.</p>
     */
    public boolean travar(UUID encounterId, String rewardId) {
        return travas.add(chave(encounterId, rewardId));
    }

    public boolean jaPago(UUID encounterId, String rewardId) {
        return travas.contains(chave(encounterId, rewardId));
    }

    /**
     * Reset ADMINISTRATIVO de um encontro inteiro.
     *
     * <p>Ela existe para o operador poder consertar um encontro quebrado, e por
     * isso e a unica forma de tirar uma trava. Automatizar isto -- por tempo, por
     * troca de mundo, por qualquer gatilho -- transformaria a trava em atraso.</p>
     *
     * @return quantas travas cairam
     */
    public int limparEncontro(UUID encounterId) {
        Objects.requireNonNull(encounterId, "encontro ausente");
        String prefixo = encounterId + "/";
        int antes = travas.size();
        travas.removeIf(chave -> chave.startsWith(prefixo));
        return antes - travas.size();
    }

    public int tamanho() { return travas.size(); }

    /** Snapshot imutavel, para o save. */
    public Set<String> chaves() { return Set.copyOf(travas); }

    /** Carga a partir do save; substitui o conteudo inteiro. */
    public void carregar(Collection<String> chaves) {
        Objects.requireNonNull(chaves, "chaves ausentes");
        travas.clear();
        for (String chave : chaves) {
            if (chave == null || chave.isBlank() || chave.indexOf('/') <= 0) {
                throw new IllegalArgumentException("trava de recompensa invalida no save: '"
                        + chave + "'. Aceita-la deixaria uma chave que nunca casa com nada,"
                        + " e o encontro correspondente voltaria a poder pagar.");
            }
            travas.add(chave);
        }
    }
}
