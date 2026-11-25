attribute vec4 aCenter;
attribute vec4 aQuat;
attribute vec4 aSpin;
attribute vec4 aInfo;
uniform vec2 uSpinPhase[65];
uniform float uFall;
uniform float uStarR;
varying vec3 vObj;
varying vec3 vWorld;
flat out vec4 vQ;
flat out vec4 vInfo;
flat out float vR;
vec4 qmul(vec4 a, vec4 b) {
    return vec4(a.w*b.xyz+b.w*a.xyz+cross(a.xyz,b.xyz), a.w*b.w-dot(a.xyz,b.xyz));
}
vec3 qrot(vec4 q, vec3 v) {
    return v + 2.*cross(q.xyz, cross(q.xyz,v)+q.w*v);
}
// After totality each rock spirals into the black sun with the planets (uFall 0..1); a hash of its centre staggers it.
float fallSeed(vec3 c) {
    return fract(sin(dot(c, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}
vec4 fallen(vec3 c) {
    if (uFall <= 0.) return vec4(c, 1.);
    float seed = fallSeed(c);
    float p = clamp((uFall - .35*seed) / .65, 0., 1.), e = pow(p, 1.6);
    float a = atan(c.y, c.x) + 6.2831853*(.35 + .3*seed)*p*p, r = mix(length(c.xy), uStarR*.98, e);
    return vec4(r*cos(a), r*sin(a), mix(c.z, 88., e), (1. - smoothstep(.8, 1., p))*(1. - .3*e));
}
void main() {
    vec2 phase = uSpinPhase[int(aSpin.w+.5)];
    vec4 q = qmul(vec4(aSpin.xyz*phase.x, phase.y), aQuat);
    vec4 f = fallen(aCenter.xyz);
    vec3 p = f.xyz + qrot(q,position)*aCenter.w*f.w;
    vObj = position;
    vWorld = (modelMatrix*vec4(p,1.)).xyz;
    vQ = q;
    vInfo = aInfo;
    vR = aCenter.w;
    gl_Position = projectionMatrix*modelViewMatrix*vec4(p,1.);
}
