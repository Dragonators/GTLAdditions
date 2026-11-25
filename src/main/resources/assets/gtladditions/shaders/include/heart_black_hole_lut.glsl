/*
 * Adapted from Eric Bruneton's black_hole/functions.glsl and model.glsl at
 * e72b3f293409893a6fa25528b29572c96fc57f57.
 * Copyright (c) 2020 Eric Bruneton. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

const float HEART_RAY_PI = 3.14159265359;
const float HEART_CRITICAL_ENERGY_SQUARED = 4.0 / 27.0;

struct HeartDiscIntersection {
    vec3 position;
    vec3 direction;
    float coverage;
};

struct HeartRayTrace {
    bool absorbed;
    vec3 escapedDirection;
    float minimumRadius;
    HeartDiscIntersection nearDisc;
    HeartDiscIntersection farDisc;
};

vec2 heartLookupUv(vec2 coordinate, ivec2 size) {
    return (0.5 + clamp(coordinate, 0.0, 1.0) * (vec2(size) - 1.0)) / vec2(size);
}

float heartApsisInverseRadius(float energySquared) {
    float x = clamp(2.0 * energySquared / HEART_CRITICAL_ENERGY_SQUARED - 1.0, -1.0, 1.0);
    return 1.0 / 3.0 + 2.0 / 3.0 * sin(asin(x) / 3.0);
}

float heartDeflectionCoordinate(float energySquared) {
    float ratio = energySquared < HEART_CRITICAL_ENERGY_SQUARED
        ? energySquared / HEART_CRITICAL_ENERGY_SQUARED
        : HEART_CRITICAL_ENERGY_SQUARED / energySquared;
    // A critical ray maps to a texture edge. Keep its coordinate finite.
    float offset = sqrt(-log(max(1.0 - ratio, 1e-7)) / 50.0);
    return energySquared < HEART_CRITICAL_ENERGY_SQUARED ? 0.5 - offset : 0.5 + offset;
}

float heartDeflectionRadiusCoordinate(float energySquared, float inverseRadius) {
    if (energySquared >= HEART_CRITICAL_ENERGY_SQUARED) {
        float offset = sign(inverseRadius - 2.0 / 3.0) * sqrt(abs(inverseRadius - 2.0 / 3.0));
        return (sqrt(2.0 / 3.0) + offset) / (sqrt(2.0 / 3.0) + sqrt(1.0 / 3.0));
    }
    float apsis = max(heartApsisInverseRadius(energySquared), 1e-7);
    return 1.0 - sqrt(max(1.0 - inverseRadius / apsis, 0.0));
}

float heartLookupDeflection(float energySquared, float inverseRadius, out float apsisDeflection) {
    float column = heartDeflectionCoordinate(energySquared);
    ivec2 size = textureSize(RayDeflectionSampler, 0);
    apsisDeflection = textureLod(RayDeflectionSampler, heartLookupUv(vec2(column, 1.0), size), 0.0).r;
    float row = heartDeflectionRadiusCoordinate(energySquared, inverseRadius);
    return textureLod(RayDeflectionSampler, heartLookupUv(vec2(column, row), size), 0.0).r;
}

float heartLookupInverseRadius(float energySquared, float angle) {
    float angleUpperBound = (1.0 + energySquared) /
        (1.0 / 3.0 + 2.0 * energySquared * sqrt(energySquared));
    vec2 coordinate = vec2(1.0 / (1.0 + 6.0 * energySquared), angle / angleUpperBound);
    return textureLod(RayInverseRadiusSampler,
        heartLookupUv(coordinate, textureSize(RayInverseRadiusSampler, 0)), 0.0).r;
}

float heartFilteredDiscCoverage(float outerInverseRadius, float innerInverseRadius, float radius, float width) {
    width = max(width, 1e-6);
    float begin = radius - width * 0.5;
    return max(0.0, min(begin + width, innerInverseRadius) - max(begin, outerInverseRadius)) / width;
}

// Each intersection stores (inverse radius, angle in the ray plane, coverage).
// Travel-time channels are retained in the source tables; the existing art uses a shared animation clock.
float heartTraceIntersections(
    float inverseRadius, float radiusDerivative, float energySquared,
    float rayAngle, float discAngle, float innerInverseRadius, float outerInverseRadius,
    out vec3 nearIntersection, out vec3 farIntersection
) {
    nearIntersection = vec3(-1.0, 0.0, 0.0);
    farIntersection = vec3(-1.0, 0.0, 0.0);
    bool trapped = energySquared <= HEART_CRITICAL_ENERGY_SQUARED && inverseRadius > 2.0 / 3.0;

    float apsisDeflection;
    float deflectionAtOrigin = heartLookupDeflection(energySquared, inverseRadius, apsisDeflection);
    float deflection = deflectionAtOrigin;
    if (radiusDerivative > 0.0) {
        deflection = energySquared < HEART_CRITICAL_ENERGY_SQUARED
            ? 2.0 * apsisDeflection - deflectionAtOrigin : -1.0;
    }

    float directionSign = radiusDerivative > 0.0 ? 1.0 : -1.0;
    float angle = deflectionAtOrigin + (directionSign > 0.0 ? HEART_RAY_PI - rayAngle : rayAngle) + directionSign * discAngle;
    float apsisAngle = apsisDeflection + HEART_RAY_PI * 0.5;
    float nearAngle = mod(angle, HEART_RAY_PI);
    float nearRadius = heartLookupInverseRadius(energySquared, nearAngle);
    if (nearAngle < apsisAngle) {
        float side = directionSign * (nearRadius - inverseRadius);
        if (side > 1e-3 || (side > -1e-3 && discAngle < rayAngle)) {
            nearIntersection.xy = vec2(nearRadius, discAngle + angle - nearAngle);
        }
    }

    angle = 2.0 * apsisAngle - angle;
    float farAngle = mod(angle, HEART_RAY_PI);
    float farRadius = heartLookupInverseRadius(energySquared, farAngle);
    if (energySquared < HEART_CRITICAL_ENERGY_SQUARED && directionSign > 0.0 && farAngle < apsisAngle) {
        farIntersection.xy = vec2(farRadius, discAngle + angle - farAngle);
    }

    float nearWidth = min(fwidth(nearRadius), fwidth(nearIntersection.x < 0.0 ? farIntersection.x : nearIntersection.x));
    float farWidth = min(fwidth(farRadius), fwidth(farIntersection.x < 0.0 ? nearIntersection.x : farIntersection.x));
    float energyWidth = fwidth(energySquared);
    nearIntersection.z = heartFilteredDiscCoverage(outerInverseRadius, innerInverseRadius, nearIntersection.x, nearWidth);
    farIntersection.z = heartFilteredDiscCoverage(outerInverseRadius, innerInverseRadius, farIntersection.x, farWidth);
    if (directionSign > 0.0 && abs(energySquared - HEART_CRITICAL_ENERGY_SQUARED) < min(energyWidth, HEART_CRITICAL_ENERGY_SQUARED)) {
        float averageInverseRadius = 2.0 / (1.0 / innerInverseRadius + 1.0 / outerInverseRadius);
        if (nearIntersection.z < 0.99) nearIntersection.x = averageInverseRadius;
        if (farIntersection.z < 0.99) farIntersection.x = averageInverseRadius;
    }
    // Evaluate derivatives for the entire pixel quad before masking captured rays.
    if (trapped) {
        nearIntersection = vec3(-1.0, 0.0, 0.0);
        farIntersection = vec3(-1.0, 0.0, 0.0);
        return -1.0;
    }
    return deflection;
}

HeartDiscIntersection heartDiscIntersection(vec3 hit, vec3 radial, vec3 tangent, float energySquared, float directionSign) {
    if (hit.x <= 0.0 || hit.z <= 0.0) {
        return HeartDiscIntersection(vec3(0.0), tangent, 0.0);
    }
    vec3 hitRadial = radial * cos(hit.y) + tangent * sin(hit.y);
    vec3 hitTangent = -radial * sin(hit.y) + tangent * cos(hit.y);
    float derivative = directionSign * sqrt(max(energySquared - hit.x * hit.x * (1.0 - hit.x), 0.0));
    vec3 direction = normalize(-derivative * hitRadial + hit.x * hitTangent);
    return HeartDiscIntersection(hitRadial / hit.x, direction, hit.z);
}

HeartRayTrace traceHeartRay(vec3 origin, vec3 direction, float innerDiscRadius, float outerDiscRadius) {
    float radius = length(origin);
    HeartDiscIntersection emptyHit = HeartDiscIntersection(vec3(0.0), direction, 0.0);
    HeartRayTrace result = HeartRayTrace(false, direction, max(radius, 1.0), emptyHit, emptyHit);
    vec3 radial = radius > 1e-6 ? origin / radius : vec3(0.0, 0.0, 1.0);
    float radialCosine = clamp(dot(radial, direction), -1.0, 1.0);
    vec3 transverse = direction - radial * radialCosine;
    float transverseLength = length(transverse);
    vec3 fallbackAxis = abs(radial.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = transverseLength >= 1e-5
        ? transverse / transverseLength : normalize(cross(radial, fallbackAxis));
    vec3 planeNormal = cross(radial, tangent);
    vec3 discNode = cross(vec3(0.0, 1.0, 0.0), planeNormal);
    float nodeLength = length(discNode);
    discNode = nodeLength > 1e-6 ? discNode / nodeLength : radial;
    if (dot(discNode, tangent) < 0.0) discNode = -discNode;
    float discAngle = acos(clamp(dot(radial, discNode), -1.0, 1.0));
    float rayAngle = acos(radialCosine);
    float inverseRadius = 1.0 / max(radius, 1.0001);
    float radiusDerivative = -inverseRadius * radialCosine / max(transverseLength, 1e-5);
    float energySquared = radiusDerivative * radiusDerivative + inverseRadius * inverseRadius * (1.0 - inverseRadius);

    vec3 nearHit, farHit;
    float deflection = heartTraceIntersections(inverseRadius, radiusDerivative, energySquared,
        rayAngle, discAngle, 1.0 / innerDiscRadius, 1.0 / outerDiscRadius, nearHit, farHit);
    result.absorbed = deflection < 0.0;
    float escapedAngle = rayAngle + max(deflection, 0.0);
    result.escapedDirection = radial * cos(escapedAngle) + tangent * sin(escapedAngle);
    if (radiusDerivative > 0.0) {
        result.minimumRadius = result.absorbed ? 1.0 : 1.0 / max(heartApsisInverseRadius(energySquared), 1e-7);
    }
    if (nodeLength > 1e-6) {
        result.nearDisc = heartDiscIntersection(nearHit, radial, tangent, energySquared, radiusDerivative > 0.0 ? 1.0 : -1.0);
        result.farDisc = heartDiscIntersection(farHit, radial, tangent, energySquared, -1.0);
    }
    // Keep analytic special cases after the derivative-dependent lookup path.
    if (radius <= 1.0001 || transverseLength < 1e-5) {
        result.absorbed = radius <= 1.0001 || radialCosine < 0.0;
        result.escapedDirection = direction;
        result.minimumRadius = result.absorbed ? 1.0 : radius;
        result.nearDisc = emptyHit;
        result.farDisc = emptyHit;
    }
    return result;
}
