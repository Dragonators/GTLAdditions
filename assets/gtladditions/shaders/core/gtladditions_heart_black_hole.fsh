#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform float DiskTilt;
uniform float Spin;
uniform vec3 HotColor;
uniform vec3 CoolColor;
uniform float Intensity;
uniform vec3 Center;
uniform vec3 CameraPosition;
uniform vec3 ViewPlaneRight;
uniform vec3 ViewPlaneUp;
uniform vec3 ViewPlaneForward;
uniform float BlackHoleAndDiskRadius;
uniform float DistortionRadius;

in vec2 localPosition;
in vec3 rayPlaneOffset;

out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float RS = 1.0;
const float ISCO = 3.0;
const float DISK_IN = 2.2;
const float DISK_OUT = 14.0;
// Radiant-projected ratio: event horizon / full accretion disk is PROJECTED_SHADOW_RADIUS / PROJECTED_DISK_RADIUS.
const float PROJECTED_SHADOW_RADIUS = 0.07464;
const float PROJECTED_DISK_RADIUS = 0.457;
const float RAY_PLANE_SCALE = 1.24;
const float EFFECT_EDGE_FADE_START = 0.88;
const float SPACE_SHELL_ALPHA = 0.38;
const float SPACE_EDGE_FADE_START = 0.52;
const float SPACE_EDGE_FADE_END = 0.98;
const float TRACE_CAMERA_RADIUS = 28.0;
const int TRACE_STEPS = 128;

float getBillboardMask(float radiusRatio) {
    float pixelWidth = max(fwidth(radiusRatio) * 2.0, 0.0015);
    return 1.0 - smoothstep(1.0 - pixelWidth, 1.0 + pixelWidth, radiusRatio);
}

float smootherStep(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / max(edge1 - edge0, 0.0001), 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float gNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.866, 0.5, -0.5, 0.866);
    for (int i = 0; i < 4; i++) {
        v += a * gNoise(p);
        p = rot * p * 2.03 + vec2(47.0, 13.0);
        a *= 0.49;
    }
    return v;
}

vec3 blackbody(float t) {
    t = clamp(t, 0.0, 2.5);
    vec3 lo = vec3(1.0, 0.18, 0.0);
    vec3 mi = vec3(1.0, 0.55, 0.12);
    vec3 hi = vec3(1.0, 0.93, 0.82);
    vec3 hot = vec3(0.65, 0.82, 1.0);
    vec3 c = mix(lo, mi, smoothstep(0.0, 0.3, t));
    c = mix(c, hi, smoothstep(0.3, 0.8, t));
    return mix(c, hot, smoothstep(0.8, 1.8, t));
}

vec4 shadeDisk(vec3 hit, vec3 vel, float time, int crossing) {
    float r = length(hit.xz);
    if (r < DISK_IN * 0.55 || r > DISK_OUT * 1.04) {
        return vec4(0.0);
    }

    float xr = ISCO / max(r, 0.01);
    float temp = pow(ISCO / r, 0.75) * pow(max(0.001, 1.0 - sqrt(xr)), 0.25);
    temp *= sqrt(max(0.02, 1.0 - RS / r));

    float phi = atan(hit.z, hit.x);
    float logR = log2(max(r, 0.1));
    float omega = max(sqrt(0.5 * RS / (r * r * r)), 0.04) * 10.0;
    float rotAngle = time * omega;
    float ca = cos(rotAngle);
    float sa = sin(rotAngle);
    vec2 rotXZ = vec2(hit.x * ca - hit.z * sa, hit.x * sa + hit.z * ca);

    float cloud = fbm(rotXZ * 1.15 + vec2(logR * 3.0));
    float detail = gNoise(rotXZ * 3.6 + vec2(100.0 + time * 0.15, time * 0.10));
    float ringA = sin(r * 10.0 + rotAngle * r * 0.30) * 0.5 + 0.5;
    float ringB = sin(r * 20.0 - rotAngle * r * 0.15) * 0.5 + 0.5;
    float rings = 0.42 + 0.58 * (ringA * 0.56 + ringB * 0.44);
    float texture = (0.22 + 0.78 * cloud) * (0.72 + 0.28 * detail) * rings;

    float orbSpeed = sqrt(0.5 * RS / max(r, DISK_IN));
    vec3 orbDir = normalize(vec3(-hit.z, 0.0, hit.x));
    float doppler = 1.0 + 2.0 * dot(normalize(vel), orbDir) * orbSpeed * Spin;
    doppler = max(0.15, doppler);
    float dopplerBoost = doppler * doppler * doppler;

    float intensity = temp * texture * 6.0;
    float innerFade = smoothstep(DISK_IN * 0.70, DISK_IN * 1.18, r);
    float iscoFade = 0.34 + 0.66 * smoothstep(ISCO * 0.85, ISCO * 1.22, r);
    float outerFade = 1.0 - smoothstep(DISK_OUT * 0.56, DISK_OUT, r);
    intensity *= innerFade * iscoFade * outerFade;

    float colorTemp = temp * pow(doppler, 1.8) * 1.2;
    vec3 color = blackbody(colorTemp);
    color = mix(color, mix(CoolColor, HotColor, clamp(colorTemp, 0.0, 1.0)), 0.08);
    color *= intensity * dopplerBoost * Intensity;

    float alpha = clamp(intensity * 1.3, 0.0, 0.96);
    if (crossing >= 2) {
        color *= 0.15;
        alpha *= 0.15;
    }

    return vec4(color, alpha);
}

vec3 starField(vec3 rd) {
    float u = atan(rd.z, rd.x) / TAU + 0.5;
    float v = asin(clamp(rd.y, -0.999, 0.999)) / PI + 0.5;
    vec3 col = vec3(0.0);

    vec2 cellA = floor(vec2(u, v) * 56.0);
    vec2 fA = fract(vec2(u, v) * 56.0);
    vec2 rA = vec2(hash(cellA), hash(cellA + 127.1));
    float dA = length(fA - rA);
    col += mix(vec3(1.0, 0.62, 0.34), vec3(0.55, 0.75, 1.0), rA.y) * pow(rA.x, 10.0) * exp(-dA * dA * 500.0) * 1.5;

    vec2 cellB = floor(vec2(u, v) * 170.0);
    vec2 fB = fract(vec2(u, v) * 170.0);
    vec2 rB = vec2(hash(cellB + 43.0), hash(cellB + 91.0));
    float dB = length(fB - rB);
    col += vec3(0.85, 0.88, 1.0) * pow(rB.x, 18.0) * exp(-dB * dB * 1000.0) * 0.9;

    return col;
}

vec3 sampleSpace(vec3 rd) {
    float u = atan(rd.z, rd.x) / TAU + 0.5;
    float v = asin(clamp(rd.y, -0.999, 0.999)) / PI + 0.5;
    vec2 uv = vec2(fract(u), clamp(v, 0.002, 0.998));
    ivec2 samplerSize = textureSize(Sampler0, 0);
    vec2 texel = 1.0 / vec2(float(samplerSize.x), float(samplerSize.y));
    vec3 sharpSpace = texture(Sampler0, uv).rgb;
    vec3 softSpace = sharpSpace * 0.52;
    softSpace += texture(Sampler0, uv + texel * vec2(1.0, 0.0)).rgb * 0.12;
    softSpace += texture(Sampler0, uv - texel * vec2(1.0, 0.0)).rgb * 0.12;
    softSpace += texture(Sampler0, uv + texel * vec2(0.0, 1.0)).rgb * 0.12;
    softSpace += texture(Sampler0, uv - texel * vec2(0.0, 1.0)).rgb * 0.12;
    vec3 space = mix(sharpSpace, softSpace, 0.22);

    float gray = dot(space, vec3(0.30, 0.59, 0.11));
    space = mix(vec3(gray), space, 0.72);
    space = pow(max(space, vec3(0.0)), vec3(0.92)) * 0.88;

    float haze = fbm(vec2(u * 3.7, v * 2.6) + vec2(0.17, 0.43));
    vec3 nebula = vec3(0.018, 0.023, 0.035) * (0.30 + haze * 0.50);
    return nebula + space + starField(rd) * 0.11;
}

vec3 toneMap(vec3 color) {
    color *= 1.05;
    vec3 a = color * (color + 0.0245786) - 0.000090537;
    vec3 b = color * (0.983729 * color + 0.4329510) + 0.238081;
    color = a / b;
    return pow(max(color, vec3(0.0)), vec3(0.92));
}

void main() {
    float worldToModel = DISK_OUT / max(BlackHoleAndDiskRadius, 0.001);
    float effectRadius = DistortionRadius * worldToModel;
    float billboardRadius = DistortionRadius / max(BlackHoleAndDiskRadius, 0.001);
    float billboardRatio = length(localPosition) / max(billboardRadius, 0.001);
    float billboardMask = getBillboardMask(billboardRatio);
    if (billboardMask < 0.0005) {
        discard;
    }

    vec3 cameraOffset = CameraPosition - Center;
    vec3 eye = vec3(
        dot(cameraOffset, ViewPlaneRight),
        dot(cameraOffset, ViewPlaneUp),
        dot(cameraOffset, ViewPlaneForward)
    ) * worldToModel;

    vec3 rayPlanePoint = vec3(
        dot(rayPlaneOffset, ViewPlaneRight),
        dot(rayPlaneOffset, ViewPlaneUp),
        dot(rayPlaneOffset, ViewPlaneForward)
    ) * worldToModel;
    vec3 sphereRay = normalize(rayPlanePoint - eye);
    float sphereB = dot(eye, sphereRay);
    float sphereC = dot(eye, eye) - effectRadius * effectRadius;
    float sphereH = sphereB * sphereB - sphereC;
    if (sphereH < 0.0) {
        discard;
    }

    float sphereRoot = sqrt(sphereH);
    float sphereT = -sphereB - sphereRoot;
    if (sphereT < 0.0) {
        sphereT = -sphereB + sphereRoot;
    }
    if (sphereT < 0.0) {
        discard;
    }

    float impact = sqrt(max(dot(eye, eye) - sphereB * sphereB, 0.0));
    float edgeRatio = max(impact / max(effectRadius, 0.001), billboardRatio);
    float edgeFade = 1.0 - smoothstep(EFFECT_EDGE_FADE_START, 1.0, edgeRatio);
    edgeFade = edgeFade * edgeFade * (3.0 - 2.0 * edgeFade);
    float spaceEdgeFade = 1.0 - smootherStep(SPACE_EDGE_FADE_START, SPACE_EDGE_FADE_END, edgeRatio);
    vec3 spherePoint = eye + sphereRay * sphereT;
    vec3 spaceDirection = normalize(sphereRay);
    vec3 traceEye = eye;
    float eyeR = length(traceEye);
    if (eyeR < 4.5) {
        traceEye = eyeR < 0.001 ? vec3(0.0, 0.35, 4.5) : traceEye * (4.5 / eyeR);
    } else if (eyeR > TRACE_CAMERA_RADIUS) {
        traceEye *= TRACE_CAMERA_RADIUS / eyeR;
    }

    float diskAngle = (DiskTilt - 0.5) * 0.48;
    float diskCos = cos(diskAngle);
    float diskSin = sin(diskAngle);
    mat2 diskRotation = mat2(diskCos, diskSin, -diskSin, diskCos);
    mat2 inverseDiskRotation = mat2(diskCos, -diskSin, diskSin, diskCos);
    vec3 traceTarget = spherePoint;
    traceTarget.yz = diskRotation * traceTarget.yz;
    traceEye.yz = diskRotation * traceEye.yz;

    vec3 fwd = normalize(-traceEye);
    vec3 right = cross(fwd, vec3(0.0, 1.0, 0.0));
    if (dot(right, right) < 0.0001) {
        right = vec3(1.0, 0.0, 0.0);
    } else {
        right = normalize(right);
    }
    vec3 up = normalize(cross(right, fwd));
    vec3 rd = normalize(traceTarget - traceEye);
    float rayForward = max(dot(rd, fwd), 0.001);
    vec2 uv = vec2(dot(rd, right), dot(rd, up)) / rayForward;

    vec3 pos = traceEye;
    vec3 vel = rd;
    vec3 angularMomentum = cross(pos, vel);
    float l2 = dot(angularMomentum, angularMomentum);
    float gravCoeff = -1.5 * RS * l2;
    float minR = 1000.0;
    bool absorbed = false;
    int crossings = 0;
    vec4 diskAccum = vec4(0.0);
    vec3 glow = vec3(0.0);

    for (int i = 0; i < TRACE_STEPS; i++) {
        float r = length(pos);
        float h = 0.16 * clamp(r - 0.4 * RS, 0.06, 3.5);
        float invR2 = 1.0 / (r * r);
        float invR5 = invR2 * invR2 / r;
        vec3 acc = (gravCoeff * invR5) * pos;
        vec3 nextPos = pos + vel * h + 0.5 * acc * h * h;
        float nextR = length(nextPos);
        float invNextR2 = 1.0 / (nextR * nextR);
        float invNextR5 = invNextR2 * invNextR2 / nextR;
        vec3 nextAcc = (gravCoeff * invNextR5) * nextPos;
        vec3 nextVel = vel + 0.5 * (acc + nextAcc) * h;

        minR = min(minR, nextR);

        if (pos.y * nextPos.y < 0.0 && diskAccum.a < 0.97) {
            float t = pos.y / (pos.y - nextPos.y);
            vec3 hit = mix(pos, nextPos, t);
            vec4 disk = shadeDisk(hit, vel, Time * 0.055, crossings);
            diskAccum.rgb += disk.rgb * disk.a * (1.0 - diskAccum.a);
            diskAccum.a += disk.a * (1.0 - diskAccum.a);
            float bright = dot(disk.rgb, vec3(0.30, 0.50, 0.20)) * disk.a;
            glow += disk.rgb * 0.025 * max(bright - 0.22, 0.0);
            crossings++;
        }

        if (nextR < 6.0) {
            float photonDist = abs(nextR - 1.5 * RS);
            glow += vec3(0.75, 0.48, 0.22) * (1.0 / (1.0 + photonDist * photonDist * 22.0)) * h * 0.0009 / max(nextR * nextR, 0.2);
            glow += vec3(0.55, 0.24, 0.08) * max(exp(-(nextR - RS) * 3.5) * h * 0.002, 0.0);
        }

        if (nextR < RS * 0.35) {
            absorbed = true;
            break;
        }
        if (nextR > 25.0 && nextR > r) {
            break;
        }
        if (nextR > 55.0) {
            break;
        }

        pos = nextPos;
        vel = nextVel;
    }

    float photonLens = exp(-abs(minR - 1.5 * RS) * 5.0);
    vec2 finalUv = uv;
    if (!absorbed) {
        vec3 escaped = normalize(vel);
        float forward = max(dot(escaped, fwd), 0.001);
        finalUv = vec2(dot(escaped, right), dot(escaped, up)) / forward;
    }
    vec3 escapedDir = normalize(vel);
    if (absorbed) {
        escapedDir = normalize(rd);
    }
    vec3 escapedSpaceDir = escapedDir;
    escapedSpaceDir.yz = inverseDiskRotation * escapedSpaceDir.yz;
    if (absorbed) {
        escapedSpaceDir = normalize(spherePoint);
    }
    float lensStrength = length(finalUv - uv);
    float lensWarp = smoothstep(0.001, 0.045, lensStrength);
    vec3 baseSpace = sampleSpace(spaceDirection);
    vec3 warpedSpace = sampleSpace(normalize(escapedSpaceDir));
    float nearLens = (1.0 - smoothstep(2.4, 7.2, minR)) * edgeFade;
    float lensAlpha = clamp(nearLens * 0.30 + photonLens * 0.18, 0.0, 0.42) * edgeFade;
    float spaceShellAlpha = SPACE_SHELL_ALPHA * spaceEdgeFade;
    float warpMix = clamp(lensWarp * 1.15, 0.0, 1.0) * edgeFade;
    vec3 replacementColor = mix(baseSpace, warpedSpace, warpMix);
    replacementColor += starField(escapedSpaceDir) * lensAlpha * 0.18;

    float ringDist = abs(minR - 1.5 * RS);
    float photonRing = exp(-ringDist * ringDist * 34.0) * edgeFade;
    vec3 ringColor = vec3(1.0, 0.72, 0.28) * photonRing * 0.22;

    float shadowAlpha = absorbed ? 1.0 : 0.0;

    float diskAlpha = diskAccum.a * edgeFade;
    vec3 diskLight = diskAccum.rgb * edgeFade;
    float localSpaceAlpha = clamp(max(spaceShellAlpha, lensAlpha + photonRing * 0.20), 0.0, 0.72);
    float backgroundAlpha = max(localSpaceAlpha, shadowAlpha);
    float emissionAlpha = clamp(max(diskAlpha, photonRing * 0.42), 0.0, 0.98);
    float coverage = backgroundAlpha + emissionAlpha * (1.0 - backgroundAlpha);
    if (coverage < 0.0005) {
        discard;
    }

    vec3 emission = toneMap(diskLight + glow * edgeFade + ringColor);
    vec3 backgroundColor = mix(replacementColor, vec3(0.0), shadowAlpha);
    vec3 premultipliedColor = backgroundColor * backgroundAlpha * (1.0 - emissionAlpha) + emission * emissionAlpha;
    premultipliedColor = mix(
        premultipliedColor,
        backgroundColor * backgroundAlpha * (1.0 - emissionAlpha) + emission,
        shadowAlpha
    );
    premultipliedColor *= billboardMask;
    coverage *= billboardMask;
    if (coverage < 0.0005) {
        discard;
    }

    fragColor = vec4(clamp(premultipliedColor, 0.0, 1.0), clamp(coverage, 0.0, 1.0));
}
