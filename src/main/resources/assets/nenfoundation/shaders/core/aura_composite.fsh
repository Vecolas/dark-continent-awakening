#version 150

// O COMPOSITE DE VOLTA NA CENA.
//
// ADITIVO, E NUNCA SUBSTITUICAO. A cena por baixo continua intacta: o que este
// passe faz e SOMAR luz. O blend aditivo esta no estado de render, e nao aqui;
// o que este fragmento devolve e a contribuicao, ja pesada.
//
// O PESO VEM DE FORA e ja traz duas coisas multiplicadas: a forca do perfil da
// tecnica (0.20 em Ten, 0.55 em Ren) e o multiplicador que o jogador escolheu.
// Calcular qualquer uma das duas aqui esconderia o numero num shader, que e o
// pior lugar possivel para um botao de tuning.
//
// O ALPHA SAI EM 1 DE PROPOSITO. Com blend aditivo (SRC_ALPHA, ONE) o alpha do
// fragmento multiplicaria a contribuicao de novo, e o peso ja esta no RGB --
// aplicar duas vezes daria um halo com o quadrado da intensidade, e "o bloom
// some quando eu abaixo o slider" e um relato que ninguem liga a um alpha.

uniform sampler2D Sampler0;

uniform float AuraPesoDoBrilho;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 brilho = texture(Sampler0, texCoord).rgb * AuraPesoDoBrilho;
    fragColor = vec4(brilho, 1.0);
}
