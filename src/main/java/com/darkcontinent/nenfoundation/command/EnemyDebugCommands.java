package com.darkcontinent.nenfoundation.command;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyDebugView;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * As ferramentas de debug de inimigo: {@code /nenenemy} (issue #113).
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. <b>NENHUMA LOGICA ESPECIFICA DE CRIATURA.</b> Nao ha um ramo para o
 * boneco, outro para o foxbear. O que define "inimigo do mod" e a FILA UNICA de
 * {@link EnemyEntityTypes} -- a mesma que o {@code FilaUnicaDeInimigosTest}
 * guarda. Um {@code instanceof} de {@link HxHEnemy} pareceria mais elegante e
 * seria errado: o foxbear esta na fila e NAO implementa a interface, entao um
 * {@code clear} baseado nela deixaria justamente ele para tras -- sem erro
 * nenhum, so um bicho que "nao some" e ninguem sabe por que.
 *
 * <p>2. <b>O SERVIDOR MEDE TUDO.</b> Posicao de spawn, entidade mirada, raio de
 * limpeza: todos saem de uma medida feita AQUI, a partir do olho e do angulo que
 * o servidor ja conhece. Nenhum subcomando aceita posicao, alvo, regiao ou dano
 * vindos do cliente -- um comando de operador que confiasse no cliente seria a
 * porta de ADR-001 aberta pelo lado de dentro.
 *
 * <p>3. <b>UMA FILA DE REGISTRO SO.</b> Esta arvore nasce dentro de
 * {@code NenCommands.aoRegistrarComandos}, o unico {@code RegisterCommandsEvent}
 * do pacote {@code command}. Um {@code @EventBusSubscriber} proprio funcionaria
 * -- e essa e a armadilha: os dois registros funcionariam, com permissoes
 * possivelmente diferentes, e nada acusaria a divergencia.
 *
 * <p>4. <b>RECUSA SEMPRE TEM MOTIVO E CHAVE.</b> Toda saida negativa passa por
 * {@link Recusa}, que carrega a chave de traducao. Comando de debug que falha em
 * silencio produz o pior relato de bug que existe -- e num comando de QA ele
 * produz algo pior ainda: a pessoa conclui que o SISTEMA esta quebrado quando
 * quem recusou foi a ferramenta.
 *
 * <p><b>O QUE FICOU DE FORA, E POR QUE (nao e esquecimento).</b>
 *
 * <p>A issue pede tambem toggles de <i>hitbox</i>, de <i>weak point</i> e de
 * <i>animacao</i>. Os tres sao DESENHO, e desenho mora no cliente. Entrega-los
 * daqui exigiria uma de duas coisas, e as duas sao piores que a ausencia:
 *
 * <ul>
 *   <li>o pacote {@code command} alcancar {@code nenfoundation.client.*} ou
 *       {@code net.minecraft.client.*} -- o que o portao
 *       {@code PacotesDeclaradosTest} reprova, e com razao: um servidor
 *       dedicado nao tem essas classes e o crash sai no login, nao no build;</li>
 *   <li>um payload novo S2C para o cliente ligar o desenho -- e id de payload e
 *       contrato congelado (ADR-004), alem de gastar protocolo numa ferramenta
 *       de teste.</li>
 * </ul>
 *
 * <p>O que seria preciso para entregar de verdade: um comando de CLIENTE
 * ({@code RegisterClientCommandsEvent}, como o {@code /nenvfx} ja faz em
 * {@code client/vfx/debug}) desenhando a caixa de golpe e as regioes de ponto
 * fraco a partir da geometria que o cliente ja tem. Isso e uma issue da lane de
 * superficie, nao desta. Ate la, o meio-caminho honesto esta entregue:
 * {@code /nenenemy info} imprime a FASE de ataque -- que e exatamente o valor
 * que escolhe o clipe de animacao no cliente --, e o F3+B do vanilla ja desenha
 * a hitbox da entidade. Entregar um toggle pela metade seria pior: um botao que
 * nao desenha nada parece sistema quebrado.
 *
 * <p>Owner: lane de inimigos.
 */
public final class EnemyDebugCommands {

    /** O nome da raiz. Uma constante porque o portao precisa nomear a mesma. */
    public static final String RAIZ = "nenenemy";

    /**
     * Teto de bichos por chamada de {@code spawn}.
     *
     * <p>LIMITE DE DESIGN, nao botao de tuning: quem digita um zero a mais nao
     * recebe erro nenhum -- recebe duzentos mobs varrendo o mundo, TPS no chao e
     * uma mira que nao consegue mais selecionar nada para remover. Dezesseis e o
     * que cabe numa arena sem que isso aconteca.
     */
    static final int LIMITE_DE_SPAWN = 16;

    /**
     * Alcance da mira, em blocos, para {@code spawn} e {@code info}.
     *
     * <p>LIMITE DE DESIGN: mais longe que isto o operador nao ve o que apareceu,
     * e um mob spawnado fora do campo de visao e um mob que ninguem lembra de
     * remover depois.
     */
    static final double ALCANCE_DA_MIRA = 64.0D;

    /** Raio padrao do {@code clear}, quando ninguem informa um. */
    static final double RAIO_PADRAO = 32.0D;

    /**
     * Raio maximo do {@code clear}.
     *
     * <p>LIMITE DE DESIGN, e este e o que mais importa: {@code clear} apaga
     * entidades sem drop e sem evento de morte. Sem teto, um raio digitado
     * errado limpa tudo que esta carregado, inclusive o bicho que a outra pessoa
     * estava usando na medicao dela -- e entidade apagada nao volta.
     */
    static final double RAIO_MAXIMO = 128.0D;

    /**
     * Folga somada a caixa do inimigo ao procurar quem esta na mira.
     *
     * <p>Nao e tuning: e a tolerancia da mira humana. Sem ela, mirar na borda de
     * um bicho estreito recusa com "nenhum inimigo na mira" enquanto a cruz esta
     * visivelmente em cima dele, e a pessoa conclui que o comando nao funciona.
     */
    private static final double FOLGA_DA_MIRA = 0.3D;

    /**
     * Quantos caracteres do uuid do alvo lembrado aparecem no relato.
     *
     * <p>Limite de legibilidade: o uuid inteiro ocupa a linha toda do chat e
     * empurra o resto do diagnostico para fora da tela. Oito caracteres bastam
     * para conferir contra {@code /data get entity} sem esconder nada.
     */
    private static final int PREFIXO_DE_UUID = 8;

    /** As entidades da fila unica, sugeridas por id completo. */
    private static final SuggestionProvider<CommandSourceStack> TIPOS_DA_FILA =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(idsDaFila(), builder);

    /**
     * Toda saida negativa deste comando, com a chave de traducao junto.
     *
     * <p>A chave vem ESCRITA INTEIRA em cada constante, e nao montada a partir
     * de um prefixo. Montada, ela ficaria invisivel para uma busca de texto --
     * e o portao que confere se toda chave existe nos dois idiomas varre a
     * fonte procurando exatamente esse texto.
     */
    public enum Recusa {
        /** O subcomando le a mira, e console nao tem mira. */
        SEM_JOGADOR("nenfoundation.enemy.debug.sem_jogador"),
        /** Id fora do namespace do mod. */
        TIPO_FORA_DO_MOD("nenfoundation.enemy.debug.tipo_fora_do_mod"),
        /** Id no namespace certo, mas ausente da fila unica. */
        TIPO_DESCONHECIDO("nenfoundation.enemy.debug.tipo_desconhecido"),
        /** Nada solido na mira: nao ha onde colocar o bicho. */
        SEM_MIRA("nenfoundation.enemy.debug.sem_mira"),
        /** Nada da fila na mira: nao ha o que diagnosticar. */
        SEM_INIMIGO_MIRADO("nenfoundation.enemy.debug.sem_inimigo_mirado"),
        /** O servidor recusou criar a entidade. */
        SPAWN_FALHOU("nenfoundation.enemy.debug.spawn_falhou"),
        /** Varredura vazia: dizer "removi 0" seria deixar o operador supor sucesso. */
        NADA_PARA_REMOVER("nenfoundation.enemy.debug.nada_para_remover");

        private final String chave;

        Recusa(String chave) {
            this.chave = chave;
        }

        public String chave() { return chave; }
    }

    private EnemyDebugCommands() {
    }

    // ------------------------------------------------------------- a arvore

    /**
     * Registra {@code /nenenemy}. Devolve a raiz para que o portao a inspecione.
     *
     * <p>Chamado por {@code NenCommands.aoRegistrarComandos}, e so por la.
     */
    public static LiteralCommandNode<CommandSourceStack> registrar(
            CommandDispatcher<CommandSourceStack> dispatcher) {

        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal(RAIZ)
                .requires(fonte -> fonte.hasPermission(NenCommands.NIVEL_DE_OPERADOR));

        raiz.then(Commands.literal("spawn")
                .then(Commands.argument("tipo", ResourceLocationArgument.id())
                        .suggests(TIPOS_DA_FILA)
                        .executes(ctx -> spawnar(ctx, 1))
                        .then(Commands.argument("quantidade",
                                        IntegerArgumentType.integer(1, LIMITE_DE_SPAWN))
                                .executes(ctx -> spawnar(ctx,
                                        IntegerArgumentType.getInteger(ctx, "quantidade"))))));

        raiz.then(Commands.literal("info").executes(EnemyDebugCommands::informar));

        // DOIS LITERAIS, e nao um argumento booleano. Com `freeze <bool>`, o
        // tab-complete oferece "true"/"false" e um engano de digitacao completa
        // para o sentido oposto sem que nada pareca estranho. Com dois
        // literais, cada sentido e uma palavra que alguem teve de escrever.
        raiz.then(Commands.literal("freeze")
                .then(Commands.literal("on").executes(ctx -> congelar(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> congelar(ctx, false))));

        raiz.then(Commands.literal("clear")
                .executes(ctx -> limpar(ctx, RAIO_PADRAO))
                .then(Commands.argument("raio",
                                DoubleArgumentType.doubleArg(1.0D, RAIO_MAXIMO))
                        .executes(ctx -> limpar(ctx, DoubleArgumentType.getDouble(ctx, "raio")))));

        return dispatcher.register(raiz);
    }

    // -------------------------------------------------------------- a fila

    /**
     * Os ids da FILA UNICA de entidades do mod, lidos do registro.
     *
     * <p>Lido na hora, nunca guardado num campo estatico: uma copia feita no
     * carregamento da classe ficaria velha depois de qualquer mudanca no
     * registro, e uma lista velha nao da erro -- ela so deixa o mob novo de fora
     * de todo comando de debug, que e exatamente como o foxbear passou meses
     * fora de tudo (#266).
     */
    static Set<ResourceLocation> idsDaFila() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> entrada
                : EnemyEntityTypes.TYPES.getEntries()) {
            ids.add(entrada.getId());
        }
        return ids;
    }

    /** {@code true} quando a entidade pertence a fila unica do mod. */
    static boolean daFila(Entity entidade, Set<ResourceLocation> fila) {
        return fila.contains(EntityType.getKey(entidade.getType()));
    }

    /** O tipo registrado com este id, se ele estiver na fila. */
    private static Optional<EntityType<?>> tipoDaFila(ResourceLocation id) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> entrada
                : EnemyEntityTypes.TYPES.getEntries()) {
            if (entrada.getId().equals(id)) {
                return Optional.of(entrada.get());
            }
        }
        return Optional.empty();
    }

    /**
     * Recusa id fora do namespace do mod.
     *
     * <p>Separado da busca na fila de proposito: os dois erros tem conserto
     * diferente. Namespace errado e erro de digitacao de quem esqueceu o
     * prefixo; id ausente da fila e uma entidade que nao existe. Uma mensagem
     * so para os dois mandaria metade das pessoas procurar no lugar errado.
     */
    static Optional<Recusa> validarNamespace(ResourceLocation id) {
        return NenFoundation.MOD_ID.equals(id.getNamespace())
                ? Optional.empty()
                : Optional.of(Recusa.TIPO_FORA_DO_MOD);
    }

    // --------------------------------------------------------------- acoes

    private static int spawnar(CommandContext<CommandSourceStack> ctx, int quantidade) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "tipo");

        // ORDEM QUE E CONTRATO, a mesma de NenCommands: o argumento e validado
        // ANTES de o jogador ser resolvido. Na ordem inversa, quem digita o
        // namespace errado no console recebe "e preciso um jogador" -- uma
        // mensagem sobre outro problema, e o erro de namespace vira
        // inalcancavel de qualquer fonte sem jogador.
        Optional<Recusa> namespace = validarNamespace(id);
        if (namespace.isPresent()) {
            return recusar(ctx, namespace.get(), id.toString());
        }

        Optional<EntityType<?>> tipo = tipoDaFila(id);
        if (tipo.isEmpty()) {
            return recusar(ctx, Recusa.TIPO_DESCONHECIDO, id.toString());
        }

        ServerPlayer operador = jogadorOuNulo(ctx);
        if (operador == null) {
            return recusar(ctx, Recusa.SEM_JOGADOR);
        }

        // A POSICAO E MEDIDA AQUI. O cliente nao manda coordenada nenhuma: ele
        // manda "spawn", e quem decide onde e o servidor, com o olho e o angulo
        // que ele ja tem. Aceitar a posicao do cliente funcionaria
        // perfeitamente com cliente honesto -- e por isso nenhum playtest
        // encontraria o problema.
        HitResult mira = operador.pick(ALCANCE_DA_MIRA, 0.0F, false);
        if (mira.getType() != HitResult.Type.BLOCK) {
            return recusar(ctx, Recusa.SEM_MIRA, texto(ALCANCE_DA_MIRA));
        }

        BlockHitResult bloco = (BlockHitResult) mira;
        BlockPos onde = bloco.getBlockPos().relative(bloco.getDirection());
        ServerLevel nivel = ctx.getSource().getLevel();

        int nascidos = 0;
        for (int i = 0; i < quantidade; i++) {
            Entity criado = tipo.get().spawn(nivel, onde, MobSpawnType.COMMAND);
            if (criado == null) {
                continue;
            }
            if (criado instanceof Mob mob) {
                CongelamentoDeIa.aplicarA(mob);
            }
            nascidos++;
        }

        if (nascidos == 0) {
            return recusar(ctx, Recusa.SPAWN_FALHOU, id.toString());
        }

        final int total = nascidos;
        // MUTACAO AVISA OS OUTROS OPERADORES. Bicho aparecendo no mundo sem
        // rastro e a origem de "de onde veio isso" em servidor compartilhado.
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "nenfoundation.enemy.debug.spawn_feito",
                total, id.toString(), onde.toShortString()), true);

        if (CongelamentoDeIa.ligado()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "nenfoundation.enemy.debug.spawn_veio_congelado"), false);
        }
        return total;
    }

    private static int informar(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer operador = jogadorOuNulo(ctx);
        if (operador == null) {
            return recusar(ctx, Recusa.SEM_JOGADOR);
        }

        Optional<Entity> mirado = inimigoNaMira(operador);
        if (mirado.isEmpty()) {
            return recusar(ctx, Recusa.SEM_INIMIGO_MIRADO, texto(ALCANCE_DA_MIRA));
        }

        // LEITURA NAO AVISA NINGUEM: o segundo argumento e false. Diagnostico
        // de uma pessoa aparecendo no chat de todos os operadores treina todo
        // mundo a ignorar a saida deles.
        for (Component linha : linhasDeInfo(mirado.get())) {
            ctx.getSource().sendSuccess(() -> linha, false);
        }
        return 1;
    }

    /**
     * O relato de UM inimigo, sem uma linha por especie.
     *
     * <p>Ele diz o que NAO tem, em vez de imprimir zero. Um mob anterior a
     * fundacao nao tem {@code EnemyRuntime}; imprimir "stagger 0,0" para ele
     * seria mentir com um numero plausivel, e alguem passaria a tarde tentando
     * entender por que aquele bicho nunca cambaleia.
     */
    static List<Component> linhasDeInfo(Entity entidade) {
        List<Component> linhas = new ArrayList<>();
        linhas.add(Component.translatable("nenfoundation.enemy.debug.info_titulo",
                EntityType.getKey(entidade.getType()).toString(), entidade.getId()));

        if (entidade instanceof HxHEnemy inimigo) {
            EnemyMetadata ficha = inimigo.enemyMetadata();
            linhas.add(Component.translatable("nenfoundation.enemy.debug.info_identidade",
                    ficha.faction().name(), ficha.threatTier().name()));
            linhas.add(Component.translatable("nenfoundation.enemy.debug.info_estados",
                    inimigo.awarenessState().name(), inimigo.combatState().name()));
        } else {
            linhas.add(Component.translatable("nenfoundation.enemy.debug.info_sem_contrato"));
        }

        if (entidade instanceof Mob mob) {
            LivingEntity alvo = mob.getTarget();
            linhas.add(Component.translatable("nenfoundation.enemy.debug.info_alvo",
                    alvo == null ? semNada() : Component.literal(alvo.getName().getString())));
        }

        Optional<EnemyDebugView> visao = entidade instanceof BaseHxHMob base
                ? base.visaoDeDebug() : Optional.empty();
        if (visao.isEmpty()) {
            linhas.add(Component.translatable("nenfoundation.enemy.debug.info_sem_runtime"));
            return linhas;
        }

        EnemyDebugView v = visao.get();
        linhas.add(Component.translatable("nenfoundation.enemy.debug.info_ataque",
                v.faseDeAtaque().name(), v.recargaRestante(), v.instanciaDeAtaque()));
        linhas.add(Component.translatable("nenfoundation.enemy.debug.info_stagger",
                texto(v.staggerAcumulado()), String.valueOf(v.cambaleando())));
        Component lembrado = v.alvoLembrado()
                .map(uuid -> (Component) Component.literal(
                        uuid.toString().substring(0, PREFIXO_DE_UUID)))
                .orElseGet(EnemyDebugCommands::semNada);
        linhas.add(Component.translatable("nenfoundation.enemy.debug.info_alvo_lembrado",
                lembrado, v.varredurasDePercepcao()));
        return linhas;
    }

    private static Component semNada() {
        return Component.translatable("nenfoundation.enemy.debug.info_nenhum");
    }

    private static int congelar(CommandContext<CommandSourceStack> ctx, boolean congelar) {
        int alcancados = CongelamentoDeIa.varrer(ctx.getSource().getServer(), congelar);

        ctx.getSource().sendSuccess(() -> Component.translatable(congelar
                ? "nenfoundation.enemy.debug.congelado"
                : "nenfoundation.enemy.debug.descongelado", alcancados), true);

        // O AVISO SAI JUNTO DO "ON", TODA VEZ. A flag NoAI e gravada no NBT da
        // entidade; o interruptor desta classe nao e. Quem congelar e reiniciar
        // o servidor encontra uma arena parada e um interruptor dizendo "off" --
        // sem erro, sem log, sem nada na tela.
        if (congelar) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "nenfoundation.enemy.debug.congelamento_aviso"), false);
        }
        return alcancados;
    }

    private static int limpar(CommandContext<CommandSourceStack> ctx, double raio) {
        ServerLevel nivel = ctx.getSource().getLevel();
        Vec3 centro = ctx.getSource().getPosition();
        Set<ResourceLocation> fila = idsDaFila();

        AABB area = new AABB(centro, centro).inflate(raio);
        List<Entity> alvos = new ArrayList<>();
        for (Entity candidato : nivel.getEntities((Entity) null, area, e -> daFila(e, fila))) {
            // A caixa e um CUBO; o raio pedido e uma esfera. Sem esta segunda
            // conferencia, um `clear 32` alcancaria 55 blocos nas diagonais --
            // e apagaria, sem erro nenhum, o bicho que a outra pessoa estava
            // medindo do outro lado da arena.
            if (candidato.position().distanceToSqr(centro) <= raio * raio) {
                alvos.add(candidato);
            }
        }

        if (alvos.isEmpty()) {
            return recusar(ctx, Recusa.NADA_PARA_REMOVER, texto(raio));
        }

        for (Entity alvo : alvos) {
            // discard(), e nao kill(): matar dispara evento de morte, dropa
            // loot e conta para o bestiario. Uma ferramenta de limpeza que
            // premia o operador com loot nao da erro -- ela contamina a
            // proxima medicao, e ninguem liga o drop ao comando.
            //
            // discard() passa por remove(), que e onde BaseHxHMob limpa o
            // runtime. A limpeza continua no ciclo de vida de quem ligou.
            alvo.discard();
        }

        final int removidos = alvos.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "nenfoundation.enemy.debug.removidos", removidos, texto(raio)), true);
        return removidos;
    }

    // ---------------------------------------------------------------- apoio

    /**
     * O inimigo da fila que a mira do operador atravessa, o mais proximo.
     *
     * <p>Medido com o olho e o angulo QUE O SERVIDOR TEM. O cliente nao informa
     * qual entidade ele acha que esta mirando: aceitar essa informacao
     * funcionaria com todo cliente honesto, e e exatamente por isso que nenhum
     * playtest acharia o problema.
     */
    private static Optional<Entity> inimigoNaMira(ServerPlayer operador) {
        Vec3 olho = operador.getEyePosition();
        Vec3 fim = olho.add(operador.getLookAngle().scale(ALCANCE_DA_MIRA));
        AABB faixa = operador.getBoundingBox()
                .expandTowards(operador.getLookAngle().scale(ALCANCE_DA_MIRA))
                .inflate(1.0D);
        Set<ResourceLocation> fila = idsDaFila();

        Entity melhor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (Entity candidato : operador.level().getEntities(operador, faixa,
                e -> daFila(e, fila))) {
            Optional<Vec3> toque = candidato.getBoundingBox().inflate(FOLGA_DA_MIRA)
                    .clip(olho, fim);
            if (toque.isEmpty()) {
                continue;
            }
            double distancia = olho.distanceToSqr(toque.get());
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = candidato;
            }
        }
        return Optional.ofNullable(melhor);
    }

    /** O jogador da fonte, ou {@code null} -- console e bloco de comando caem aqui. */
    private static ServerPlayer jogadorOuNulo(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException semJogador) {
            return null;
        }
    }

    /** Recusa com motivo traduzido. Devolve zero, que e o codigo de falha. */
    private static int recusar(CommandContext<CommandSourceStack> ctx, Recusa motivo,
            Object... argumentos) {
        ctx.getSource().sendFailure(Component.translatable(motivo.chave(), argumentos));
        return 0;
    }

    /** Numero com uma casa e ponto decimal, independente do locale do servidor. */
    private static String texto(double valor) {
        return String.format(Locale.ROOT, "%.1f", valor);
    }
}
