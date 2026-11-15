#version 150
// Positions are camera-relative world coordinates.
// With Wear on, Sampler1 and Sampler3 hold the worn-metal picture and its normals, projected along the model's axes.
#moj_import <gtladditions:heart_scene_hand.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform mat4 ModelMat;
uniform vec3 Center;
uniform float Power;
uniform vec3 BaseColor;
uniform vec3 Emissive;
uniform float Cut;
uniform float NormalScale;
uniform float Metal;
uniform float Rough;
uniform float Wear;

flat in mat3 ObjectToWorld;
in vec3 WorldPos;
in vec3 WorldNormal;
in vec3 ObjectPos;
in vec3 ObjectNormal;
in vec2 TexUv;

out vec4 fragColor;

vec3 lin(vec3 c) { return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c)); }
vec3 srgb(vec3 c) { c = max(c, vec3(0.0)); return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c)); }

// The worn-metal picture repeats every WEAR_TILE model blocks; WEAR_MEAN is its linear mean, so the material colour stays the average.
const float WEAR_TILE = 0.6;
const float WEAR_MEAN = 0.343;
// Spread of the worn metal's light and dark, and the share of its relief.
const float WEAR_CONTRAST = 0.7;
const float WEAR_BUMP = 0.5;

// Tangent frame from screen derivatives, as no tangents are stored; v runs down the picture (glTF), so y is turned over.
vec3 bend(vec3 n, vec3 dpx, vec3 dpy, vec2 dux, vec2 duy, vec3 m) {
    vec3 a = cross(dpy, n);
    vec3 b = cross(n, dpx);
    vec3 t = a * dux.x + b * duy.x;
    vec3 s = a * dux.y + b * duy.y;
    float k = inversesqrt(max(max(dot(t, t), dot(s, s)), 1e-20));
    return normalize(mat3(t * k, s * k, n) * m);
}

void main() {
    vec4 t = texture(Sampler0, TexUv);
    vec3 e = texture(Sampler1, TexUv).rgb;
    vec3 bump = texture(Sampler2, TexUv).rgb;
    vec3 mr = Wear > 0.5 ? vec3(1.0) : texture(Sampler3, TexUv).rgb;
    // Derivatives in uniform control flow, before any branch.
    vec3 dpx = dFdx(WorldPos);
    vec3 dpy = dFdy(WorldPos);
    vec2 dux = dFdx(TexUv);
    vec2 duy = dFdy(TexUv);
    vec3 n = normalize(WorldNormal);
    if (!gl_FrontFacing) n = -n;
    if (NormalScale > 0.0) n = bend(n, dpx, dpy, dux, duy, normalize((bump * 2.0 - 1.0) * vec3(NormalScale, -NormalScale, 1.0)));
    vec3 base = lin(BaseColor * t.rgb);
    if (Wear > 0.5) {
        vec3 o = normalize(ObjectNormal);
        if (!gl_FrontFacing) o = -o;
        vec3 w = pow(abs(o), vec3(4.0));
        w /= dot(w, vec3(1.0));
        vec3 q = ObjectPos / WEAR_TILE;
        vec3 a = texture(Sampler1, q.zy).rgb * w.x + texture(Sampler1, q.xz).rgb * w.y + texture(Sampler1, q.xy).rgb * w.z;
        base *= clamp(1.0 + (lin(a) / WEAR_MEAN - 1.0) * WEAR_CONTRAST, 0.3, 1.7);
        vec3 nx = texture(Sampler3, q.zy).xyz * 2.0 - 1.0;
        vec3 ny = texture(Sampler3, q.xz).xyz * 2.0 - 1.0;
        vec3 nz = texture(Sampler3, q.xy).xyz * 2.0 - 1.0;
        nx.xy *= WEAR_BUMP;
        ny.xy *= WEAR_BUMP;
        nz.xy *= WEAR_BUMP;
        // Projected normals are in model space.
        vec3 bent = normalize(vec3(nx.xy + o.zy, o.x).zyx * w.x + vec3(ny.xy + o.xz, o.y).xzy * w.y + vec3(nz.xy + o.xy, o.z) * w.z);
        // Added to the normal the material's own picture may already bend.
        n = normalize(n + normalize(ObjectToWorld * bent) - normalize(ObjectToWorld * o));
    }
    float metal = clamp(Metal * mr.b, 0.0, 1.0);
    float rough = clamp(Rough * mr.g, 0.08, 1.0);
    vec3 l = normalize(Center - WorldPos);
    vec3 v = normalize(-WorldPos);
    float nl = max(dot(n, l), 0.0);
    float mu = abs(dot(n, v));
    vec3 diffuse = base * (1.0 - 0.45 * metal);
    vec3 col = diffuse * (vec3(0.11, 0.12, 0.14) + vec3(0.05) * n.y + vec3(0.06, 0.08, 0.11) * pow(1.0 - mu, 2.0));
    col += base * metal * vec3(0.05, 0.06, 0.08) * (0.6 + 0.4 * n.y);
    col += diffuse * vec3(1.0, 0.93, 0.84) * nl * (0.4 + 0.75 * Power);
    float shine = mix(6.0, 120.0, (1.0 - rough) * (1.0 - rough));
    // Never below the 4% of glass: the black "ReflectiveGlass" materials are marked fully metal and would reflect nothing at all.
    col += max(mix(vec3(0.04), base, metal), vec3(0.04)) * pow(max(dot(n, normalize(l + v)), 0.0), shine) * (shine + 8.0) / 25.0 * nl * (0.4 + 0.6 * Power);
    vec3 color = srgb(col) + Emissive * e * (0.45 + 0.55 * Power);
    if ((Cut > 0.0 && t.a < Cut) || sceneHandOccluded(gl_FragCoord.xy)) discard;
    fragColor = vec4(color, 1.0);
}
