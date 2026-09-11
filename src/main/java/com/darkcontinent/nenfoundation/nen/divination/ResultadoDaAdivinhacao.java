package com.darkcontinent.nenfoundation.nen.divination;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * O que a agua faz, para cada categoria.
 *
 * <p>ISTO E CANONE, e e por isso que vale a pena. O capitulo 60 lista seis
 * reacoes distintas para a Water Divination, e elas sao o jeito do jogo
 * DESCOBRIR a categoria em vez de a pessoa ESCOLHER num menu.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. O RESULTADO E DADO, e nao um {@code switch} espalhado no ritual. Com um
 * switch, "cada categoria produz um resultado distinguivel" nao e uma coisa que
 * se possa perguntar ao codigo -- so lendo. Aqui um portao consegue afirmar que
 * as seis estao cobertas, uma vez cada, com particula, som e texto diferentes.
 *
 * <p>2. SAO TRES SINAIS, e nao um texto so: particula, som e mensagem. A
 * mensagem sozinha e invisivel para quem esta com o chat fechado, e o teste
 * canonico e um teste VISUAL. Tres sinais tambem dao ao portao tres eixos para
 * exigir distincao -- dois resultados com a mesma particula sao
 * indistinguiveis na pratica, mesmo com textos diferentes.
 *
 * <p>3. SPECIALIZATION E "algo fora da lista", de proposito. O cânone descreve
 * as cinco reacoes concretas e, para a sexta, diz apenas que acontece algo
 * DIFERENTE -- sem fixar o que. Inventar aqui uma sexta reacao concreta e
 * apresenta-la com a mesma cara das cinco seria transformar um silencio do
 * material em afirmacao. A escolha de particula e som e abstracao de gameplay;
 * o que e cânone e so que ela nao se parece com nenhuma das outras.
 */
public enum ResultadoDaAdivinhacao {

    /** O volume da agua muda. */
    VOLUME(NenCategory.ENHANCEMENT, "volume",
            () -> ParticleTypes.SPLASH, () -> SoundEvents.BUCKET_EMPTY),

    /** O sabor da agua muda. */
    SABOR(NenCategory.TRANSMUTATION, "sabor",
            () -> ParticleTypes.EFFECT, () -> SoundEvents.BREWING_STAND_BREW),

    /** A cor da agua muda. */
    COR(NenCategory.EMISSION, "cor",
            () -> ParticleTypes.GLOW, () -> SoundEvents.AMETHYST_BLOCK_CHIME),

    /** Aparecem impurezas na agua. */
    IMPUREZAS(NenCategory.CONJURATION, "impurezas",
            () -> ParticleTypes.ASH, () -> SoundEvents.COMPOSTER_FILL),

    /** A folha se move. */
    FOLHA(NenCategory.MANIPULATION, "folha",
            () -> ParticleTypes.CHERRY_LEAVES,
            () -> SoundEvents.CHERRY_WOOD_HANGING_SIGN_STEP),

    /** Acontece algo que nao esta na lista. */
    ESTRANHO(NenCategory.SPECIALIZATION, "estranho",
            () -> ParticleTypes.PORTAL, () -> SoundEvents.ENCHANTMENT_TABLE_USE);

    /**
     * Indice por categoria, derivado dos valores.
     *
     * <p>Escrito assim, e nao a mao: um mapa literal divergiria do enum no dia
     * em que alguem acrescentasse um resultado -- e o sintoma seria uma
     * categoria que nunca produz reacao nenhuma, sem erro no log.
     */
    private static final Map<NenCategory, ResultadoDaAdivinhacao> POR_CATEGORIA =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(
                    ResultadoDaAdivinhacao::categoria, Function.identity()));

    private final NenCategory categoria;
    private final String nome;
    private final Supplier<SimpleParticleType> particula;
    private final Supplier<SoundEvent> som;

    ResultadoDaAdivinhacao(NenCategory categoria, String nome,
            Supplier<SimpleParticleType> particula, Supplier<SoundEvent> som) {
        this.categoria = categoria;
        this.nome = nome;
        this.particula = particula;
        this.som = som;
    }

    /**
     * O resultado de uma categoria.
     *
     * <p>Vazio para {@link NenCategory#UNDETERMINED}: nao ha reacao para a
     * ausencia de categoria. Devolver um resultado qualquer aqui faria a agua
     * reagir para quem nao tem nada a descobrir.
     */
    public static Optional<ResultadoDaAdivinhacao> para(NenCategory categoria) {
        return Optional.ofNullable(POR_CATEGORIA.get(categoria));
    }

    public NenCategory categoria() {
        return this.categoria;
    }

    /**
     * A particula. Resolvida SO AQUI, e nunca no carregamento da classe.
     *
     * <p>POR QUE PREGUICOSA: {@code ParticleTypes} e {@code SoundEvents} tocam
     * o registro do Minecraft no {@code <clinit>} deles. Com os valores diretos
     * nos campos, carregar este enum exigia o jogo de pe -- e a suite JUnit
     * morria com "Not bootstrapped" so por perguntar quais categorias estao
     * cobertas.
     *
     * <p>A alternativa seria tirar particula e som do enum "para poder testar".
     * Isso inverteria a ordem: o dado ficaria mais pobre para caber na
     * ferramenta. Assim, a cobertura e as frases sao conferidas no JUnit, e a
     * distincao de particula e som no gametest, onde o registro existe.
     */
    public SimpleParticleType particula() {
        return this.particula.get();
    }

    public SoundEvent som() {
        return this.som.get();
    }

    /** Chave de traducao da frase que o jogador le. O texto nunca sai do codigo. */
    public String chaveDeTraducao() {
        return "nenfoundation.divinacao." + this.nome;
    }

    /**
     * Chave da frase para quem REFAZ o teste ja sabendo.
     *
     * <p>Existe porque repetir o teste nao pode dar a mesma frase de
     * descoberta. "Voce descobre que..." dito pela segunda vez sugere que algo
     * mudou; e nada mudou, e nada pode mudar.
     */
    public String chaveDeRepeticao() {
        return "nenfoundation.divinacao.repete." + this.nome;
    }
}
