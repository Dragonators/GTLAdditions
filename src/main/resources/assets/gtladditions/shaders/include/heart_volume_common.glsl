const float DISTORTION_RADIUS_SCALE = 1.13;
const float HEARTBEAT_FREQUENCY = 0.14;
const float SPACE_HEARTBEAT_RADIUS_AMPLITUDE = 0.045;

float getHeartbeatPulse(float time) {
    float phase = sin(time * HEARTBEAT_FREQUENCY);
    return phase * phase;
}

float getDistortionRadius(float time, float blackHoleAndDiskRadius) {
    return max(blackHoleAndDiskRadius, 0.001) *
        DISTORTION_RADIUS_SCALE *
        (1.0 + getHeartbeatPulse(time) * SPACE_HEARTBEAT_RADIUS_AMPLITUDE);
}

float getDistortionRadius(float time) {
    return getDistortionRadius(time, BlackHoleAndDiskRadius);
}

float getVolumeProxyRadius(float time, float blackHoleAndDiskRadius, float spaceFadeRadius) {
    float radius = max(blackHoleAndDiskRadius, 0.001);
    return max(
        getDistortionRadius(time, radius),
        radius * max(spaceFadeRadius, 0.001)
    );
}

float getVolumeProxyRadius(float time) {
    return getVolumeProxyRadius(time, BlackHoleAndDiskRadius, SpaceFadeRadius);
}

bool intersectSphere(
    vec3 rayOrigin,
    vec3 rayDirection,
    vec3 sphereCenter,
    float sphereRadius,
    out float entryT,
    out float exitT
) {
    vec3 offset = rayOrigin - sphereCenter;
    float b = dot(offset, rayDirection);
    float c = dot(offset, offset) - sphereRadius * sphereRadius;
    float h = b * b - c;
    if (h < 0.0) {
        return false;
    }
    h = sqrt(h);
    entryT = -b - h;
    exitT = -b + h;
    return exitT > 0.0;
}

float smootherStep(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / max(edge1 - edge0, 0.0001), 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

float inverseCieLightness(float lightness) {
    float l = clamp(lightness, 0.0, 1.0) * 100.0;
    float fy = (l + 16.0) / 116.0;
    return (l > 8.0) ? fy * fy * fy : l / (24389.0 / 27.0);
}

float perceptualSpaceAlpha(float solidRadius, float fadeRadius, float closestRadius) {
    float perceivedTransparency = smootherStep(solidRadius, fadeRadius, closestRadius);
    float linearTransmittance = inverseCieLightness(perceivedTransparency);
    return 1.0 - linearTransmittance;
}

float getSpaceVolumeAlpha(
    vec3 rayOrigin,
    vec3 rayDirection,
    vec3 center,
    float entryT,
    float exitT,
    float solidRadius,
    float fadeRadius
) {
    float segmentStart = max(entryT, 0.0);
    float segmentEnd = max(exitT, segmentStart);
    vec3 rayToCenter = center - rayOrigin;
    float closestT = clamp(dot(rayToCenter, rayDirection), segmentStart, segmentEnd);
    vec3 closestPoint = rayOrigin + rayDirection * closestT;
    float closestRadius = length(closestPoint - center);
    return perceptualSpaceAlpha(solidRadius, fadeRadius, closestRadius);
}
