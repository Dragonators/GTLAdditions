attribute vec3 ecol;
varying vec3 vN;
varying vec3 vC;
varying vec3 vE;
varying vec3 vP;
varying vec3 vO;

attribute vec4 nexusM0;
attribute vec4 nexusM1;
attribute vec4 nexusM2;
attribute vec4 nexusM3;
attribute float nexusIntensity;
flat varying float nexusIntensityValue;

void main() {
    mat4 nexusModel = mat4(nexusM0, nexusM1, nexusM2, nexusM3);
    nexusIntensityValue = nexusIntensity;
    vN = normalize(mat3(nexusModel) * normal);
    vC = color;
    vE = ecol;
    vO = position;
    vP = (nexusModel * vec4(position, 1.)).xyz;
    gl_Position = projectionMatrix * viewMatrix * vec4(vP, 1.);
}
