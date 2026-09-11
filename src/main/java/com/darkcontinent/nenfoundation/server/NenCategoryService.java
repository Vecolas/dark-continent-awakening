package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.api.event.CategoriaAtribuidaEvent;
import com.darkcontinent.nenfoundation.api.event.CategoriaReveladaEvent;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.category.SorteioDeCategoria;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A porta unica da categoria de Nen: atribuir e revelar.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. SAO DUAS OPERACOES, e nao uma. Ter categoria e saber qual e sao dois
 * fatos diferentes sobre o jogador, e o schema v1 ja os separa em
 * {@code category} e {@code category_revealed}. Colapsar as duas faria a Water
 * Divination CRIAR a categoria em vez de descobri-la -- e ai o ritual nao
 * seria um teste, seria um sorteio com cenario.
 *
 * <p>2. A REVELACAO NAO SORTEIA NADA. {@link #revelar} so liga um booleano. Se
 * ela pudesse atribuir "quando faltasse", o caminho de quem revelou sem ter
 * categoria ficaria escondido para sempre atras de um conserto automatico, e
 * ninguem descobriria que o ritual estava sendo chamado fora de ordem.
 *
 * <p>3. IDEMPOTENTES AS DUAS. Atribuir a quem ja tem nao troca a categoria;
 * revelar a quem ja sabe nao reanuncia. Quest que completa duas vezes, comando
 * repetido e clique duplo existem.
 *
 * <p>4. ATRIBUIR EXIGE DESPERTAR, e a recusa tem nome. Sem isso, todo jogador
 * do servidor -- inclusive quem nunca tocou em Nen -- poderia carregar uma
 * categoria escondida, e o invariante "categoria real implica desperto"
 * deixaria de valer justamente onde ele e util: no relatorio, na quest e no
 * portao.
 *
 * <p>5. O SYNC VEM DE GRACA. {@link NenProfileService#atualizar} publica o
 * snapshot quando a mudanca e efetiva. Na atribuicao isso NAO vaza nada: o
 * snapshot leva {@code categoriaVisivel()}, que continua UNDETERMINED ate a
 * revelacao. Reenviar aqui criaria um segundo caminho para a mesma verdade.
 *
 * <p>6. A ORDEM E CONTRATO: gravar, depois anunciar. Quem escuta os eventos le
 * o perfil e ve o estado novo, nunca o antigo.
 *
 * <p>PONTO CEGO DECLARADO: <b>nada chama {@link #atribuir} sozinho hoje.</b>
 * Nem o despertar, nem o login. Os chamadores sao o ritual de Water Divination
 * e os comandos de seed, que sao da outra lane do M3 (issues #61 e #62). Ate
 * eles existirem, um jogador desperta e fica sem categoria -- o que esta
 * correto pelo desenho, mas nao e o fluxo final.
 */
public final class NenCategoryService {

    private NenCategoryService() {
    }

    /** O que aconteceu na tentativa de atribuir uma categoria. */
    public enum Atribuicao {
        /** Atribuiu agora. Categoria gravada, ainda escondida, evento anunciado. */
        ATRIBUIU,

        /** Ja tinha uma categoria real. Nada mudou, e isso nao e erro. */
        JA_TINHA,

        /**
         * O jogador nao despertou. Recusa com motivo.
         *
         * <p>Quem mostra isso ao jogador usa a chave
         * {@code nenfoundation.error.nao_desperto}, que ja existe.
         */
        NAO_DESPERTO
    }

    /** O que aconteceu na tentativa de revelar a categoria. */
    public enum Revelacao {
        /** Revelou agora. Marco posto, snapshot reenviado, evento anunciado. */
        REVELOU,

        /** O jogador ja sabia. Nada mudou, e isso nao e erro. */
        JA_SABIA,

        /**
         * Nao ha categoria para revelar.
         *
         * <p>Recusa, e nao sorteio silencioso: ver a decisao 2 no topo.
         */
        SEM_CATEGORIA
    }

    // --------------------------------------------------------- atribuicao

    /**
     * Da ao jogador uma categoria especifica, ainda escondida dele.
     *
     * @param jogador   quem recebe; sempre server-side
     * @param categoria uma das seis REAIS
     * @throws IllegalArgumentException se {@code categoria} for
     *         {@link NenCategory#UNDETERMINED} -- que e a AUSENCIA de
     *         categoria, nao uma categoria. Apagar a categoria de alguem seria
     *         outra operacao, com outro nome, e ninguem pediu por ela. Um
     *         no-op silencioso aqui deixaria o chamador achando que gravou.
     */
    public static Atribuicao atribuir(ServerPlayer jogador, NenCategory categoria) {
        Objects.requireNonNull(jogador, "jogador");
        Objects.requireNonNull(categoria, "categoria");
        if (!categoria.eReal()) {
            throw new IllegalArgumentException(
                    "UNDETERMINED e a ausencia de categoria, e nao uma categoria"
                            + " atribuivel. Para limpar um perfil use o reset.");
        }

        PersistentNenData antes = NenProfileService.ler(jogador);
        if (!antes.awakened()) {
            return Atribuicao.NAO_DESPERTO;
        }
        if (antes.category().eReal()) {
            return Atribuicao.JA_TINHA;
        }

        NenProfileService.atualizar(jogador, perfil -> comCategoria(perfil, categoria));
        NeoForge.EVENT_BUS.post(new CategoriaAtribuidaEvent(jogador, categoria));
        return Atribuicao.ATRIBUIU;
    }

    /**
     * Da ao jogador a categoria que o mundo e o UUID dele determinam.
     *
     * <p>O sorteio e deterministico de proposito: o mesmo mundo e o mesmo
     * jogador dao sempre o mesmo resultado, entao um bug de categoria e
     * reproduzivel. Ver {@link SorteioDeCategoria}.
     */
    public static Atribuicao atribuirPorSorteio(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return atribuir(jogador, sorteioPara(jogador));
    }

    /**
     * Qual categoria o sorteio daria a este jogador, sem gravar nada.
     *
     * <p>Publico porque comando de seed e diagnostico precisam PREVER o
     * resultado sem alterar o perfil. Consultar e atribuir por dois caminhos
     * diferentes seria a mesma verdade em duas fontes; aqui a atribuicao chama
     * esta mesma funcao.
     */
    public static NenCategory sorteioPara(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return SorteioDeCategoria.sortear(
                jogador.serverLevel().getSeed(), jogador.getUUID());
    }

    // --------------------------------------------------------- revelacao

    /**
     * Conta ao jogador qual e a categoria dele.
     *
     * <p>So liga {@code category_revealed} e poe o marco. A gravacao faz o
     * servico de perfil reenviar o snapshot, e so entao a categoria aparece
     * para o cliente -- porque o snapshot leva {@code categoriaVisivel()}.
     */
    public static Revelacao revelar(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");

        PersistentNenData antes = NenProfileService.ler(jogador);
        if (!antes.category().eReal()) {
            return Revelacao.SEM_CATEGORIA;
        }

        // O QUE DECIDE SE ANUNCIA E SO O BOOLEANO, e nao o marco.
        //
        // Um perfil pode chegar aqui revelado mas sem o marco -- save antigo,
        // ou alguem que ligou o campo por outro caminho. A gravacao abaixo
        // conserta isso, e tem de consertar EM SILENCIO: quem ja sabia a
        // categoria nao pode ver a cutscene da descoberta de novo so porque um
        // marco estava faltando. Anunciar pelo estado que se conserta e como
        // se descobre que o onboarding dispara duas vezes -- em producao.
        boolean jaSabia = antes.categoryRevealed();

        NenProfileService.atualizar(jogador, NenCategoryService::comRevelacao);

        if (jaSabia) {
            return Revelacao.JA_SABIA;
        }
        NeoForge.EVENT_BUS.post(
                new CategoriaReveladaEvent(jogador, antes.category()));
        return Revelacao.REVELOU;
    }

    // ------------------------------------------------------ mutacoes puras

    /**
     * Grava a categoria e deixa a revelacao como esta.
     *
     * <p>Separado e {@code static} para poder ser exercitado sem um servidor de
     * pe: logica que so roda com o jogo aberto nao e exercitada.
     *
     * <p>Note o que ele NAO faz: nao liga {@code category_revealed}. Se ligasse
     * "por conveniencia", a Water Divination deixaria de ter o que descobrir.
     */
    static PersistentNenData comCategoria(PersistentNenData antes, NenCategory categoria) {
        if (antes.category() == categoria) {
            return antes;
        }
        return new PersistentNenData(
                antes.schemaVersion(),
                antes.awakened(),
                categoria,
                antes.categoryRevealed(),
                antes.auraPotential(),
                antes.control(),
                antes.output(),
                antes.techniqueProficiency(),
                antes.unlockedTechniques(),
                antes.unlockedAbilities(),
                antes.progressionFlags());
    }

    /**
     * Liga {@code category_revealed} e poe o marco, sem tocar em mais nada.
     *
     * <p>Conserta tambem o estado pela metade -- revelado sem marco, ou marco
     * sem revelado --, pela mesma razao que o despertar conserta o dele: o
     * sintoma seria uma quest que nunca completa, sem erro nenhum no log.
     */
    static PersistentNenData comRevelacao(PersistentNenData antes) {
        if (antes.categoryRevealed() && antes.temMarco(Marcos.CATEGORIA_REVELADA)) {
            return antes;
        }
        Set<ResourceLocation> marcos = new LinkedHashSet<>(antes.progressionFlags());
        marcos.add(Marcos.CATEGORIA_REVELADA);
        return new PersistentNenData(
                antes.schemaVersion(),
                antes.awakened(),
                antes.category(),
                true,
                antes.auraPotential(),
                antes.control(),
                antes.output(),
                antes.techniqueProficiency(),
                antes.unlockedTechniques(),
                antes.unlockedAbilities(),
                marcos);
    }
}
