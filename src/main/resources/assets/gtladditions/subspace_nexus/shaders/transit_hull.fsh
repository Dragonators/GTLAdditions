uniform vec3 uCenter;
uniform float uPower;
uniform float uAmb;
uniform sampler2D uMap;
uniform sampler2D uNMap;
uniform vec3 uHot;
uniform vec3 uRim;
varying vec3 vNw;
varying vec3 vW;
varying vec3 vO;
varying vec3 vNo;
varying mat3 vRot;
varying vec2 vUv;
varying vec3 vBase;
varying vec3 vEmit;
// metal, rough, textured (uMap with uNMap), worn (both laid by object-space projection)
flat varying vec4 vMat;
varying float vHeat;
varying float vAlpha;
varying float vWave;

const float WEAR_TILE = 0.6, WEAR_MEAN = 0.343, WEAR_CONTRAST = 0.7, WEAR_BUMP = 0.5;

vec3 wearAlbedo(vec3 q, vec3 w) {
    return texture2D(uMap, q.zy).rgb * w.x + texture2D(uMap, q.xz).rgb * w.y + texture2D(uMap, q.xy).rgb * w.z;
}

vec3 wearNormal(vec3 q, vec3 w, vec3 o) {
    vec3 nx = texture2D(uNMap, q.zy).xyz * 2.0 - 1.0, ny = texture2D(uNMap, q.xz).xyz * 2.0 - 1.0, nz = texture2D(uNMap, q.xy).xyz * 2.0 - 1.0;
    nx.xy *= WEAR_BUMP;
    ny.xy *= WEAR_BUMP;
    nz.xy *= WEAR_BUMP;
    return normalize(vec3(nx.xy + o.zy, o.x).zyx * w.x + vec3(ny.xy + o.xz, o.y).xzy * w.y + vec3(nz.xy + o.xy, o.z) * w.z);
}

vec3 lin(vec3 c) { return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c)); }

vec3 srgb(vec3 c) {
    c = max(c, vec3(0.0));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c));
}

vec3 bend(vec3 n, vec3 dpx, vec3 dpy, vec2 dux, vec2 duy, vec3 m) {
    vec3 a = cross(dpy, n), b = cross(n, dpx), t = a * dux.x + b * duy.x, s = a * dux.y + b * duy.y;
    float k = inversesqrt(max(max(dot(t, t), dot(s, s)), 1e-20));
    return normalize(mat3(t * k, s * k, n) * m);
}

void main() {
    if (vAlpha <= 0.002) discard;
    // Every sample is taken unconditionally: the material flags change across a mesh, derivatives must not.
    vec3 picture = texture2D(uMap, vUv).rgb;
    vec3 bump = texture2D(uNMap, vUv).rgb;
    vec3 dpx = dFdx(vW), dpy = dFdy(vW);
    vec2 dux = dFdx(vUv), duy = dFdy(vUv);
    vec3 o = normalize(vNo);
    vec3 n = normalize(vNw);
    if (!gl_FrontFacing) {
        n = -n;
        o = -o;
    }

    vec3 w = pow(abs(o), vec3(4.0));
    w /= dot(w, vec3(1.0));
    vec3 q = vO / WEAR_TILE;
    vec3 wear = clamp(1.0 + (lin(wearAlbedo(q, w)) / WEAR_MEAN - 1.0) * WEAR_CONTRAST, 0.3, 1.7);
    vec3 worn = normalize(n + normalize(vRot * wearNormal(q, w, o)) - normalize(vRot * o));
    n = vMat.z > 0.5 ? bend(n, dpx, dpy, dux, duy, normalize(bump * 2.0 - 1.0) * vec3(1.0, -1.0, 1.0)) : n;
    n = vMat.w > 0.5 ? worn : n;

    vec3 V = normalize(cameraPosition - vW);
    float fres = pow(1.0 - abs(dot(n, V)), 2.2);
    vec3 base = lin(vBase * mix(vec3(1.0), picture, vMat.z)) * mix(vec3(1.0), wear, vMat.w);
    float metal = clamp(vMat.x, 0.0, 1.0), rough = clamp(vMat.y, 0.08, 1.0);
    vec3 L = normalize(uCenter - vW);
    float nl = max(dot(n, L), 0.0);
    vec3 diffuse = base * (1.0 - 0.45 * metal);
    vec3 col = diffuse * (vec3(0.11, 0.12, 0.14) + vec3(0.05) * n.z + vec3(0.06, 0.08, 0.11) * fres);
    col += diffuse * vec3(0.30, 0.33, 0.40) * uAmb * (0.55 + 0.45 * n.z);
    col += base * metal * vec3(0.05, 0.06, 0.08) * (0.6 + 0.4 * n.z);
    col += diffuse * vec3(1.0, 0.93, 0.84) * nl * (0.4 + 0.75 * uPower);
    float shine = mix(6.0, 120.0, (1.0 - rough) * (1.0 - rough));
    col += max(mix(vec3(0.04), base, metal), vec3(0.04)) * pow(max(dot(n, normalize(L + V)), 0.0), shine) * (shine + 8.0) / 25.0 * nl * (0.4 + 0.6 * uPower);
    vec3 lit = srgb(col) + vEmit * (0.45 + 0.55 * uPower);

    lit += mix(uHot, uRim, 0.5 + 0.5 * fres) * vWave * (0.35 + 0.95 * fres);
    lit += mix(uHot, uRim, clamp(fres * 1.15, 0.0, 1.0)) * fres * clamp(vHeat * 0.72, 0.0, 1.0) * 0.78;
    gl_FragColor = vec4(lit * vAlpha, vAlpha);
}
