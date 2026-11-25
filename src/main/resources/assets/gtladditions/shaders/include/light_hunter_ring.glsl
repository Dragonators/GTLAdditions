// Emission and visibility use the actual ring field in machine-local space, including interior views.
flat in vec2 ringShape;
const float RING_SUPPORT = 1.65;
const float RING_EXPOSURE = 2.0;

float ringFieldDistance(vec3 p, float radius) {
    return length(vec2(p.x, length(p.yz) - radius));
}

// 0 at the first ring, 1 at the last.
float ringAge() {
    return clamp((-180.55 - ringShape.x - 16.0) / 30.25, 0.0, 1.0);
}

vec3 ringFieldEmission(vec3 p, float radius) {
    float radial = length(p.yz) - radius;
    float d2 = p.x * p.x + radial * radial;
    if (d2 >= RING_SUPPORT * RING_SUPPORT) return vec3(0.0);
    float core = 0.56 / (1.77245385 * 0.10) * exp(-d2 / 0.0100);
    float shoulder = 0.32 / (1.77245385 * 0.36) * exp(-d2 / 0.1296);
    float haze = 0.11 / (1.77245385 * 0.78) * exp(-d2 / 0.6084);
    float edge = d2 <= 1.35 * 1.35 ? 1.0 : 1.0 - smoothstep(1.35, RING_SUPPORT, sqrt(d2));
    float age = ringAge();
    vec3 coreColor = mix(vec3(0.72, 0.93, 1.0), vec3(0.74, 0.62, 1.0), age);
    vec3 haloColor = mix(vec3(0.16, 0.56, 1.0), vec3(0.46, 0.22, 0.99), age);
    return (coreColor * core + haloColor * (shoulder + haze)) * edge;
}

vec2 ringCylinderInterval(vec2 origin, vec2 direction, float radius) {
    float a = dot(direction, direction);
    float c = dot(origin, origin) - radius * radius;
    if (a < 0.00000001) return c <= 0.0 ? vec2(-1e20, 1e20) : vec2(1.0, -1.0);
    float b = dot(origin, direction);
    float discriminant = b * b - a * c;
    if (discriminant < 0.0) return vec2(1.0, -1.0);
    float root = sqrt(max(discriminant, 0.0));
    return vec2(-b - root, -b + root) / a;
}

void integrateRingInterval(vec3 origin, vec3 direction, float base, vec2 interval, float gain, inout vec3 light) {
    if (interval.y <= interval.x) return;
    vec3 start = origin + direction * interval.x;
    float length = interval.y - interval.x;
    float cursor = 0.0;
    // At the largest animated radius (13.104), a field interval is under 19 blocks.
    // The 0.20 minimum step plus at most five layer-boundary splits fits in 128 steps.
    for (int i = 0; i < 128; i++) {
        if (cursor >= length) break;
        float d = ringFieldDistance(start + direction * cursor, ringShape.y);
        float segment = min(clamp(d * 0.8, 0.20, 0.70), length - cursor);
        float distance = base + interval.x + cursor;
        segment = layerClipSegment(distance, segment);
        // Four-point Gaussian quadrature resolves the bright core without tying
        // the sample lattice to an opaque occluder's moving end point.
        vec3 emission = vec3(0.0);
        emission += ringFieldEmission(start + direction * (cursor + segment * 0.069431844203), ringShape.y) * 0.173927422569;
        emission += ringFieldEmission(start + direction * (cursor + segment * 0.330009478208), ringShape.y) * 0.326072577431;
        emission += ringFieldEmission(start + direction * (cursor + segment * 0.669990521792), ringShape.y) * 0.326072577431;
        emission += ringFieldEmission(start + direction * (cursor + segment * 0.930568155797), ringShape.y) * 0.173927422569;
        emission *= gain;
        light += emission * segment;
        layerAccumulateEmission(emission, segment, distance);
        cursor += segment;
    }
}

vec4 volumeRing() {
    // Use the exit of a convex proxy. This remains valid inside the box or field.
    if (gl_FrontFacing) discard;
    vec3 direction = normalize(orthoCamera > 0.5 ? localForward : localPosition - localEye);
    vec3 eye = orthoCamera > 0.5 ? localPosition - direction * 700.0 : localEye;
    vec3 origin = eye - vec3(ringShape.x, 0.0, 0.0);
    vec3 eyeView = (ModelViewMat * vec4(eye, 1.0)).xyz;
    vec3 dirView = (ModelViewMat * vec4(direction, 0.0)).xyz;
    float sceneZ = texelFetch(SceneDepth, ivec2(effectScreenPixel() - SceneViewport), 0).r * 2.0 - 1.0;
    float viewZ = (ProjMat[3][2] - sceneZ * ProjMat[3][3]) / (sceneZ * ProjMat[2][3] - ProjMat[2][2]);
    float end = (viewZ - eyeView.z) / dirView.z;
    if (end <= 0.0) discard;

    vec2 axial = vec2(0.0, end);
    if (abs(direction.x) < 0.000001) {
        if (abs(origin.x) > RING_SUPPORT) discard;
    } else {
        vec2 sides = (vec2(-RING_SUPPORT, RING_SUPPORT) - origin.x) / direction.x;
        axial = vec2(max(min(sides.x, sides.y), 0.0), min(max(sides.x, sides.y), end));
    }
    if (axial.y <= axial.x) discard;

    // Rebase near the field before the quadratic to avoid cancellation for
    // distant and orthographic cameras. Skip the empty centre of the ring.
    float base = axial.x;
    origin += direction * base;
    vec2 outer = ringCylinderInterval(origin.yz, direction.yz, ringShape.y + RING_SUPPORT);
    outer = vec2(max(outer.x, 0.0), min(outer.y, axial.y - base));
    if (outer.y <= outer.x) discard;
    vec2 inner = ringCylinderInterval(origin.yz, direction.yz, max(ringShape.y - RING_SUPPORT, 0.0));

    layerBegin(eyeView, dirView);
    vec3 light = vec3(0.0);
    float age = ringAge();
    float pulse = pow(0.5 + 0.5 * sin(AngularTime * 2.1 - age * 4.2), 3.0);
    float gain = tint.a * mix(1.0, 0.56, age) * (0.80 + 0.38 * pulse);
    if (inner.y > inner.x && inner.y > outer.x && inner.x < outer.y) {
        integrateRingInterval(origin, direction, base, vec2(outer.x, min(outer.y, inner.x)), gain, light);
        integrateRingInterval(origin, direction, base, vec2(max(outer.x, inner.y), outer.y), gain, light);
    } else {
        integrateRingInterval(origin, direction, base, outer, gain, light);
    }
    return layerFinish(light, RING_EXPOSURE);
}
