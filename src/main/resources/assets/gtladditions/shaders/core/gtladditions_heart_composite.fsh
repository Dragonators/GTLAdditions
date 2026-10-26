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

#moj_import <gtladditions:heart_color_transform.glsl>

in vec2 texCoord;

out vec4 fragColor;

const float SOFT_EDGE_ALPHA_SCALE = 1.0;
const float BLOOM_INTENSITY = 0.217;

void main() {
    if (HandMaskEnabled > 0.5 && texelFetch(HandMaskSampler, ivec2(gl_FragCoord.xy+CompositeOrigin), 0).r > 0.5) discard;
    vec4 sharp = texture(EffectSampler, texCoord);
    vec4 blurred = texture(BlurredEffectSampler, texCoord);
    float mask = texture(MaskSampler, texCoord).a;
    float blurredMask = texture(BlurredMaskSampler, texCoord).a;

    float blurScale = max(max(BlurInputScale.x, BlurInputScale.y), 1.0);
    float hardMask = clamp(mask, 0.0, 1.0);
    float softMask = smoothstep(0.006 / blurScale, 0.22, blurredMask);
    float maskOuterEdge = softMask * (1.0 - hardMask) * SOFT_EDGE_ALPHA_SCALE;
    vec4 color = sharp + blurred * maskOuterEdge * (1.0 - sharp.a);
    float presence = max(max(hardMask, maskOuterEdge), sharp.a);

    if (presence < 0.0005 || color.a < 0.0005) {
        discard;
    }
    float alpha = clamp(color.a, 0.0, 1.0);
    vec3 radiance = color.rgb / max(alpha, 0.0005);
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    bloom += texture(BloomSampler1, texCoord).rgb * 0.8;
    bloom += texture(BloomSampler2, texCoord).rgb * 0.6;
    bloom += texture(BloomSampler3, texCoord).rgb * 0.4;
    bloom += texture(BloomSampler4, texCoord).rgb * 0.2;
    radiance += bloom * BLOOM_INTENSITY;
    fragColor = vec4(heartDisplayColor(radiance) * alpha, alpha);
}
