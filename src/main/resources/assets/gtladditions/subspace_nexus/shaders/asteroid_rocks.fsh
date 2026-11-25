uniform sampler2DArray uMaps;
uniform sampler2DArray uNormals;
uniform vec3 uStarC;
uniform vec3 uCam;
uniform vec3 uKey;
uniform vec3 uFillC;
uniform float uAmbK;
uniform float uGlow;
uniform float uNeg;
uniform vec3 uGold;
varying vec3 vObj;
varying vec3 vWorld;
flat in vec4 vQ;
flat in vec4 vInfo;
flat in float vR;
vec3 qrot(vec4 q, vec3 v) {
    return v + 2.*cross(q.xyz, cross(q.xyz,v)+q.w*v);
}
float tile(vec2 p, float layer) {
    float m = abs(fract(p.y*.5)*2.-1.);
    return texture(uMaps,vec3(p.x,.25+.5*m,layer)).r;
}
void main() {
    vec3 d = normalize(vObj);
    float u = (dot(d.xy,d.xy)>1e-12?atan(d.y,d.x):0.)/6.28318530718;
    float a = fract(u), b = fract(u+.5);
    vec2 ll = vec2(a,acos(clamp(d.z,-1.,1.))/3.14159265359);
    vec2 dx = dFdx(ll), dy = dFdy(ll);
    float dx2 = dFdx(b), dy2 = dFdy(b);
    if (abs(dx2)<abs(dx.x)) dx.x = dx2;
    if (abs(dy2)<abs(dy.x)) dy.x = dy2;
    vec3 no = normalize(textureGrad(uNormals,vec3(ll,vInfo.w),dx,dy).xyz*2.-1.);
    vec3 ns = normalize(textureLod(uNormals,vec3(ll,vInfo.w),2.).xyz*2.-1.);
    float tex;
    if (vInfo.z>.5) {
        tex = textureGrad(uMaps,vec3(ll,vInfo.x),dx,dy).r;
    } else {
        vec3 w = pow(abs(ns),vec3(4.));
        w /= max(w.x+w.y+w.z,1e-6);
        float seed = fract(vInfo.w*.37+vInfo.x*.61+vR*17.);
        vec3 p = vObj*.7+seed*7.;
        tex = w.x*tile(p.yz*vec2(.25,1.),vInfo.x)
            + w.y*tile(p.zx*vec2(.25,1.),vInfo.x)
            + w.z*tile(p.xy*vec2(.25,1.),vInfo.x);
    }
    // Both vectors live in the unmirrored design frame. The view matrix handles
    // controller orientation/reflection, exactly as it handles uCam.
    vec3 N = normalize(mat3(modelMatrix)*qrot(vQ,no));
    vec3 L = normalize(uStarC-vWorld), V = normalize(uCam-vWorld);
    float mu0 = max(dot(N,L),0.), mu = max(dot(N,V),.03);
    float key = mix(mu0,2.*mu0/(mu0+mu+.3),.55);
    vec3 out3 = normalize(vec3(normalize(vWorld.xy)*.62,-.78));
    float fill = .5+.5*dot(N,out3);
    vec3 tint = vInfo.x<.5?vec3(1.,.92,.84):(vInfo.x<1.5?vec3(1.,.88,.75):vec3(1.,.90,.79));
    vec3 c = tint*vInfo.y*tex*2.*(uKey*key+uFillC*fill*fill+vec3(uAmbK));
    if (uNeg>0.) {
        float vs = dot(normalize(mat3(modelMatrix)*qrot(vQ,ns)),V);
        float edge = sqrt(max(0.,1.-vs*vs));
        c = mix(c,vec3(.02)+uGold*.5*smoothstep(.5,.95,edge),uNeg);
    }
    gl_FragColor = vec4(c*uGlow,1.);
}
