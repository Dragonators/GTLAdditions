attribute vec4 aCenter;
attribute vec4 aQuat;
attribute vec4 aSpin;
attribute vec4 aInfo;
uniform vec2 uSpinPhase[65];
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
void main() {
    vec2 phase = uSpinPhase[int(aSpin.w+.5)];
    vec4 q = qmul(vec4(aSpin.xyz*phase.x, phase.y), aQuat);
    vec3 p = aCenter.xyz + qrot(q,position)*aCenter.w;
    vObj = position;
    vWorld = (modelMatrix*vec4(p,1.)).xyz;
    vQ = q;
    vInfo = aInfo;
    vR = aCenter.w;
    gl_Position = projectionMatrix*modelViewMatrix*vec4(p,1.);
}
