---
name: disciplina-de-engenharia
description: Convencoes de implementacao destiladas de um projeto real — comunicacao por evento em vez de caminho estrutural, valor derivado lido na hora e nunca congelado, numero ajustavel fora do codigo, portao que morde dos dois lados, ponto cego declarado, e o catalogo de falhas SILENCIOSAS (o falso verde). Use ao escrever ou revisar codigo, ao criar teste/portao/regua, ao decidir onde mora um numero ou uma configuracao, ao investigar um bug que "nao da erro nenhum", ao nomear branch, commit ou PR, ao recortar trabalho em issue, e ao relatar uma entrega. Vale para qualquer linguagem e qualquer dominio.
---

# Disciplina de engenharia

Estas regras vieram de um projeto onde quase todo defeito caro foi
**silencioso**: o programa continuou rodando, o teste continuou verde, e o
sintoma apareceu semanas depois. Elas nao sao estilo — cada uma existe porque
alguem pagou por ela. O criterio que atravessa tudo:

> **Uma falha que grita custa uma tarde. Uma falha que passa custa um mes.**
> Prefira sempre o desenho em que o erro e barulhento.

---

## 1. As duas regras que sustentam um projeto

**1. Comunique por CONTRATO, nunca por caminho estrutural.**

Quem faz algo anuncia; quem se importa escuta. Nada navega pela arvore de
objetos, pela hierarquia de pastas, pelo DOM ou pela cadeia de pais para
alcancar um colaborador.

```
# ruim -- quebra assim que alguem mover uma peca de lugar
pai.pai.filhos["hud"].atualizar(vida)
raiz.buscar("/app/Main/HUD").atualizar(vida)

# bom
eventos.emitir("dano_recebido", vida, vida_maxima)
```

Excecao legitima: buscar por **papel** (`primeiro_com_papel("alvo")`, injecao
por interface, registro por capacidade). O que se proibe e depender da POSICAO
de alguem — nao de conhecer sua existencia.

**2. Valor derivado se LE no instante em que se usa; nunca se guarda ja
calculado.**

```
# ruim -- congela o multiplicador no momento da criacao
self.velocidade = 120.0 * dificuldade.multiplicador()

# bom -- responde a mudanca, inclusive para quem ja existe
def velocidade_atual(self):
    return self.velocidade_base * dificuldade.multiplicador()
```

Vale para preco com imposto, timeout com backoff, permissao efetiva, tema,
feature flag: **um valor multiplicado e guardado e uma copia que vai divergir da
fonte**, e a divergencia nao da erro.

Corolario: **funcao de dominio nao recebe o valor ja pronto.** Uma assinatura
`mover(velocidade)` convida o chamador a calcular uma vez e guardar. Receba o
OBJETO e pergunte a ele na hora. Se houver excecao — um valor que de proposito
nao escala — ela e **declarada em comentario**, nao esquecida.

---

## 2. Onde mora um numero

**Numero que alguem vai querer ajustar sem programar nao mora no codigo.** Ele
vira configuracao, dado, arquivo de perfil — algo que se edita sem recompilar e
sem entender a linguagem.

A pergunta e literal: *"alguem vai querer girar isso numa sessao de ajuste?"* Se
sim, ele sai do codigo.

E **numero novo tem de nascer medivel**: botao que ninguem consegue medir e
botao que nunca sera girado com confianca. A regua entra junto do sistema que
ela mede — nunca antes (regua que mede o vazio e cerimonia) e nunca depois
(numero sem regua vira folclore).

O inverso tambem e regra: **numero que foi para a configuracao tem de SAIR do
codigo.** Deixado nos dois, o do codigo e sobrescrito em runtime, nenhum teste
de comportamento acusa nada, e a proxima pessoa passa uma tarde girando o botao
morto.

**Limite de design NAO e botao de tuning.** "Quantos itens cabem nas maos",
"quanto tempo uma transicao pode durar": isso e constante no codigo, de
proposito. Botao ajustavel e ajustado — para tres, na primeira vez que alguem
achar que falta espaco.

---

## 3. Nomes, tipos e comentarios

- **Um idioma so**, e o do time. Misturar custa mais do que ganha. O que a
  plataforma impoe fica como e.
- **Tipar sempre que der.** O erro aparece mais cedo e o autocomplete funciona.
- **Comente o PORQUE, nunca o QUE.** `# soma 1 na vida` nao serve para nada.
  `# nao reinicia o efeito em andamento: com evento continuo ele fica preso
  ligado` serve.
- **Todo arquivo abre com um bloco dizendo o que ele faz e QUAL DECISAO ele
  carrega.** E o que permite entender o arquivo sem perguntar a quem escreveu.
- Evento no passado (`pedido_concluido`), constante em MAIUSCULA, privado com
  marcador de privado. Consistencia importa mais que a escolha.
- **Comportamento novo nao se escreve na mao: usa-se o vocabulario que existe.**
  Cinco copias da mesma logica com nomes diferentes divergem, e o sintoma
  aparece no PRODUTO e nunca no console.

---

## 4. Portoes: o que separa teste de carimbo

**Um portao tem de morder dos dois lados.** Alimentado com um caso que DEVE
reprovar, ele reprova. Regua que nunca reprova e carimbo; regua que reprova tudo
esta medindo a si mesma.

Quatro perguntas antes de confiar num portao:

1. **Ele mede a coisa certa?** Portao que mede o lugar errado aprova o dobro do
   que devia e fica verde para sempre.
2. **Ele tem CONTROLE?** Piso escrito a mao inventa a propria escala. Compare
   contra a mesma medicao sem o efeito, ou contra a versao anterior.
3. **Ele conta as proprias verificacoes?** Tabela vazia nao e aprovacao: um laco
   que caiu inteiro no `continue` imprime "tudo certo" com ZERO linhas medidas.
   Portao com zero verificacoes tem de REPROVAR.
4. **Ele afirma a REGRA ou um NUMERO?** Portao que crava um valor envelhece com
   o botao de tuning e passa a reprovar o codigo certo. Afirme a invariante e
   guarde o numero historico ao lado, como nota.

**Dois niveis de teste, respondendo perguntas diferentes:**

| | Responde | Quebra quando |
|---|---|---|
| unitario, segundos | "a conta esta certa?" | voce mexeu em logica pura ou num numero |
| ponta a ponta, minutos | "o fluxo inteiro funciona?" | voce mexeu em montagem, integracao ou fluxo |

Rode o rapido primeiro. E **nenhum teste e escrito antes de existir logica para
testar**.

Detalhes: `references/portoes-e-reguas.md`.

---

## 5. O falso verde

**Exit 0 nao significa "o projeto esta sao".** As formas mais comuns de passar
sem ter olhado nada:

- **Codigo que nenhum caminho alcanca nao e verificado.** Erro de sintaxe num
  arquivo que ninguem importa passa em todo job do CI. O que fecha isso e uma
  varredura que CARREGA todo arquivo do projeto e exige que ele seja utilizavel
  — nao que ele exista.
- **O detector obvio geralmente mente.** Carregar um arquivo quebrado costuma
  devolver um objeto invalido, e nao nulo; recarregar costuma dar falso positivo
  em qualquer coisa que ja tenha instancia viva. Descubra o detector confiavel
  da sua plataforma e escreva ao lado dele POR QUE os outros nao servem.
- **Comportamento mais lento que um tick escapa do teste rapido.** Se o arnes
  encerra o caso em 0,1 s e o comportamento leva 1 s para comecar, ele nunca
  aconteceu — e o teste fica verde sem ter olhado nada. Ciclo longo pede suite
  propria.
- **O que o teste NAO prova precisa estar escrito.** Mantenha uma tabela
  "ferramenta / prova / **nao** prova". Sem ela, todos assumem que o verde cobre
  mais do que cobre.

---

## 6. Ponto cego DECLARADO

**Item que fica fora da lista some da conta — e sumir e pior que reprovar.**

Verificacao que percorre "o que esta na lista" nunca acusa o que nunca entrou
nela. O conserto e sempre o mesmo: **varra a FONTE** (o diretorio, o esquema, o
registro) e exija que cada item esteja ou coberto ou **numa lista nomeada de
divida** (`SEM_COBERTURA_AINDA`).

E a lista de divida morde dos dois lados: nome fora dela tem de estar coberto,
nome DENTRO dela tem de continuar descoberto. Sem a segunda metade, a linha fica
para sempre cobrindo em silencio o dia em que aquilo se perder.

Mesma logica para regra que so a interface le, campo que so a documentacao usa,
codigo que nenhum fluxo ativo exercita: **e justamente por nao ser usado que
precisa continuar conferido.** Codigo que ninguem roda apodrece.

---

## 7. Duas fontes para a mesma verdade

Toda vez que a mesma informacao existe em dois lugares, elas divergem — e **a
que vale costuma ser a errada**.

- **Copia clonada carrega o campo do original.** Clonar um arquivo para criar o
  proximo e o caminho normal, e campo que mente nao da erro. Cruze as duas
  fontes num portao.
- **Configuracao declarada + constante paralela no consumidor**: o consumidor
  ganha, a configuracao vira arquivo orfao, e a entrega inteira que a ajustou
  nao muda nada no produto.
- **Tabela do gerador + tabela do verificador** tem de mudar juntas. "Esta no
  gerador" nao quer dizer "esta cobrado".
- **Derive em vez de duplicar.** Se B pode nascer de A, faca B nascer de A,
  mesmo que hoje os dois sejam iguais.

---

## 8. Defaults, sentinelas e enums

- **Campo com default UTIL faz todo dado mentir.** Campo de classe que nasce com
  valor ativo e herdado por tudo que nao o declara: qualquer pergunta do tipo
  "esta coisa faz X?" responde SIM para todas. **Campo nasce em NEUTRO.**
- **O valor ZERO de um enum e o que todo mundo esquecido recebe.** Faca dele o
  neutro. Afirmar errado e pior que nao afirmar.
- **Sentinela de "nao configurado" e NEGATIVO quando zero e valor valido.** Zero
  desligando transforma um ajuste legitimo em "nao faz nada", em silencio.
- **Enum serializado como inteiro: valor novo entra NO FIM.** Inserir no meio
  reescreve o significado de todo dado ja salvo, sem uma linha no console.
- **Campo novo com default quebra todo helper de "vazio"** que zera so os campos
  antigos — e a suite passa a reprovar apontando para o lugar errado.
- **Colecao dimensionada por literal e indexada por enum e uma bomba com
  timer.** O tamanho sai de `len(Enum)`, sempre.

---

## 9. Aritmetica que some

- **Percentual sobre inteiro desaparece no arredondamento.** "+10%" sobre 2
  continua 2; reduzir 25% de 1 nao reduz nada — a regra existe, aparece na
  interface e nao faz NADA. Acumule a fracao e cobre quando ela fechar uma
  unidade, ou separe "somar" de "multiplicar" em campos distintos e faca a conta
  uma vez so, no fim.
- **Grandeza inteira limita o efeito a DEGRAUS.** Entre 15% e 25% pode nao haver
  nada: os dois dao o mesmo numero de passos. Meca os degraus antes de escolher
  a porcentagem.
- **Zero num divisor ou num contador de sorteio trava o laco.** Consuma sempre
  por uma funcao com PISO, nunca pelo campo cru.
- **Escala e precisao: prefira fator inteiro.** Meio passo reamostra e borra —
  vale para imagem, para grade e para layout.

---

## 10. Codigo morto e regra falsa

**Codigo morto que afirma uma regra ERRADA e pior que codigo morto.** Ele volta
a rodar no dia em que alguem criar o primeiro caso que o alcanca, e faz a coisa
errada sem uma linha no console.

Ao encontrar um caminho que nunca executa: ou ele volta a valer (e a regra e
corrigida), ou ele sai. Deixar "por seguranca" e deixar uma armadilha armada.

---

## 11. Ordem, ciclo de vida e reentrancia

- **Quem LIGA, DESLIGA — e o par nao mora nos botoes de saida.** Estado global
  ligado por um componente e desligado no ciclo de vida DELE, nunca espalhado
  pelos varios lugares de onde se pode sair. Espalhar e o desenho que ja perdeu
  uma chamada critica, com sintoma silencioso.
- **Inicializacao tem ORDEM, e quem vem antes nao enxerga quem vem depois.**
  Quem vem depois PUXA o que precisa na propria inicializacao; um evento cobre
  as mudancas em runtime.
- **A operacao que muda estado tem uma ORDEM que e contrato.** Numa transacao,
  entregue ANTES de debitar: se a entrega pode ser recusada, debitar primeiro
  cobra por algo que nao foi entregue — e isso nao tem desfazer. Escreva a ordem
  no codigo e cubra-a com um portao que a INVERTE de proposito.
- **Toda funcao que espera antes de mexer em estado global precisa de trava.**
  Sem ela, duas entradas concorrentes pulam uma etapa inteira.
- **Operacao de ativacao tem de ser idempotente.** Reentrar numa coisa ja ativa
  nao pode recomecar o trabalho.
- **Recurso declarado uma vez e usado por varias instancias e estado global
  disfarcado.** Crie por instancia.
- **Trabalho iniciado morre com quem o iniciou**, e no lugar certo da hierarquia
  — nunca pendurado em quem por acaso o criou.

---

## 12. Investigacao

- **O sintoma separa os modos de falha; confira qual antes de investigar
  qualquer outra coisa.** Travou = uma coisa; erro imediato e explicito = outra.
  Diagnostico errado consome sessoes inteiras confirmando a hipotese errada.
- **Comparar contra a arvore suja mente se o defeito ja esta commitado.** Para
  perguntar "isto e meu?", compare contra o commit ANTERIOR a mudanca.
- **Prefira ler o estado REAL a supor.** Consulte a fonte viva quando existir
  uma; um arquivo lido como texto vale menos que o estado que o sistema carrega.
- **Processo que termina e nao sai continua consumindo.** Para separar "preso"
  de "trabalhando", meca o DELTA de uso em alguns segundos, nao o instantaneo.
- **Aviso nao reprova nada.** Um `warning` no lugar de um erro e um defeito que
  vive para sempre. Se a condicao e invalida, ela precisa de portao.

---

## 13. Pedido que outra pessoa (ou um gerador) vai executar

Vale para arte, texto, dados, conteudo gerado por modelo — qualquer entrega
subjetiva. **O pedido vem ANTES da entrega, e ele e uma ficha curta:**

1. **O que a peca E**, descrita como objeto — nunca o que ela FAZ no sistema.
   Palavra de funcao vira efeito literal na saida.
2. **Os atributos que ja existem no dado** (cor, nome, identificador): **a
   entrega obedece ao dado, e nao o contrario.**
3. **O que a separa das VIZINHAS**, nomeando contra quem ela corre risco de se
   confundir.
4. **A familia** a que pertence e o regime que a governa.
5. **Onde e em que tamanho ela sera consumida** — e esse numero sai da constante
   do CONSUMIDOR, nunca de suposicao.
6. **Qual portao a cobra.** Se nao ha nenhum, o pedido diz isso com todas as
   letras.

**Se quem pediu nao informou algo, nao se inventa em silencio: preenche-se pela
convencao e ANUNCIA-SE que foi assim** — para o pedido poder ser corrigido antes
da entrega, e nao depois. Identidade preenchida por padrao e nao anunciada e o
mesmo que inventada, so que sem ninguem para discordar.

E: **peca que passa em toda regua ainda pode estar errada pelo CONJUNTO.**
Nenhuma medicao pega "isso nao pertence a esta familia". Uma linha escrita antes
pega.

**Entrega parcial de uma familia le como quebrado, e nao como incompleto.**
Metade do conjunto no formato novo e metade no antigo parece defeito para quem
usa. Familia se entrega inteira.

---

## 14. Git e trabalho em equipe

```
main            sempre verde
feat/<desc>     funcionalidade nova
fix/<desc>      correcao
tune/<desc>     so ajuste de configuracao
docs/<desc>     so documentacao
```

- Ninguem commita direto no ramo principal; todo merge passa por PR.
- **Commit imperativo, minusculo, sem ponto final, e revertivel sozinho.** Se a
  mensagem precisa de um "e" no meio, provavelmente sao dois commits.
- **Um assunto por PR.** Se mexeu num arquivo hostil a merge, diga qual no
  titulo — isso avisa aos outros que aquele arquivo esta travado.
- **PR aberto nao e trabalho entregue.** Verde e esquecido custa mais do que
  parece. Feche o que esta aberto antes de comecar frente nova.
- **Uma pessoa por arquivo hostil a merge por vez** — o gerado, o serializado, o
  binario, o que a ferramenta reescreve. Arquivos de codigo distintos nunca
  conflitam; esses sempre.
- **Nao deixe arquivo desses pela metade numa branch.** Enquanto estiver aberto,
  ninguem toca nele — e trabalho inacabado nao se retoma nem por quem o comecou.
- **Arquivo reescrito pela ferramenta nao aceita documentacao.** Configuracao
  normalizada automaticamente reordena secoes e APAGA comentarios. A decisao
  mora no documento que ninguem reescreve, e o diff dele se confere antes de
  commitar: ele aparece modificado por motivos que nao sao seus.
- **Quando o codigo e o texto discordarem, o codigo ganha e o texto se
  atualiza.** Uma verdade por assunto, num lugar so.
- **Escreva o que os outros vao executar no nivel de quem nao conhece a
  ferramenta**: passo a passo, comando literal, sem jargao.

---

## Referencias

- `references/portoes-e-reguas.md` — portao que morde, controle, regua que mede
  em vez de aprovar, arnes de teste, o que cada ferramenta NAO prova.
- `references/armadilhas-silenciosas.md` — o catalogo por familia: dados e
  defaults, duas fontes, ordem e ciclo de vida, texto e i18n, arredondamento,
  arquivos gerados.
- `references/o-verificador-tambem-mente.md` — o falso verde uma camada acima:
  comando de CI que sai 0 com job vermelho, substituicao que nao casou e disse
  "ok", glob que virou literal, teste que fixou o ambiente errado, duble que
  escondeu integracao quebrada, verificador que acusa a propria documentacao.
- `references/processo-e-entrega.md` — issue como unidade de trabalho, entrega
  que declara o que NAO foi verificado, verde local antes do PR, decisao que
  contradiz decisao anterior, e fronteira de arquivo no trabalho paralelo.
