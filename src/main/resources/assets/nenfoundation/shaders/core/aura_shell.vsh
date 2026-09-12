#version 150

// SEM `#moj_import`, DE PROPOSITO. O resolvedor de include do Minecraft procura
// em `shaders/include/` e a resolucao entre namespaces e justamente o tipo de
// detalhe que muda entre versoes. As duas funcoes de neblina que este shader
// usa cabem em oito linhas; inline-las troca uma dependencia fragil por
// duplicacao pequena e visivel. Se o vanilla mudar a formula, o teto de risco e
// a aura receber neblina levemente diferente do resto -- nao um shader que nao
// compila.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 normalVista;
out vec3 posVista;

float distanciaDeNeblina(vec3 pos, int forma) {
    if (forma == 0) {
        return length(pos);
    }
    return max(length(pos.xz), abs(pos.y));
}

void main() {
    vec4 vista = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vista;

    vertexDistance = distanciaDeNeblina(Position, FogShape);
    vertexColor = Color;
    texCoord0 = UV0;

    // A NORMAL VAI PARA O ESPACO DE VISTA porque o Fresnel compara normal com a
    // direcao da camera, e la a camera esta na origem olhando para -Z. Sem a
    // matriz de normal separada: a transformacao do jogador e rotacao mais
    // escala uniforme, entao a ModelViewMat basta.
    normalVista = normalize((ModelViewMat * vec4(Normal, 0.0)).xyz);
    posVista = vista.xyz;
}
