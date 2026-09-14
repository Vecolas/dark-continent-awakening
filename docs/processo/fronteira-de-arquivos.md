# Fronteira de arquivos: como duas pessoas nao se destroem

Combinacao verbal nao protege nada. O que protege e **fronteira escrita**.

Este documento e a fronteira. Ele diz quem toca o que, quais arquivos travam o
repositorio inteiro, e o que fazer quando voce precisa de uma mudanca num
arquivo que nao e seu.

---

## 1. Arquivos hostis a merge

Arquivos de codigo distintos **nunca** conflitam. Estes **sempre**:

| Arquivo | Por que |
| --- | --- |
| `build.gradle` | reescrito por ferramenta, sem separacao por assunto |
| `gradle.properties` | uma linha por decisao global |
| `settings.gradle` | idem |
| `NenFoundation.java` | ponto de registro de todo subsistema |
| `network/NenProtocol.java` | tabela unica, todos acrescentam linhas |
| `registry/*` (quando existir) | idem |
| `config/NenConfig.java` | idem |
| `assets/**/lang/*.json` | gerado a partir do M3; ate la, editado a mao pelos dois |
| `.github/workflows/*` | um arquivo, dois interesses |
| `config/NenClientConfig.java` | mesmo motivo do `NenConfig`: uma linha por decisao, todos acrescentam |
| `client/NenFoundationClient.java` | ponto de registro de todo listener de cliente — e o `NenFoundation.java` da lane de superficie |
| `client/vfx/render/AuraRenderTypes.java` | tabela unica de tipos de render; todos acrescentam linhas (**a partir do AV0**) |
| `client/vfx/AuraVisualSystem.java` | orquestra os passes; e o ponto de entrada unico do efeito (**a partir do AV0**) |
| `assets/**/shaders/*` | pipeline de shader e uma coisa so; dois autores no mesmo `.fsh` conflitam sempre (**a partir do AV1**) |

**Regra:** uma pessoa por vez em cada um deles.

**Regra:** nao deixe um desses pela metade numa branch. Enquanto a branch
estiver aberta, ninguem toca no arquivo — e trabalho inacabado nao se retoma
nem por quem comecou.

**Regra:** se o seu PR toca um arquivo hostil, **diga no titulo**. Isso avisa a
outra pessoa que o arquivo esta travado.

```
feat: aura pool e regeneracao [toca NenFoundation.java]
```

---

## 2. O comando que engole o trabalho do outro

`git add -A` e `git commit -a` varrem a arvore inteira, inclusive o que a outra
frente esta escrevendo naquele instante.

Isso **nao da erro**. O commit e valido, o push funciona, o PR abre. So aparece
depois, quando a outra pessoa troca de branch e o git avisa que sobrescreveria
um arquivo — ou quando nao aparece nunca.

Enquanto houver duas frentes vivas na mesma maquina ou no mesmo clone:

```bash
git add caminho/do/arquivo.java     # um por um, nomeando
git diff --cached --name-only       # confira ANTES de commitar
git commit -m "feat: ..."
```

O mesmo vale para qualquer ferramenta que varre a arvore. Rode formatador e
datagen **nos seus arquivos**, nao no projeto inteiro.

`git status` vazio depois de commitar prova que nada ficou de fora. **Nunca**
prova que nada entrou demais.

### Se ja aconteceu

- **E nada foi para a `main`:** `git reset --soft HEAD~1`, `git reset`,
  restagear so o que e seu, recommitar, `git push --force-with-lease`. O
  trabalho alheio volta a ficar solto, intacto.
- **E ja chegou na `main`:** avise a outra pessoa **antes de qualquer outra
  coisa**. Ela esta prestes a resolver conflitos contra uma versao que ninguem
  escreveu.

---

## 3. Contrato primeiro, implementacao depois

Quando as duas frentes precisam da mesma coisa, o caminho e sempre este:

1. Um PR **minusculo** com a interface, o record ou o enum. So o contrato.
2. Merge imediato.
3. As duas frentes trabalham em paralelo contra o contrato ja estabilizado.

Sem isso, as duas inventam nomes diferentes para a mesma coisa e a integracao
vira renomeacao.

Os contratos ja congelados estao no [ADR-004](../adr/ADR-004-identidade-congelada.md).

---

## 4. Quando voce precisa mudar um arquivo que nao e seu

**Nao contorne por fora.** Contornar cria duas verdades, e a que vale costuma
ser a errada.

Escreva o que precisa ser mudado — numa issue, num comentario de PR, numa
mensagem — e espere. O relato de bloqueio de uma frente e material de trabalho
da outra, nao formalidade.

---

## 5. Recurso unico tem dono unico

Uma so frente por vez pode:

- rodar `runServer` (porta fixa);
- rodar `runData` (escreve em `src/generated/resources`);
- mexer no mundo de teste compartilhado.

---

## 6. Antes de comecar uma frente nova

**PR aberto nao e trabalho entregue.** Verde e esquecido custa mais do que
parece.

Feche o que esta aberto antes de abrir a proxima frente.
