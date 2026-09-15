# Como se prova que a aura ficou certa

Roteiro de verificação da trilha **AV**. A direção está em
[`../vfx/direcao-visual-da-aura.md`](../vfx/direcao-visual-da-aura.md); os
números em [`../vfx/perfis-visuais.md`](../vfx/perfis-visuais.md).

---

## 1. O problema desta trilha

**Render não é unit-testável.** Um build verde aqui prova menos do que em
qualquer outro marco do projeto, e fingir o contrário é o falso verde mais caro
que esta trilha pode produzir.

Então o que se prova se divide em três, e cada parte tem método próprio:

| O quê | Como |
| --- | --- |
| lógica sem tela (transição, LOD, visibilidade, orçamento, seed) | JUnit |
| ciclo de vida no servidor (nada client-only vaza; sem estado fantasma) | GameTest + `runServer` |
| **aparência** | captura comparada contra referência, por gente olhando |

O terceiro não vira verde. Ele vira **imagem arquivada com data e commit**, e
é isso que se revisa num PR.

---

## 2. Modo de captura

Sem ele, cada captura sai com hora diferente, FOV diferente, pose diferente e
skin diferente — e a comparação vira achismo.

`AuraCaptureMode` (dev/OP local) faz, de uma vez:

- trava a hora do dia **no valor em que ela estava** — e não num meio-dia fixo,
  senão `ten_noite` e `ten_caverna` seriam impossíveis;
- trava o clima;
- esconde o HUD;
- força terceira pessoa e pede sempre quatro blocos de recuo;
- neutraliza andar, strafe, pulo, agachamento e sprint para a pose assentar em
  idle;
- alterna Ten/Ren/Zetsu por comando.

> **A distância pedida é fixa; a distância real ainda respeita parede.** A
> colisão vanilla roda depois do ajuste e aproxima a câmera quando não há quatro
> blocos livres atrás do jogador. Ignorá-la permitiria enxergar através de
> blocos. A arena de captura precisa deixar esse espaço livre. A trava de pose
> também não cancela impulso já aplicado pelo servidor, como knockback.

Comandos de dev, **como existem no código** (`AuraDebugCommands`):

```
/nenvfx on | off                      liga/desliga o DESENHO da aura neste cliente
/nenvfx state ten|ren|zetsu|off|auto  força o estado visual do jogador LOCAL
/nenvfx output <0..1> | auto
/nenvfx particulas <0..2> | auto      sem escrever na config
/nenvfx ribbons <n> | auto
/nenvfx lod full|near|medium|far|hidden|auto
/nenvfx ajuste <nome> <valor> | auto  alpha_interno, alpha_borda, alpha_externo,
                                      fresnel, fluxo, ruido
/nenvfx freeze                        alterna o congelamento
/nenvfx tuning                        abre os sliders
/nenvfx capture modo on | off
/nenvfx capture agora <nome>          uma imagem
/nenvfx capture lote <etiqueta>       o conjunto de estados daquele ponto
/nenvfx reset | status
```

Três diferenças em relação ao que este documento previa, e cada uma tem motivo:

| Previsto | O que existe | Por quê |
| --- | --- | --- |
| `/nenvfx bloom <0..1>` | **não existe** | o passe de pós-processamento nasce no AV5. Botão que não gira nada é o erro nº 7 do `CLAUDE.md` em forma de comando |
| `/nenvfx lod <0..4>` | `lod` por **nome** | a tabela de LOD tem nomes (`FULL`…`HIDDEN`); um índice numérico seria uma segunda forma de dizer a mesma coisa, e as duas divergiriam |
| — | `/nenvfx particulas` | o critério do ADR-015 é uma captura com densidade **zero**. Sem isto ela custava editar o TOML e reentrar no mundo — e duas imagens com uma reentrada no meio não compartilham hora, pose nem enquadramento |

**As capturas caem em `screenshots/nenfoundation-av/`**, já nomeadas com data,
commit e nível de bloom. O commit vem do build (`nenfoundation-build.properties`,
escrito pelo `processResources`); num clone sem `.git` ele sai como `sem-git`, e
não vazio.

**Só local, só com permissão.** Nada disso muda estado autoritativo — é
aparência, não técnica.

E um overlay de dev (**F6**, `AuraDebugRenderer`) com: chamadas de desenho,
filamentos, partículas, jogadores com aura, o commit do build e o que está
forçado na mão. Os sliders ficam numa tela própria (`/nenvfx tuning`): alpha
interno, alpha de borda, alpha externo, Fresnel, fluxo, ruído e contagem de
ribbon — **direção de arte sem recompilar é a diferença entre uma tarde e uma
semana.**

Duas regras do overlay que não são cosméticas:

- **campo sem consumidor aparece como `--`, e nunca como zero.** O tamanho do
  alvo de bloom (AV5) e a visibilidade calculada por observador (AV6) não
  existem: escrever `0` neles afirmaria que o passe rodou e não custou nada;
- **enquanto houver qualquer sobreposição, o overlay grita `OVERRIDE ATIVO --
  esta captura NÃO vale como aprovação`.** Sem isso, alguém aprova um gate com
  um slider que não está em perfil nenhum, e o jogo que os jogadores veem nunca
  foi aquele.

Nada do que sai da tela de tuning persiste: tudo morre no logout, e o número só
vira decisão quando alguém o escreve em `assets/nenfoundation/nen_vfx/`, onde o
resto do time vê o diff.

---

## 3. Protocolo de comparação A/B

Toda captura de aprovação usa, obrigatoriamente:

- o mesmo local (a arena de captura do mundo de regressão);
- o mesmo FOV;
- a mesma hora travada;
- a mesma skin;
- a mesma distância de câmera;
- a mesma versão de assets.

E é comparada lado a lado com a referência correspondente:

| Captura | Referência |
| --- | --- |
| `ten_dia` | [B — Layered Ten](../aura-art/referencia-b-ten-em-camadas.png) |
| `ren_dia` | [C — Ren pressure](../aura-art/referencia-c-ren-pressao.png) |
| `zetsu` | [D — Zetsu suppression](../aura-art/referencia-d-zetsu.png) |

Guardar em `docs/testing/capturas/AV<n>/`, com data, commit e nível de bloom no
nome do arquivo.

### O conjunto mínimo por gate

```
ten_dia        ten_noite        ten_caverna
ren_dia        ren_noite        ren_caverna
zetsu
ten_slim       ten_armadura     ten_overlay_skin
ten_correndo   ten_agachado     ten_nadando
ten_2b  ten_5b  ten_10b  ten_20b  ten_40b
primeira_pessoa_ten   primeira_pessoa_ren
sem_particulas_ten    <- o critério do ADR-015
sem_bloom_ten         sem_bloom_ren
```

---

## 4. O teste que decide se a fundação está certa

> Rodar com `vfx.densidadeDeParticulas = 0.0` e `vfx.bloom = OFF`.
>
> **Aprovado** se ainda se lê "essa pessoa está em Ten".
> **Reprovado** se parece o jogador normal.

Este é o critério do [ADR-015](../adr/ADR-015-aura-e-geometria-e-shader.md), e
ele reprova o gate do AV3 inteiro. Ele existe porque a maneira mais comum de
falhar nesta trilha é compensar uma shell fraca com mais partícula.

---

## 5. Matriz por gate

### AV0 — a shell segue o corpo

| Verificação | Como |
| --- | --- |
| a shell acompanha corrida, pulo, ataque, agachar, nadar | captura em movimento, terceira pessoa |
| `slim` usa o modelo `slim` | dois clientes, skins Steve e Alex |
| nenhuma classe client-only no servidor dedicado | `runServer` limpo, log sem `NoClassDefFoundError` |
| ligar/desligar não deixa estado preso | `/nenvfx off`, relog, morte, troca de dimensão |

**Se a shell não acompanha a animação, a trilha PARA aqui.** É para isso que o
AV0 existe: descobrir isso custa uma tarde, e não um mês.

### AV1 — shell autoral

| Verificação | Como |
| --- | --- |
| a borda é mais forte que o miolo | captura a 5 e a 20 blocos |
| não parece armadura nem plástico | comparação com a referência B |
| ruído não parece nuvem | captura estática ampliada |
| legível de dia, não estoura à noite | `ten_dia` × `ten_noite` |
| a skin continua legível | idem |
| a segunda camada da skin não engole a aura | skin com overlay completo |

### AV2 — ribbons

| Verificação | Como |
| --- | --- |
| os filamentos nascem na superfície | captura a 2 blocos |
| eles acompanham braços e pernas ao correr | vídeo curto, terceira pessoa |
| nenhum flutua solto sem origem | idem |
| a pool é estável (sem *jitter* por frame) | captura congelada em dois frames seguidos |
| o seed é determinístico | mesma seed → mesma curva, em JUnit |

### AV3 — Ten final

Todos os itens de "TEN aprovado se" da direção visual, **mais** o teste sem
partícula e sem bloom, **mais** primeira pessoa, **mais** a tabela de
distâncias.

### AV4 — Ren

Todos os itens de "REN aprovado se", **mais**:

| Verificação | Como |
| --- | --- |
| nenhum bloco foi quebrado | inspeção do mundo depois de 5 min de Ren |
| nenhum `ItemEntity` foi criado | `/data` ou contagem de entidades |
| a transição Ten→Ren é lida como liberação | vídeo curto, e o *overshoot* precisa ser visível |
| o impulso de câmera não incomoda | ativar Ren 20 vezes seguidas |
| observadores **não** recebem impulso de câmera | segundo cliente |

### AV5 — bloom

| Verificação | Como |
| --- | --- |
| `OFF`, `FAST` e `HIGH` produzem a mesma leitura, em graus | três capturas do mesmo frame |
| **aura não aparece através de parede** | jogador em Ren atrás de um bloco sólido, câmera do outro lado |
| aura parcialmente ocluída sangra poucos pixels | jogador metade atrás de um canto |
| `F3+T` não crasha e não some com o efeito | recarregar recurso com Ren ligado |
| redimensionar a janela recria os alvos | maximizar/restaurar 10 vezes; medir memória |
| falha de shader cai para `FAST` | forçar falha; conferir log e ausência de crash |
| sem aura na tela, o passe é pulado | contador em modo dev marcando zero |

### AV6 — Zetsu

| Verificação | Como |
| --- | --- |
| ausência total para observadores | segundo cliente, captura comparada com D |
| nenhum contorno, nenhum bloom residual | captura com bloom `HIGH` |
| a transição fecha entre 200 e 500 ms | vídeo quadro a quadro |
| o cliente não recebe dado de aura de quem está suprimido | inspecionar o delta recebido, não a tela |

O último item é o único desta tabela que **não** se verifica olhando: olhar a
tela prova que o cliente escondeu, não que ele não recebeu.

### AV7 — multiplayer, armadura e poses

| Verificação | Como |
| --- | --- |
| 2, 5 e 10 jogadores com aura | servidor dedicado |
| A vê B ativar e desativar | dois clientes reais |
| relog, morte, respawn e troca de dimensão | sem estado visual preso |
| armadura, capa, elytra | captura por peça |
| nadar, rastejar, dormir, montar, arco, besta, escudo | captura por pose |
| `default` e `slim` | os dois |

### AV8 — performance

| Cenário | Medir |
| --- | --- |
| 10 jogadores em Ren dentro de 16 blocos | tempo de frame, *draw calls*, partículas, ribbons |
| 20 jogadores em Ten dentro de 32 blocos | idem |
| nenhuma aura visível | **custo zero**, não "custo pequeno" |
| 10 minutos de Ren contínuo | memória estável; nenhum alvo vazado |

Com `spark`, antes e depois, arquivado em `docs/testing/perfis/`.

---

## 6. O que este roteiro NÃO prova

Entra em [`o-que-nao-provamos.md`](o-que-nao-provamos.md) quando cada gate
fechar, mas já se sabe:

- **captura não é vídeo.** *Jitter*, cintilação e engasgo de animação aparecem
  em movimento e desaparecem numa imagem parada;
- **uma máquina não é o parque.** Tudo que for medido aqui foi medido em uma
  GPU só;
- **skin de teste não é skin de jogador.** Skins com transparência, com overlay
  completo ou desenhadas fora do padrão vão encontrar casos que estas não
  encontram;
- **shader pack não é testável exaustivamente.** A matriz cobre quatro
  ambientes, e existem dezenas;
- **olho humano cansa.** Comparação lado a lado com a referência, na mesma
  sessão — e não de memória, dias depois.
