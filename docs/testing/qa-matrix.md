# Matriz de QA

Duas listas: o **smoke test** que se roda a cada PR relevante, e a **matriz de
aceitacao do MVP**, que e o criterio de release.

Cenario marcado como bloqueante e bloqueante mesmo. Nao existe "passa com
ressalva" nesta tabela — ou passa, ou o marco nao fecha.

---

## Smoke test (a cada PR que toca o nucleo)

Cinco minutos. Roda no perfil **dev-minimal** — NeoForge mais Nen Foundation e
nada mais.

```bash
./gradlew build              # compila + testes unitarios + portoes
./gradlew runGameTestServer  # comportamento em jogo; procure "required tests"
./gradlew runClient          # cliente abre, mundo novo carrega
./gradlew runServer          # servidor dedicado sobe sem crash
```

> **Nunca conclua nada a partir do codigo de saida de uma tarefa `run*`.** Ja
> houve duas vezes em que o Gradle imprimiu `BUILD SUCCESSFUL` com o jogo
> morto dentro. Procure a linha: `Done (` para o servidor, `required tests`
> para o gametest.

- [ ] `build` verde, e a saida diz quantos testes rodaram (nao zero).
- [ ] `runGameTestServer` diz **`All N required tests passed`**.
- [ ] Cliente abre e um mundo novo carrega.
- [ ] Servidor dedicado sobe. **Nenhum `NoClassDefFoundError`** no log.
- [ ] Entrar no servidor pelo cliente funciona.
- [ ] Sair e voltar funciona.

O terceiro item e o que pega a classe client-only vazada para o nucleo, e ele
so pega em `runServer` de verdade — nao em singleplayer, que roda um servidor
interno no mesmo processo do cliente.

---

## Matriz de aceitacao do MVP

| Cenario | Esperado | Bloqueia release? |
| --- | --- | --- |
| Jogador novo | sem Nen; HUD minimo ou oculto | **sim** |
| Despertar | perfil criado, atualizado e sincronizado | **sim** |
| Categoria | persiste depois de logout e restart | **sim** |
| Categoria escondida | nao vaza para o cliente antes da revelacao | **sim** |
| Ten, Ren, Zetsu, Gyo | ativacao, cancelamento e custo corretos | **sim** |
| Aura em zero | tecnica encerra corretamente, sem valor negativo | **sim** |
| Morte | progresso persiste; runtime reseta conforme politica | **sim** |
| Relog | nenhuma duplicacao nem limpeza indevida de progresso | **sim** |
| Troca de dimensao | snapshot correto, nenhuma habilidade fantasma | **sim** |
| Dois jogadores | estado proprio e remoto nunca se misturam | **sim** |
| Servidor dedicado | boot e jogo sem crash de classe client-only | **sim** |
| Pack completo | nenhum conflito critico das integracoes aprovadas | **sim** |
| Epic Fight | pode falhar sem bloquear o nucleo | **nao** para o nucleo; sim para a entrada dele no pack |

---

## Um jogador de verdade, sem ninguem clicar

O cliente entra num servidor local sozinho, e os comandos vao por RCON. Sem
isto, "um jogador conectado" e etapa manual — e etapa manual nao acontece toda
vez.

**1.** `run/server/server.properties`:

```properties
online-mode=false
enable-rcon=true
rcon.password=dev
rcon.port=25575
level-name=mundo-de-regressao
gamemode=creative
```

**2.** Tres terminais:

```bash
./gradlew runServer                          # 1
./gradlew runClient -PentrarEm=localhost:25565   # 2
```

**3.** Comandos por RCON no terceiro (qualquer cliente RCON serve).

### Dois jogadores ao mesmo tempo

```bash
./gradlew runClient -PentrarEm=localhost:25565 -Pjogador=Gon
./gradlew runClient -PentrarEm=localhost:25565 -Pjogador=Kurapika
```

`-Pjogador` faz duas coisas: passa `--username` e da a cada nome o PROPRIO
diretorio de execucao. Dois clientes dividindo `run/client` brigam pelo
`options.txt` e pelo log, e o segundo sobrescreve o diagnostico do primeiro --
que e justamente o que se quer comparar.

Com `online-mode=false` o UUID vem do nome, entao nomes diferentes sao
jogadores diferentes de verdade, com perfis separados no save.

> **Duas instancias do Gradle no MESMO diretorio de projeto travam uma na
> outra** (lock de execucao). Para dois clientes, use dois `git worktree` --
> o segundo pode ser `--detach` no mesmo commit.

> **ATENCAO -- LIMITE DE MAQUINA MEDIDO, NAO SUPOSTO.** Na maquina onde isto
> foi tentado (16 GB, com ~1,9 GB livres), **dois clientes Minecraft
> simultaneos nao sobem**: ambos congelam em "Loaded 0 entity animations",
> queimando CPU sem avancar. Matar um fez o outro conectar em segundos, o que
> descarta erro de configuracao e aponta contencao de recurso.
>
> Ou seja: **a QA de dois jogadores precisa de duas maquinas, ou de bem mais
> memoria livre.** Nao adianta insistir nesta.

**4.** Para observar o que o cliente RECEBEU, ligue o diagnostico em
`run/client/config/nenfoundation-common.toml`:

```toml
[dev]
	enabled = true
```

O log do cliente passa a trazer `snapshot recebido #N: ...` e
`delta recebido #N: ...`. Sem isso, "o payload chegou" so da para ver olhando o
overlay na tela — o que nao serve para relato de bug nem para verificacao.

**5.** Ao terminar: `save-all flush` e `stop` por RCON. O playerdata fica em
`run/server/<level-name>/playerdata/<uuid>.dat`, e e dele que sai a fixture de
`src/test/resources/saves/playerdata/`.

---

## Cenarios de abuso

O criterio aqui e diferente: nenhum deles produz erro no log. Todos produzem
uma partida injusta.

| Tentativa | Esperado |
| --- | --- |
| Spam de pacote de ativacao | rate limit corta; o servidor nao trava |
| Ativar habilidade nao desbloqueada | recusa com motivo; nada acontece |
| Alvo fora de alcance, atras de parede, em outra dimensao | recusa; o servidor reconstroi o alvo pelo id |
| Aura negativa forcada por pacote | impossivel: o cliente nao envia aura |
| Morrer de proposito para limpar cooldown de PvP | politica explicita e testada, nao acidente |
| Relogar para limpar cooldown | idem |
| Trocar de dimensao com habilidade canalizando | instancia encerra com `DIMENSION_CHANGE` |
| Deslogar com construct no mundo | construct e limpo; nenhum tick orfao |
| Dois clientes pedindo a mesma acao no mesmo tick | uma execucao, nao duas |

---

## Cenario de estresse (a partir do M2)

O criterio "o Nen nao e gargalo" so significa alguma coisa contra um cenario
escrito. Este e ele:

- servidor dedicado, 4 jogadores;
- todos despertos, com Ren ligado;
- 2 deles usando habilidade de Emission em cadencia continua;
- 20 mobs hostis num raio de 32 blocos.

Medicao com **spark**, perfil arquivado em `docs/testing/perfis/` e comparado
com a execucao anterior — nunca contra um numero escrito a mao. Piso inventado
inventa a propria escala.

---

## Antes de qualquer release

- [ ] Matriz de aceitacao inteira, em servidor dedicado, com duas pessoas.
- [ ] Mundo novo percorrido do inicio ao fim **sem nenhum comando de admin**.
- [ ] Save da versao anterior abre e migra
      ([save-migrations.md](save-migrations.md)).
- [ ] Perfil de spark arquivado e comparado.
- [ ] [`o-que-nao-provamos.md`](o-que-nao-provamos.md) atualizado.
- [ ] Status do Epic Fight declarado por escrito.
- [ ] Version-lock e changelog atualizados.
