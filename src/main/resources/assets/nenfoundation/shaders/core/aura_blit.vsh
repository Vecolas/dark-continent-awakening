#version 150

// O VERTEX SHADER DOS TRES PASSES DE POS-PROCESSAMENTO.
//
// UM SO PARA OS TRES, e nao um por passe: eles desenham exatamente o mesmo
// quadro de tela cheia, e tres copias identicas de dez linhas divergiriam na
// primeira vez que alguem mexesse numa delas. O que muda entre os passes e o
// FRAGMENTO.
//
// SEM MATRIZ DE PROJECAO, de proposito. A posicao ja chega em coordenada
// normalizada de dispositivo (-1 a 1), entao nao ha o que projetar. Uma
// ProjMat aqui seria mais um uniform para o Minecraft preencher e mais uma
// coisa para dar errado no dia em que a linha de RenderSystem mudar -- e ela
// muda a cada versao.

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = UV0;
}
