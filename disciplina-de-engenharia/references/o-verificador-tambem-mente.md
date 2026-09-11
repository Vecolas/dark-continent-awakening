# O verificador tambem mente

O catalogo de `armadilhas-silenciosas.md` trata do codigo que falha calado.
Este trata da camada acima: **a ferramenta que diz que verificou e nao
verificou**. E o falso verde mais caro, porque vem carimbado.

> Toda vez que uma verificacao passa, pergunte: **ela CONSEGUIRIA falhar?**

Cada caso abaixo aconteceu de verdade e passou despercebido por pelo menos um
commit.

---

## O comando que sai com 0 e reporta vermelho

`gh run watch --exit-status` devolveu **0** numa execucao com dois jobs
`failure`. Quem confiou no codigo de saida seguiu adiante com o CI vermelho.

**Regra:** para estado de CI, leia o campo de conclusao, nao o codigo de saida.

```bash
# ruim -- o exit code nao reflete a conclusao dos jobs
gh run watch "$id" --exit-status

# bom -- pergunta pelo dado, nao pelo efeito colateral
gh run list --limit 2 --json name,conclusion
gh run view "$id" --json jobs --jq '.jobs[] | "\(.name): \(.conclusion)"'
```

Generalize: **ferramenta de orquestracao costuma reportar "eu rodei", nao "deu
certo".** Peca o resultado explicitamente.

---

## A substituicao que nao casou, e o script que disse "ok"

O padrao mais recorrente de todos. Bateu tres vezes no mesmo projeto:

```python
# ruim -- imprime sucesso mesmo quando replace() nao encontrou nada
s = s.replace(antigo, novo)
open(p, 'w').write(s)
print('atualizado')
```

O texto alvo tinha sido reformatado por um formatador automatico, `replace()`
devolveu a string intacta, e a mensagem disse que deu certo. **Dois ADRs
ficaram fora do indice** e ninguem soube por horas.

```python
# bom -- a ausencia do alvo e um erro, nao um no-op
assert antigo in s, f'alvo nao encontrado em {p}'
s = s.replace(antigo, novo)
```

E depois de escrever, **confira o resultado**, nao a intencao:

```bash
grep -c "^| \[00" docs/adr/README.md   # 9
ls docs/adr/0*.md | wc -l              # 9
```

Corolario: **edicao por texto exato e fragil quando um formatador toca o
arquivo.** Prefira edicao por linha/estrutura, ou reescreva o arquivo inteiro.

---

## O filtro que virou literal

```json
"security:check": "... && vitest run tests/unit/security-*.test.ts"
```

No shell POSIX o glob expande; no Windows chega literal ao runner, que trata o
argumento como **filtro de nome de arquivo**, nao como glob. Resultado:

```
No test files found, exiting with code 1
```

O script se chamava "check de seguranca" e nao rodava teste de seguranca nenhum.

**Regra:** nao dependa de expansao do shell dentro de `package.json`. Passe o
filtro no formato que a ferramenta entende (`vitest run security-`), ou escreva
um script proprio.

---

## O teste que fixou o ambiente errado

Um teste comparava o cabecalho servido com a politica esperada:

```ts
// ruim -- so exercita a variante de desenvolvimento, para sempre
expect(csp).toBe(buildPolicy({ development: true }))
```

O servidor de teste rodava em modo dev, onde a politica e deliberadamente mais
frouxa. **A politica de producao nunca passou por um navegador** — e ela era
diferente. O teste era verde e nao provava o que importava.

```ts
// bom -- a variante vem do alvo, e existe um jeito de rodar contra producao
const EM_PRODUCAO = process.env.E2E_TARGET === 'prod'
expect(csp).toBe(buildPolicy({ development: !EM_PRODUCAO }))
```

**Regra:** se dev e producao diferem em algo que o teste afirma, o teste tem de
poder rodar nos dois. Um alvo que nunca e exercitado nao esta coberto.

---

## O duble que escondeu a integracao quebrada

Uma camada foi testada com um duble fiel ao protocolo. Sete testes verdes.
Quando finalmente rodou contra a dependencia real, **nao funcionava**: um sufixo
na URL desligava o componente, e o duble nao tinha como saber disso.

**Regra:** duble prova a SUA logica, nunca a integracao. Enquanto nao houver um
teste contra a coisa real, escreva isso na entrega — e abra a issue.

Ver `portoes-e-reguas.md`, secao "O que cada ferramenta prova — e o que ela NAO
prova".

### A variante pior: o teste que CRAVA o endereco errado

Segundo caso, mesma familia, com um agravante. Um adapter de servico externo
nasceu apontando para um endereco que **nao existe** — o servico morava em outro
host. Vinte e um testes unitarios, verdes, porque o `fetch` era dublado: a URL
nunca era visitada.

Entre eles havia um que afirmava a URL:

```
expect(chamadas[0].url).toContain('/api/tablebase/standard?fen=')
```

Esse teste nao era neutro. Ele **selava** o defeito: quem corrigisse o endereco
veria um teste ficar vermelho e teria motivo para achar que a correcao e que
estava errada.

E o sintoma em producao seria MUDO por construcao. O adapter tinha degradacao
graciosa — qualquer falha vira `null` — e `null` era tambem o valor legitimo de
"esta posicao nao tem resposta". Nenhuma tela mostraria nada: o recurso
simplesmente nunca funcionaria, para sempre.

**Tres regras que saem disso:**

1. **Teste unitario afirma a REGRA, nao o endereco.** "O identificador vai
   codificado na query do endereco configurado" envelhece bem; o caminho
   literal escrito a mao vira uma segunda fonte da verdade que ninguem
   atualiza (SKILL, secao 7).
2. **Degradacao graciosa exige teste contra a coisa real.** Quanto melhor o
   componente engole falha, menos ele grita quando esta todo errado. As duas
   propriedades sao a mesma propriedade.
3. **O teste contra a coisa real fica FORA do verde do dia a dia**, com config
   ou marcador proprio, e roda a mao. Portao que pisca vermelho por causa da
   rede treina todo mundo a ignorar vermelho — e ai ele nao vale nada no dia em
   que estiver certo.

O canario que prova esse arranjo tem duas metades: com o endereco errado, o
teste de contrato reprova **e** os unitarios continuam verdes. A segunda metade
e a que mostra o tamanho do buraco.

---

## O verificador que acusa a propria documentacao

Um script recusava segredo com prefixo publico. Comecou a reprovar o arquivo que
**explica a regra**, porque o texto precisa citar exemplos proibidos.

Falso positivo nao e chateacao: **e como uma verificacao morre.** Depois de
duas ou tres, as pessoas param de ler a saida.

```js
// bom -- varre codigo, nao comentario
function semComentarios(texto, arquivo) {
  if (/\.ya?ml$/.test(arquivo)) return texto.replace(/(^|\s)#[^\r\n]*/g, ' ')
  return texto.replace(/\/\*[\s\S]*?\*\//g, ' ').replace(/\/\/[^\r\n]*/g, ' ')
}
```

E prove nos **dois** sentidos, com canario descartavel:

```bash
# 1. estado limpo -> passa
# 2. vazamento real em codigo -> ACUSA
# 3. o mesmo nome em comentario -> NAO acusa
```

---

## A referencia de terceiro que nao existe

`google/osv-scanner-action@v2` falhou com "unable to find version v2": nao havia
tag flutuante, so `v2.5.x`. Corrigido para a raiz do repositorio, falhou de
novo: "Top level 'runs:' section is required" — a action morava num
subdiretorio. **Duas tentativas, dois palpites, dois commits vermelhos.**

**Regra:** confirme caminho e tag na fonte antes de commitar.

```bash
gh api repos/<org>/<repo>/tags --jq '.[0:5][].name'
gh api "repos/<org>/<repo>/contents?ref=<tag>" --jq '.[].name'
```

---

## A permissao que so falta no evento certo

O mesmo job passava em `push` e falhava em `pull_request` com
`403 Resource not accessible by integration`: naquele evento a ferramenta lista
os commits do PR pela API, e o token so tinha `contents: read`.

**Regra:** permissao de CI se testa no evento em que o job realmente roda. Job
verde no `push` nao diz nada sobre o `pull_request`.

---

## Preco, limite e plano: nunca de memoria

Limite de plano gratuito e preco mudam, e a resposta errada leva a uma decisao
de arquitetura errada. **Consulte a pagina oficial e cite a fonte.**

Quando uma fonte de terceiro contradisser a oficial, diga que ha divergencia em
vez de escolher em silencio.

---

## Resumo

| Sintoma | O que suspeitar |
|---|---|
| comando de CI saiu 0 | leia a conclusao dos jobs, nao o exit code |
| script imprimiu "atualizado" | `replace()` casou mesmo? afirme o alvo |
| "nenhum teste encontrado" | glob expandido pelo shell errado |
| teste verde ha muito tempo | ele consegue falhar? qual variante ele exercita? |
| tudo passa com duble | integracao real ainda nao foi provada |
| verificador acusa arquivo de doc | falso positivo mata a verificacao |
| action/ferramenta "nao encontrada" | confirme caminho e tag na fonte |
| verde no push, vermelho no PR | permissao depende do evento |
