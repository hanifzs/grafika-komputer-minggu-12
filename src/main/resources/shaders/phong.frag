#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 baseColor;
uniform sampler2D diffuseMap;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadows;

float shadowFactor(vec3 normal, vec3 lightDir) {
    vec3 projected = FragPosLightSpace.xyz / FragPosLightSpace.w;
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.z < 0.0 ||
        projected.x < 0.0 || projected.x > 1.0 ||
        projected.y < 0.0 || projected.y > 1.0) return 0.0;

    float bias = max(0.006 * (1.0 - dot(normal, lightDir)), 0.0008);
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float occluded = 0.0;
    // PCF 3x3 menghaluskan tepi bayangan; bias mengurangi shadow acne.
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float nearest = texture(shadowMap, projected.xy + vec2(x, y) * texel).r;
            occluded += projected.z - bias > nearest ? 1.0 : 0.0;
        }
    }
    return occluded / 9.0;
}

void main() {
    vec3 n = normalize(Normal);
    vec3 l = normalize(lightPos - FragPos);
    vec3 v = normalize(viewPos - FragPos);
    float diffuse = max(dot(n, l), 0.0);

    float specular = 0.0;
    if (diffuse > 0.0) {
        if (useBlinnPhong) {
            vec3 halfway = normalize(l + v);
            specular = pow(max(dot(n, halfway), 0.0), 32.0);
        } else {
            vec3 reflected = reflect(-l, n);
            specular = pow(max(dot(v, reflected), 0.0), 16.0);
        }
    }
    float shadow = useShadows ? shadowFactor(n, l) : 0.0;
    vec3 albedo = useTexture ? texture(diffuseMap, TexCoords).rgb : baseColor;
    vec3 ambient = 0.22 * lightColor * albedo;
    vec3 lit = (1.0 - shadow) * (diffuse * lightColor * albedo + 0.75 * specular * lightColor);
    FragColor = vec4(ambient + lit, 1.0);
}
