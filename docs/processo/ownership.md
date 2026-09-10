# Ownership: quem cuida de que area

O objetivo **nao** e dividir o mod em duas metades. E criar **areas primarias**
e contratos estaveis, para que as duas pessoas raramente precisem do mesmo
arquivo ao mesmo tempo.

Toda mudanca relevante passa por review da outra pessoa. Owner nao significa
"decide sozinho"; significa "e a mao que escreve, e a primeira a ser
perguntada".

> **CODEOWNERS nao existe neste repositorio.** Em repositorio privado, a
> aplicacao automatica de CODEOWNERS e de protecao de branch exige plano pago do
> GitHub. Esta tabela e disciplina, nao trava — e o historico de qualquer
> projeto mostra o que acontece sem trava. Ver [a secao de bloqueios](#bloqueios-conhecidos).

---

## Tabela de ownership

| Area | Owner primario | Segundo | Review obrigatorio |
| --- | --- | --- | --- |
| Build, registries, API base | Dev A | Dev B auxilia | Dev B |
| Persistencia, perfil, migracoes | Dev A | Dev B escreve os testes | Dev B |
| Aura, categoria, regras de combate | Dev A | Dev B integra UI e testes | Dev B |
| Networking server-side | Dev A | Dev B faz o handler de cliente | Dev B |
| HUD, keybind, FX, cliente | Dev A define o contrato de payload | Dev B | Dev A |
| Datapacks, definicoes, lang | Dev A define o schema | Dev B escreve os dados | Dev A |
| Integracoes (FTB, KubeJS, Patchouli, Jade) | Dev B | Dev A consulta | Dev A |
| Habilidades de prova | dividem por classe | — | a outra pessoa |
| QA em servidor dedicado | Dev B executa a matriz | Dev A corrige o nucleo | ambos |
| Profiling de performance | Dev A instrumenta | Dev B mede e reproduz | ambos |
| Modpack, version-lock, manifesto | Dev B | Dev A | Dev A |

O mesmo mapa aparece em cada `package-info.java`, junto do marco em que aquele
pacote nasce. Se esta tabela e um `package-info` discordarem, **o
`package-info` ganha** — ele esta mais perto do codigo — e esta tabela se
atualiza.

---

## Revisao cruzada nos marcos

Alem do review normal de PR, alguns marcos pedem revisao cruzada explicita:

- **M4:** Dev A revisa Zetsu e Gyo; Dev B revisa Ten e Ren. Ninguem revisa a
  propria maquina de estados.
- **M5:** depois da **sexta** habilidade de prova, os dois fazem uma revisao
  arquitetural juntos, procurando condicionais especificas de habilidade que
  precisam virar componente. Essa revisao esta marcada no roadmap de proposito:
  e o momento em que a API ou prova que serve, ou mostra onde nao serve.

---

## Bloqueios conhecidos

| Bloqueio | Quem destrava | Estado |
| --- | --- | --- |
| Protecao de branch e CODEOWNERS exigem GitHub pago em repo privado | decisao dos dois (pagar, abrir o repo, ou aceitar) | em aberto |
| Licenca do codigo ainda e *All Rights Reserved* por padrao | decisao dos dois | em aberto — ver ADR-007 |
| Handles do GitHub das duas pessoas nao estao registrados aqui | preencher esta tabela | em aberto |
| Nenhuma maquina do projeto tem JDK 21 confirmado | instalar Temurin 21 | ver README |

Bloqueio sem nome nao serve. "Depende de aprovacao" nao e bloqueio; "depende de
a pessoa X decidir Y" e.

---

## Dev A e Dev B

Substitua pelos nomes reais quando as duas pessoas estiverem no repositorio.
Enquanto isso, a tabela vale pelos papeis:

- **Dev A** — nucleo: dominio, servidor, persistencia, rede, regras.
- **Dev B** — superficie: cliente, HUD, dados, integracoes, modpack, QA.

A divisao nao e hierarquica. Ela existe porque essas duas metades quase nunca
tocam nos mesmos arquivos.
