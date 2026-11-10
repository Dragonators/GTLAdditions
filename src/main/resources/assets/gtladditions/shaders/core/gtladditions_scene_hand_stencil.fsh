#version 150

uniform sampler2D SceneHandMask;

// Writes only the stencil: a pixel outside the completed hand mask keeps it clear.
void main() {
    if (texelFetch(SceneHandMask, ivec2(gl_FragCoord.xy), 0).r <= 0.5) discard;
}
