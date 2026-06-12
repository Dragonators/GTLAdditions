#version 150

uniform sampler2D EffectSampler;
uniform sampler2D BlurredEffectSampler;
uniform sampler2D MaskSampler;
uniform sampler2D BlurredMaskSampler;
uniform vec2 BlurInputScale;

in vec2 texCoord;

out vec4 fragColor;

const float SOFT_EDGE_ALPHA_SCALE = 1.0;

void main() {
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
    fragColor = vec4(clamp(color.rgb, 0.0, 1.0), clamp(color.a, 0.0, 1.0));
}
