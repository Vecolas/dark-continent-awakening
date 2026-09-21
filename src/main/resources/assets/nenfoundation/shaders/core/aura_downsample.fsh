#version 150

// DOWNSAMPLE PARA MEIA RESOLUCAO.
//
// QUATRO AMOSTRAS, E NAO UMA. Reduzir pela metade pegando um pixel de cada
// quatro e o caminho mais curto para CINTILACAO: uma ribbon fina de um pixel
// aparece e some conforme ela cruza a grade de amostragem, e o efeito e um
// filamento que pisca em movimento sem piscar na imagem parada -- ou seja, um
// defeito que nenhuma captura encontra.
//
// As quatro amostras sao os centros dos quatro texels de origem que cabem no
// texel de destino. O filtro linear do proprio sampler ja faria parte disso,
// mas depender dele amarra o resultado ao estado de filtragem da textura, que
// e a ultima coisa que alguem lembra de conferir.

uniform sampler2D Sampler0;

// Meio texel da textura de ORIGEM, ja em coordenada normalizada.
uniform vec2 AuraMeioTexel;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 soma =
          texture(Sampler0, texCoord + vec2(-AuraMeioTexel.x, -AuraMeioTexel.y))
        + texture(Sampler0, texCoord + vec2( AuraMeioTexel.x, -AuraMeioTexel.y))
        + texture(Sampler0, texCoord + vec2(-AuraMeioTexel.x,  AuraMeioTexel.y))
        + texture(Sampler0, texCoord + vec2( AuraMeioTexel.x,  AuraMeioTexel.y));
    fragColor = soma * 0.25;
}
