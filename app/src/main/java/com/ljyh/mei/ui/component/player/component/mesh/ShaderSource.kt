package com.ljyh.mei.ui.component.player.component.mesh

object ShaderSource {

    const val MESH_VERTEX_SHADER = """
#version 300 es
precision highp float;

in vec2 a_pos;
in vec3 a_color;
in vec2 a_uv;

out vec3 v_color;
out vec2 v_uv;

uniform float u_aspect;

void main() {
    v_color = a_color;
    v_uv = a_uv;
    vec2 pos = a_pos;
    if (u_aspect > 1.0) {
        pos.y *= u_aspect;
    } else {
        pos.x /= u_aspect;
    }
    gl_Position = vec4(pos, 0.0, 1.0);
}
"""

    const val MESH_FRAGMENT_SHADER = """
#version 300 es
precision highp float;

in vec3 v_color;
in vec2 v_uv;

uniform sampler2D u_texture;
uniform float u_time;
uniform float u_volume;

out vec4 fragColor;

float gradientNoise(vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
}

vec2 rot(vec2 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

void main() {
    float volumeEffect = u_volume * 2.0;
    float timeVolume = u_time + u_volume;

    float dither = gradientNoise(gl_FragCoord.xy) / 255.0 - 0.5 / 255.0;

    vec2 centered = v_uv - vec2(0.5);
    vec2 rotated = rot(centered, timeVolume * 2.0);
    vec2 finalUV = rotated * max(0.001, 1.0 - volumeEffect) + vec2(0.5);

    finalUV = clamp(finalUV, 0.0, 1.0);

    vec4 result = texture(u_texture, finalUV);

    float alphaVolume = max(0.5, 1.0 - u_volume * 0.5);
    result.rgb *= v_color * alphaVolume;
    result.a *= alphaVolume;

    result.rgb += vec3(dither);

    float dist = distance(v_uv, vec2(0.5));
    float vignette = smoothstep(0.8, 0.3, dist);
    result.rgb *= 0.6 + vignette * 0.4;

    fragColor = result;
}
"""

    const val QUAD_VERTEX_SHADER = """
#version 300 es
precision highp float;

in vec2 a_pos;
in vec2 a_texCoord;

out vec2 v_texCoord;

void main() {
    v_texCoord = a_texCoord;
    gl_Position = vec4(a_pos, 0.0, 1.0);
}
"""

    const val QUAD_FRAGMENT_SHADER = """
#version 300 es
precision highp float;

in vec2 v_texCoord;

uniform sampler2D u_texture;
uniform float u_alpha;

out vec4 fragColor;

void main() {
    vec4 color = texture(u_texture, v_texCoord);
    fragColor = vec4(color.rgb, color.a * u_alpha);
}
"""
}