#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelStep;
uniform int KernelRadius;

in vec2 texCoord;

out vec4 fragColor;

// For radius r: sigma = r / 3, w(i) = (0.39894 / sigma) * exp(-0.5 * i*i / (sigma*sigma)).
// Finite, unnormalized kernel; each positive tap pair uses its weighted linear-filter offset.
vec3 bloomPair(float offset, float weight) {
    return (texture(Sampler0, texCoord + TexelStep * offset).rgb +
            texture(Sampler0, texCoord - TexelStep * offset).rgb) * weight;
}

vec3 bloomKernel6() {
    return texture(Sampler0, texCoord).rgb * 0.1994700000
        + bloomPair(1.4073334000, 0.2970163279)
        + bloomPair(3.2942149722, 0.0917537566)
        + bloomPair(5.0000000000, 0.0087641001);
}

vec3 bloomKernel10() {
    return texture(Sampler0, texCoord).rgb * 0.1196820000
        + bloomPair(1.4663011646, 0.2143825001)
        + bloomPair(3.4218947671, 0.1380806022)
        + bloomPair(5.3787164048, 0.0625399687)
        + bloomPair(7.3373781628, 0.0199133241)
        + bloomPair(9.0000000000, 0.0031262626);
}

vec3 bloomKernel14() {
    return texture(Sampler0, texCoord).rgb * 0.0854871429
        + bloomPair(1.4827874166, 0.1615327822)
        + bloomPair(3.4599076871, 0.1287340360)
        + bloomPair(5.4371957060, 0.0855593015)
        + bloomPair(7.4147440342, 0.0474213224)
        + bloomPair(9.3926409645, 0.0219179795)
        + bloomPair(11.3709691900, 0.0084475783)
        + bloomPair(13.0000000000, 0.0017651999);
}

vec3 bloomKernel18() {
    return texture(Sampler0, texCoord).rgb * 0.0664900000
        + bloomPair(1.4895848401, 0.1284697563)
        + bloomPair(3.4757135714, 0.1119182490)
        + bloomPair(5.4618796741, 0.0873132676)
        + bloomPair(7.4481042327, 0.0610011113)
        + bloomPair(9.4344079746, 0.0381655709)
        + bloomPair(11.4208111470, 0.0213835661)
        + bloomPair(13.4073334000, 0.0107290241)
        + bloomPair(15.3939936778, 0.0048206869)
        + bloomPair(17.0000000000, 0.0012010098);
}

vec3 bloomKernel22() {
    return texture(Sampler0, texCoord).rgb * 0.0544009091
        + bloomPair(1.4930273116, 0.1063123533)
        + bloomPair(3.4837350796, 0.0969153980)
        + bloomPair(5.4744540812, 0.0820443996)
        + bloomPair(7.4651906990, 0.0644988500)
        + bloomPair(9.4559512670, 0.0470870560)
        + bloomPair(11.4467420536, 0.0319225227)
        + bloomPair(13.4375692447, 0.0200973332)
        + bloomPair(15.4284389272, 0.0117496406)
        + bloomPair(17.4193570733, 0.0063790341)
        + bloomPair(19.4103295254, 0.0032160932)
        + bloomPair(21.0000000000, 0.0009013824);
}

void main() {
    if (KernelRadius > 0) {
        vec3 sum;
        if (KernelRadius == 6) sum = bloomKernel6();
        else if (KernelRadius == 10) sum = bloomKernel10();
        else if (KernelRadius == 14) sum = bloomKernel14();
        else if (KernelRadius == 18) sum = bloomKernel18();
        else if (KernelRadius == 22) sum = bloomKernel22();
        else {
            float sigma = float(KernelRadius) / 3.0;
            float coefficient = 0.39894 / sigma;
            sum = texture(Sampler0, texCoord).rgb * coefficient;
            for (int i = 1; i < 22; ++i) {
                if (i >= KernelRadius) break;
                float offset = float(i);
                float weight = coefficient * exp(-0.5 * offset * offset / (sigma * sigma));
                sum += (texture(Sampler0, texCoord + TexelStep * offset).rgb +
                        texture(Sampler0, texCoord - TexelStep * offset).rgb) * weight;
            }
        }
        fragColor = vec4(sum, 0.0);
        return;
    }
    vec4 color = texture(Sampler0, texCoord) * 0.22702703;
    color += texture(Sampler0, texCoord + TexelStep * 1.38461538) * 0.31621622;
    color += texture(Sampler0, texCoord - TexelStep * 1.38461538) * 0.31621622;
    color += texture(Sampler0, texCoord + TexelStep * 3.23076923) * 0.07027027;
    color += texture(Sampler0, texCoord - TexelStep * 3.23076923) * 0.07027027;
    fragColor = color;
}
