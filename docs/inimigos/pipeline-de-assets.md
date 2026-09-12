# Pipeline de assets dos inimigos

Esta e a convencao de fonte e export para GeckoLib **4.8.3**. Ela pertence ao
baseline EN0; controllers, bones e o dummy executavel pertencem a #114 e #138.

## Fonte versionada

O arquivo editavel do Blockbench nao e recurso de runtime. Ele mora fora de
`src/main/resources` e entra no Git:

```text
art-source/enemies/<enemy_id>/<enemy_id>.bbmodel
```

`enemy_id` usa `lower_snake_case` e deve ser o mesmo id da definition. Texturas
fonte auxiliares ficam no mesmo diretorio. O `.gitignore` nao ignora
`art-source/`; nenhum JAR de ferramenta ou biblioteca entra ali.

## Export GeckoLib 4

GeckoLib 4 usa os caminhos abaixo:

```text
src/main/resources/assets/nenfoundation/
|-- geo/entity/<enemy_id>.geo.json
|-- animations/entity/<enemy_id>.animation.json
`-- textures/entity/<enemy_id>/<variant>.png
```

O diretorio `assets/nenfoundation/geckolib/models` pertence ao layout do
GeckoLib 5 e nao deve ser usado enquanto a versao pinada for 4.8.3. A fonte
oficial do formato 4 e a documentacao de
[Geo Models](https://github.com/bernie-g/geckolib/wiki/Geo-Models-%28Geckolib4%29).

## Regra de entrega

Uma PR de arte nomeia separadamente fonte e exports no stage. O build exclui
qualquer `.bbmodel` que apareca por engano em resources, mas isso e rede de
seguranca: o local correto continua sendo `art-source/enemies/`.

Nenhum keyframe aplica dano, stagger, grab ou recompensa. O servidor publica o
action id e decide as janelas de gameplay; o cliente apenas escolhe e reproduz
o clip correspondente.
