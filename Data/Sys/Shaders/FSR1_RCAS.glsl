// FidelityFX Super Resolution 1.0 - RCAS (Robust Contrast-Adaptive Sharpening) Shader
// AMD FidelityFX Super Resolution
// Copyright (c) 2021 Advanced Micro Devices, Inc. All rights reserved.
// SPDX-License-Identifier: MIT
// Simplified GLSL implementation for Dolphin

#version 450 core
#extension GL_GOOGLE_include_directive : require

layout(local_size_x = 16, local_size_y = 16) in;

// Input texture (upscaled from EASU)
layout(binding = 0) uniform sampler2D inputTexture;

// Output texture (sharpened)
layout(binding = 1, rgba8) uniform writeonly image2D outputImage;

// Constants for RCAS
layout(std140, binding = 2) uniform RCASConstants
{
  vec4 Const0;  // Sharpness parameter in x component
} rcasConst;

// FSR1 RCAS filter (simplified contrast-adaptive sharpening)
vec4 FsrRcasF(ivec2 pos, float sharpness)
{
  vec2 texelSize = 1.0 / vec2(textureSize(inputTexture, 0));
  vec2 uv = (vec2(pos) + 0.5) * texelSize;
  
  // Sample center and 4-connected neighbors
  vec4 center = texture(inputTexture, uv);
  vec4 north = texture(inputTexture, uv + vec2(0.0, -texelSize.y));
  vec4 south = texture(inputTexture, uv + vec2(0.0, texelSize.y));
  vec4 east = texture(inputTexture, uv + vec2(texelSize.x, 0.0));
  vec4 west = texture(inputTexture, uv + vec2(-texelSize.x, 0.0));
  
  // Compute local min and max for contrast
  vec4 minValue = min(min(min(north, south), min(east, west)), center);
  vec4 maxValue = max(max(max(north, south), max(east, west)), center);
  
  // Sharpening amount based on local contrast
  // sharpness: 0.0 = max sharpening, 2.0 = no sharpening
  float sharpenAmount = clamp(1.0 - sharpness * 0.5, 0.0, 1.0);
  
  // Weighted sum for sharpening
  vec4 sum = north + south + east + west;
  vec4 sharpened = center + (center * 4.0 - sum) * sharpenAmount * 0.25;
  
  // Clamp to avoid over/undershooting
  sharpened = clamp(sharpened, minValue, maxValue);
  
  return sharpened;
}

void main()
{
  ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
  ivec2 outputSize = imageSize(outputImage);
  
  if (gid.x >= outputSize.x || gid.y >= outputSize.y)
    return;
  
  float sharpness = rcasConst.Const0.x;
  vec4 color = FsrRcasF(gid, sharpness);
  imageStore(outputImage, gid, color);
}
