#version 150

// Local coordinates are Z-up.
out vec4 fragColor;

vec3 srgb(vec3 c) {
    c = max(c, vec3(0.));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1. / 2.4)) - .055, step(vec3(.0031308), c));
}

uniform sampler2D tColor, tDepth;
uniform sampler2D tNoise;
uniform vec4 uViewport;
uniform vec2 uResolution;
uniform mat4 uInvProj, uCamWorld;
uniform vec3 uCam, uEyeScale, uTint;
uniform float uSeat, uFloor, uOn, uMist, uMistH, uShroud, uRise, uStir, uLight, uMistGlow, uClimb;

// 64 z slices in an 8x8 atlas, each with a one-texel periodic XY border.
vec4 noiseSlice(vec2 p, float slice) {
    float z = mod(slice, 64.);
    vec2 tile = vec2(mod(z, 8.), floor(z / 8.));
    return textureLod(tNoise, (tile * 66. + 1. + fract(p) * 64.) / 528., 0.);
}

vec4 sampleVolumeNoise(vec3 p) {
    float z = fract(p.z) * 64. - .5;
    return mix(noiseSlice(p.xy, floor(z)), noiseSlice(p.xy, floor(z) + 1.), fract(z));
}

vec3 lin(vec3 c) {
    return mix(c / 12.92, pow((c + .055) / 1.055, vec3(2.4)), step(vec3(.04045), c));
}

uniform ivec2 uSwirlWords;

// The quotient is below 512. A half-unit bias covers reciprocal rounding and
// leaves at most one extra divisor, which can be subtracted after the last word.
uint reduceSwirlWord(uint value, uint divisor, float invDivisor) {
    uint quotient = uint(max(0., float(value) * invDivisor - .5));
    return value - quotient * divisor;
}

float swirlAngle(float radius) {
    // The 10.5-block mist gives a Q23 divisor at most 2.26 * 2^23.
    // Even a partial remainder of 1.6 divisors fits in uint after a seven-bit shift.
    uint divisor = uint((1. + radius * .12) * 8388608.);
    float invDivisor = 1. / float(divisor);
    uvec2 words = uvec2(uSwirlWords);
    uint remainder;
    if (words.x == 0u) {
        remainder = reduceSwirlWord(words.y, divisor, invDivisor);
    } else {
        remainder = reduceSwirlWord(words.x, divisor, invDivisor);
        remainder = reduceSwirlWord((remainder << 4u) | (words.y >> 28u), divisor, invDivisor);
        for (int shift = 21; shift >= 0; shift -= 7)
            remainder = reduceSwirlWord((remainder << 7u) | ((words.y >> uint(shift)) & 127u), divisor, invDivisor);
    }
    if (remainder >= divisor) remainder -= divisor;
    return 6.28318530718 * (float(remainder) / float(divisor));
}

// p is in eye-centred blocks; de is the approximate distance to the eye surface.
float mistDensity(vec3 p, out float de) {
    vec3 s = uEyeScale;
    float k0 = length(p / s), k1 = length(p / (s * s));
    de = k0 * (k0 - 1.) / max(k1, 1e-4);
    if (k0 < 1.)
        return 0.;
    float r = length(p.xy), h = max(p.z, 0.), out1 = max(r - s.x, 0.);
    float H = (.5 + .3 * uShroud) * uMistH;
    float pool = exp(-pow(out1 / 2.8, 1.5)) * (1. - smoothstep(8.2, 9.8, r)) * exp(-h / (H * (1. - .55 * smoothstep(0., 3., out1))));
    float climbH = (.25 + .15 * uShroud) * s.z * uMistH * mix(uClimb, 1., uShroud);
    float climb = exp(-max(de, 0.) / .6) * (1. - smoothstep(climbH * .45, climbH, h));
    float prof = max(pool, climb);
    if (prof < .02)
        return 0.;
    float a = swirlAngle(r), cs = cos(a), sn = sin(a);
    vec3 q = vec3(cs * p.x - sn * p.y, sn * p.x + cs * p.y, p.z * .7 - uRise);
    q += (sampleVolumeNoise(q * .03 + vec3(.13, .71, .37)).rgb - .5) * (2.6 + 3. * uStir);
    float nz = sampleVolumeNoise(q * .05).a * .5 + sampleVolumeNoise(q * .11 + .31).r * .32 + sampleVolumeNoise(q * .24 + .57).g * .18;
    return clamp(prof * 2.4 - .35 + (nz - .5) * 3.6 * smoothstep(.05, .4, prof), 0., 1.) * uMist;
}

void main() {
    vec2 uv = (gl_FragCoord.xy - uViewport.xy) / uResolution;
    vec4 scene = texture(tColor, uv);
    vec3 col = lin(scene.rgb);
    if (uOn > .5) {
        float d = texture(tDepth, uv).x;
        vec2 ndc = (gl_FragCoord.xy - uViewport.xy) / uViewport.zw * 2. - 1.;
        vec4 vp = uInvProj * vec4(ndc, d * 2. - 1., 1.);
        vp /= vp.w;
        vec3 P = (uCamWorld * vp).xyz, rd = normalize(P - uCam);
        float tScene = d >= 1. ? 1e5 : length(P - uCam);
        vec3 c = vec3(0., 0., uSeat), Pe = P - c, o = uCam - c;
        // The floor hidden under the eye must not become a gold disk when viewed from inside.
        vec3 floorEye = Pe / uEyeScale;
        if (d < 1. && abs(P.z - uFloor) < .02 && dot(floorEye, floorEye) >= 1. && length(Pe.xy) < 11.5) {
            float gapR = max(length(Pe.xy) - uEyeScale.x, 0.);
            col *= mix(.30, 1., smoothstep(0., 3., gapR)) +
                uTint * uLight * exp(-gapR / 1.4) * 2. * (1. - smoothstep(9.5, 11.5, length(Pe.xy)));
        }
        if (uMist > 0.) {
            float R = 10.5, top = uEyeScale.z * (.9 + .5 * uMistH);
            float tzn = -1e20, tzf = 1e20;
            if (abs(rd.z) < 1e-6) {
                if (o.z < 0. || o.z > top) {
                    fragColor = scene;
                    return;
                }
            } else {
                float tz0 = -o.z / rd.z, tz1 = (top - o.z) / rd.z;
                tzn = min(tz0, tz1);
                tzf = max(tz0, tz1);
            }
            float A = dot(rd.xy, rd.xy), B = dot(o.xy, rd.xy), Cc = dot(o.xy, o.xy) - R * R, disc = B * B - A * Cc;
            if (disc > 0. || A < 1e-8) {
                float trn = -1e20, trf = 1e20;
                if (A < 1e-8) {
                    if (Cc > 0.) {
                        fragColor = scene;
                        return;
                    }
                } else {
                    float sq = sqrt(max(disc, 0.));
                    trn = (-B - sq) / A;
                    trf = (-B + sq) / A;
                }
                float tn = max(max(tzn, trn), 0.), tf = min(min(tzf, trf), tScene);
                if (tf > tn) {
                    const int STEPS = 48;
                    float dt = (tf - tn) / float(STEPS),
                          jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(.06711056, .00583715))));
                    float Tr = 1.;
                    vec3 Ls = vec3(0.);
                    for (int i = 0; i < STEPS; i++) {
                        vec3 p = o + rd * (tn + (float(i) + jitter) * dt);
                        float de, dens = mistDensity(p, de);
                        if (dens > .001) {
                            float a = exp(-dens * 4. * dt);
                            vec3 lit = uTint * uLight * exp(-max(de, 0.) / .3) * uMistGlow +
                                vec3(.004, .0042, .005) * (.6 + .4 * smoothstep(0., 1.5, p.z));
                            Ls += Tr * (1. - a) * lit;
                            Tr *= a;
                            if (Tr < .01)
                                break;
                        }
                    }
                    col = col * Tr + Ls;
                }
            }
        }
    }
    fragColor = vec4(srgb(col), scene.a);
}
