# Armadilhas silenciosas

Catalogo por familia. O criterio de entrada e sempre o mesmo: **nao houve erro
no console.** O programa rodou, o teste passou, e a coisa estava errada.

---

## Dados, defaults e enums

- **Campo com default UTIL faz todo dado mentir.** Um campo de classe que nasce
  com valor ativo e herdado por tudo que nao o declara: a pergunta "esta coisa
  faz X?" responde SIM para todas, e nada quebra em uso — so a medicao muda de
  significado. Campo nasce em NEUTRO.
- **Valor ZERO de enum e o neutro por acidente.** Todo objeto criado sem tocar
  no campo cai nele. Se o zero for uma categoria com significado, todo esquecido
  passa a AFIRMAR algo que ninguem escolheu. Afirmar errado e pior que nao
  afirmar.
- **Sentinela: negativo para "herda/nao configurado" quando zero e valido.**
  "Zero desliga" transforma o ajuste legitimo `0` em "nao faz nada" — em
  silencio.
- **Enum serializado como inteiro: valor novo entra NO FIM.** Inserir no meio
  reescreve o significado de todo dado ja gravado.
- **Colecao dimensionada por literal, indexada por enum.** Funciona ate o dia em
  que o enum cresce — e ai estoura DEPOIS de ja ter impresso metade da saida,
  entao a saida parece meio certa. Dimensione por `len(Enum)`.
- **Campo novo quebra os helpers de "vazio"** que zeram so os campos antigos: os
  casos passam a reprovar apontando para o sistema, com o defeito no helper.
- **Propriedade desconhecida costuma ser IGNORADA em silencio.** Declarar o
  campo no lugar errado (no objeto e nao no dado, ou vice-versa) deixa o valor
  nulo e o comportamento inteiro sumir — sem uma linha no console. Confira qual
  dos dois lados e o dono.
- **Campo opcional tem de continuar opcional.** Torna-lo obrigatorio quebra tudo
  que ainda nao migrou; a migracao e progressiva por decisao, nao por descuido.

---

## Duas fontes para a mesma verdade

- **Copia clonada carrega o campo do original.** Clonar e o caminho normal para
  criar o proximo, e o campo herdado mente para todo mundo que perguntar. Cruze
  as duas fontes num portao.
- **Identificador unico clonado junto.** Dois recursos com o mesmo id: a
  ferramenta resolve um dos dois em silencio, e nao ha aviso.
- **Configuracao declarada + constante paralela no consumidor.** O consumidor
  ganha; a configuracao vira arquivo orfao, e a entrega que a ajustou nao muda
  nada no produto — medida, aprovada e sem efeito.
- **Um segundo objeto de configuracao escrito por cima do primeiro.** O caminho
  que nao passa parametro recebe as melhorias; o caminho principal fica com o
  estado antigo. Sentinela de heranca resolve; duplicata nao.
- **Tabela do gerador e tabela do verificador** tem de mudar juntas. "Esta no
  gerador" nao quer dizer "esta cobrado".
- **Contar por NOME quando o sistema renomeia duplicatas.** Se o container
  renomeia o segundo filho de mesmo nome, um filtro por nome acha sempre UM — e
  a metrica fica verde desde que nasceu. Conte por grupo/tag/tipo.

---

## Ordem e ciclo de vida

- **Quem LIGA, DESLIGA, no ciclo de vida do proprio componente.** Espalhar o
  desligamento pelos varios botoes de saida perde um deles — e o estado global
  sobrevive a troca de tela.
- **Inicializacao tem ordem: quem vem antes nao enxerga quem vem depois.** Quem
  vem depois PUXA a preferencia na propria inicializacao; um evento cobre o
  runtime.
- **Codigo que roda ANTES da configuracao chegar.** Se o ciclo de vida dispara
  antes de `configurar()`, tudo que depende de parametro age com o default: no
  lugar errado, com o tamanho errado. Chame a aplicacao de aparencia/estado nas
  DUAS pontas, ou dispare o efeito de dentro de `configurar()`.
- **"As vezes acerta" e pior que "nunca acerta"**: passa no teste e falha na
  carga real, que e exatamente quando importa.
- **Consulta que responde com o estado do passo ANTERIOR.** Quem acabou de
  nascer nao existe para o servidor/indice ainda, e quem ja esta dentro da
  regiao nunca "entra" nela. Use a consulta direta e sincrona.
- **Reentrancia sem trava pula uma etapa inteira** quando duas entradas chegam
  juntas.
- **Ativacao nao idempotente** recomeca o trabalho ao reentrar.
- **A ordem da transacao e contrato**: entregue antes de debitar. Recusa depois
  do debito nao tem desfazer.
- **Encadeamento nao e empilhamento.** Uma guarda contra "iniciar de novo
  enquanto roda" nao impede iniciar no instante em que o anterior termina — com
  evento continuo, o efeito fica ligado para sempre. Precisa de intervalo
  minimo, e medido em relogio de PAREDE se o proprio efeito mexe no tempo.
- **Contador de SEQUENCIA e contador de PASSO sao coisas diferentes**, e o que
  os separa e quem os zera. Reusar o errado produz laco infinito com o codigo
  parecendo certo.
- **Bandeira de "ja anunciado"**: sem ela, um valor oscilando em volta do limiar
  reentra na transicao a cada iteracao e nada mais acontece.

---

## Ponto cego e codigo morto

- **Item fora da lista SOME da conta.** Varra a fonte, nao a lista.
- **Codigo morto que afirma regra falsa** volta a rodar no primeiro caso que o
  alcance e faz a coisa errada em silencio.
- **Regra que so a interface le** e a mais fragil: nada em uso a consulta, entao
  um dado que a esqueca funciona perfeitamente e so aparece no lugar errado —
  parecendo deliberado.
- **Funcionalidade completa, testada, medida e NUNCA CHAMADA.** Acontece: o
  consumidor real usa outro caminho mais simples e ninguem percebe, porque nao
  ha erro — so um resultado pior. Ao entregar um subsistema, confira quem o
  CHAMA em producao, nao quem o testa.
- **Arte/recurso declarado nao prova que alguem o USA.** Declaracao no arquivo +
  ausencia do chamador = tudo verde e nada acontecendo. O portao tem de exercitar
  o caminho, nao conferir o arquivo.
- **Portao cravado num caso so cobre um caso so.** Se a regra e de classe, o
  portao VARRE a classe.

---

## Arredondamento e aritmetica

- **Percentual sobre inteiro some.** A regra existe, aparece na interface, e nao
  faz nada. Acumule a fracao; ou separe soma de multiplicacao e arredonde uma
  vez so, no fim.
- **Grandeza inteira produz DEGRAUS**: faixas inteiras de porcentagem sao
  indistinguiveis entre si.
- **Zero num divisor ou num consumo de laco trava tudo.** Consuma por funcao com
  piso.
- **Escala nao inteira reamostra e degrada.** Vale para imagem, grade e layout.
- **Slider linear que passa por conversao logaritmica nao e percentual.** 0,8
  pode significar 97% do fundo de escala. Derive o padrao de uma CONTA e cubra a
  conta num portao.
- **"Desligado" tem de ser desligado de verdade** — nao o menor valor
  representavel, que ainda e perceptivel. Um "off" que ainda age le como bug.

---

## Texto, i18n e formatacao

- **Traduza o MOLDE, nunca o resultado.** `tr("Total: %d") % n` esta certo;
  `tr("Total: %d" % n)` nao casa com chave nenhuma e some em silencio.
- **Chave = texto de origem** mantem tudo legivel, mas **editar o original
  quebra a traducao em silencio.** Portao: toda string que chega a tela tem par
  na tabela.
- **Fallback tem de ser o idioma que existe sem tabela**, senao pedir o idioma
  nativo cai no estrangeiro.
- **Quebra de linha dentro da chave** parte a tabela de importacao ao meio.
- **Glifo que a fonte nao tem SOME sem erro** — e um buraco no texto parece bug
  de layout. Onde o simbolo importa, desenhe a forma.
- **Texto cortado por largura maxima le como QUEBRADO, nao como abreviado.**
  Corte explicitamente, e com caractere que a fonte garante.
- **Alinhamento a direita se aplica DENTRO da caixa dada**: passar a borda
  direita como origem desenha o texto inteiro fora do painel. Nao ha erro — ha
  um painel pela metade que parece proposital, e nenhum teste de logica pega.
- **Traducao automatica de componente com marcador embutido** deixa o rotulo no
  idioma errado, sem erro. Desligue e traduza a mao.
- **Funcao estatica geralmente nao tem acesso ao tradutor**: devolva a chave e
  deixe quem desenha traduzir. De quebra, o teste passa a nao depender do idioma
  da maquina.

---

## Arquivos gerados e ferramentas

- **Arquivo normalizado pela ferramenta apaga comentarios** e reordena secoes. A
  decisao mora no documento que ninguem reescreve.
- **Confira o diff dele antes de commitar**: ele muda por motivos que nao sao
  seus.
- **Cache regenerado nao se commita**; identificadores estaveis se commitam.
- **Plugin que injeta configuracao** tem de ser desativado antes de empacotar —
  build publicada apontando para coisa fora do pacote ja aconteceu.
- **Registro novo so existe depois do passo de indexacao.** Rodar antes produz
  "identificador nao declarado" e, as vezes, um processo que nao encerra.
- **Ferramenta que edita pelo editor grava em disco so no SAVE.** Sem ele, o
  controle de versao nao ve a mudanca.
- **Processo em modo mudo nem sempre encerra sozinho** — e o esquecido gira num
  nucleo por horas, segurando handles de arquivo.
- **Reprocessar pelo mesmo funil um artefato que ja passou por ele DEGRADA.**
  Requantizacao, recompressao, re-normalizacao: cada passada come detalhe.
  Artefato pronto se ACRESCENTA, nao se "melhora" repassando.
- **Guarde o master** e reprocesse dele. Reprocessar o derivado e o caminho para
  perder a fonte sem perceber.

---

## Medicao e conclusao

- **Comparar contra a arvore suja mente se o defeito ja esta commitado.**
  Compare contra o commit anterior a mudanca.
- **Regua sem controle inventa a propria escala** e reprova tudo.
- **Regua que mede o objeto errado fica verde para sempre.**
- **A alavanca obvia raramente e a maior.** Meca as duas antes de girar: uma
  varredura simples costuma mostrar que o botao "obvio" move 3 pontos e o outro
  move 47.
- **Duas metas do plano podem ser incompativeis por construcao.** Quando subir A
  derruba B por definicao, o conserto nao e insistir: e mudar o mecanismo. Meca,
  mostre a tabela, escolha explicitamente qual meta cai.
- **Suavizar para caber num numero e o jeito errado.** Um filtro que derruba a
  metrica costuma destruir a coisa medida. Trave em que nao se confia empurra o
  trabalho para o lado errado com a autoridade de um numero.
