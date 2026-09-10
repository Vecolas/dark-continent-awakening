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
| `NenProfileMigratorTest` | o migrador recusa versao invalida e futura | que uma migracao real preserva um mundo real |
| `ProtocoloCongeladoTest` | codigo e documento concordam sobre id, direcao e versao | que o handler valida o que devia |
| `IndiceDeAdrTest` | nenhum ADR esta fora do indice | que as decisoes estao sendo seguidas |
| `PacotesDeclaradosTest` | todo pacote se declara; o nucleo nao importa cliente por `import` | violacao por reflexao, por nome de classe em string ou por classe interna |
| `runClient` | o mod carrega num cliente | comportamento em servidor dedicado, que e outro classloader e outro conjunto de classes |
| `runServer` | o mod carrega num servidor dedicado | comportamento com dois jogadores, que e onde desync aparece |
| `gameTestServer` | os gametests registrados passam | o que nao virou gametest |
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

**5. Aviso nao reprova nada.**
Um `warning` no lugar de um erro e um defeito que vive para sempre. Se a
condicao e invalida, ela precisa de portao.

---

## O que FOI executado no M0 (2026-09-10)

| Comando | Resultado |
| --- | --- |
| `gradlew build` | **BUILD SUCCESSFUL**, `Testes executados: 21`, JAR gerado |
| `gradlew runServer` | **`Done (8.041s)`**, sem `NoClassDefFoundError`, gametest namespace `nenfoundation` habilitado |
| portoes alimentados com quebra deliberada | reprovaram, e os dois com a mensagem certa |

O JDK 21 usado foi um Temurin 21.0.12.1 portatil, fora do sistema. **A maquina
nao tem JDK 21 instalado** — ver a tabela de limites abaixo.

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

## Limites conhecidos AGORA (M0)

| Limite | Consequencia | Quando fecha |
| --- | --- | --- |
| **Nenhuma maquina do projeto tem JDK 21 instalado.** A verificacao usou um JDK portatil em diretorio temporario, que nao sobrevive. | ninguem consegue buildar hoje sem instalar o Temurin 21 | ao instalar; ver README |
| `runClient` **nunca foi executado** | o mod carrega em servidor dedicado; ninguem viu ele carregar num cliente | proxima sessao |
| Nunca houve **dois jogadores** | desync, vazamento de estado entre perfis e latencia sao inteiramente nao verificados | M1 em diante |
| Nenhum **gametest** existe | comportamento em jogo nao tem cobertura automatizada nenhuma | M1 |
| Os payloads sao uma **tabela, nao codigo** | a direcao esta congelada e conferida, mas nenhum byte trafega | M1 |
| Nao ha portao de **carregamento de registro** | uma definicao orfa passaria despercebida | M5 |
| Nao ha **medicao de performance** | "nao e gargalo" e opiniao | M2 (primeiro spark) |
| **CI nunca executou** | o workflow nunca rodou; o YAML pode ter erro | primeiro push |
| O repositorio esta **dentro do OneDrive** | ja causou uma falha real de build (`Unable to delete file`) | ao mover para fora |
| O `PacotesDeclaradosTest` so ve `import` | violacao por reflexao ou por nome de classe em string passa | sem previsao |
