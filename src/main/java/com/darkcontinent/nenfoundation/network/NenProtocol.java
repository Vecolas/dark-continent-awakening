package com.darkcontinent.nenfoundation.network;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * O protocolo de rede do Nen Foundation: versao, ids e direcoes.
 *
 * <p>CONTRATO CONGELADO (plano tecnico, secao 22; ADR-001).
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A tabela abaixo e a UNICA fonte de verdade sobre quais payloads
 * existem e para que lado eles viajam. {@code docs/multiplayer/protocol.md}
 * descreve os mesmos payloads em prosa, e um portao em
 * {@code ProtocoloCongeladoTest} cruza os dois e reprova se divergirem — em
 * qualquer direcao, payload faltando ou sobrando. Documento e codigo
 * discordando sobre direcao de pacote e exatamente o tipo de divergencia que
 * ninguem nota ate alguem explorar.
 *
 * <p>2. Nenhum payload C2S pode carregar aura, dano, cooldown ou unlock. O
 * cliente manda INTENCAO ("quero ativar Ren"); o servidor decide. Isto esta
 * escrito no campo {@link Registro#regra()} de cada entrada, para que a regra
 * viaje junto do id em vez de morar so num documento.
 *
 * <p>3. {@link #VERSION} sobe sempre que um payload muda de formato, some ou
 * troca de direcao. Cliente e servidor com versoes diferentes precisam falhar
 * ALTO no handshake, nao interpretar bytes errados em silencio.
 *
 * <p>M0 congela a tabela. O registro efetivo dos handlers e a implementacao dos
 * records acontecem no M1 — registrar payload sem handler derruba o jogo, e
 * handler vazio "por enquanto" e a armadilha da secao 10 da disciplina.
 *
 * <p>ARQUIVO HOSTIL A MERGE: uma pessoa por vez.
 */
public final class NenProtocol {

    /** Versao do protocolo. Sobe a cada mudanca de formato, direcao ou remocao. */
    public static final int VERSION = 6;

    /**
     * Nomes de campo que um payload C2S NAO pode carregar, em nenhuma
     * circunstancia.
     *
     * <p>Sao os valores que o servidor decide. Um campo com qualquer um destes
     * nomes num payload que o cliente envia significa que o cliente esta
     * afirmando o proprio estado.
     */
    public static final Set<String> PROIBIDOS_EM_C2S = Set.of(
            "aura", "dano", "cooldown", "unlock", "multiplicador", "resultado");

    /**
     * Um payload congelado.
     *
     * @param id      identificador estavel
     * @param direcao para que lado ele viaja
     * @param campos  os nomes dos campos que ele carrega. Esta lista e DADO
     *     ESTRUTURADO, e nao prosa, de proposito: o portao que verifica se um
     *     payload C2S carrega estado do servidor precisa comparar nomes, e nao
     *     procurar palavra dentro de uma frase. Um portao que casa substring
     *     numa descricao reprova a frase "sem custo nem dano" — ele mede o
     *     texto, e nao a coisa. Quando os records nascerem no M1, um portao
     *     novo compara os componentes de cada record com esta lista.
     * @param regra   a razao de seguranca, em prosa, para quem le
     */
    public record Registro(ResourceLocation id, Direcao direcao, List<String> campos, String regra) {

        public Registro {
            campos = List.copyOf(campos);
        }
    }

    private static Registro c2s(String caminho, List<String> campos, String regra) {
        return new Registro(NenFoundation.id(caminho), Direcao.C2S, campos, regra);
    }

    private static Registro s2c(String caminho, List<String> campos, String regra) {
        return new Registro(NenFoundation.id(caminho), Direcao.S2C, campos, regra);
    }

    /**
     * A tabela congelada. A ORDEM nao importa; os ids importam.
     *
     * <p>Acrescentar uma linha aqui obriga a acrescentar a mesma linha em
     * {@code docs/multiplayer/protocol.md}, senao o portao reprova.
     */
    public static final List<Registro> TABELA = List.of(
            c2s("activate_technique_request",
                    List.of("tecnicaId"),
                    "o jogador apertou a tecla; o servidor decide se pode"),
            c2s("adjust_output_request",
                    List.of("variacao"),
                    "o jogador enviou a variacao (+/-); o servidor limita de 0 a 100%"),
            c2s("deactivate_technique_request",
                    List.of("tecnicaId"),
                    "o servidor decide se o desligamento e legitimo"),
            c2s("activate_ability_request",
                    List.of("habilidadeId", "slot", "alvoIdCandidato", "posicaoCandidata"),
                    "alvo e posicao sao CANDIDATOS; o servidor reconstroi o alvo pelo id"),
            s2c("nen_profile_snapshot",
                    List.of("categoriaVisivel", "tecnicasDesbloqueadas",
                            "habilidadesDesbloqueadas", "marcos"),
                    "estado de leitura para a interface; enviado so ao dono do perfil"),
            s2c("nen_runtime_delta",
                    List.of("aura", "auraMaxima", "outputPercent",
                            "tecnicasAtivas", "cooldowns", "alocacao"),
                    "delta de runtime; o HUD interpola aura e output"),
            s2c("ability_fx_event",
                    List.of("habilidadeId", "posicao", "variante"),
                    "som, particula e animacao; nao altera nenhuma logica no cliente"),
            s2c("nen_error_feedback",
                    List.of("chaveDeTraducao"),
                    "motivo legivel de uma recusa; nunca revela estado alheio"),
            s2c("aura_presence",
                    List.of("entidadeId", "sinal"),
                    "o UNICO payload sobre terceiros; so o que alguem ao lado"
                            + " perceberia. Zetsu manda NENHUM, igual a quem nunca"
                            + " despertou -- o segredo nao atravessa a rede")
    );

    /** Ids dos payloads que o CLIENTE pode enviar. Derivado da tabela. */
    public static List<ResourceLocation> idsC2S() {
        return TABELA.stream().filter(r -> r.direcao() == Direcao.C2S).map(Registro::id).toList();
    }

    /** Ids dos payloads que o SERVIDOR pode enviar. Derivado da tabela. */
    public static List<ResourceLocation> idsS2C() {
        return TABELA.stream().filter(r -> r.direcao() == Direcao.S2C).map(Registro::id).toList();
    }

    private NenProtocol() {
    }
}
