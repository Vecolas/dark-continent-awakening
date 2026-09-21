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
| `CabanaDeCheckpointTest` | que a cabana de cada checkpoint tem madeira embaixo (galho ou eixo central), que ela nunca nasce abaixo do pé da árvore, que a âncora cai na janela de `nearest()`, que o jogador pousa dentro dela, e que o checkpoint é escrito antes da copa, e que o poço de luz limpa a folha do piso para cima | **que ela é encontrável** — isso foi reduzido a "tem céu e tem luz", que é necessário e não suficiente: ninguém mede se há rota até lá nem a que distância a luz vence a névoa. E **que a cabana existe em tela**. Nenhum portão coloca um bloco: porta virada para o vazio, pilar torto ou telhado atravessando um galho só aparecem numa captura. E **que dá para subir**. Alcance de pulo, espaço livre acima da varanda e se a rota entre dois checkpoints é escalável continuam sem régua. E o relevo do Overworld não entra na conta: com `y` fixo, um balcão pode nascer dentro de uma encosta |
| `TexturaDaFolhaLuminosaTest` | que a textura é 16x16, majoritariamente verde, com dourado esparso e AGRUPADO, com recorte de folha, e sem emenda ao ladrilhar | **que ela é bonita**, e que ela é o que o gerador Python produz hoje — o portão não fala Python e não regera o PNG. Um PNG editado à mão que satisfaça as cinco propriedades passa, e o gerador vira ficção |
| `CaminhosDeDatapackTest` | que nenhum diretório de datapack ficou com nome de 1.20 (o jogo os ignora em silêncio), e que todo bloco com loot table está numa tag `minecraft:mineable/*` -- as duas coisas que um bloco precisa para dropar | **que o drop acontece**. Ele cruza arquivo com arquivo; não quebra um bloco em jogo, não sabe se a ferramenta escolhida é a certa para o material, e não vê um bloco que exista em código e não tenha loot table nenhuma |
| `TraducaoDeConteudoTest` | que todo bloco e item que a FONTE registra tem nome em `en_us` **e** em `pt_br`, que nenhuma chave de nome sobrou sem conteúdo correspondente, e que os dois idiomas cobrem o mesmo conjunto de chaves | **que o padrão de registro continua sendo o que ele sabe ler.** Ele casa `BLOCKS.register…("id"` e `ITEMS.register…("id"` como TEXTO: um helper novo, um laço sobre uma lista de ids ou um registro montado por concatenação passa despercebido, e o conteúdo fica sem cobertura sem que nada reprove — a única defesa é o piso de contagem, que denuncia a regra casando nada, não a regra casando de menos. E **nada sobre o texto**: nome errado, tradução trocada entre os dois idiomas ou a palavra "TODO" escrita no valor passam verdes |
| `HudAssetsTest` | os cinco PNGs modulares da HUD existem sem sobra, preservam transparencia e mantem as dimensoes esperadas | qualidade visual em jogo, recorte da cabeca, legibilidade do badge ou alinhamento com cada GUI Scale |
| `runClient` | o mod carrega num cliente, os listeners sobem, e — com `-PentrarEm=host:porta` — ele entra num servidor sozinho e recebe payload de verdade | comportamento com DOIS jogadores; latencia real; qualquer coisa desenhada na tela, que ninguem olhou |
| `runServer` | o mod carrega num servidor dedicado | comportamento com dois jogadores, que e onde desync aparece |
| `NenCommandsTest` | a arvore de `/nen` tem uma raiz so, ela exige permissao (provado contra um controle), nenhum comando escapa dela, e `reset` so e executavel com `confirmar` | que os comandos FAZEM o que dizem — nenhum foi executado contra um jogador de verdade |
| Ficha do jogador (#44) | projeção e paginação testadas; um cliente real mostrou unlock/lock em servidor dedicado, com capturas inspecionadas | teclas físicas, dois jogadores e conflitos com outros mods; ver [hud-jogador.md](hud-jogador.md) |
| `runGameTestServer` | jogo carregado, perfil/runtime e 120 ticks de Aura pelo scheduler real; **145 cenários**, entre eles os inimigos: geometria do ponto fraco, fase de ataque voltando para IDLE, manada não se ferindo, e o agarrão do sapo montando, recusando desmontagem e soltando por tempo e por morte | rede física, desenho do cliente e performance não são provados pelo listener de captura; a vítima dos cenários de inimigo é um golem, não um jogador |
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
| **A interrupção nunca foi vista acontecendo** | quatorze dos dezessete perfis de stagger eram inalcançáveis por aritmética, e nada reprovava. Hoje `StaggerPorPapel` deriva os números e `StaggerAlcancavelTest` reprova o perfil morto — mas a régua é deliberadamente **otimista**: ela alimenta o acumulador com o dano CRU da arma, ignorando a armadura do próprio mob, que em jogo reduz esse número antes de ele virar stagger. Um perfil que não fecha nem assim está morto com certeza; **o contrário não vale.** Passar ali não prova que a interrupção acontece no ritmo certo, que é legível na tela, nem que o número de acertos é o valor divertido | QA manual dos inimigos |
| Nenhum dos números novos de stagger foi JOGADO | "cai em cinco acertos seguidos" é uma decisão de design que ninguém testou com as mãos. Cinco pode ser frustrante e dois pode ser trivial; a régua só garante que o botão está vivo, não que ele está no lugar certo | sessão de balanceamento |
| A ausência só é medida onde alguém pensou em medi-la | `CorpoDeTodoInimigoTest` cobre geo, animação, textura e renderer dos inimigos publicados. Nada garante que a próxima coluna esquecida seja notada antes de aparecer em jogo | disciplina, não portão |
| Os seis oficiais de Chimera não têm gametest | teia, dreno, sequência de golpes, arranque com fadiga, veneno e comando aéreo estão provados como **regra pura**. Que a entidade chame essas regras em todo caminho de saída, e que `Goal`, navegação e sincronização de fase se comportem com um `Level` real, é afirmação lida no código | gametest dos oficiais |
| O recrutamento cruzado entre esquadrões nunca rodou | `cheetah_leader`, `scorpion_leader` e `avian_commander` fundam bando e recrutam `BaseChimeraAnt` vizinhas — inclusive as de outra frente, que podem já ter lógica de esquadrão própria. Não é crash: é comportamento cruzado não exercitado | gametest com duas espécies |
| O Nen das formigas decide e não ativa | toda formiga entra com fração de aura `0.0`, declarada, porque o núcleo não publica aura de mob (#126). Os ramos `ESCONDIDA`, `GUARDAR`, `ROMPER` e o veto de mergulho por Zetsu existem, têm teste unitário e **nunca disparam numa partida hoje** | quando o núcleo publicar pool de mob |
| Gametest que TRAVA não reprova — fica pendurado | o deadlock do posto natural mostrou o pior modo de falha do `runGameTestServer`: o servidor para de carregar o mundo, nenhum cenário roda, nada é impresso e o processo fica vivo. Rode sempre com `timeout`, e desconfie de ausência de resumo tanto quanto de falha | disciplina, não portão |
| O peixe nunca NADOU até a isca | `MasterOfTheSwampGameTest` prova em jogo a bocada, a linha arrebentando com quem foge e a recusa de isca fora d'água — mas os três entregam o anzol na boca. A navegação aquática até a isca, a captura com a mão vazia e o cansaço por tempo continuam sem cenário | gametest de navegação |
| O julgamento do Kiriko nunca rodou em jogo | `RegrasDeJulgamento` é pura e a régua do perfil garante que um golpe reprova da melhor nota possível. Mas nascer disfarçado, não adotar alvo sozinho, aprovar depois de 200 ticks parado, largar a recompensa e ir embora, e reprovar por ferir uma vaca na frente dele são cálculo, não comportamento medido | gametest do Kiriko |
| A recompensa da aprovação é a mesma da morte | a ficha diz que o prêmio do Kiriko é quest/bestiário; hoje aprovar e matar caem na mesma loot table, então o que separa os dois caminhos é só o trabalho | quando houver bestiário |
| O veredito do Kiriko não sobrevive ao save | exame e reprovação são runtime (ADR-002, deliberado): recarregar devolve um kiriko que recomeça a observar, e um reprovado volta pendente. É escolha, não descuido — e não foi exercitada | gametest com restart |
| A captura e a morte a pancada dão o mesmo loot | a ficha diz que o prêmio da captura é bestiário, conquista e quest; nada disso existe ainda, então o que separa os dois caminhos hoje é só o trabalho, não a recompensa | quando houver bestiário |
| Os mobs continuam mudos, mas **não por ferramenta** | a máquina segue sem `ffmpeg` e sem `oggenc`. O que mudou é que `pip install soundfile` traz o `libsndfile`, e ele escreve **OGG Vorbis de verdade** — conferido byte a byte no cabeçalho (`OggS…vorbis`). O bloqueio deixou de ser de ambiente e passou a ser de escopo: som é EN13, e nenhum dos oito mobs registrados emite nada | PR de identidade sonora (EN13) |
| O Great Stamp é mudo | a diretriz exige sons próprios e ele não tem nenhum. Não empresta som de vanilla: simplesmente não emite. Nenhum portão reprova silêncio, então isto fica escrito em dois lugares | PR de identidade sonora |
| O Foxbear nunca foi visto NASCER num mundo gerado | a issue #266 lhe deu `SpawnPlacement` lendo a faixa de luz do perfil (`0..12`), tag de bioma e biome modifier — os três eram ausentes, e por isso ele nunca nascia. Que agora ele nasça, e em densidade jogável, é mundo gerado e olho humano: nenhum portão gera terreno | playtest de floresta |
| A faixa `0..12` do Foxbear entrou em vigor sem nunca ter valido | o número estava escrito no perfil desde o começo e não chegava a lugar nenhum; ligá-lo é correção de botão morto, não decisão de balanceamento. Os outros seis usam `0..15`. Se `12` não era a intenção, o sintoma será um urso que quase só aparece na sombra | sessão de balanceamento |
| `FilaUnicaDeInimigosTest` não sabe se o placement é o CERTO | ele exige que todo id registrado TENHA placement, atributos, perfil, loot e tradução — nunca que o placement combine com a espécie. Um peixe registrado com `ON_GROUND` passa | olho humano |
| Ninguém viu a ave voar | o voo, a batida de aviso, a subida do telégrafo e o mergulho são lidos do estado publicado pelo servidor; o `COLAGEM_VERTICAL = 1.05F` que cola o tronco na hitbox é conta, não observação. Só `runClient` à mão responde | QA manual dos inimigos |
| A ave não foge e não se salva na água | sem `FloatGoal` (ligaria `setCanFloat(true)` contra o `false` da navegação, criando duas fontes para a mesma verdade) e sem Goal de fuga: com vida crítica ela só muda de `combatState`. É escolha declarada no código, não descuido — mas o custo em jogo nunca foi visto | quando houver playtest de canyon |
| A fundação EN1 ainda não tem prova visual ou multiplayer | percepção com orçamento, stagger, `AttackController` ligado à entidade, perfis de spawn e o `EnemyRuntime` têm 60+ testes puros; o servidor já executou os GameTests. Ainda faltam cliente, dois jogadores e julgamento humano | gate do EN1 (#139) |
| O orçamento de percepção conta CHAMADAS, e não milissegundos | `PerceptionController.varredurasFeitas()` prova que a varredura cara não roda todo tick. Quanto custa cada varredura, e o que acontece com quarenta mobs num chunk, continua sem régua | EN15, com `spark` |
| `SafeReleaseSpot` não tem consumidor | ele existe, está testado e nenhum mob o chama: o sapo usa a desmontagem do vanilla, que já resolve o caso dele. A soltura dentro da pedra continua possível para o primeiro mob que segure sem montar | quando o Melanin Lizard ou o Crab Heavy soltarem alguém |
| `DummyEnemyEntity.ouvir` não tem produtor | a audição existe, está coberta por teste e **nada no jogo a alimenta**. Um mob que só ouve o que ninguém emite é um sentido desligado que parece ligado | quando o Radio Rat emitir o primeiro relatório |
| A reconciliação de encontro depois do restart é cálculo, não observação | o controlador sabe distinguir "a entidade sobreviveu" de "o chunk está dormindo", e essa distinção é o que impede dois chefes. Nenhum servidor foi reiniciado no meio de um encontro | gate do EN4 (#141) |
| Nenhuma trava de recompensa passou por disco | o `RewardLedger` e o `EncounterSavedData` foram provados por ida e volta em memória. A corrida que eles impedem é justamente a que **sobrevive ao restart** | gate do EN5 (#142) |
| As sete criaturas de Greed Island ainda não foram vistas em jogo | EntityType, atributos, loot, tradução, perfil, geo, animação, textura e renderer existem; o smoke GameTest confirma inicialização dos sete. Aparência, animação, som, telegraph, hitbox e legibilidade continuam manuais | primeiro `runClient` completo |
| O passe EN13 de áudio e legibilidade ainda não foi aprovado visualmente | O catálogo automatizado cobre vozes, arquivos OGG, subtitles e bordas de telegraph; ainda falta confirmar no cliente o volume, a leitura sem cor, o modo de gráficos baixos e que VFX não escondem hitboxes | `runClient` com os 23 encontros |
| A matriz EN14 ainda não foi executada com 2 e 4 jogadores reais | Os testes cobrem as corridas de ledger/card, captura, disconnect durante grab, unload de líder e participantes; ainda falta servidor dedicado com 1/2/4 jogadores, ping alto e observador Gyo | `runServer` dedicado e dois clientes adicionais |
| Gyo específico por observador não está provado | A porta de percepção Chimera permanece neutra até a issue de núcleo #126 entregar Gyo; implementar essa regra em inimigos criaria uma segunda autoridade de Nen | conclusão da #126 e novo teste multiplayer |
| Nenhuma criatura de Greed Island nasce naturalmente | a dimensão existe com terreno próprio, mas os sete perfis são `ENCOUNTER_ONLY` e não possuem placement natural; o acesso e os encontros podem ser acionados por comando | quando houver spawner natural ou encontro jogado em sessão |
| A fundação natural de Chimera ainda não foi observada em servidor real | o produtor por chunk é determinístico, raro, persistente e protegido contra duplicação; o GameTest prova materialização de três peões, mas não observa exploração de um mundo novo nem restart de disco | QA manual EN8/EN9 |
| A intenção de Nen da Chimera não ativa nada | `TacticalNenController` decide QUANDO usar Ten, Ren, Gyo, Zetsu e Ken; a ligação com o registro de técnicas **não existe**, e não podia nascer no pacote de inimigos sem virar a segunda autoridade sobre Nen | integração com o núcleo |
| A porta de aura dos inimigos continua inerte, nos dois lados | `PercepcaoDeAura.NENHUMA` é a única implementação, e `PercepcaoDeAuraDeChimera.DORMENTE` é literalmente ela. Enquanto a #126 (Gyo) estiver aberta, quem-vê-o-quê é autoridade do núcleo | #126 |

## O que os portões da COPA da World Tree não provam

Detalhe completo em [`../worldtree/copa-e-folhagem.md`](../worldtree/copa-e-folhagem.md),
seções 5-B a 5-E. O resumo do que fica **sem prova**:

| Limite | Por que ele existe | Quando some |
| --- | --- | --- |
| **Nenhum bloco da copa foi visto em jogo** | tudo o que existe é geometria pura medida em JUnit e a projeção desenhada por `SilhuetaDaCopa`. O renderer do Minecraft, com luz, oclusão e névoa, não foi consultado | primeiro `runClient` voando pela copa |
| "**Madeira nua**" não é "madeira **visível**" | `madeiraDoGalhoAparece` anda pelo eixo do galho e pergunta se algum disco **daquele** galho cobre o ponto. Ela não sabe de oclusão, de distância, nem da copa de um galho **vizinho** passando por cima. É condição necessária | captura comparada, por gente olhando |
| O teto de custo por chunk é **visitas**, não milissegundos | 800.000 visitas no pior chunk protege contra regressão de ordem de grandeza; não afirma que o custo atual é aceitável | `runServer` voando pela copa, com tempo de geração medido |
| **O custo de LUZ das folhas luminosas nunca foi medido** | 15% da copa emite luz 15, e a propagação é um BFS por fonte. O agrupamento do campo é a mitigação escolhida — e é uma hipótese, não uma medida. Se a dimensão engasgar ao carregar a copa, este é o primeiro suspeito | `runServer` com `spark`, medindo o tempo de luz por chunk |
| O índice espacial prova **equivalência**, não velocidade | `indiceNaoMudaResultado` garante que ele devolve o mesmo que o laço linear, e `chunkDistanteNaoTemCandidato` garante que um chunk longe custa zero candidatos. O ganho em tempo real não foi cronometrado | idem |
| A fração de 15% de folha luminosa é **amostrada** | uma prateleira a cada treze, com passo proporcional ao raio. O intervalo por seed vai de 11,9% a 17,2% — a dispersão é da copa, não da amostragem | nunca; varrer tudo custaria mais que a suíte inteira |
| **Mundo já gerado não muda** | chunks existentes ficam com a copa antiga, e a fronteira entre o gerado e o novo é visível. Vale para qualquer mudança de worldgen; aqui a diferença é grande o bastante para notar | nunca; é a natureza de worldgen |
| A **identidade de Hunter × Hunter** não é mensurável | as três folhas por altitude, a luminosa e a resina são a tentativa. Se ler como floresta genérica, é ajuste de paleta | nunca por portão |

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
| Os seis fatores por região **nunca foram vistos diferentes de 1.0 em jogo** | o renderer multiplica por região desde o AV1 e o overlay F6 agora mostra os seis — mas **nada no jogo produz distribuição desigual ainda**: Gyo, Ko e Ryu são marcos de Nen, não da trilha AV. O caminho todo está provado em JUnit com os valores de Gyo e de Ko, e não em tela | quando a primeira técnica de foco existir (F1) |
| "Não vaza por parede" é teste de olho | o gate do AV5 é uma captura com o jogador atrás de um bloco; um vazamento de poucos pixels em ângulo raro passa | sem previsão |
| Bloom `FAST` e `HIGH` são duas fontes do mesmo halo | vão divergir com o tempo; a trava é captura de comparação arquivada, e não um portão | sem previsão |
| **A bancada de captura nunca rodou em jogo** | `/nenvfx`, o overlay F6, os sliders e o lote têm teste unitário da lógica sem tela (614 testes verdes), e **nenhum deles foi digitado num cliente**. O que pode falhar em jogo: o comando não aparecer, o lote fotografar o quadro errado, o modo de captura não devolver a câmera | na primeira sessão de `runClient` que produzir um lote |
| O modo de captura trava o **input para pose idle** e pede quatro blocos de câmera, não a posição física | knockback e outros impulsos do servidor ainda movem o jogador; a colisão vanilla aproxima a câmera quando há parede atrás, de propósito | nunca por completo; a arena A/B precisa ter piso estável e quatro blocos livres atrás |
| A série de distância do jogador LOCAL não exercita LOD | a distância dele para a própria câmera é zero, e o nível fica sempre `FULL`. Uma série `2b..40b` tirada em si mesmo mede tamanho na tela, e não a tabela de corte | quando a série for tirada com um segundo jogador |
| O contador de chamadas de desenho não é tempo de quadro | ele conta descargas de lote de aura, o que é um proxy. Memória, tempo de quadro e alocação por quadro continuam sem régua | AV8, com `spark` |
| O commit no nome da captura é o commit do **build**, não o da árvore | com mudança não commitada em cima, a imagem aponta para um código que não é exatamente o que gerou ela | nunca; é o custo aceito por não ter git em runtime |
| O áudio de Ten ainda não foi ouvido por duas pessoas | o portão automático prova o contêiner Ogg, mono, duração, registro, legenda e borda `OFF → TEN`; não prova que o timbre evita eletricidade, que a atenuação parece natural nem que vinte minutos sem loop são a decisão certa | gate humano do AV3 (#187) |
| Os **143 GameTests nunca tinham rodado** | `runGameTestServer` crashava no primeiro lote desde o EN13 (#148): `EnemySoundEvents` registrava 120 sons num `DeferredRegister` já fechado, porque nada carregava a classe durante o registro — o único caminho até ela era `BaseHxHMob.getVoice()`, chamado quando um mob tenta falar. Corrigido; no merge da trilha AV na `main` (2026-09-21) o servidor respondeu **"All 143 required tests passed"** | corrigido |
| **Quatro GameTests de encontro falhavam**, e estavam mascarados pelo crash | `reconciliarcombichovivonaoduplica`, `matartudoconcluiedestravaarecompensa`, `oencontroarmadocolocacriaturanomundo`, `encontroabandonadofalhaemvezdeficarpreso`. Corrigidas na lane de inimigos por `40ec661` (PR #294), que chegou à `main` por um caminho diferente da trilha AV. O merge das duas pontas passou 143/143 | corrigido |
| A **distância máxima** nunca foi girada em jogo | `vfx.distanciaMaxima` ganhou consumidor (`AuraLodEfetivo`) e teste; que o corte se comporte bem com um segundo jogador a 30, 50 e 80 blocos é captura | gate do AV7 (#205) |
| **Nenhum número de performance foi medido** | o que existe são propriedades *estruturais*: curva escrevendo em vetor de fora, `RenderType` constantes, malhas assadas uma vez, perfil interpolado memorizado, tetos fora da config. Taxa de alocação, tempo de quadro, tempo de GPU e memória dos alvos continuam sem régua — isso é perfil de `spark` ou JFR arquivado, com 10 jogadores em Ren | #206, e `spark` precisa entrar em `supported-mods.md` antes |
| O **orçamento de ~20% do tempo de frame** é um alvo, e não uma medição | ele está escrito em `perfis-visuais.md` §12 e nunca foi comparado com nada. Aplicar otimização antes de ter a medição arquivada é otimizar o terceiro item da ordem de custos e relatar vitória | #206 |
| O **tick time do servidor dedicado** com 10 jogadores em Ren nunca foi comparado com 10 parados | se ele for distinguível, algo de VFX vazou para o servidor — e isso é **P0**, não tuning. A verificação exige `runServer` e uma sessão combinada | #206 |
| O **memo de perfil** é de UMA entrada, e isso é escolha | dez jogadores em estados *diferentes* intercalados erram o memo e recalculam — que é o comportamento de antes, e não uma regressão. Quantos acertos ele tem de verdade, num jogo real, não foi medido | #206 |
| A **silhueta screen-space** não foi feita, e não vai ser no MVP | a aura aparece por fora da armadura **padrão** engrossando a borda (0.065 em Ten, 0.090 em Ren, por peça vestida). Uma peça de outro mod arbitrariamente grande engole qualquer deformação que caiba no teto de design, e perseguir isso é perseguir o infinito ([ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md)) | nunca, por decisão |
| **Armadura de outros mods** está fora | mesma razão da linha acima; é custo assumido, e não lacuna a descobrir | nunca, por decisão |
| **Nenhuma das nove poses foi capturada** | a shell recebe a pose do `PlayerModel` por cópia — nunca por `setupAnim` —, e as colunas verticais são suprimidas nas poses horizontais (dormindo, nadando, elytra) porque o eixo do osso aponta para o lado. Que a película fique *aderida* em cada pose é olho humano, nos dois modelos | gate do AV7 (#205) |
| **`AuraLivingRenderLayer` não existe, e é deliberado** | o adaptador (`AuraModelAdapter`, `HumanoidAuraAdapter`, `GeoAuraAdapter`, `AuraOssosDoInimigo`) existe e tem teste. A *layer* não: `AuraVisualSystem` só sabe responder por `Player`, e nenhum mob tem Nen antes do EN10 (#145). Uma layer registrada sobre um estado que não pode existir é régua que mede o vazio — o próprio `perfis-visuais.md` §1 chama isso de cerimônia | EN10 (#145), junto do primeiro inimigo com Nen |
| O **`GeoAuraAdapter` nunca desenhou nada** | ele resolve osso por nome declarado e recusa com motivo — uma linha por osso, nunca por frame —, e isso tem teste. O que não existe é a *shell* sobre um `GeoBone`: inflar um bone exige reconstruir os cubos dele com folga, e isso é trabalho do EN10. Até lá um mob GeckoLib receberia filamentos e não a película | EN10 (#145) |
| **Capa e elytra não recebem tratamento nenhum**, e isso é a entrega | elas entram na leitura pela luz do halo, que é composite de tela e cobre o que estiver lá. O que foi decidido é que elas **não** ganham shell própria nem ribbons. Que isso baste — e que a elytra planando não deixe rastro — é captura | gate do AV7 (#205) |
| **"O cliente de B não recebe o dado de A" não é verificável por teste de unidade** | olhar a tela prova que o cliente *escondeu*, e nunca que ele *não recebeu*. O item de rede é inspeção do delta com log de payload em modo dev, com dois clientes reais — é o único item do gate #201 que não se verifica olhando | gate do AV6 (#201) |
| A coluna **observador** do resolvedor nunca foi exercitada | `gyoDoObservador` e `inDoAlvo` chegam sempre `false`: Gyo e In são marcos de Nen (F1 e F2), e o laço por observador no servidor depende de #126. A tabela inteira está fixada em JUnit com os dois valores; em jogo, só a linha trivial rodou | quando Gyo existir (#126) |
| A inspeção do AV6 será feita com um **cliente honesto** | ninguém construiu um cliente modificado para tentar ler o que não deveria chegar. A defesa real é o servidor mandar `NENHUM` — o mesmo byte de quem nunca despertou —, e isso é decisão de protocolo, não de render | nunca por este gate |
| O **pulso de supressão** nunca foi visto | ele é HUD e não aura, então não há por onde vazar para um observador — isso é por construção. Que ele leia como *fechamento* e não como *dano recebido* é olho humano | gate do AV6 (#201) |
| O **modo permissivo** de visibilidade nunca foi usado em jogo | `/nenvfx permissivo` acende o aviso de sobreposição no overlay, e uma captura tirada com ele ligado não vale como aprovação. Que o aviso apareça de verdade na tela não foi verificado — como o resto da bancada de captura | primeira sessão de `runClient` |
| O **passe de brilho nunca rodou numa GPU** -- e na primeira vez que rodou, **derrubou o cliente** | a cadeia compila e tem teste da aritmética. O que faltava era um pixel. Na primeira sessão de `runClient` (2026-09-21) o passe de brilho pediu um segundo `VertexConsumer` enquanto o primeiro ainda estava na mão, e o `BufferSource` encerrou o primeiro por dentro: `IllegalStateException: Not building!` ao ligar Ren com dois jogadores. Corrigido em #299, com portão estrutural (`LoteDaAuraTest`). **O resto da cadeia continua sem um pixel visto** | gate do AV5 (#198) |
| **"O halo respeita parede" é afirmação de construção** | a máscara vem de `copyDepthFrom` da cena, e o teste de profundidade está ligado no tipo de render. Que isso baste — e que o sangramento na silhueta em oclusão parcial fique em poucos pixels — é captura com o jogador atrás de um bloco, a 3, 8 e 16 blocos | gate do AV5 (#198) |
| **Vidro, folhas e água não ocluem o halo** | e isso é limite conhecido, não bug a descobrir: a profundidade é copiada depois dos blocos SÓLIDOS e antes das entidades, porque é aí que a aura é desenhada. Translúcidos vêm depois e não estão no buffer copiado | sem previsão; copiar depois exigiria redesenhar a aura fora do laço de entidades |
| **`FAST` e `HIGH` são duas fontes do mesmo halo, e `FAST` nunca foi comparado com `HIGH`** | `FAST` alarga geometria e `HIGH` sangra luz de verdade; eles vão divergir com o tempo. A trava é a captura de comparação do mesmo quadro nos três níveis, arquivada e refeita a cada mudança de qualquer um dos dois — e ela não existe | gate do AV5 (#198) |
| A **detecção de shader pack** nunca foi exercitada com um pack instalado | `DeteccaoDeShaderPack` pergunta ao Iris por reflexão e, se qualquer coisa falhar, responde "não há pack" — o caminho seguro. Que a resposta positiva chegue de verdade com Iris carregado e um pack ativo não foi verificado | matriz do AV8 (#207) |
| Os contadores de alvo **criado/liberado** nunca foram lidos depois de dez resizes | eles existem lado a lado no overlay exatamente porque framebuffer não liberado não dá erro. Ninguém os leu ainda | gate do AV5 (#198) |
| O **anel de pressão** e os **detritos** nunca tocaram o chão de um mundo real | a geometria do anel, o raio irregular, a população de detritos e os tetos têm teste sem tela; o que não foi exercitado é a sondagem de chão em declive, em escada, dentro d'água, em folhagem e sobre bloco não-cúbico. O que pode falhar: anel enterrado, anel flutuando, ou a sondagem devolvendo o bloco errado | gate humano do AV4 (#193) |
| **Nenhum bloco quebrado** é afirmação de construção, e não de medição | `hasPhysics = false`, nenhuma chamada a `BlockState` durante a vida da partícula e nenhum `ItemEntity` em caminho nenhum — mas a inspeção de `BlockState` antes e depois de 5 minutos de Ren contínuo, e a contagem de entidades, são do gate humano | gate humano do AV4 (#193) |
| A **escada de espessuras** nunca foi vista em movimento | os degraus são finos o bastante em teste (menos de 0,1 unidade de modelo na faixa de Ten a Ren), e isso é aritmética. Que a troca de degrau seja *invisível em jogo* durante a subida é olho humano | gate humano do AV4 (#193) |
| O **loop de Ren** pode estalar na emenda | todo componente é periódico no comprimento do arquivo por construção, e isso fecha a costura do sinal. O que fica fora: o Vorbis acrescenta amostras de priming na codificação, e um clique residual é possível. Não foi ouvido em jogo | primeira sessão com Ren sustentado |
| O **impulso de câmera** não foi sentido por ninguém | o pico, o término exato em zero e a ausência de oscilação estão fixados em teste; "ativar Ren vinte vezes seguidas sem que incomode" é critério de corpo, e não de assert. E que **observadores não recebam nada** está garantido por construção — o disparo mora no ramo exclusivo do jogador local — mas não foi verificado com um segundo cliente | gate humano do AV4 (#193) |
| O **zumbido de terceiros não reage ao output deles** | e não pode reagir: o sinal que chega ao cliente é deliberadamente pobre (`SinalDeAura`, três valores), e mandar o output do vizinho para o cliente esconder seria transformar o áudio num vazamento de informação. O loop do vizinho toca num volume médio fixo | quando (e se) o protocolo passar a carregar magnitude |

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
