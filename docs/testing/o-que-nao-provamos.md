# O que o verde NAO prova

`gradlew build` sair com codigo 0 **nao** significa que o projeto esta sao.

Esta tabela existe para que ninguem assuma que o verde cobre mais do que cobre.
Ela e atualizada junto com cada ferramenta nova, e junto com cada limite
descoberto.

---

## Ferramenta por ferramenta

| Ferramenta | O que ela PROVA | O que ela **nao** prova |
| --- | --- | --- |
| `gradlew compileJava` | o Java compila contra o NeoForge pinado | que qualquer coisa funciona em jogo |
| `gradlew test` (JUnit) | logica pura: codec, migracao, invariantes, portoes de documentacao | nada que precise do Minecraft carregado: registro, tick, rede, mundo |
| `NenCategoryTest` | os ids estao congelados e traduzidos nos dois idiomas | que a categoria e atribuida corretamente em jogo |
| `PersistentNenDataTest` | o codec faz ida e volta em JSON | **que uma chave renomeada seria notada** — ele escreve e le com o nome NOVO nos dois lados, entao um rename passa verde. Quem pega isso e o `FixturesDeSaveTest` |
| `FixturesDeSaveTest` | um dado v1 CONGELADO continua sendo lido, campo por campo; ida e volta em NBT binario comprimido passando pelo disco; ha fixture para cada versao de schema publicada | que o **attachment** grava e le no save de um mundo real — isso e o ciclo de vida do jogador, e precisa de gametest |
| `PlayerdataRealTest` | que um `.dat` gravado por um SERVIDOR DE VERDADE, salvando um jogador de verdade, continua legivel pelo nosso codec — e que o attachment esta onde o NeoForge o poe | que o dado esteja correto em jogo; ele so prova que continua sendo LIDO |
| `NenProfileMigratorTest` | o migrador recusa versao invalida e futura | que uma migracao real preserva um mundo real |
| `ProtocoloCongeladoTest` | codigo e documento concordam sobre id, direcao e versao | que o handler valida o que devia |
| `IndiceDeAdrTest` | nenhum ADR esta fora do indice | que as decisoes estao sendo seguidas |
| `PacotesDeclaradosTest` | todo pacote se declara; o nucleo nao importa cliente por `import` | violacao por reflexao, por nome de classe em string ou por classe interna |
| `HudAssetsTest` | os cinco PNGs modulares da HUD existem sem sobra, preservam transparencia e mantem as dimensoes esperadas | qualidade visual em jogo, recorte da cabeca, legibilidade do badge ou alinhamento com cada GUI Scale |
| `runClient` | o mod carrega num cliente, os listeners sobem, e — com `-PentrarEm=host:porta` — ele entra num servidor sozinho e recebe payload de verdade | comportamento com DOIS jogadores; latencia real; qualquer coisa desenhada na tela, que ninguem olhou |
| `runServer` | o mod carrega num servidor dedicado | comportamento com dois jogadores, que e onde desync aparece |
| `NenCommandsTest` | a arvore de `/nen` tem uma raiz so, ela exige permissao (provado contra um controle), nenhum comando escapa dela, e `reset` so e executavel com `confirmar` | que os comandos FAZEM o que dizem — nenhum foi executado contra um jogador de verdade |
| Ficha do jogador (#44) | projeção e paginação testadas; um cliente real mostrou unlock/lock em servidor dedicado, com capturas inspecionadas | teclas físicas, dois jogadores e conflitos com outros mods; ver [hud-jogador.md](hud-jogador.md) |
| `runGameTestServer` | jogo carregado, perfil/runtime e 120 ticks de Aura pelo scheduler real; **94 cenários**, entre eles os inimigos: geometria do ponto fraco, fase de ataque voltando para IDLE, manada não se ferindo, e o agarrão do sapo montando, recusando desmontagem e soltando por tempo e por morte | rede física, desenho do cliente e performance não são provados pelo listener de captura; a vítima dos cenários de inimigo é um golem, não um jogador |
| `NenDespertarGameTest` | que os eventos de despertar disparam de verdade no barramento, que cancelar **impede a gravação**, que despertar duas vezes não anuncia duas vezes, e que dois jogadores despertam independentes | que o despertar tenha consequência diferente por origem — hoje `OrigemDoDespertar` só viaja nos eventos, e **nenhuma origem muda nada** |
| CI do GitHub | o build e reprodutivel numa maquina limpa | qualquer coisa que exija Minecraft rodando |
| `Task :test FROM-CACHE` no log do CI | os inputs batem com uma execucao anterior que passou | que os testes rodaram **nesta** execucao. O portao do `doLast` nao dispara em tarefa cacheada, e a linha "Testes executados" some do log. Quem confere o numero e o passo "Contagem de testes", que soma os XML |

---

## As formas de falso verde que este projeto precisa vigiar

**1. Codigo que nenhum caminho alcanca nao e verificado.**
Uma classe de habilidade que ninguem registra compila e passa em tudo. O que
fecha isso e um portao que CARREGA todo registro e exige que cada definicao
seja utilizavel — nao que o arquivo exista. Ainda **nao existe** neste projeto;
entra no M5, junto do registro de habilidades.

**2. Comportamento mais lento que um tick escapa do teste rapido.**
Regeneracao de aura, exaustao e cooldown longo levam segundos. Um gametest que
encerra em 0,1 s nunca viu nenhum deles acontecer, e fica verde sem ter olhado
nada. Ciclo longo pede suite propria — entra no M2.

**3. Suite vazia imprime aprovacao.**
Um filtro errado, um sourceSet mal configurado, uma deteccao de JUnit que
falhou: a tarefa passa com zero testes. Por isso o `build.gradle` **reprova**
quando nenhum teste executa.

**4. O detector obvio mente.**
Carregar um recurso quebrado costuma devolver um objeto invalido, e nao nulo.
Recarregar costuma dar falso positivo em qualquer coisa que ja tenha instancia
viva. Quando descobrirmos o detector confiavel de cada caso, ele fica
documentado ao lado, com o motivo de os outros nao servirem.

**5. A tarefa do Gradle pode passar enquanto o jogo dentro dela morre.**
Ja aconteceu duas vezes neste repositorio, e as duas vezes o codigo de saida
mentiu:

- `runServer` saiu 0 enquanto o mod derrubava o carregamento com
  `Cannot get config value before config is loaded`;
- `runGameTestServer` sai 0 enquanto o servidor morre com
  `No test functions were given!`.

Conclusao operacional: **nunca conclua nada a partir de `BUILD SUCCESSFUL` numa
tarefa `run*`.** A conclusao vem de procurar a linha certa no log — `Done (` para
o servidor, o resumo de gametests para o gametest.

**6. Aviso nao reprova nada.**
Um `warning` no lugar de um erro e um defeito que vive para sempre. Se a
condicao e invalida, ela precisa de portao.

---

## O que FOI executado no M0 (2026-09-10)

| Comando | Resultado |
| --- | --- |
| `gradlew build` | **BUILD SUCCESSFUL**, `Testes executados: 21`, JAR gerado |
| `gradlew runServer` | **`Done (8.041s)`**, sem `NoClassDefFoundError`, gametest namespace `nenfoundation` habilitado |
| portoes alimentados com quebra deliberada | reprovaram, e os dois com a mensagem certa |

Aquela verificacao usou um JDK portatil, fora do sistema. **Isso deixou de ser
um limite:** o Temurin 21.0.12 foi instalado na maquina em 2026-09-10, e o
`gradlew build` roda verde sem nenhum JDK portatil.

Uma pegadinha sobreviveu, e vale conhecer antes de investigar o Gradle a toa:
`java -version` nesta maquina responde **16.0.2**, porque um JDK 16 antigo
continua primeiro no `PATH`. O build nao usa o `PATH` — ele usa `JAVA_HOME`,
que aponta para o 21.

### Os tres defeitos que a execucao encontrou

Ficam registrados porque cada um e uma classe de erro, nao um acidente:

1. **Metodo estatico com o mesmo nome de um accessor de record nao compila.**
   `ActivationResult.permitido()` colidia com o componente `permitido`.
   Encontrado por `compileJava`.
2. **Ler configuracao no construtor do mod derruba o carregamento.**
   `Cannot get config value before config is loaded`. Encontrado **so** pelo
   `runServer` — a compilacao e os 21 testes passaram com o defeito presente.
3. **O portao que casava substring em prosa media o texto, e nao a coisa.**
   Ele reprovava a frase "sem custo nem dano". Substituido por comparacao de
   nomes de campo, dado estruturado.

### E o falso verde que a execucao encontrou

Dois, e sao os mais instrutivos desta entrega:

**`gradlew test` ficou UP-TO-DATE com a arvore quebrada.** Com uma quebra
DELIBERADA aplicada — direcao de payload trocada em `protocol.md`, um ADR
apagado do indice — o comando saiu **BUILD SUCCESSFUL**. Os testes nem
rodaram: o Gradle nao sabia que aqueles arquivos eram entrada da tarefa. Os
portoes estavam certos, os testes estavam certos, e mesmo assim o build
aprovou. Corrigido com `inputs.files(...)` no `build.gradle`, e re-verificado
com a mesma quebra: as duas reprovaram.

**`gradlew runServer` saiu com codigo 0 enquanto o servidor travava.** A tarefa
do Gradle teve sucesso; o jogo dentro dela nao. Ninguem pode concluir "o
servidor sobe" a partir do codigo de saida — a conclusao vem de procurar
`Done (` no log.

---

## O que foi verificado com um jogador de verdade (2026-09-10)

Pela primeira vez, um cliente e um servidor dedicado conversaram de verdade —
cliente entrando sozinho por `--quickPlayMultiplayer`, comandos por RCON.

| O que | Evidencia |
| --- | --- |
| Um jogador real entra num servidor dedicado | `Dev[/[::1]:58160] logged in with entity id 25` |
| **O payload S2C trafega, e o cliente o recebe** | `snapshot recebido #1: categoria=undetermined tecnicas=2 ...` no log do CLIENTE |
| O guarda `hasChannel` deixa passar quem PODE receber | nenhum `may not be sent`, e o snapshot chegou |
| Os comandos mutam um perfil real | `unlock` x4, `lock`, e o perfil mudou |
| `reset confirmar` imprime o dump ANTES de destruir | o dump saiu no console, depois "Perfil de Dev zerado" |
| O perfil sobrevive a desconexao e reconexao | as tecnicas desbloqueadas antes reapareceram no snapshot da reconexao |
| O attachment e gravado no save do mundo | `nenfoundation:nen_persistente` dentro do playerdata, agora versionado como fixture |

O procedimento inteiro esta em
[qa-matrix.md](qa-matrix.md) e em
`src/test/resources/saves/playerdata/LEIA-ME.md`.

---

## Limites conhecidos agora (após QA local da M2)

| Limite | Consequência | Quando fecha |
| --- | --- | --- |
| Dois clientes reais só em loopback | atraso de Internet, perda e comportamento remoto não medidos | QA de rede ampliada / M7 |
| C2S segue recusando técnicas/habilidades inexistentes | aceitação, custo, alvo e alcance exigem motores próprios | M4/M5 |
| Não há portão de carregamento de registro | definição órfã pode passar despercebida | M5 |
| Primeiro spark cobre cenário leve | não prova estresse, modpack ou ausência de regressão sob carga | M7 |
| Clone antigo continua no OneDrive | outro agente pode trabalhar lá; não sobrescrever nem apagar | coordenação dos clones |
| `PacotesDeclaradosTest` só vê imports | reflexões e nomes em strings não cobertos | sem previsão |
| Capturas de HUD são estáticas | não medem fluidez/ergonomia ou conflitos com mods | gameplay manual / M7 |
| Reconexão real M2 usou novo processo | limpeza na mesma instância só coberta por teste de cache | ampliar QA manual |
| `technique unlock` só valida namespace | não confirma registro da técnica, ainda inexistente | M4 |
| Morte/dimensão de técnicas ativas não existe nesta fase | o teste atual cobre perfil/runtime, não efeitos de combate | M4/M5 |
| Zetsu não expõe o jogador a nada | no cânone a defesa de aura some; não há dano de Nen (`nen/combat` só tem `package-info`), então hoje o único custo real de Zetsu é não liberar Output — ele é mais seguro do que deveria ser | M5 |
| Limitador vs. levantador de teto só se encontram em teste | Zetsu (único limitador) exclui Ren (único levantador), então a ordem das duas passagens de `tetoDe` não é exercitada por nada em jogo; a prova é o GameTest `quemAbaixaVenceQuemLevanta`, com técnicas falsas que convivem | quando nascer uma técnica que combine com ambos |
| A aura visual nunca foi vista em jogo | os testes cobrem precedência, curva de transição e contagem de partículas; que algo apareça na tela só `runClient` à mão responde | QA manual do M4 |
| Aura de terceiros nunca foi vista com dois clientes | o canal `aura_presence` existe e tem gametest com dois jogadores server-side, mas que o pacote vire desenho na tela do outro só a QA com dois clientes responde | gate do M4 (#91) |
| A alocação por região não foi vista em jogo | ela agora viaja no delta e move as partículas pela altura do corpo, mas que Gyo na cabeça pareça Gyo na cabeça só olhando responde | gate do M4 (#91) |
| A aura não foi sentida em jogo | Ten, Ken e Zetsu mudam o dano recebido e há gametest pelo barramento, mas se o combate ficou trivial ou injusto **nenhum teste percebe** — só jogar percebe | gate do M4 (#91) |
| Os braços não defendem nada | a faixa atingida sai da altura da fonte do dano, e não há como saber que alguém bloqueou com o braço. A aura neles é só ofensiva (Shu, Ko) até existir um sistema de bloqueio, que o vanilla não dá | #213 || O sinal é o mesmo para todos os observadores | `aura_presence` não filtra por quem olha, porque a única regra de ocultação hoje depende só do alvo (Zetsu). Com Gyo (#126) a decisão passa a depender do observador e o envio vira um laço por observador — a forma do payload não muda | #126 |
| Ko nunca foi usado num golpe real | o prazo, a concentração e a exposição têm gametest, mas acertar *dentro* da janela de um segundo é coisa que só jogando se sabe se é possível | gate do M4 (#91) |
| `AuraImpactState` (ripple de Ten) não tem quem o dispare | não há dano de Nen; `nen/combat/` tem só o `package-info` | #103, bloqueada por #127 |
| O atordoamento na parede do Great Stamp não tem gametest | a fase voltando para IDLE e o multiplicador da testa agora são medidos em jogo (`GreatStampGameTest`), mas bater numa parede a meio da corrida não: faltou arena com obstáculo | quando a arena ganhar parede |
| A carga nunca perseguiu alvo que se mexe | o gametest usa golem de ferro parado; que a corrida de 20 ticks alcance um jogador correndo é conta no papel, não medição | QA manual dos inimigos |
| Ninguém viu o Great Stamp na tela | a geometria é emprestada do hoglin e o telégrafo de cabeça baixa lê a fase sincronizada; que a pose leia como "vai investir" só `runClient` à mão responde | QA manual dos inimigos |
| A manada nunca nasceu num mundo | o biome modifier e a tag de bioma existem e o placement lê a faixa de luz do perfil, mas nenhum mundo foi gerado para conferir que o spawn natural acontece | QA manual dos inimigos |
| O multiplicador da testa não tem prova de lado único | o teste garante que resolver e catálogo não divergem, mas que nenhum OUTRO ponto do mod multiplique o mesmo dano é hoje só disciplina, não portão | quando nascer dano de Nen (M5) |
| O agarrão nunca prendeu um JOGADOR | `FrogInWaitingGameTest` prova em jogo que a vítima monta, que a desmontagem é recusada, que a soltura acontece por tempo e por morte do sapo — mas a vítima é um golem de ferro: `makeMockServerPlayerInLevel()` devolve um jogador criativo, que o sapo recusa como presa de propósito | QA manual com jogador real |
| O dano contra jogador é escalado pela dificuldade | `mobAttack` contra jogador passa por `Player.hurt`, que zera no PACÍFICO e reduz no FÁCIL; os números do perfil foram medidos contra um golem, que não sofre essa escala. A mordida deixou de decidir o agarrão por causa disso, mas o custo real da janela para um jogador continua sem medição | QA manual com jogador real |
| A recusa de desmontagem é ampla de propósito | enquanto o agarrão dura, `EntityMountEvent` é cancelado para aquela vítima — inclusive se a desmontagem vier de fora (um `/tp`, outro mod). O prejuízo está limitado aos 100 ticks; soltar de graça por qualquer efeito seria pior | quando houver razão medida para afinar |
| Ninguém viu o sapo enterrado na tela | o corpo some por `skipDraw` e sobra cabeça e olhos, e a boca abre pela fase publicada pelo servidor; o sinal da rotação da boca e a leitura de "perturbação no solo" só `runClient` à mão respondem | QA manual dos inimigos |
| O agarrão não sobrevive ao save | vítima, relógio e dano acumulado são runtime; ao recarregar, o sapo ejeta quem o vanilla restaurou como passageiro. É escolha, não descuido — e o gametest não recarrega mundo nenhum, então esse caminho continua sem prova | gametest com restart |
| O disfarce do Man-faced Ape nunca enganou um JOGADOR | o gametest prova em jogo que ele trava quando o alvo olha, avança quando o alvo vira as costas, revela com telégrafo e chama o bando — mas o observador é um golem cuja direção de olhar o teste escreve. Que um jogador real leia "ele parou porque estou olhando" é legibilidade de gameplay, e isso nenhum portão mede | QA manual com jogador real |
| Ninguém viu a troca de silhueta do macaco | disfarçado ele usa a geometria do aldeão e revelado a do piglin, escolhidas pela verdade publicada pelo servidor; que a troca aconteça no mesmo quadro que a textura, e que os braços do telégrafo subam em vez de descer, só `runClient` à mão responde | QA manual dos inimigos |
| O bando só foi medido em arena | três macacos a menos de 12 blocos revelam juntos no gametest; se o raio faz sentido num mundo gerado, com bando espalhado por uma selva, ninguém mediu | QA manual dos inimigos |
| Cenário de gametest vizinho pode conversar | o raio de bando de 12 blocos não conhece fronteira de arena, então os cenários do macaco rodam em lotes separados. Isso é precaução: nenhum caso de contaminação real foi observado, e nada avisa se um mob novo criar o mesmo alcance sem separar lote | quando houver um segundo mob com efeito em área |
| **Os ovos do Spider Eagle não existem** | o objetivo do encounter na seção 40 é roubar ovos sem matar a mãe. Este slice entrega a ave, a coleira do ninho e o mergulho — a parte que torna o roubo *possível* (quem recua é poupado, provado em gametest). O ovo em si exige um bloco de ninho, e este mod ainda não tem registro de blocos nenhum: é um PR próprio, com pacote e portão próprios | PR do bloco de ninho |
| O ninho nunca atravessou um restart de verdade | o gametest faz a ida e volta pelo mesmo caminho que o save usa (`saveWithoutId`/`load`), com a ave longe do ninho, e prova que ela não re-ancora. Que o NBT sobreviva a um servidor reiniciado de fato, ninguém mediu | gametest com restart |
| O corpo próprio do Great Stamp nunca foi visto | modelo, esqueleto, animações, textura e renderer são dele agora, e `CoerenciaDeGeckoLibTest` prova que geo, animação, textura e código concordam. Que a silhueta leia como javali gigante, que a pose de windup leia como "vai investir" e que as presas não apontem para dentro, só `runClient` à mão responde — o sinal das rotações foi calculado, não visto | QA manual dos inimigos |
| O corpo próprio do Frog-In-Waiting nunca foi visto | o enterrado virou animação de verdade (o corpo afunda abaixo de `y=0` e sobram crânio e olhos) em vez do truque de esconder partes do modelo vanilla. Que os olhos fiquem na altura certa do solo, e o quanto a mandíbula pode abrir antes de atravessar a garganta, só `runClient` responde | QA manual dos inimigos |
| O disfarce do Man-faced Ape nunca enganou ninguém na tela | ele tem duas silhuetas próprias, e modelo, textura e animação trocam pela mesma pergunta; o corpo humano persiste durante os 10 ticks da revelação para o corte não ficar seco. Se o vulto encapuzado engana a 20 blocos, se o rosto humano no corpo de primata assusta ou vira piada, e se as mãos grandes demais são pista sem ser entrega — nada disso tem régua, e a prévia ortográfica que a lane gerou não tem perspectiva nem névoa | QA manual dos inimigos |
| A envergadura da Spider Eagle é conta, não observação | a asa nasce dobrada (1,19 bloco, quase exatamente a hitbox) e só a animação abre (1,94). O número vem de girar a ponta 90° no pivô da dobra; qualquer outro ângulo dá outro alcance. Se a asa dobrada lê como "dobrada" ou como "meio aberta" à distância de jogo, só a tela responde | QA manual dos inimigos |
| Coplanaridade só foi medida na pose parada | o gerador reprova faces coplanares de mesma direção em repouso — é a única falha de modelo que nenhum portão do repositório enxerga. Duas faces que só ficam coplanares **depois** de a animação girar um osso não são cobertas por nada, e aparecem como cintilação | sem previsão |
| O Foxbear tem cinco dos doze clipes sem gatilho | o arquivo entrega os doze que a diretriz nomeia, mas `claw_swipe`, `bite`, `short_charge`, `idle_sniff` e `hurt` dependem de eventos que a entidade ainda não publica. Clipe sem quem o toque é dívida declarada, não cobertura | quando a entidade publicar ataque e dano |
| Gametest que TRAVA não reprova — fica pendurado | o deadlock do posto natural mostrou o pior modo de falha do `runGameTestServer`: o servidor para de carregar o mundo, nenhum cenário roda, nada é impresso e o processo fica vivo. Rode sempre com `timeout`, e desconfie de ausência de resumo tanto quanto de falha | disciplina, não portão |
| O peixe nunca NADOU até a isca | `MasterOfTheSwampGameTest` prova em jogo a bocada, a linha arrebentando com quem foge e a recusa de isca fora d'água — mas os três entregam o anzol na boca. A navegação aquática até a isca, a captura com a mão vazia e o cansaço por tempo continuam sem cenário | gametest de navegação |
| O julgamento do Kiriko nunca rodou em jogo | `RegrasDeJulgamento` é pura e a régua do perfil garante que um golpe reprova da melhor nota possível. Mas nascer disfarçado, não adotar alvo sozinho, aprovar depois de 200 ticks parado, largar a recompensa e ir embora, e reprovar por ferir uma vaca na frente dele são cálculo, não comportamento medido | gametest do Kiriko |
| A recompensa da aprovação é a mesma da morte | a ficha diz que o prêmio do Kiriko é quest/bestiário; hoje aprovar e matar caem na mesma loot table, então o que separa os dois caminhos é só o trabalho | quando houver bestiário |
| O veredito do Kiriko não sobrevive ao save | exame e reprovação são runtime (ADR-002, deliberado): recarregar devolve um kiriko que recomeça a observar, e um reprovado volta pendente. É escolha, não descuido — e não foi exercitada | gametest com restart |
| A captura e a morte a pancada dão o mesmo loot | a ficha diz que o prêmio da captura é bestiário, conquista e quest; nada disso existe ainda, então o que separa os dois caminhos hoje é só o trabalho, não a recompensa | quando houver bestiário |
| Sons continuam bloqueados por ferramenta | nem Great Stamp nem Frog-In-Waiting emitem som; a máquina não tem `ffmpeg` nem encoder Vorbis, e o Minecraft só toca `.ogg`. É bloqueio de ambiente, não de escopo — e enquanto durar, nenhum mob fecha a ficha do §14 | quando houver encoder |
| O Great Stamp é mudo | a diretriz exige sons próprios e ele não tem nenhum. Não empresta som de vanilla: simplesmente não emite. Nenhum portão reprova silêncio, então isto fica escrito em dois lugares | PR de identidade sonora |
| Ninguém viu a ave voar | o voo, a batida de aviso, a subida do telégrafo e o mergulho são lidos do estado publicado pelo servidor; o `COLAGEM_VERTICAL = 1.05F` que cola o tronco na hitbox é conta, não observação. Só `runClient` à mão responde | QA manual dos inimigos |
| A ave não foge e não se salva na água | sem `FloatGoal` (ligaria `setCanFloat(true)` contra o `false` da navegação, criando duas fontes para a mesma verdade) e sem Goal de fuga: com vida crítica ela só muda de `combatState`. É escolha declarada no código, não descuido — mas o custo em jogo nunca foi visto | quando houver playtest de canyon |

## O que a trilha AV não vai provar, e já se sabe disso

Aberto junto do [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md), antes
de a primeira linha ser escrita — e não depois de o gate fechar. O roteiro do
que **é** verificável está em [`av-aura-visual.md`](av-aura-visual.md).

| Limite | Por que ele existe | Quando some |
| --- | --- | --- |
| **Aparência não vira verde** | render não é unit-testável; o que se prova sem tela é transição, LOD, visibilidade, seed e orçamento. O resto é captura comparada, por gente olhando | nunca; o método é a captura arquivada |
| Captura é imagem parada | *jitter*, cintilação e engasgo aparecem em movimento e somem numa imagem estática | quando houver vídeo curto no protocolo |
| Uma GPU só | todo número de performance desta trilha foi medido numa máquina | quando a segunda pessoa medir na dela |
| Quatro ambientes de renderização, e existem dezenas | vanilla, Embeddium, Iris sem pack e Iris com pack cobrem o comum, não o real | nunca por completo; a matriz cresce por relato |
| Skins de teste não são skins de jogador | transparência, overlay completo e desenhos fora do padrão vão achar casos que Steve e Alex não acham | por relato de jogador |
| `AuraVisualPreset` tem campos que ninguém lê | `shellScale` e `edgeIntensity` existem desde o M4 sem renderer; são config órfã dentro do código | AV1, quando o perfil de datapack os substituir |
| O renderer ainda não lê a distribuição | o delta já carrega a alocação e as **partículas** já se movem por ela (#211), mas nenhuma geometria multiplica intensidade por região — o consumidor visual do [ADR-014](../adr/ADR-014-alocacao-de-aura-por-regiao.md) só nasce com a shell | AV1 (#175) |
| "Não vaza por parede" é teste de olho | o gate do AV5 é uma captura com o jogador atrás de um bloco; um vazamento de poucos pixels em ângulo raro passa | sem previsão |
| Bloom `FAST` e `HIGH` são duas fontes do mesmo halo | vão divergir com o tempo; a trava é captura de comparação arquivada, e não um portão | sem previsão |

As antigas alegações de impossibilidade de dois clientes e falta de resync
foram superadas pela QA M1/M2. A evidência atual está em
[qa-matrix.md](qa-matrix.md) e [m2-aura-sync.md](m2-aura-sync.md);
não devem ser copiadas como bloqueios atuais.

## Falso verde corrigido na M2

O motor e o HUD tinham testes isolados, mas não havia registro do motor no
scheduler de produção. A integração agora é exercitada por 120 ticks de
GameTest e por dois clientes reais. Os sete GameTests passam; o listener do
novo cenário captura envelopes e não finge provar rede física.

A remoção deliberada do registro em código de produção foi bloqueada pela
revisão automática e não executada. Entradas inválidas, canal ausente, exceção
de transporte e mutação reentrante foram exercitados em testes isolados.
