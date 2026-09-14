package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.enemy.encounter.EncounterInstance;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterState;
import com.darkcontinent.nenfoundation.enemy.encounter.RewardLedger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * Converte um desfecho em card -- exatamente uma vez, e nunca por dois caminhos.
 *
 * <p><b>A corrida que este servico existe para perder e concreta:</b> dois
 * jogadores acertam o golpe final no mesmo tick. Os dois veem o bicho cair, os
 * dois tem direito de pedir o card, e o servidor tem de pagar UM. Se a checagem e
 * o pagamento forem duas linhas -- "ja pagou? entao pague" -- o segundo jogador
 * entra entre elas, e o item sai em dobro sem erro nenhum: as duas transacoes sao
 * legitimas, so que a mesma.</p>
 *
 * <p><b>A trava e a da fundacao de encontro, e nao uma segunda.</b> Um ledger
 * proprio aqui pareceria mais limpo e seria a duplicata que o sistema inteiro
 * existe para impedir: com dois registros, um reset administrativo limparia um e
 * deixaria o outro, e o card voltaria a poder sair. Uma verdade, um lugar.</p>
 *
 * <p><b>O limite de copias e cobrado no mesmo ato.</b> Emitir primeiro e contar
 * depois deixaria a ultima copia sair duas vezes -- e, num sistema cujo ponto e
 * a escassez, a copia excedente e o bug.</p>
 */
public final class CardConversionService {

    private final RewardLedger ledger;
    private final Map<ResourceLocation, CardSpec> catalogo;
    /** Copias ja emitidas por card. Contagem do MUNDO, nao do jogador. */
    private final Map<ResourceLocation, Integer> emitidas = new HashMap<>();

    public CardConversionService(RewardLedger ledger, Map<ResourceLocation, CardSpec> catalogo) {
        this.ledger = Objects.requireNonNull(ledger, "ledger ausente");
        Objects.requireNonNull(catalogo, "catalogo de cards ausente");
        catalogo.forEach((id, spec) -> {
            if (spec == null) throw new IllegalArgumentException("card nulo em " + id);
            if (!spec.monsterId().equals(id)) {
                throw new IllegalArgumentException("a chave '" + id + "' aponta para o card de '"
                        + spec.monsterId() + "': a divergencia so apareceria como um card que"
                        + " nao casa com o bicho que o soltou, e a colecao nunca fecharia");
            }
        });
        this.catalogo = Map.copyOf(catalogo);
    }

    public Map<ResourceLocation, CardSpec> catalogo() { return catalogo; }

    public int emitidas(ResourceLocation monsterId) {
        return emitidas.getOrDefault(Objects.requireNonNull(monsterId, "criatura ausente"), 0);
    }

    /**
     * A UNICA operacao que emite card.
     *
     * <p>Checagem e emissao acontecem na mesma chamada de proposito -- ver o
     * javadoc da classe. Quem chamar isto duas vezes para o mesmo episodio recebe
     * {@link Optional#empty()} na segunda, e essa resposta significa <b>nao
     * pague</b>, e nao "deu erro".</p>
     *
     * @param encontro o episodio; tem de estar COMPLETED
     * @param monsterId a criatura convertida
     * @param resultado como ela saiu de cena
     * @return a ficha do card quando o chamador DEVE entregar um; vazio quando nao
     */
    public Optional<CardSpec> converter(EncounterInstance encontro, ResourceLocation monsterId,
            DefeatResult resultado) {
        return converterInterno(encontro, monsterId, null, resultado);
    }

    /**
     * Converte uma entidade especifica. O UUID faz parte da trava para que uma
     * matilha possa emitir um card por criatura, sem abrir uma segunda vez para
     * a mesma entidade.
     */
    public Optional<CardSpec> converter(EncounterInstance encontro, ResourceLocation monsterId,
            UUID entityId, DefeatResult resultado) {
        Objects.requireNonNull(entityId, "entidade ausente");
        if (!encontro.desfechos().containsKey(entityId)) {
            throw new IllegalArgumentException("entidade nao tem desfecho no encontro: " + entityId);
        }
        // Saves anteriores usavam uma trava por especie. Se ela existir, o
        // primeiro pagamento daquela versao ja ocorreu; nao reabrimos a porta
        // no upgrade e duplicamos o card no restart.
        if (ledger.jaPago(encontro.id(), chaveDeRecompensa(monsterId))) {
            return Optional.empty();
        }
        return converterInterno(encontro, monsterId, entityId, resultado);
    }

    private synchronized Optional<CardSpec> converterInterno(EncounterInstance encontro,
            ResourceLocation monsterId, UUID entityId, DefeatResult resultado) {
        Objects.requireNonNull(encontro, "encontro ausente");
        Objects.requireNonNull(monsterId, "criatura ausente");
        Objects.requireNonNull(resultado, "resultado ausente");

        if (!resultado.converte()) return Optional.empty();
        if (encontro.estado() != EncounterState.COMPLETED) {
            throw new IllegalStateException("conversao pedida para o encontro " + encontro.id()
                    + " em estado " + encontro.estado() + ": so COMPLETED converte. Converter"
                    + " antes premiaria quem abandonou o combate no meio.");
        }

        CardSpec spec = catalogo.get(monsterId);
        if (spec == null) {
            throw new IllegalArgumentException("nao ha card para '" + monsterId + "'. Conhecidos: "
                    + catalogo.keySet() + ". Uma referencia torta aqui viraria uma captura que"
                    + " nunca paga, e o jogador culparia a propria condicao de captura.");
        }

        int ja = emitidas(monsterId);
        if (!spec.cabeMaisUma(ja)) return Optional.empty();

        // A trava e o unico ponto de exclusao, e ela e atomica. O contador de
        // copias so avanca DEPOIS dela: incrementar antes faria a tentativa
        // recusada consumir uma copia do mundo, e a escassez viraria erosao.
        String chave = entityId == null
                ? chaveDeRecompensa(monsterId)
                : chaveDeRecompensa(monsterId, entityId);
        if (!ledger.travar(encontro.id(), chave)) return Optional.empty();

        emitidas.merge(monsterId, 1, Integer::sum);
        return Optional.of(spec);
    }

    /**
     * A chave de transacao dentro do ledger.
     *
     * <p>A forma legada inclui a criatura porque um episodio pode converter mais de
     * um tipo de alvo. Para o caminho de producao, use a sobrecarga com UUID: um
     * Wolf Pack derrubado inteiro paga um card por lobo sem permitir repetir o
     * mesmo individuo.</p>
     */
    public static String chaveDeRecompensa(ResourceLocation monsterId) {
        return "gi_card/" + monsterId;
    }

    /** Chave estável por criatura; UUID evita colidir dentro de uma matilha. */
    public static String chaveDeRecompensa(ResourceLocation monsterId, UUID entityId) {
        Objects.requireNonNull(monsterId, "criatura ausente");
        return chaveDeRecompensa(monsterId) + "/" + Objects.requireNonNull(entityId,
                "entidade ausente");
    }

    /** Carga do save: substitui a contagem inteira, nunca soma. */
    public void carregarEmitidas(Map<ResourceLocation, Integer> contagem) {
        Objects.requireNonNull(contagem, "contagem ausente");
        contagem.forEach((id, valor) -> {
            if (id == null || valor == null || valor < 0) {
                throw new IllegalArgumentException("contagem de copias invalida no save: "
                        + id + " = " + valor);
            }
        });
        emitidas.clear();
        emitidas.putAll(contagem);
    }

    public Map<ResourceLocation, Integer> emitidas() { return Map.copyOf(emitidas); }
}
