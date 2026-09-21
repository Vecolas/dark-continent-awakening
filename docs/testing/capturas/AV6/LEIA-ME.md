# AV6 — ausência para observadores, e o cliente não recebe (#201)

Duas provas de **naturezas diferentes**, e é isso que torna este gate diferente
de todos os outros da trilha.

Referência: [`referencia-d-zetsu.png`](../../../aura-art/referencia-d-zetsu.png)
— e ela é **literal**: o corpo é o corpo.

---

## As sete capturas

```
zetsu                  bloom OFF
zetsu_bloom_high       bloom HIGH, o caso delator
zetsu_observador       do cliente B, lado a lado com a referência D
zetsu_noite            zetsu_caverna
ten_para_zetsu         (VÍDEO, quadro a quadro)
ren_para_zetsu         (VÍDEO, quadro a quadro)
```

A comparação com a referência D é **na mesma sessão**, lado a lado. Olho humano
cansa; comparação de memória não vale.

---

## A prova que NÃO se faz olhando

> **Olhar a tela prova que o cliente escondeu. Nunca prova que ele não
> recebeu.**

O cliente de B precisa ser inspecionado com **log de payload em modo dev**:
enquanto A está em Zetsu, chega `aura_presence` com `NENHUM` — o mesmo byte de
quem nunca despertou — e **nenhum outro campo de aura**.

Este é o único item do gate que não se verifica com imagem, e é o mais
importante: a defesa real é o servidor não mandar, e não o cliente esconder.

---

## Reprova se

- sobrar **qualquer** contorno ou halo residual com `vfx.bloom = HIGH`
- o fechamento sair da faixa de 200 a 500 ms em algum dos dois vídeos
- o fechamento for lido como rampa única, e não em fases
- sobrar estado visual após `/nenvfx off`, relog, morte ou troca de dimensão
- `runServer` acusar `NoClassDefFoundError` ou `may not be sent to the client`

---

## O que este gate NÃO prova

A inspeção é feita com um **cliente honesto**: ninguém construiu um cliente
modificado para tentar ler o que não deveria chegar.

E a coluna **observador** do resolvedor nunca foi exercitada: `gyoDoObservador`
e `inDoAlvo` chegam sempre `false`, porque Gyo e In são marcos de Nen (F1 e F2)
e o laço por observador no servidor depende de #126. A tabela inteira está
fixada em JUnit; em jogo, só a linha trivial rodou.

O **pulso de supressão** é HUD, e não aura — não há por onde vazar para um
observador, e isso é por construção. O que a captura julga é se ele lê como
*fechamento* e não como *dano recebido*.
