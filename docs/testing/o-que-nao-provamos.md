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
| `runGameTestServer` | jogo carregado, perfil/runtime e 120 ticks de Aura pelo scheduler real; sete cenários | rede física, desenho do cliente e performance não são provados pelo listener de captura |
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
| Ken não defende de nada | ele é a principal defesa geral contra Nen no cânone, e não há dano de Nen (#127). Hoje ele entrega teto, dreno e uma presença própria — e nada mais | #127 |
| O sinal é o mesmo para todos os observadores | `aura_presence` não filtra por quem olha, porque a única regra de ocultação hoje depende só do alvo (Zetsu). Com Gyo (#126) a decisão passa a depender do observador e o envio vira um laço por observador — a forma do payload não muda | #126 |
| `AuraImpactState` (ripple de Ten) não tem quem o dispare | não há dano de Nen; `nen/combat/` tem só o `package-info` | #103, bloqueada por #127 |

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
