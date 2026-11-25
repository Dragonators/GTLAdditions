// The eclipse plane and its lift share the star-to-observer axis.
uniform vec3 uCenter;
uniform vec3 uCam;
uniform float uRad;
uniform vec2 uOff;
uniform float uLift;
varying vec2 vUv;
void main() {
    vec3 view = uCam - uCenter;
    float dist = length(view);
    view = dist > 1e-9 ? view / dist : vec3(0., -1., 0.);
    // Use the same celestial plane as the limb, rather than shifting along screen Z.
    vec3 right = vec3(-view.y, view.x, 0.);
    right = dot(right, right) > 1e-12 ? normalize(right) : normalize(vec3(1., 0., 0.) - view * view.x);
    vec3 up = normalize(cross(view, right));
    vec2 xy = position.xy * 2.;
    vec2 offset = (xy + uOff) * uRad;
    vec3 point = uCenter + view * uLift + right * offset.x + up * offset.y;
    vUv = xy;
    gl_Position = projectionMatrix * viewMatrix * vec4(point, 1.);
}
