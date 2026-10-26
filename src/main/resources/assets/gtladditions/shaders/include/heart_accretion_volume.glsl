/*
 * GLSL port of MisterPrada/singularity, revision
 * 51313b398583a84c9347470ce4b575e05739e302:
 * src/Experience/Worlds/MainWorld/BlackHole.js and Utils/TSL-utils.js.
 * Original author: Mister & Prada. See HEART-ASSET-SOURCES.txt.
 */

const int HEART_VOLUME_STEPS = 128;
const int HEART_VOLUME_SAMPLES = 4;
const float HEART_VOLUME_OPAQUE = 0.999;
const float HEART_VOLUME_STEP_SIZE = 0.0071;
const float HEART_VOLUME_GRAVITY = 0.3;
const float HEART_VOLUME_CORE_RADIUS = 0.13;
const float HEART_VOLUME_HALF_WIDTH = 0.03;
// The two normalised noise layers have |noise| <= 1.112. Beyond this height even
// their maximum density cannot make localOpacity positive.
const float HEART_VOLUME_ACTIVE_HALF_WIDTH = 0.047;
// Angular speed is R^-1.5, equal to the rigid rate at the reference radius.
// Inside the inner radius the flow turns rigidly so the core never spins out of control.
const float HEART_VOLUME_FLOW_SPEED = 0.1;
const float HEART_VOLUME_FLOW_REFERENCE_RADIUS = 0.5;
const float HEART_VOLUME_FLOW_INNER_RADIUS = 0.3;
// Shear winds the noise without bound, so two layers restart half a period apart and
// cross-fade while each is still loosely wound.
const float HEART_VOLUME_FLOW_PERIOD = 12.0;
// Per-channel mean of heart_noise_deep.png; blending around it keeps the noise contrast.
const vec3 HEART_VOLUME_NOISE_MEAN = vec3(0.733, 0.730, 0.733);

struct HeartAccretionVolume {
    vec3 color;
    float opacity;
};

float heartSmoothRange(float value, float begin, float end) {
    float t = clamp((value - begin) / (end - begin), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

vec3 heartCatmullRom(float t, vec3 d, vec3 c, vec3 b, vec3 a) {
    return 0.5 * (2.0 * b + (-a + c) * t +
        (2.0 * a - 5.0 * b + 4.0 * c - d) * t * t +
        (-a + 3.0 * b - 3.0 * c + d) * t * t * t);
}

vec3 heartVolumeColorRamp(float value) {
    const vec4 a = vec4(0.95, 0.71, 0.44, 0.050);
    const vec4 b = vec4(0.14, 0.05, 0.03, 0.425);
    const vec4 c = vec4(0.0, 0.0, 0.0, 1.0);
    float ab = clamp((value - a.w) / (b.w - a.w), 0.0, 1.0);
    float bc = clamp((value - b.w) / (c.w - b.w), 0.0, 1.0);
    if (value < b.w) return heartCatmullRom(1.0 - ab, a.rgb, a.rgb, b.rgb, c.rgb);
    if (value < c.w) return heartCatmullRom(ab - bc, a.rgb, b.rgb, c.rgb, c.rgb);
    return c.rgb;
}

vec2 heartFlowUv(vec2 position, float phase) {
    float c = cos(phase), s = sin(phase);
    return vec2(c * position.x - s * position.y, s * position.x + c * position.y) * 2.0;
}

HeartAccretionVolume sampleHeartVolume(vec3 position, float noiseLod, vec2 layerWeights, vec2 layerSeconds) {
    if (abs(position.z) >= HEART_VOLUME_ACTIVE_HALF_WIDTH || dot(position, position) < HEART_VOLUME_CORE_RADIUS * HEART_VOLUME_CORE_RADIUS)
        return HeartAccretionVolume(vec3(0.0), 0.0);
    float discRadius = length(position.xy);
    float angularSpeed = HEART_VOLUME_FLOW_SPEED * pow(
        max(discRadius, HEART_VOLUME_FLOW_INNER_RADIUS) / HEART_VOLUME_FLOW_REFERENCE_RADIUS, -1.5);
    float twist = discRadius * 4.270;
    // The second layer starts half a turn round so the two noise layers never coincide.
    vec2 uvA = heartFlowUv(position.xy, twist - angularSpeed * layerSeconds.x);
    vec2 uvB = heartFlowUv(position.xy, twist - angularSpeed * layerSeconds.y + 3.14159265);
    vec3 zOffset = vec3(-HEART_VOLUME_HALF_WIDTH, 0.0, HEART_VOLUME_HALF_WIDTH) - position.z;
    vec3 bands = max(vec3(1.0) - zOffset * zOffset /
        (HEART_VOLUME_HALF_WIDTH * HEART_VOLUME_HALF_WIDTH), vec3(0.0));
    // Explicit LOD keeps texture sampling defined across the variable-length ray loop.
    vec3 noise = HEART_VOLUME_NOISE_MEAN +
        (textureLod(DiscNoiseSampler, uvA, noiseLod).rgb - HEART_VOLUME_NOISE_MEAN) * layerWeights.x +
        (textureLod(DiscNoiseSampler, uvB, noiseLod).rgb - HEART_VOLUME_NOISE_MEAN) * layerWeights.y;
    vec3 offsetNoise = HEART_VOLUME_NOISE_MEAN +
        (textureLod(DiscNoiseSampler, uvA * 1.002, noiseLod).rgb - HEART_VOLUME_NOISE_MEAN) * layerWeights.x +
        (textureLod(DiscNoiseSampler, uvB * 1.002, noiseLod).rgb - HEART_VOLUME_NOISE_MEAN) * layerWeights.y;
    float density = length(noise * bands);
    float offsetDensity = length(offsetNoise * bands);
    float ramp = discRadius + (density - 0.780) * 1.5 + (density - offsetDensity) * 19.750;
    vec3 emission = max(heartVolumeColorRamp(ramp), vec3(0.0)) * 2.0 + vec3(0.14, 0.129, 0.09);

    float radialOpacity = heartSmoothRange(discRadius, 1.0, 0.0);
    float localOpacity = heartSmoothRange(abs(position.z) - (density - 0.750) * 0.60,
        HEART_VOLUME_HALF_WIDTH, 0.0) * radialOpacity;

    return HeartAccretionVolume(emission, localOpacity);
}

// Reference inverse-square steering for an interval of length 2 * halfStep. The direction is
// bent from the interval start and applied after both halves, which sets the visible horizon.
vec3 heartVolumeSteer(vec3 position, vec3 direction, float halfStep) {
    float radius = max(length(position), 0.0001);
    float gravityFade = clamp((1.0 - radius) / 0.5, 0.0, 1.0);
    vec3 steered = direction - position / radius * (halfStep * HEART_VOLUME_GRAVITY * gravityFade / (radius * radius));
    return steered * inversesqrt(max(dot(steered, steered), 0.000001));
}

vec3 heartVolumeToLinear(vec3 color) {
    color = max(color, vec3(0.0));
    return mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), color));
}

HeartAccretionVolume traceHeartAccretionVolume(vec3 origin, vec3 direction, float seconds, bool captured, float footprint) {
    float noiseLod = clamp(log2(max(footprint * 2048.0, 1.0)), 0.0, 6.0);
    // One stable per-pixel seed offsets every integration interval. Independent complete
    // volume estimates remove coherent camera-centred shells without changing the density.
    uvec2 pixel = uvec2(floor(gl_FragCoord.xy));
    uint sampleHash = pixel.x * 1664525u + pixel.y * 1013904223u;
    sampleHash = (sampleHash ^ (sampleHash >> 16u)) * 2246822519u;
    sampleHash = (sampleHash ^ (sampleHash >> 13u)) * 3266489917u;
    sampleHash ^= sampleHash >> 16u;
    float sampleSeed = float(sampleHash & 16777215u) / 16777216.0;
    HeartAccretionVolume samples[HEART_VOLUME_SAMPLES];
    for (int sampleIndex = 0; sampleIndex < HEART_VOLUME_SAMPLES; ++sampleIndex) samples[sampleIndex] = HeartAccretionVolume(vec3(0.0), 0.0);
    vec3 rayPosition = origin;
    vec3 rayDirection = direction;
    HeartAccretionVolume result = HeartAccretionVolume(vec3(0.0), 0.0);
    float cycle = fract(seconds / HEART_VOLUME_FLOW_PERIOD);
    float fade = 1.0 - abs(2.0 * cycle - 1.0);
    // Normalise the pair so the blend keeps a single layer's noise variance throughout the fade.
    vec2 layerWeights = vec2(fade, 1.0 - fade) * inversesqrt(fade * fade + (1.0 - fade) * (1.0 - fade));
    vec2 layerSeconds = vec2(cycle, fract(cycle + 0.5)) * HEART_VOLUME_FLOW_PERIOD;

    for (int stepIndex = 0; stepIndex < HEART_VOLUME_STEPS; ++stepIndex) {
        // End the last empty segment at the disc slab before starting regular samples.
        // This anchors their phase to the material instead of the camera or proxy sphere;
        // otherwise the fixed step shells sweep visible rings across the disc as the eye moves.
        float slabGap = abs(rayPosition.z) - 2.0 * HEART_VOLUME_HALF_WIDTH;
        if (slabGap > 0.00001 && slabGap < 2.0 * HEART_VOLUME_STEP_SIZE * abs(rayDirection.z) && rayPosition.z * rayDirection.z < 0.0) {
            float entryHalfStep = 0.5 * slabGap / abs(rayDirection.z);
            vec3 entryDirection = heartVolumeSteer(rayPosition, rayDirection, entryHalfStep);
            rayPosition += rayDirection * (2.0 * entryHalfStep);
            rayDirection = entryDirection;
        }
        vec3 steered = heartVolumeSteer(rayPosition, rayDirection, HEART_VOLUME_STEP_SIZE);
        rayPosition += rayDirection * HEART_VOLUME_STEP_SIZE;
        float radius = length(rayPosition);

        if (radius < HEART_VOLUME_CORE_RADIUS) {
            if (captured) {
                for (int sampleIndex = 0; sampleIndex < HEART_VOLUME_SAMPLES; ++sampleIndex) {
                    samples[sampleIndex].color *= samples[sampleIndex].opacity;
                    samples[sampleIndex].opacity = 1.0;
                }
            }
            break;
        }

        float minimumOpacity = 0.0;
        if (abs(rayPosition.z) < HEART_VOLUME_ACTIVE_HALF_WIDTH + HEART_VOLUME_STEP_SIZE * abs(rayDirection.z)) {
            minimumOpacity = 1.0;
            float phase = fract(sampleSeed + float(stepIndex) * 0.61803398875);
            for (int sampleIndex = 0; sampleIndex < HEART_VOLUME_SAMPLES; ++sampleIndex) {
                if (samples[sampleIndex].opacity < HEART_VOLUME_OPAQUE) {
                    float offset = fract(phase + float(sampleIndex) / float(HEART_VOLUME_SAMPLES)) - 0.5;
                    vec3 position = rayPosition + rayDirection * (offset * 2.0 * HEART_VOLUME_STEP_SIZE);
                    HeartAccretionVolume local = sampleHeartVolume(position, noiseLod, layerWeights, layerSeconds);
                    float weight = (1.0 - samples[sampleIndex].opacity) * local.opacity;
                    samples[sampleIndex].color = mix(samples[sampleIndex].color, local.color, weight);
                    samples[sampleIndex].opacity = mix(samples[sampleIndex].opacity, 1.0, local.opacity);
                }
                minimumOpacity = min(minimumOpacity, samples[sampleIndex].opacity);
            }
        }
        rayPosition += rayDirection * HEART_VOLUME_STEP_SIZE;
        rayDirection = steered;
        if (minimumOpacity >= HEART_VOLUME_OPAQUE) break;
        if (dot(rayPosition, rayPosition) > 1.0001 && dot(rayPosition, rayDirection) > 0.0) break;
    }
    for (int sampleIndex = 0; sampleIndex < HEART_VOLUME_SAMPLES; ++sampleIndex) {
        float opacity = samples[sampleIndex].opacity;
        result.color += heartVolumeToLinear(samples[sampleIndex].color / max(opacity, 0.0001)) * opacity / float(HEART_VOLUME_SAMPLES);
        result.opacity += opacity / float(HEART_VOLUME_SAMPLES);
    }
    return result;
}
