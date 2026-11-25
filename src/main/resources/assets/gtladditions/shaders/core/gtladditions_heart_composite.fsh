#version 150

uniform sampler2D EffectSampler;
uniform sampler2D BlurredEffectSampler;
uniform sampler2D MaskSampler;
uniform sampler2D BlurredMaskSampler;
uniform sampler2D BloomSampler;
uniform sampler2D BloomSampler1;
uniform sampler2D BloomSampler2;
uniform sampler2D BloomSampler3;
uniform sampler2D BloomSampler4;
uniform vec2 BlurInputScale;
uniform vec2 CompositeOrigin;
uniform sampler2D HandMaskSampler;
uniform float HandMaskEnabled;
uniform sampler2D SceneDepth;
uniform mat4 InverseProjection;
uniform vec2 SceneSize;
// One proxy sphere per column, view-space centre and radius; a zero radius ends the list.
uniform mat4 HeartSpheres;

#moj_import <gtladditions:heart_color_transform.glsl>

in vec2 texCoord;

out vec4 fragColor;

const float SOFT_EDGE_ALPHA_SCALE = 1.0;
const float BLOOM_INTENSITY = 0.217;

bool sceneCoversHearts(ivec2 pixel) {
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    if (depth >= 1.0) return false;
    vec4 view = InverseProjection * vec4((vec2(pixel) + 0.5) / SceneSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 point = view.xyz / view.w;
    float scene = length(point);
    vec3 ray = point / scene;
    float nearest = -1.0;
    for (int i = 0; i < 4; ++i) {
        vec4 sphere = HeartSpheres[i];
        if (sphere.w <= 0.0) break;
        float along = dot(sphere.xyz, ray);
        float reach = sphere.w * sphere.w - dot(sphere.xyz, sphere.xyz) + along * along;
        float entry = reach > 0.0 ? along - sqrt(reach) : along;
        nearest = i == 0 ? entry : min(nearest, entry);
    }
    return scene < nearest;
}

void main() {
    if (HandMaskEnabled > 0.5 && texelFetch(HandMaskSampler, ivec2(gl_FragCoord.xy+CompositeOrigin), 0).r > 0.5) discard;
    vec4 sharp = textureLod(EffectSampler, texCoord, 0.0);
    vec4 blurred = textureLod(BlurredEffectSampler, texCoord, 0.0);
    float mask = textureLod(MaskSampler, texCoord, 0.0).a;
    float blurredMask = textureLod(BlurredMaskSampler, texCoord, 0.0).a;

    float blurScale = max(max(BlurInputScale.x, BlurInputScale.y), 1.0);
    float hardMask = clamp(mask, 0.0, 1.0);
    float softMask = smoothstep(0.006 / blurScale, 0.22, blurredMask);
    float maskOuterEdge = softMask * (1.0 - hardMask) * SOFT_EDGE_ALPHA_SCALE;
    vec4 color = sharp + blurred * maskOuterEdge * (1.0 - sharp.a);
    float presence = max(max(hardMask, maskOuterEdge), sharp.a);

    if (presence < 0.0005 || color.a < 0.0005) {
        discard;
    }
    // The soft edge spreads past the volume's own pixels; on an occluder it would show the Heart through it.
    if (hardMask < 0.0005 && sceneCoversHearts(ivec2(gl_FragCoord.xy + CompositeOrigin))) discard;
    float alpha = clamp(color.a, 0.0, 1.0);
    vec3 radiance = color.rgb / max(alpha, 0.0005);
    vec3 bloom = textureLod(BloomSampler, texCoord, 0.0).rgb;
    bloom += textureLod(BloomSampler1, texCoord, 0.0).rgb * 0.8;
    bloom += textureLod(BloomSampler2, texCoord, 0.0).rgb * 0.6;
    bloom += textureLod(BloomSampler3, texCoord, 0.0).rgb * 0.4;
    bloom += textureLod(BloomSampler4, texCoord, 0.0).rgb * 0.2;
    radiance += bloom * BLOOM_INTENSITY;
    fragColor = vec4(heartDisplayColor(radiance) * alpha, alpha);
}
