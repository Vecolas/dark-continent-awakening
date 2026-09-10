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
