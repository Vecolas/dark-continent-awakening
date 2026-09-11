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
| Protecao de branch e CODEOWNERS exigem GitHub pago | — | **risco ACEITO** conscientemente, ADR-008 |
| Licenca do codigo | — | **decidida**: All Rights Reserved, ADR-008 |
| Nenhuma maquina tem JDK 21 | — | **resolvido**: Temurin 21.0.12 instalado; build verde sem JDK portatil |
| Handles do GitHub das duas pessoas nao estao registrados aqui | a segunda pessoa entrar no repositorio | **resolvido**: ver a secao abaixo |
| O repositorio vive dentro do OneDrive | mover quando nao houver worktree ativa | em aberto por escolha; ver CONVENCOES secao 10 |

> **Sobre a protecao de branch:** aceitar o risco NAO e autorizar push direto.
> A regra de PR continua valendo; o que mudou e que agora esta escrito que ela
> nao tem mecanismo por tras, e que um push direto na `main` nao produz aviso
> nenhum. Ver ADR-008.

Bloqueio sem nome nao serve. "Depende de aprovacao" nao e bloqueio; "depende de
a pessoa X decidir Y" e.

---

## Dev A e Dev B

| Papel | Handle | Como foi confirmado |
| --- | --- | --- |
| **Dev A** — nucleo: dominio, servidor, persistencia, rede, regras | **@Vecolas** | responsavel pelo repositorio; autor da maioria dos PRs de nucleo |
| **Dev B** — superficie: cliente, HUD, dados, integracoes, modpack, QA | **@jonex-01** | autor dos PRs #78 e #83 (HUD, AOP, assets), mergeados na `main` |

A divisao nao e hierarquica. Ela existe porque essas duas metades quase nunca
tocam nos mesmos arquivos.

> **Como os handles foram descobertos, e por que isso importa:** eles nao foram
> perguntados a ninguem -- saem do `author.login` dos PRs mergeados. Handle
> registrado de ouvido e handle que erra uma letra e nao notifica ninguem, que
> e exatamente a falha que esta linha existe para evitar.

**Uma issue com "Owner: Dev B" agora pode ser atribuida de verdade.** Ate aqui
ela nao notificava ninguem -- era um papel sem endereco.
