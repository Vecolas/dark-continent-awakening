# Portoes e reguas

Duas ferramentas diferentes, e confundi-las e o erro mais caro desta familia.

| | O que faz | Roda no CI? |
|---|---|---|
| **portao** | responde PASSOU/REPROVOU sobre uma invariante | sim, sempre |
| **regua** | MEDE e imprime um numero; nao aprova nada | nao, de proposito |

Regua no CI vira ruido: medicao sem alguem olhando o numero nao serve para
nada. Portao fora do CI vira opinional, e opinional nao segura regressao.

---

## O portao tem de morder dos dois lados

Antes de confiar num portao, **alimente-o com um caso que DEVE reprovar** e
confira que ele reprova.

- Portao de "duas coisas nao podem se parecer": passe duas peças identicas de
  proposito. Se ele aprovar, ele nao mede nada.
- Portao de ordem de operacoes: **inverta a ordem no codigo de teste** e exija a
  falha. Foi assim que se descobriu que uma transacao debitava sem entregar.
- Portao de teto: force o valor acima do teto. Teto que nunca e alcancado e teto
  que nunca foi testado.

**Regua que reprova TUDO tambem esta quebrada.** Um limiar escrito a mao sobre
uma grandeza que varia pouco por construcao reprova todos os casos e nao
significa nada. Se todo mundo reprova, suspeite da regua antes do codigo.

---

## Controle: a regua precisa da propria escala

Nunca compare com um numero inventado. Compare com:

- **a mesma medicao sem o efeito** (o mesmo caso com a feature desligada);
- **a mesma medicao com outra semente** (o ruido do proprio sorteio);
- **a versao anterior**, versionada junto do codigo.

E **constante de um contexto nao se herda para outro.** Um piso medido sobre um
arquivo isolado nao vale sobre a tela montada; aplicado no lugar errado, ele
reprova tudo sem dizer por que. Piso certo e RELATIVO ao mesmo contexto.

---

## Portao que crava numero envelhece

```
# ruim -- afirma um valor; quebra na proxima sessao de tuning
igual(renda_do_nivel, 60)

# bom -- afirma a regra que o valor serve
ok(renda_do_nivel > 0 and sobra_reaproveitada == 0)
```

Guarde o numero historico ao lado, como comentario ("medido em 2026-05: 60").
Ele documenta sem cobrar.

---

## Conte as proprias verificacoes

```
if verificacoes == 0:
    reprovar("a suite nao mediu nada")
```

Um laco que caiu inteiro no `continue`, um filtro que nao casou com nada, um
diretorio vazio: todos imprimem "tudo certo". **Tabela vazia nao e aprovacao.**

---

## Varra a FONTE, nao a lista

Portao que percorre uma lista escrita a mao nunca acusa o item que nunca entrou
nela — ele SOME da conta. Percorra o diretorio, o esquema, o registro; exija que
cada item esteja **coberto** ou **numa lista nomeada de divida**, e cobre os
dois lados dessa lista (nome fora dela tem de estar coberto; nome dentro dela
tem de continuar descoberto).

---

## Arnes de teste: onde ele mente

- **Estado global entre casos.** Registro por grupo/papel e global: um objeto
  esquecido por outra suite aparece na busca do seu caso, e a medicao passa a
  ser sobre a coisa errada. **Aponte o alvo a mao** e afaste o cenario do ponto
  onde todo mundo nasce.
- **Limpeza pela metade.** Liberar o filho e deixar a raiz na arvore contamina
  todas as suites seguintes — e o sintoma aparece em testes que passavam ha
  meses. Libere a RAIZ, e desligue do pai ANTES de procurar por ela.
- **Contar iteracoes em vez de esperar a CONDICAO.** Sem interface grafica o
  laco roda centenas de vezes por segundo: "30 quadros" pode cobrir 5% da
  animacao. Espere a condicao, com um teto grande so como rede.
- **Assumir o recurso compartilhado.** Se o codigo resolve um destino por busca
  global no instante do uso, pergunte a ELE onde vai colocar, em vez de
  adivinhar.
- **Estado deixado ligado no fim.** Suite que pausa, trava ou muda idioma tem de
  desfazer — senao as seguintes congelam sem imprimir nada, ate o timeout.
- **Idioma/locale nao fixado.** Teste que le texto ja traduzido passa na maquina
  de quem tem o SO em portugues e quebra no CI, que roda em ingles. Fixe.
- **Objeto que so entra no sistema no passo seguinte.** Precisa de um `await`/
  tick antes da asercao; sem ele o relatorio sai antes da suite terminar e as
  verificacoes somem da conta, sem erro nenhum.

---

## O que cada ferramenta prova — e o que ela NAO prova

Mantenha esta tabela escrita no repositorio. Exemplo do formato:

| Ferramenta | Prova | **Nao** prova |
|---|---|---|
| build / import | que os artefatos existem e registram | nada sobre codigo que nenhum caminho alcanca |
| teste ponta a ponta | que o fluxo principal funciona | aparencia, layout, leitura, desempenho |
| unitario | que a conta esta certa | se o resultado **parece** bom |
| captura visual | leitura visual | so funciona com interface; nunca em modo mudo |

Sem essa tabela, todo mundo assume que o verde cobre mais do que cobre.

---

## Regua nova entra junto do sistema

Nunca antes: regua que mede o vazio e cerimonia, e ela e escrita com suposicoes
que o sistema real vai desmentir (um numero medido no lugar errado por seis dias
porque a regua nasceu antes do consumidor).

Nunca depois: numero sem regua vira folclore, e a sessao de ajuste discute
gosto.
