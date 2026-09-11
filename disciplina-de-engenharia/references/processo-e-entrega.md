# Processo e entrega

A secao 14 do SKILL cobre ramo, commit e PR. Esta cobre o que vem antes e o que
vem depois: **como o trabalho e recortado** e **o que acompanha a entrega**.

O criterio e o mesmo do resto: prefira o desenho em que o esquecimento e
barulhento.

---

## Issue e a unidade de trabalho

Abra issue ANTES de codar quando qualquer um destes for verdade:

- a mudanca envolve **mais de uma decisao** que alguem poderia questionar depois;
- **algo fora do seu alcance bloqueia parte do trabalho** (credencial, conta,
  aprovacao, servico de terceiro);
- o trabalho **nao cabe numa revisao de uma sentada**;
- ja se sabe de **divida que ficara para depois**.

Uma issue tem cinco campos, e o quarto e o quinto sao os que costumam faltar:

1. **Objetivo** — uma frase sobre o que passa a ser possivel.
2. **Entregas** — checklist.
3. **Criterio de aceite** — como saber que acabou.
4. **Fora de escopo** — o que explicitamente NAO entra. Sem isso, uma frente
   invade a seguinte e ninguem percebe ate a revisao.
5. **Bloqueios, com nome** — "depende de credencial" nao serve; "depende de a
   pessoa X criar o projeto Y" serve.

**Divida descoberta no meio do caminho vira issue no MESMO dia.** Divida que so
existe na cabeca de quem escreveu nao existe: some junto com o contexto.

**Uma issue por assunto.** Se o titulo precisa de um "e", provavelmente sao duas.

---

## A entrega declara o que NAO foi verificado

A secao 6 do SKILL trata de ponto cego no codigo. Aqui e no relatorio.

Toda entrega termina dizendo o que ficou sem prova. Exemplos reais, e o que cada
um custou:

| Declarado | O que aconteceu depois |
|---|---|
| "o contrato roda contra duble, nao contra a dependencia real" | ao rodar de verdade, achou-se um bug que desligava o componente |
| "a politica de producao nunca passou por um navegador" | a primeira execucao contra producao reprovou |
| "os orcamentos sao chute, nunca foram medidos" | virou issue antes de virar lentidao em producao |

**Relatar o limite e parte da entrega, nao confissao de fracasso.** O oposto —
entregar calado e deixar o proximo descobrir — e o que custa caro.

Regra pratica: se voce escreveria "acho que funciona", escreva por que acha, e o
que faltou para saber.

---

## Verde local antes de abrir PR

O CI e **rede de seguranca, nao primeira verificacao**. Rodar a suite completa na
maquina antes de abrir o PR nao e zelo: e o que impede o ramo principal de ficar
vermelho enquanto todo mundo espera.

Num projeto real, tres quebras seguidas do CI tinham a mesma causa: nenhuma
delas exigia o CI para ser descoberta.

- arquivo escrito a mao e commitado sem passar pelo formatador;
- referencia a ferramenta de terceiro que nao existia;
- script cujo filtro nao expandia naquele sistema operacional.

Se o PR mexe em algo que difere entre ambientes (politica de seguranca, build,
integracao), rode tambem **na variante que vai para producao**. Ver
`o-verificador-tambem-mente.md`.

---

## Nunca afrouxar a verificacao para ficar verde

Proibido, sem excecao:

- desligar uma protecao "temporariamente";
- apagar ou pular teste vermelho em vez de entender por que;
- baixar o nivel de um scanner para esconder achado;
- adicionar supressao de lint sem comentario dizendo o motivo;
- trocar asercao por uma fraca (`e verdadeiro`) so para parar de reclamar.

**Ou a verificacao esta errada e conserta-se a verificacao, ou o codigo esta
errado e conserta-se o codigo.** Nao ha terceira saida.

Corolario: verificacao que dispara em falso positivo tem de ser consertada com a
mesma urgencia de uma que falha em negativo. As duas destroem a confianca, e sem
confianca ninguem le a saida.

---

## Decisao que contradiz uma decisao anterior

Acontece, e nao e problema. **Trocar em silencio e o problema.**

Quando a decisao nova contraria uma registrada:

1. registre a nova **apontando qual ela altera** e em que;
2. escreva **o custo assumido**, nao so o beneficio;
3. deixe explicito **o que NAO muda** — e a parte que evita erosao. Se um
   principio sobrevive a mudanca, diga isso, senao ele se perde no meio;
4. atualize o indice das decisoes. **E tenha um portao que verifique o indice**:
   varra a fonte (o diretorio), nao a lista.

O item 4 nao e cerimonia: duas decisoes ja ficaram fora de um indice porque a
edicao por texto exato falhou em silencio. Indice de decisoes que omite uma
decisao e pior que nao ter indice — quem confia nele conclui que a decisao nao
existe.

---

## Numero de terceiro nao se cita de memoria

Preco, limite de plano, cota, versao minima: **muda, e a resposta errada leva a
uma decisao de arquitetura errada.** Consulte a fonte oficial e cite-a.

Se uma fonte secundaria contradisser a oficial, **diga que ha divergencia** em
vez de escolher a mais conveniente em silencio.

---

## Trabalho paralelo: a fronteira e o arquivo

Quando varias frentes correm ao mesmo tempo — pessoas ou geradores — o que evita
destruicao mutua nao e combinacao verbal, e **fronteira escrita**:

- cada frente recebe uma **lista explicita de arquivos que pode tocar**;
- e uma **lista do que e proibido**, com os compartilhados no topo: manifesto de
  dependencias, indice, configuracao de build, contrato comum;
- **contrato compartilhado se congela ANTES** de as frentes comecarem. Sem isso,
  duas frentes inventam nomes diferentes para a mesma coisa;
- **recurso unico tem dono unico**: se uma ferramenta usa uma porta fixa ou um
  arquivo de estado, so uma frente por vez pode roda-la;
- quem nao pode tocar num arquivo **relata o que precisa ser mudado nele** em
  vez de contornar por fora. Contorno vira duas verdades (SKILL, secao 7).

E na integracao: **o relato de bloqueio de cada frente e material de trabalho**,
nao formalidade. Foi assim que se descobriu um bug que nenhum teste pegava.

### O comando que engole o trabalho dos outros

A fronteira escrita nao protege nada se o comando de commit ignorar fronteira.
**`git add -A` e `git commit -a` varrem a arvore inteira**, inclusive o que as
outras frentes estao escrevendo naquele instante.

O estrago real observado: um commit de seis arquivos levou junto quase 2.000
linhas de duas outras frentes, **em progresso**. Uma delas estava com uma quebra
DELIBERADA aplicada — parte de um canario — e foi essa versao que entrou no
commit. Se aquele commit tivesse chegado ao ramo principal, o codigo rodaria com
o defeito e o portao correspondente reprovaria, apontando para uma frente que
nao tinha culpa.

Duas propriedades tornam isso caro:

1. **Nao da erro.** O commit e valido, o push funciona, o PR abre. So aparece
   depois, quando alguem troca de ramo e o git avisa que sobrescreveria um
   arquivo — ou quando nao aparece nunca.
2. **Captura estado intermediario.** Trabalho alheio pela metade nao e so
   "codigo de outra pessoa no meu commit": e codigo que ninguem afirmou estar
   pronto, congelado num instante arbitrario.

As regras que restam:

- **Stage explicito, caminho por caminho.** Enquanto houver outra frente viva na
  mesma arvore, `git add <caminho>` nomeando cada arquivo, e conferir com
  `git diff --cached --name-only` ANTES de commitar.
- **Formatador tambem varre tudo.** `pnpm format`, `black .`, `gofmt -w .`
  reescrevem arquivo alheio em progresso e produzem o mesmo estrago, com um
  diff que parece inofensivo. Rode a ferramenta **nos seus arquivos**.
- **Se ja aconteceu e nada foi ao ramo principal**, o conserto e
  `git reset --soft HEAD~1`, `git reset`, restagear so o que e seu, recommitar e
  `git push --force-with-lease`. O trabalho alheio volta a ficar solto, intacto.
- **Se ja chegou ao ramo principal**, nao tente separar por reversao: avise as
  frentes atingidas antes de qualquer outra coisa, porque elas estao prestes a
  resolver conflitos contra uma versao que ninguem escreveu.

O mesmo vale para a arvore limpa no fim: `git status` vazio depois de commitar
so prova que nada ficou de fora — nunca que nada entrou demais.
