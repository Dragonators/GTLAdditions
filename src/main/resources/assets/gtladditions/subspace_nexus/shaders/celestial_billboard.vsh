uniform vec3 uCenter;
uniform vec3 uCam;
uniform float uSize;
varying vec2 vUv;
void main() {
    vec3 view = uCam - uCenter;
    float distance = length(view);
    view = distance > 1e-9 ? view / distance : vec3(0., -1., 0.);
    // Z is world up; match NexusBillboardView, including its pole fallback.
    vec3 right = vec3(-view.y, view.x, 0.);
    right = dot(right, right) > 1e-12 ? normalize(right) : normalize(vec3(1., 0., 0.) - view * view.x);
    vec3 up = normalize(cross(view, right));
    vec3 point = uCenter + (right * position.x + up * position.y) * uSize;
    vUv = position.xy * 2.;
    gl_Position = projectionMatrix * viewMatrix * vec4(point, 1.);
}
