#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform float AuraTime;
uniform float AuraFresnelPower;
uniform float AuraFlowSpeed;
uniform float AuraNoiseScale;
uniform float AuraEdgeBoost;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 normalVista;
in vec3 posVista;

out vec4 fragColor;

vec4 neblinaLinear(vec4 cor, float distancia, float inicio, float fim, vec4 corDaNeblina) {
    if (distancia <= inicio) {
        return cor;
    }
    float fator = distancia < fim ? smoothstep(inicio, fim, distancia) : 1.0;
    return vec4(mix(cor.rgb, corDaNeblina.rgb, fator * corDaNeblina.a), cor.a);
}

void main() {
    // FRESNEL: a borda das referencias. A intensidade cresce onde a superficie
    // fica de perfil para a camera. `abs` porque a shell nao tem cull: as faces
    // de tras chegam com a normal invertida, e sem o `abs` elas apagariam.
    vec3 paraCamera = normalize(-posVista);
    float fresnel = pow(1.0 - abs(dot(normalize(normalVista), paraCamera)), AuraFresnelPower);

    // FLUXO VERTICAL, ANCORADO NO CORPO. A UV da skin e estavel no modelo, entao
    // o ruido fica preso a superficie em vez de nadar quando a camera anda --
    // que e o que aconteceria ancorando em posicao de mundo ou de vista.
    // O `v` DIMINUI com o tempo para o desenho subir pelo corpo: na UV da skin,
    // v cresce para BAIXO.
    vec2 baseUv = texCoord0 * AuraNoiseScale;
    vec2 uvLento = vec2(baseUv.x, baseUv.y - AuraTime * AuraFlowSpeed);
    vec2 uvFino = vec2(baseUv.x * 2.7 + 0.37, baseUv.y * 2.7 - AuraTime * AuraFlowSpeed * 1.9);

    // DOIS NIVEIS, DE UMA TEXTURA SO: um lento e largo, outro fino e rapido. A
    // soma quebra a uniformidade sem virar nuvem -- a textura e de filamentos,
    // e nao de manchas.
    float n1 = texture(Sampler0, uvLento).r;
    float n2 = texture(Sampler0, uvFino).r;
    float ruido = n1 * 0.65 + n2 * 0.35;

    // O RUIDO MODULA, MAS NAO APAGA. Um piso garante que a pelicula exista no
    // corpo inteiro; sem ele, a aura viraria so listras soltas.
    float corpo = mix(0.45, 1.0, ruido);
    float intensidade = corpo * (0.30 + AuraEdgeBoost * fresnel);

    vec4 cor = vertexColor * ColorModulator;
    cor.a *= intensidade;
    if (cor.a < 0.004) {
        discard;
    }

    // A BORDA PUXA PARA BRANCO. Nas referencias o contorno e quase branco e o
    // azul aparece sobretudo por fora; sem isto a aura fica um azul chapado.
    cor.rgb = mix(cor.rgb, vec3(1.0), clamp(fresnel * 0.6, 0.0, 1.0));

    fragColor = neblinaLinear(cor, vertexDistance, FogStart, FogEnd, FogColor);
}
