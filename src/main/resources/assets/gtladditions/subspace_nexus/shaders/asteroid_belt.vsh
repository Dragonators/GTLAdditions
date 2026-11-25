attribute float aSize;attribute vec4 aCol;attribute float aSeed;uniform float uScale;uniform vec3 uStarC;uniform float uFall;uniform float uStarR;varying vec4 vCol;varying float vSeed;varying vec2 vLd;
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
void main(){vec4 f=fallen(position);vec4 mv=modelViewMatrix*vec4(f.xyz,1.);gl_Position=projectionMatrix*mv;float s=aSize*f.w*uScale/max(.1,-mv.z);vCol=aCol;vSeed=aSeed;
  vec4 sc=projectionMatrix*viewMatrix*vec4(uStarC,1.);vec2 d=sc.xy/sc.w-gl_Position.xy/gl_Position.w;vLd=normalize(vec2(d.x,-d.y)+1e-5);
  if(s<2.5){vCol.a*=s/2.5;s=2.5;}gl_PointSize=min(s,600.);}