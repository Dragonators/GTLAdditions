#version 150

uniform float Time;
uniform vec3 Center;
uniform vec3 CameraPosition;
uniform vec3 FacingAxis;
uniform float BlackHoleAndDiskRadius;
uniform float SpaceSolidRadius;
uniform float SpaceFadeRadius;
uniform float RotationSpeed;
uniform vec3 BlackHoleRotation;
uniform float PassMode;

#moj_import <gtladditions:heart_volume_common.glsl>

in vec3 volumeSurfacePosition;
in vec3 volumeModelPosition;

out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float RS = 1.0;
const float ISCO = 3.0;
const float DISK_IN = 2.2;
const float DISK_OUT = 14.0;
const float SMOOTH_TICK_SECONDS = 0.05;
const float LENS_ALPHA_SCALE = 1.0;
const float BACKGROUND_ALPHA_SCALE = 1.0;
const float SHADOW_CORE_RADIUS_SCALE = 1.0;
const float EMISSION_ALPHA_VISIBILITY = 0.0;
const vec3 HOT_COLOR = vec3(1.0, 0.83, 0.42);
const vec3 COOL_COLOR = vec3(0.9, 0.24, 0.06);
const float HEART_INTENSITY = 1.35;
// Radiant-projected ratio: event horizon / full accretion disk is PROJECTED_SHADOW_RADIUS / PROJECTED_DISK_RADIUS.
const float PROJECTED_SHADOW_RADIUS = 0.07464;
const float PROJECTED_SHADOW_CONTACT_RADIUS = 0.132;
const float PROJECTED_DISK_RADIUS = 0.457;
const float SHADOW_CONTACT_RING_WIDTH = 0.012;
const int TRACE_STEPS = 200;

vec3 getModelForward() {
    vec3 forward = vec3(FacingAxis.x, 0.0, FacingAxis.z);
    if (dot(forward, forward) < 0.0001) {
        return vec3(0.0, 0.0, 1.0);
    }
    return normalize(forward);
}

vec3 getModelRight(vec3 forward) {
    return normalize(cross(vec3(0.0, 1.0, 0.0), forward));
}

float getSpin(vec3 forward) {
    return dot(forward, vec3(1.0, 0.0, 1.0)) < 0.0 ? -1.0 : 1.0;
}

vec3 toModelSpace(vec3 worldVector, vec3 modelRight, vec3 modelUp, vec3 modelForward) {
    return vec3(
        dot(worldVector, modelRight),
        dot(worldVector, modelUp),
        dot(worldVector, modelForward)
    );
}

vec3 fromModelSpace(vec3 modelVector, vec3 modelRight, vec3 modelUp, vec3 modelForward) {
    return modelRight * modelVector.x + modelUp * modelVector.y + modelForward * modelVector.z;
}

vec3 rotateAroundAxis(vec3 vector, vec3 axis, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vector * c + cross(axis, vector) * s + axis * dot(axis, vector) * (1.0 - c);
}

void rotateBasis(inout vec3 modelRight, inout vec3 modelUp, inout vec3 modelForward, vec3 axis, float angle) {
    vec3 normalizedAxis = normalize(axis);
    modelRight = rotateAroundAxis(modelRight, normalizedAxis, angle);
    modelUp = rotateAroundAxis(modelUp, normalizedAxis, angle);
    modelForward = rotateAroundAxis(modelForward, normalizedAxis, angle);
}

vec2 sphericalUv(vec3 direction) {
    vec3 rd = normalize(direction);
    return vec2(
        atan(rd.z, rd.x) / TAU + 0.5,
        asin(clamp(rd.y, -0.999, 0.999)) / PI + 0.5
    );
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

float fbmLite(vec2 p) {
    float v = 0.5 * gNoise(p);
    p = mat2(0.866, 0.5, -0.5, 0.866) * p * 2.03 + vec2(47.0, 13.0);
    v += 0.25 * gNoise(p);
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

vec4 shadeDisk(vec3 hit, vec3 vel, float time, int crossing, float spin) {
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
    float doppler = 1.0 + 2.0 * dot(normalize(vel), orbDir) * orbSpeed * spin;
    doppler = max(0.15, doppler);
    float dopplerBoost = doppler * doppler * doppler;

    float intensity = temp * texture * 6.0;
    float innerFade = smoothstep(DISK_IN * 0.70, DISK_IN * 1.18, r);
    float iscoFade = 0.34 + 0.66 * smoothstep(ISCO * 0.85, ISCO * 1.22, r);
    float outerFade = 1.0 - smoothstep(DISK_OUT * 0.56, DISK_OUT, r);
    intensity *= innerFade * iscoFade * outerFade;

    float colorTemp = temp * pow(doppler, 1.8) * 1.2;
    vec3 color = blackbody(colorTemp);
    color = mix(color, mix(COOL_COLOR, HOT_COLOR, clamp(colorTemp, 0.0, 1.0)), 0.08);
    color *= intensity * dopplerBoost * HEART_INTENSITY;

    float alpha = clamp(intensity * 1.3, 0.0, 0.96);
    if (crossing >= 2) {
        color *= 0.15;
        alpha *= 0.15;
    }

    return vec4(color, alpha);
}

vec3 flowStarDirection(vec3 rd, float seconds, vec3 axis, float speed, float sway, float swaySpeed) {
    float angle = seconds * speed + sin(seconds * swaySpeed) * sway;
    return rotateAroundAxis(rd, normalize(axis), angle);
}

vec3 starField(vec3 rd, float time) {
    float seconds = time * SMOOTH_TICK_SECONDS;
    vec3 col = vec3(0.0);

    vec2 uvA = sphericalUv(flowStarDirection(rd, seconds, vec3(0.35, 0.91, 0.22), 0.010, 0.045, 0.017));
    vec2 cellA = floor(uvA * 56.0);
    vec2 fA = fract(uvA * 56.0);
    vec2 rA = vec2(hash(cellA), hash(cellA + 127.1));
    float dA = length(fA - rA);
    col += mix(vec3(1.0, 0.62, 0.34), vec3(0.55, 0.75, 1.0), rA.y) * pow(rA.x, 10.0) * exp(-dA * dA * 500.0) * 1.5;

    vec2 uvB = sphericalUv(flowStarDirection(rd, seconds, vec3(-0.62, 0.19, 0.76), -0.016, 0.030, 0.023));
    vec2 cellB = floor(uvB * 170.0);
    vec2 fB = fract(uvB * 170.0);
    vec2 rB = vec2(hash(cellB + 43.0), hash(cellB + 91.0));
    float dB = length(fB - rB);
    col += vec3(0.85, 0.88, 1.0) * pow(rB.x, 18.0) * exp(-dB * dB * 1000.0) * 0.9;

    vec2 uvNebula = sphericalUv(flowStarDirection(rd, seconds, vec3(0.18, 0.53, -0.83), 0.006, 0.070, 0.011));
    vec2 nebulaFlow = vec2(seconds * 0.012, sin(seconds * 0.019) * 0.18);
    float nebula = fbmLite(uvNebula * 3.0 + nebulaFlow) *
        fbmLite(uvNebula * 5.5 + 10.0 - nebulaFlow.yx * 0.7);
    col += vec3(0.10, 0.04, 0.14) * pow(nebula, 3.0);

    return col;
}

vec3 toneMap(vec3 color) {
    color *= 1.05;
    vec3 a = color * (color + 0.0245786) - 0.000090537;
    vec3 b = color * (0.983729 * color + 0.4329510) + 0.238081;
    color = a / b;
    return pow(max(color, vec3(0.0)), vec3(0.92));
}

void main() {
    float blackHoleAndDiskRadius = max(BlackHoleAndDiskRadius, 0.001);
    float spaceSolidRadius = max(SpaceSolidRadius, 0.0);
    float spaceFadeRadius = max(SpaceFadeRadius, spaceSolidRadius + 0.001);
    float diskAnimationTime = Time * SMOOTH_TICK_SECONDS * RotationSpeed;
    float volumeRadius = getVolumeProxyRadius(Time, blackHoleAndDiskRadius, spaceFadeRadius);
    float fadeRadius = blackHoleAndDiskRadius * spaceFadeRadius;
    vec3 baseModelForward = getModelForward();
    vec3 baseModelRight = getModelRight(baseModelForward);
    vec3 baseModelUp = vec3(0.0, 1.0, 0.0);
    vec3 modelForward = rotateAroundAxis(baseModelForward, baseModelUp, BlackHoleRotation.y);
    vec3 modelRight = normalize(cross(baseModelUp, modelForward));
    vec3 modelUp = baseModelUp;
    vec3 pitchAxis = modelRight;
    rotateBasis(modelRight, modelUp, modelForward, pitchAxis, BlackHoleRotation.x);
    vec3 rollAxis = modelUp;
    rotateBasis(modelRight, modelUp, modelForward, rollAxis, BlackHoleRotation.z);
    modelRight = normalize(modelRight);
    modelUp = normalize(modelUp);
    modelForward = normalize(modelForward);
    float spin = getSpin(baseModelForward);
    float solidRadius = blackHoleAndDiskRadius * spaceSolidRadius;

    vec3 rayToSurface = volumeSurfacePosition - CameraPosition;
    if (dot(rayToSurface, rayToSurface) < 0.000001) {
        discard;
    }
    vec3 worldRay = normalize(rayToSurface);

    float entryT = 0.0;
    float exitT = 0.0;
    if (!intersectSphere(CameraPosition, worldRay, Center, volumeRadius, entryT, exitT)) {
        discard;
    }

    float traceEntryT = max(entryT, 0.0);
    float effectFade = getSpaceVolumeAlpha(
        CameraPosition,
        worldRay,
        Center,
        entryT,
        exitT,
        solidRadius,
        fadeRadius
    );
    if (effectFade < 0.0005) {
        discard;
    }
    if (PassMode > 0.5) {
        fragColor = vec4(effectFade, effectFade, effectFade, effectFade);
        return;
    }

    vec3 cameraToCenter = Center - CameraPosition;
    float closestT = clamp(dot(cameraToCenter, worldRay), traceEntryT, exitT);
    vec3 closestPoint = CameraPosition + worldRay * closestT;
    float impactDistance = length(closestPoint - Center);
    float impactRatio = impactDistance / blackHoleAndDiskRadius;
    float impactProjectedRadius = impactRatio * PROJECTED_DISK_RADIUS;
    float worldToModel = DISK_OUT / blackHoleAndDiskRadius;
    vec3 entryWorld = CameraPosition + worldRay * traceEntryT;
    vec3 traceEye = toModelSpace(entryWorld - Center, modelRight, modelUp, modelForward) * worldToModel;
    vec3 rd = normalize(toModelSpace(worldRay, modelRight, modelUp, modelForward));

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
            vec4 disk = shadeDisk(hit, vel, diskAnimationTime, crossings, spin);
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
    float nearLens = 1.0 - smoothstep(2.4, 7.2, minR);
    float lensAlpha = clamp(nearLens * 0.34 + photonLens * 0.18, 0.0, 0.58) * effectFade * LENS_ALPHA_SCALE;
    float spaceShellAlpha = effectFade;
    vec3 escapedDirection = normalize(vel);
    vec3 escapedWorldDirection = fromModelSpace(escapedDirection, modelRight, modelUp, modelForward);
    vec3 backgroundDirection = toModelSpace(
        escapedWorldDirection,
        baseModelRight,
        baseModelUp,
        baseModelForward
    );
    vec3 backgroundColor = absorbed ? vec3(0.0) : starField(normalize(backgroundDirection), Time);

    float shadowAlphaRaw = absorbed ? 1.0 : 0.0;
    float shadowCoreRadius = max(PROJECTED_SHADOW_CONTACT_RADIUS * SHADOW_CORE_RADIUS_SCALE, PROJECTED_SHADOW_RADIUS);
    float shadowCoreAlpha = 1.0 - smoothstep(shadowCoreRadius * 0.82, shadowCoreRadius, impactProjectedRadius);
    float shadowAlpha = max(shadowAlphaRaw, shadowCoreAlpha) * effectFade;

    float ringDist = abs(minR - 1.5 * RS);
    float tracedPhotonRing = exp(-ringDist * ringDist * 34.0);
    float contactRingDist = abs(impactProjectedRadius - shadowCoreRadius);
    float contactPhotonRing = exp(
        -(contactRingDist * contactRingDist) /
        max(SHADOW_CONTACT_RING_WIDTH * SHADOW_CONTACT_RING_WIDTH, 0.000001)
    );
    float photonRing = max(tracedPhotonRing, contactPhotonRing * 0.85) * effectFade;
    vec3 ringColor = vec3(1.0, 0.72, 0.28) * photonRing * 0.22;

    float diskAlpha = diskAccum.a * effectFade;
    vec3 diskLight = diskAccum.rgb * effectFade;
    float shellBackgroundAlpha = clamp(spaceShellAlpha * BACKGROUND_ALPHA_SCALE, 0.0, 1.0);
    float lensBackgroundAlpha = clamp(lensAlpha + photonRing * 0.20, 0.0, 0.72);
    float backgroundAlpha = max(max(shellBackgroundAlpha, lensBackgroundAlpha), shadowAlpha);
    float emissionAlpha = clamp(max(diskAlpha, photonRing * 0.42), 0.0, 0.98);
    vec3 emission = toneMap(diskLight + glow * effectFade + ringColor);
    float emissionBrightness = max(max(emission.r, emission.g), emission.b);
    emissionAlpha *= mix(
        1.0,
        smoothstep(0.025, 0.18, emissionBrightness),
        clamp(EMISSION_ALPHA_VISIBILITY, 0.0, 1.0)
    );
    float coverage = backgroundAlpha + emissionAlpha * (1.0 - backgroundAlpha);
    if (coverage < 0.0005) {
        discard;
    }

    vec3 premultipliedColor = backgroundColor * backgroundAlpha * (1.0 - emissionAlpha) + emission * emissionAlpha;
    premultipliedColor = mix(
        premultipliedColor,
        backgroundColor * backgroundAlpha * (1.0 - emissionAlpha) + emission,
        shadowAlpha
    );
    if (coverage < 0.0005) {
        discard;
    }

    fragColor = vec4(clamp(premultipliedColor, 0.0, 1.0), clamp(coverage, 0.0, 1.0));
}
