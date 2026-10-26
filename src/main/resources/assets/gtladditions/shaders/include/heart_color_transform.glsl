/*
 * ACES filmic and sRGB output transforms adapted from Three.js r180.
 * Copyright (c) 2010-2025 three.js authors. MIT; see THREE-LICENSE.txt.
 * Reference: src/renderers/shaders/ShaderChunk/tonemapping_pars_fragment.glsl.js.
 * The selected Singularity renderer uses ACESFilmicToneMapping, exposure 1.2.
 */

vec3 heartDisplayColor(vec3 radiance) {
    const mat3 inputMatrix = mat3(
        vec3(0.59719, 0.07600, 0.02840),
        vec3(0.35458, 0.90834, 0.13383),
        vec3(0.04823, 0.01566, 0.83777)
    );
    const mat3 outputMatrix = mat3(
        vec3(1.60475, -0.10208, -0.00327),
        vec3(-0.53108, 1.10813, -0.07276),
        vec3(-0.07367, -0.00605, 1.07602)
    );
    vec3 color = inputMatrix * (max(radiance, vec3(0.0)) * (1.2 / 0.6));
    vec3 a = color * (color + 0.0245786) - 0.000090537;
    vec3 b = color * (0.983729 * color + 0.4329510) + 0.238081;
    color = clamp(outputMatrix * (a / b), 0.0, 1.0);
    return mix(color * 12.92, 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055,
        step(vec3(0.0031308), color));
}
