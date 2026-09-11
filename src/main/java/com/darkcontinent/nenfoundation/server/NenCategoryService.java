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
 * A porta unica da categoria: atribuir e revelar.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. SAO DUAS OPERACOES, e nao uma. Atribuir define qual e a categoria;
 * revelar faz o jogador SABER. Se fossem a mesma coisa, a Adivinhacao da Agua
 * nao descobriria nada -- ela criaria a categoria na hora, e o ritual viraria
 * um sorteio com animacao. O schema v1 ja separa {@code category} de
 * {@code category_revealed}, e este servico e o que respeita essa separacao.
 *
 * <p>2. SO QUEM DESPERTOU TEM CATEGORIA. Atribuir a quem nao despertou devolve
 * {@link Resultado#NAO_DESPERTOU} e nao escreve nada. O motivo esta no proprio
 * {@link NenCategory}: {@code UNDETERMINED} e "sem Nen, ou com Nen e categoria
 * ainda nao sorteada". Um jogador nunca-tocado com categoria gravada quebra
 * essa leitura, e toda pergunta do tipo "esta pessoa tem Nen?" passa a
 * precisar de duas consultas em vez de uma.
 *
 * <p>3. AS DUAS OPERACOES SAO IDEMPOTENTES, e a segunda chamada nao anuncia de
 * novo. Quest que completa duas vezes, comando repetido e clique duplo
 * existem; o desenho que trata isso como excecao e o desenho que dispara o
 * onboarding em dobro.
 *
 * <p>4. A ORDEM E CONTRATO: gravar, depois anunciar. O evento so dispara com o
 * perfil ja persistido, para que quem o escuta leia o estado NOVO. E
 * {@link NenProfileService#atualizar} publica o snapshot sozinho -- reenviar
 * aqui criaria um segundo caminho para a mesma verdade.
 *
 * <p>5. NAO HA MARCO DE "CATEGORIA ATRIBUIDA", e a ausencia e deliberada. Os
 * marcos viajam INTEIROS no snapshot ate o cliente
 * ({@code SnapshotDePerfilS2C.marcos}). Um marco por categoria atribuida seria
 * exatamente o vazamento que {@code categoriaVisivel()} existe para impedir; e
 * um marco generico so repetiria, numa segunda fonte, o que
 * {@code category() != UNDETERMINED} ja diz. O marco existe para a REVELACAO,
 * que e o fato que quests querem.
 *
 * <p>6. TROCAR A CATEGORIA DE ALGUEM NAO EXISTE AQUI. Nao ha
 * {@code reatribuir}. Isso e uma decisao de design que ninguem tomou, e um
 * metodo que a antecipasse seria a porta pela qual ela entraria sem discussao.
 * Ver {@code /nen reset}, que apaga o perfil inteiro e diz o que apagou.
 */
public final class NenCategoryService {

    private NenCategoryService() {
    }

    /** O que aconteceu na tentativa de atribuir. */
    public enum Atribuicao {
        /** Sorteou e gravou agora. */
        ATRIBUIU,

        /** Ja tinha categoria. Nada mudou, e isso nao e erro. */
        JA_TINHA,

        /** O jogador nao despertou. Nada foi escrito. */
        NAO_DESPERTOU
    }

    /** O que aconteceu na tentativa de revelar. */
    public enum Revelacao {
        /** O jogador soube agora. */
        REVELOU,

        /** Ja sabia. Nada mudou, e isso nao e erro. */
        JA_SABIA,

        /**
         * Nao ha o que revelar: a categoria ainda e
         * {@link NenCategory#UNDETERMINED}.
         *
         * <p>Revelar antes de atribuir gravaria {@code category_revealed=true}
         * sobre {@code UNDETERMINED}, e o jogador passaria a "saber" que e
         * indeterminado -- um estado que nenhuma tela sabe desenhar e que a
         * atribuicao seguinte tornaria mentira em silencio.
         */
        SEM_CATEGORIA
    }

    /**
     * Sorteia e grava a categoria deste jogador, sem revela-la.
     *
     * <p>O sorteio e determinista: mesma semente de mundo e mesmo jogador,
     * mesma categoria. Ver {@link SorteioDeCategoria}.
     */
    public static Atribuicao atribuir(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");

        PersistentNenData antes = NenProfileService.ler(jogador);
        if (!antes.awakened()) {
            return Atribuicao.NAO_DESPERTOU;
        }
        if (antes.category().eReal()) {
            return Atribuicao.JA_TINHA;
        }

        NenCategory sorteada = SorteioDeCategoria.sortear(
                sementeDoMundo(jogador), jogador.getUUID());

        NenProfileService.atualizar(jogador, perfil -> comCategoria(perfil, sorteada));

        NeoForge.EVENT_BUS.post(new CategoriaAtribuidaEvent(jogador, sorteada));
        return Atribuicao.ATRIBUIU;
    }

    /**
     * Faz o jogador SABER qual e a sua categoria.
     *
     * <p>Ligar {@code category_revealed} muda o que
     * {@link PersistentNenData#categoriaVisivel()} devolve, e o snapshot sai
     * de novo por conta do servico de perfil. E so depois disso que o evento
     * dispara -- quem o escuta ja pode contar com o cliente informado.
     */
    public static Revelacao revelar(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");

        PersistentNenData antes = NenProfileService.ler(jogador);
        if (!antes.category().eReal()) {
            return Revelacao.SEM_CATEGORIA;
        }
        if (antes.categoryRevealed()) {
            return Revelacao.JA_SABIA;
        }

        NenProfileService.atualizar(jogador, NenCategoryService::comRevelacao);

        NeoForge.EVENT_BUS.post(new CategoriaReveladaEvent(jogador, antes.category()));
        return Revelacao.REVELOU;
    }

    /**
     * A semente que alimenta o sorteio.
     *
     * <p>Vem do OVERWORLD, e nao do nivel em que o jogador esta. Sorteando
     * pela semente do nivel atual, um jogador que despertasse no Nether
     * receberia outra categoria -- e o bug seria "a categoria muda de
     * dimensao", impossivel de adivinhar a partir do sintoma.
     */
    private static long sementeDoMundo(ServerPlayer jogador) {
        return jogador.getServer().overworld().getSeed();
    }

    /**
     * Grava a categoria e mantem {@code category_revealed} como estava.
     *
     * <p>Separado e {@code static} para poder ser exercitado sem um servidor
     * de pe. Note o que ele NAO faz: nao revela. Um metodo que fizesse as duas
     * coisas seria o unico lugar de onde a separacao poderia se perder.
     */
    static PersistentNenData comCategoria(PersistentNenData antes, NenCategory categoria) {
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

    /** Liga a revelacao e poe o marco, sem tocar na categoria. */
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
