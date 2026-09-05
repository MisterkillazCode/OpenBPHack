#version 150

uniform float U_GameTime;
uniform vec2 ScreenSize;
uniform sampler2D MainDepthSampler;
uniform sampler2D MainColorSampler;
uniform mat4 U_InverseProjectionMatrix;
uniform mat4 U_InverseViewMatrix;
uniform vec3 U_CameraPosition;

// Number
uniform float U_FogDensity;
uniform float U_GrainIntensity;
uniform float U_ScanSpeed;
uniform float U_LoopEnabled;
uniform float U_ScanDuration;
uniform float U_StyleMode; // 0.0 =  (Fog), 1.0 = inside (Otherworld)

in vec2 texCoord;
out vec4 fragColor;

float noise(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

vec3 clipToView(vec2 uv, float depth) {
    vec4 clipPos = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = U_InverseProjectionMatrix * clipPos;
    return viewPos.xyz / viewPos.w;
}

void main() {
    float rawDepth = texture(MainDepthSampler, texCoord).r;
    vec4 sceneColor = texture(MainColorSampler, texCoord);
    
    // BasicNumberConfig
    vec3 fogColor;
    vec3 tintColor;
    float grainMult;
    float brightness;

    if (U_StyleMode < 0.5) {
        // ---  (Fog World) ---
        fogColor = vec3(0.55, 0.55, 0.58);    // GrayWhiteColorbig
        tintColor = vec3(0.9, 0.9, 0.95);    // Color
        grainMult = 1.0;                     // Markpoint
        brightness = 0.8;                    // 
    } else {
        // --- inside (Otherworld) ---
        fogColor = vec3(0.15, 0.05, 0.02);   // RedColor
        tintColor = vec3(0.6, 0.2, 0.1);     // ColorMove
        grainMult = 2.5;                     // point
        brightness = 0.4;                    // Dark
    }

    // 1. Color
    vec3 gray = vec3(dot(sceneColor.rgb, vec3(0.299, 0.587, 0.114)));
    vec3 horrorBase = mix(sceneColor.rgb, gray * tintColor, 0.6) * brightness;

    // 2. Air
    float dist = (rawDepth >= 1.0) ? 100.0 : length(clipToView(texCoord, rawDepth));
    float fogFactor = exp(-dist * U_FogDensity);
    
    // 3. point
    float g = noise(texCoord + (U_GameTime * 0.1));
    float grain = (g - 0.5) * U_GrainIntensity * grainMult;

    // 4. /Scan (Reveal Effect)
    float timeCycle = U_GameTime * U_ScanSpeed;
    if (U_LoopEnabled > 0.5) timeCycle = mod(timeCycle, U_ScanDuration);
    float mainRadius = pow(max(timeCycle, 0.0), 4.0);
    
    float noiseOffset = noise(texCoord * 4.0 + U_GameTime * 0.1) * 3.0;
    float alphaMask = 1.0 - smoothstep(mainRadius, mainRadius + 12.0, dist + noiseOffset);

    // 5. mostComplete
    vec3 styledScene = mix(fogColor, horrorBase, fogFactor) + grain;
    
    // Scan (ForGray，insideForRed)
    vec3 edgeColor = (U_StyleMode < 0.5) ? vec3(0.1) : vec3(0.25, 0.0, 0.0);
    float edge = smoothstep(mainRadius, mainRadius + 4.0, dist + noiseOffset) * 
                 smoothstep(mainRadius + 6.0, mainRadius + 2.0, dist + noiseOffset);

    float vignette = smoothstep(0.9, 0.2, length(texCoord - 0.5));
    vec3 finalColor = mix(sceneColor.rgb, styledScene, alphaMask) + edgeColor * edge;
    finalColor *= (vignette + 0.1);

    fragColor = vec4(finalColor, 1.0);
}