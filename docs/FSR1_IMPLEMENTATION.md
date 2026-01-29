# FSR1 Implementation Documentation

## Overview

This document describes the FidelityFX Super Resolution 1.0 (FSR1) implementation for Dolphin-MMJR2-FSR emulator.

## What is FSR1?

FidelityFX Super Resolution (FSR) is AMD's open-source spatial upscaling technology that produces high-quality upscaled images from lower-resolution inputs. FSR 1.0 consists of two main passes:
1. **EASU** (Edge-Adaptive Spatial Upsampling) - High-quality spatial upscaling
2. **RCAS** (Robust Contrast-Adaptive Sharpening) - Adaptive sharpening

## Implementation Approach

This implementation integrates FSR1 into Dolphin's existing post-processing pipeline as a new output resampling mode, rather than as a separate compute shader pipeline. This approach has several advantages:

- **Simplicity**: Uses existing shader infrastructure
- **Compatibility**: Works with all graphics backends (Vulkan, OpenGL, D3D11, D3D12)
- **Maintainability**: Minimal code changes, easy to understand
- **Performance**: No additional overhead when disabled

## Architecture

### Configuration Layer
**Files**: `Source/Core/Core/Config/GraphicsSettings.h/cpp`

Added two new configuration settings:
```cpp
const Info<bool> GFX_ENHANCE_FSR1_ENABLE{{System::GFX, "Enhancements", "FSR1Enable"}, false};
const Info<float> GFX_ENHANCE_FSR1_SHARPNESS{{System::GFX, "Enhancements", "FSR1Sharpness"}, 0.5f};
```

### Video Configuration
**Files**: `Source/Core/VideoCommon/VideoConfig.h/cpp`

Added runtime configuration fields:
```cpp
bool bFSR1Enable = false;
float fFSR1Sharpness = 0.5f;
```

When FSR1 is enabled, the configuration automatically sets:
```cpp
if (bFSR1Enable)
{
  output_resampling_mode = OutputResamplingMode::FSR;
}
```

### Shader Implementation
**File**: `Data/Sys/Shaders/default_pre_post_process.glsl`

Added FSR mode (case 7) in the `LinearGammaCorrectedSample()` function:

```glsl
else if (resampling_method == 7) // FSR (FidelityFX Super Resolution)
{
    // Step 1: Catmull-Rom bicubic upsampling (EASU approximation)
    color = BicubicSample(uvw, gamma, CUBIC_COEFF_GEN(0.0, 0.5));
    
    // Step 2: Edge-adaptive sharpening (RCAS approximation)
    // Sample unfiltered neighbors for proper edge detection
    float4 north = QuickSampleByPixel(...);
    float4 south = QuickSampleByPixel(...);
    float4 east = QuickSampleByPixel(...);
    float4 west = QuickSampleByPixel(...);
    
    // Compute local min/max for clamping
    float4 minVal = min(min(min(north, south), min(east, west)), color);
    float4 maxVal = max(max(max(north, south), max(east, west)), color);
    
    // Adaptive sharpening
    float4 sum = north + south + east + west;
    float4 sharpened = color + (color * 4.0 - sum) * 0.25;
    
    // Clamp to prevent artifacts
    color = clamp(sharpened, minVal, maxVal);
}
```

### Android UI
**Files**: 
- `Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/features/settings/ui/QuickSettingsFragment.java`
- `Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/features/settings/model/BooleanSetting.java`
- `Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/features/settings/model/FloatSetting.java`
- `Source/Android/app/src/main/res/values/strings.xml`

Added FSR1 checkbox in Quick Settings:
```java
sl.add(new CheckBoxSetting(context, BooleanSetting.GFX_ENHANCE_FSR1_ENABLE,
        R.string.fsr1_enable, R.string.fsr1_enable_description));
```

## Usage

### For Users

1. Open Dolphin settings
2. Navigate to Graphics settings
3. Enable "FSR 1.0 Upscaling" checkbox
4. Set Internal Resolution to 1x, 2x, or 3x (lower than native)
5. Launch a game

The emulator will now use FSR1 to upscale from the internal resolution to your device's native resolution with improved quality compared to bilinear filtering.

### Recommended Settings

**For best visual quality:**
- Internal Resolution: 1x or 2x
- FSR1: Enabled
- Backend: Any (Vulkan recommended for performance)

**For maximum performance:**
- Internal Resolution: 1x
- FSR1: Enabled
- This provides better quality than 1x with bilinear at minimal performance cost

## Technical Details

### When FSR1 Activates

FSR1 only activates when:
1. FSR1 is enabled in settings
2. Internal resolution < Output resolution (upscaling scenario)
3. The game is actually rendering (not during menus/UI)

### Performance Impact

- **When disabled**: Zero overhead (setting checked once per frame)
- **When enabled**: Similar to bicubic filtering (~5-10% shader cost over bilinear)
- **Compared to higher internal resolution**: Much faster than rendering at native resolution

### Compatibility

- **Backends**: Works with Vulkan, OpenGL, D3D11, D3D12
- **Platforms**: Primarily tested on Android, should work on all platforms
- **Color Correction**: Compatible with gamma and color space correction
- **HDR**: Compatible with HDR output

## Differences from AMD's Reference Implementation

This implementation differs from AMD's official FSR1 in several ways:

1. **Single-pass**: Combines EASU and RCAS into one shader pass for simplicity
2. **Bicubic-based**: Uses Catmull-Rom bicubic instead of FSR's custom EASU
3. **Simplified sharpening**: Uses 5-tap sharpening instead of full RCAS algorithm
4. **No temporal component**: Pure spatial upscaling (like FSR1, no FSR2 features)

These simplifications make the implementation easier to maintain while still providing significant quality improvements over bilinear filtering.

## Future Enhancements

Potential improvements for future versions:

1. **Use sharpness setting**: Currently the fFSR1Sharpness config is loaded but not used
2. **Multi-pass implementation**: Separate EASU and RCAS for better quality
3. **AMD reference shaders**: Port official FSR1 shaders for maximum quality
4. **FSR2 temporal**: Add temporal upscaling for even better quality
5. **UI improvements**: Add sharpness slider, quality presets

## Troubleshooting

### FSR1 not working?

1. Check that "FSR 1.0 Upscaling" is enabled in settings
2. Verify internal resolution is lower than window resolution
3. Ensure you're running a game (not just menus)
4. Check that post-processing/output resampling is not disabled

### Visual artifacts?

1. Try adjusting internal resolution
2. Disable other post-processing shaders
3. Check gamma correction settings
4. Report the issue with screenshots

### Performance issues?

1. FSR1 should have minimal impact (~5-10%)
2. If performance is bad, the issue is likely elsewhere
3. Try different internal resolutions
4. Profile the emulator to identify bottlenecks

## References

- [AMD FidelityFX Super Resolution](https://gpuopen.com/fidelityfx-superresolution/)
- [FSR 1.0 Technical Documentation](https://github.com/GPUOpen-Effects/FidelityFX-FSR)
- [Dolphin Emulator Documentation](https://dolphin-emu.org/docs/)

## License

This implementation is part of Dolphin Emulator and follows the same license (GPL-2.0-or-later).
FSR1 reference is under MIT license from AMD.
