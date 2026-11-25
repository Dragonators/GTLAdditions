#version 150

// CPU-reduced sky angle and 12-second material flow phase.
uniform vec2 AnimationPhase;
uniform vec3 Center;
uniform vec3 CameraPosition;
uniform vec3 FacingAxis;
uniform float BlackHoleAndDiskRadius;
uniform float SpaceSolidRadius;
uniform float SpaceFadeRadius;
uniform vec2 BlackHoleRotation;
uniform float PassMode;
uniform float VolumeProxyRadius;
uniform sampler2D RayDeflectionSampler;
uniform sampler2D RayInverseRadiusSampler;
uniform sampler2D DiscNoiseSampler;
uniform sampler2D GalaxySampler;
uniform sampler2D MaterialSampler;
uniform vec2 MaterialUvScale;
uniform vec2 MaterialPixelScale;

#moj_import <gtladditions:heart_volume_common.glsl>
#moj_import <gtladditions:heart_black_hole_lut.glsl>
#moj_import <gtladditions:heart_accretion_volume.glsl>
#moj_import <gtladditions:heart_scene_depth.glsl>

in vec3 volumeSurfacePosition;
out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float DISK_IN = 2.2;
const float DISK_OUT = 14.0;
const float SMOOTH_TICK_SECONDS = 0.05;
const float PROJECTED_SHADOW_RADIUS = 0.07464;
const float PROJECTED_SHADOW_CONTACT_RADIUS = 0.132;
const float PROJECTED_DISK_RADIUS = 0.457;
const float SHADOW_CONTACT_RING_WIDTH = 0.012;
const float GALAXY_RADIANCE_SCALE = 6.78494e-5;
const float GALAXY_EXPOSURE = 0.12;
// Scale both tangent directions about the Galactic centre (+X).
const float GALAXY_SCALE = 1.0 / 2.4;
// Viewed from the machine's front, the galactic band leans 15 degrees from vertical.
const float GALAXY_ROLL = 1.30899694;

vec3 getModelForward() {
    vec3 forward = vec3(FacingAxis.x, 0.0, FacingAxis.z);
    return dot(forward, forward) < 0.0001 ? vec3(0.0, 0.0, 1.0) : normalize(forward);
}

vec3 toModelSpace(vec3 vector, vec3 right, vec3 up, vec3 forward) {
    return vec3(dot(vector, right), dot(vector, up), dot(vector, forward));
}

vec3 fromModelSpace(vec3 vector, vec3 right, vec3 up, vec3 forward) {
    return right * vector.x + up * vector.y + forward * vector.z;
}

vec3 rotateAroundAxis(vec3 vector, vec3 axis, float angle) {
    float c = cos(angle), s = sin(angle);
    return vector * c + cross(axis, vector) * s + axis * dot(axis, vector) * (1.0 - c);
}

vec2 sphericalUv(vec3 direction) {
    return vec2(atan(direction.z, direction.x) / TAU + 0.5,
                asin(clamp(direction.y, -0.999999, 0.999999)) / PI + 0.5);
}

vec3 scaledGalaxyDirection(vec3 direction, out float footprintScale) {
    // Uniform stereographic scaling preserves local cloud shapes on the sphere.
    // This closed form stays finite at the opposite pole without a seam or tiling.
    float scaleSquared = GALAXY_SCALE * GALAXY_SCALE;
    float axial = clamp(direction.x, -1.0, 1.0);
    float denominator = (1.0 - axial) + scaleSquared * (1.0 + axial);
    footprintScale = 2.0 * GALAXY_SCALE / denominator;
    float scaledAxial = (scaleSquared * (1.0 + axial) - (1.0 - axial)) / denominator;
    return vec3(scaledAxial, direction.yz * footprintScale);
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 starField(vec3 direction, float footprint) {
    vec3 sky = rotateAroundAxis(direction, vec3(0.0, 0.0, 1.0), GALAXY_ROLL);
    sky = rotateAroundAxis(sky, vec3(1.0, 0.0, 0.0), 0.22);
    // Drift around galactic north after setting the plane, so the band stays upright.
    sky = rotateAroundAxis(sky, vec3(0.0, 1.0, 0.0), -1.2 + AnimationPhase.x);
    float galaxyFootprintScale;
    vec3 galaxySky = scaledGalaxyDirection(sky, galaxyFootprintScale);
    float latitudeScale = max(sqrt(max(1.0 - galaxySky.y * galaxySky.y, 0.0)), 0.02);
    float galaxyLod = clamp(log2(max(
        footprint * galaxyFootprintScale * 2048.0 / (TAU * latitudeScale), 1.0)), 0.0, 10.0);
    vec3 color = textureLod(GalaxySampler, sphericalUv(galaxySky), galaxyLod).rgb *
        GALAXY_RADIANCE_SCALE * GALAXY_EXPOSURE;

    vec2 cells = sphericalUv(sky) * vec2(210.0, 105.0);
    vec2 cell = floor(cells);
    vec2 center = vec2(hash(cell), hash(cell + 127.1));
    float starRadius = length(fract(cells) - center);
    float visibility = 1.0 - smoothstep(0.003, 0.025, footprint);
    color += mix(vec3(1.0, 0.72, 0.43), vec3(0.65, 0.8, 1.0), center.y) *
        pow(center.x, 16.0) * exp(-starRadius * starRadius * 850.0) * visibility * 1.6;
    return color;
}

vec4 traceSceneMaterial(vec3 worldRay, vec3 modelRay, vec3 right, vec3 up, vec3 forward,
                       float discRadius, bool captured, float footprint, float sceneT) {
    float entryT, exitT;
    if (!intersectSphere(CameraPosition, worldRay, Center, discRadius, entryT, exitT) || sceneT <= max(entryT, 0.0))
        return vec4(0.0);
    vec3 origin = toModelSpace(CameraPosition + worldRay * max(entryT, 0.0) - Center, right, up, forward) / discRadius;
    // The reference volume's disc is in XY; this renderer's disc is in XZ.
    origin = vec3(origin.x, -origin.z, origin.y);
    vec3 direction = vec3(modelRay.x, -modelRay.z, modelRay.y);
    float sceneLimit = sceneT >= 1e19 ? 1e20 : (sceneT - max(entryT, 0.0)) / discRadius;
    HeartAccretionVolume volume = traceHeartAccretionVolume(origin, direction, AnimationPhase.y, captured, footprint, sceneLimit);
    return vec4(volume.color, volume.opacity);
}

void main() {
    float discRadius = max(BlackHoleAndDiskRadius, 0.001);
    float solidRadius = discRadius * max(SpaceSolidRadius, 0.0);
    float fadeRadius = discRadius * max(SpaceFadeRadius, SpaceSolidRadius + 0.001);
    float volumeRadius = max(VolumeProxyRadius, 0.001);
    vec3 baseForward = getModelForward();
    vec3 baseRight = normalize(cross(vec3(0.0, 1.0, 0.0), baseForward));
    vec3 baseUp = vec3(0.0, 1.0, 0.0);
    // y yaws the tilt axis; x tilts the disc.
    // In-plane axes follow the projected machine front; spinning them about the normal would duplicate Keplerian flow.
    // Tilt stays within 30 degrees, so the front never aligns with the normal.
    vec3 tiltAxis = normalize(cross(baseUp, rotateAroundAxis(baseForward, baseUp, BlackHoleRotation.y)));
    vec3 up = normalize(rotateAroundAxis(baseUp, tiltAxis, BlackHoleRotation.x));
    vec3 forward = normalize(baseForward - up * dot(up, baseForward));
    vec3 right = cross(up, forward);

    vec3 rayToSurface = volumeSurfacePosition - CameraPosition;
    float rayLengthSquared = dot(rayToSurface, rayToSurface);
    vec3 worldRay = rayLengthSquared > 0.000001
        ? rayToSurface * inversesqrt(rayLengthSquared) : vec3(0.0, 0.0, 1.0);
    float entryT, exitT;
    bool intersects = intersectSphere(CameraPosition, worldRay, Center, volumeRadius, entryT, exitT);
    vec2 screenUv = gl_FragCoord.xy / PassSize;
    float sceneDepth = heartSceneDepthAt(screenUv);
    float sceneT = heartSceneDistance(screenUv, sceneDepth);
    float effectFade = getSpaceVolumeAlpha(CameraPosition, worldRay, Center, entryT, min(exitT, sceneT), solidRadius, fadeRadius);
    bool visible = intersects && sceneT > max(entryT, 0.0) && effectFade >= 0.0005 && rayLengthSquared > 0.000001;

    vec3 traceEntry = CameraPosition + worldRay * max(entryT, 0.0) - Center;
    vec3 traceEye = toModelSpace(traceEntry, right, up, forward) * (DISK_OUT / discRadius);
    vec3 modelRay = normalize(toModelSpace(worldRay, right, up, forward));
    HeartRayTrace trace = traceHeartRay(traceEye, modelRay, DISK_IN, DISK_OUT);
    // Derivative-dependent sky filtering must run before any lane leaves its pixel quad.
    float skyFootprint = max(length(dFdx(trace.escapedDirection)), length(dFdy(trace.escapedDirection)));
    // The material target has fewer pixels; retain the full-resolution noise footprint.
    vec2 pixelScale = PassMode > 1.5 ? MaterialPixelScale : vec2(1.0);
    float materialFootprint = max(length(dFdx(modelRay)) * pixelScale.x, length(dFdy(modelRay)) * pixelScale.y) *
        max(length(CameraPosition - Center) / discRadius, 1.0);
    if (!visible) discard;

    // Geometry within the outer shell needs physical core coverage, even when
    // it lies beyond the smaller material sphere. Beyond the shell, retain the
    // complete lensed background appearance.
    float materialSceneT = sceneT < exitT ? sceneT : 1e20;
    if (PassMode > 1.5) {
        fragColor = traceSceneMaterial(worldRay, modelRay, right, up, forward, discRadius, trace.absorbed, materialFootprint, materialSceneT);
        return;
    }
    // Keep the material pass uniform. The full-resolution branch only repairs a depth
    // discontinuity; its explicit LOD remains defined when neighbouring lanes diverge.
    vec4 material = textureLod(MaterialSampler, gl_FragCoord.xy * MaterialUvScale, 0.0);
    if (heartMaterialDepthEdge(screenUv, sceneDepth, sceneT, exitT))
        material = traceSceneMaterial(worldRay, modelRay, right, up, forward, discRadius, trace.absorbed, materialFootprint, materialSceneT);

    vec3 escapedWorld = fromModelSpace(trace.escapedDirection, right, up, forward);
    vec3 backgroundDirection = normalize(toModelSpace(escapedWorld, baseRight, baseUp, baseForward));
    vec3 background = trace.absorbed ? vec3(0.0) : starField(backgroundDirection, skyFootprint);
    vec3 discEmission = material.rgb;

    float closestT = clamp(dot(Center - CameraPosition, worldRay), max(entryT, 0.0), max(exitT, max(entryT, 0.0)));
    // The sky image belongs to the far shell. The closest point to the centre is
    // only an impact parameter: inside the sphere it can lie at the eye and would
    // put an opaque starfield in front of nearby blocks when looking sideways.
    float backgroundVisibility = sceneT >= 1e19 ? 1.0 : 1.0 - smoothstep(sceneT - 0.05, sceneT, exitT);
    vec3 color = discEmission + background * (1.0 - material.a) * backgroundVisibility;
    float impact = length(CameraPosition + worldRay * closestT - Center) / discRadius * PROJECTED_DISK_RADIUS;
    float rimRadius = max(PROJECTED_SHADOW_CONTACT_RADIUS, PROJECTED_SHADOW_RADIUS);
    float rimDistance = impact - rimRadius;
    float rim = exp(-rimDistance * rimDistance / (SHADOW_CONTACT_RING_WIDTH * SHADOW_CONTACT_RING_WIDTH));
    color += vec3(1.0, 0.72, 0.28) * rim * 0.045 * backgroundVisibility;
    // Keep emission above 1.0 until bloom and tone mapping in the final composite.
    float coverage = backgroundVisibility + material.a * (1.0 - backgroundVisibility);
    fragColor = vec4(max(color, vec3(0.0)) * effectFade, coverage * effectFade);
}
