#version 150

// O DESFOQUE, EM UM EIXO SO.
//
// SEPARAVEL, E POR ISSO DOIS PASSES. Um desfoque 2D de raio R custa (2R+1)^2
// amostras por pixel; feito em dois passes de um eixo cada, custa 2*(2R+1).
// Com sete taps a diferenca e de 225 para 30 -- custo quadratico por nada, e o
// desfoque e o segundo item mais caro da ordem de custos do AV8.
//
// UM SO ARQUIVO PARA OS DOIS EIXOS. `AuraPassoDoBorrao` chega como (dx, 0) no
// passe horizontal e (0, dy) no vertical, ja em coordenada normalizada e ja
// multiplicado pelo raio. Dois .fsh identicos a menos de uma linha e a
// duplicacao que diverge no primeiro ajuste -- e shader e a parte do jogo que
// mais quebra entre versoes, entao quanto menos arquivo, menor a reescrita.
//
// OS PESOS CHEGAM PRONTOS, em dois vec4. Recalcular uma gaussiana por pixel e
// por quadro e gastar ALU para reproduzir oito numeros que nao mudam; a conta
// mora na CPU, uma vez por mudanca de raio. Um array de oito floats seria mais
// natural, e nao existe: o `Uniform` do Minecraft para em quatro componentes.
//
// TAP ZERO E O CENTRO. Os sete restantes sao simetricos, entao o laco soma dos
// dois lados -- quinze amostras no total, com peso zero nos taps que o raio
// atual nao alcanca.

uniform sampler2D Sampler0;

uniform vec2 AuraPassoDoBorrao;
uniform vec4 AuraPesosA;
uniform vec4 AuraPesosB;

in vec2 texCoord;

out vec4 fragColor;

vec4 lado(float peso, float indice) {
    if (peso <= 0.0) {
        return vec4(0.0);
    }
    vec2 d = AuraPassoDoBorrao * indice;
    return (texture(Sampler0, texCoord + d) + texture(Sampler0, texCoord - d)) * peso;
}

void main() {
    vec4 soma = texture(Sampler0, texCoord) * AuraPesosA.x;
    soma += lado(AuraPesosA.y, 1.0);
    soma += lado(AuraPesosA.z, 2.0);
    soma += lado(AuraPesosA.w, 3.0);
    soma += lado(AuraPesosB.x, 4.0);
    soma += lado(AuraPesosB.y, 5.0);
    soma += lado(AuraPesosB.z, 6.0);
    soma += lado(AuraPesosB.w, 7.0);
    fragColor = soma;
}
