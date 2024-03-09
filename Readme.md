# Dolphin MMJR2 - VBI Fork
This fork is intended mostly for personal use. My goal is to incorporate a series of pull requests from Sam Belliveau into the MMJR2 codebase that implement the VBI Skip speed hack. Dolphin team abandoned non-integer scaling a long time ago. While I completely understand their reasons, I have a hard time letting this feature go. Many Android devices with potato chipsets, like Anbernic or Retroid, can run most games at 1.5x nearly full speed. 2x resolution is usually too much for them, and framerate suffers significantly. Unfortunately, enabling non-integer scaling on the most recent codebase is way out of my league, so I'm going the other way. I want to bring the VBI Skip Hack to MMJR2. All changes to the code are handled by pull requests, credited and linked to original pull requests from Official Dolphin repository.

BEWARE: this is based on old source code! Things may break, saves may corrupt, apps may crash, and there might be no fixes in the near future! If you don't want to experiment or are new to emulation, stick with Official Dolphin!
-----------------------------------------------------------------------
Thank you Dolphin team for your amazing work. Your dedication and hard work have made it possible for people to play their favorite games on modern hardware. Keep up the great work!” 🐬👏 Thanks to the developers of the original MMJR and MMJR2 fork, and Lumince for keeping MMJR2 alive for so long.  
-----------------------------------------------------------------------
# Dolphin MMJR2 Fork
Mainly, this fork is meant for personal use. This repo's only function at the current time is to update MMJR2 to the latest Dolphin Offical Dev Source code without scoped storage changes merged.
If you want scoped storage, go use Dolphin Official Builds. I have no use for scoped storage, nor any changes related to it. I wont be merging these changes. Have a nice day!

An Android-only performance-focused dolphin fork, rebased on top of latest dolphin development builds and reimplementing MMJ UX and performance improvements, plus adding our own.

Grab the latest build in the [releases](https://github.com/Lumince/Dolphin-MMJR2/releases) section, or check for new version in the in-app updater. Old MMJR v1.0 builds can be found at the old repository [here](https://github.com/Bankaimaster999/Dolphin-MMJR/releases). 1.0 and 2.0 builds can be installed without conflicts as they use different folders, but **savestates are not compatible**. We kindly ask you to avoid misusing GitHub Issues and Pull Requests.

This fork wouldn't be possible without the crazy amount of work that developers much more skilled than us put into Dolphin.
# New Discord Server
Join [here](https://discord.gg/NQ9jH8cUeZ)

## Dolphin - A GameCube and Wii Emulator

[Homepage](https://dolphin-emu.org/) | [Project Site](https://github.com/dolphin-emu/dolphin) | [Buildbot](https://dolphin.ci/) | [Forums](https://forums.dolphin-emu.org/) | [Wiki](https://wiki.dolphin-emu.org/) | [GitHub Wiki](https://github.com/dolphin-emu/dolphin/wiki) | [Issue Tracker](https://bugs.dolphin-emu.org/projects/emulator/issues) | [Coding Style](https://github.com/dolphin-emu/dolphin/blob/master/Contributing.md) | [Transifex Page](https://app.transifex.com/delroth/dolphin-emu/dashboard/)

Dolphin is an emulator for running GameCube and Wii games on Windows,
Linux, macOS, and recent Android devices. It's licensed under the terms
of the GNU General Public License, version 2 or later (GPLv2+).

Please read the [FAQ](https://dolphin-emu.org/docs/faq/) before using Dolphin.

## System Requirements

### Android

* OS
    * Android 5.0 Lollipop or higher (SDK >= 21).
* Processor
    * A processor with support for 64-bit applications (either ARMv8 or x86-64).
* Graphics
    * A graphics processor that supports OpenGL ES 3.0 or higher. Performance varies heavily with [driver quality](https://dolphin-emu.org/blog/2013/09/26/dolphin-emulator-and-opengl-drivers-hall-fameshame/).
    * A graphics processor that supports standard desktop OpenGL features is recommended for best performance.

Dolphin can only be installed on devices that satisfy the above requirements. Attempting to install on an unsupported device will fail and display an error message.

## Building for Android

These instructions assume familiarity with Android development. If you do not have an
Android dev environment set up, see [AndroidSetup.md](AndroidSetup.md).

Make sure to pull submodules before building:
```sh
git submodule update --init
```

If using Android Studio, import the Gradle project located in `./Source/Android`.

Android apps are compiled using a build system called Gradle. Dolphin's native component,
however, is compiled using CMake. The Gradle script will attempt to run a CMake build
automatically while building the Java code.

## Uninstalling

When Dolphin has been installed with the NSIS installer, you can uninstall
Dolphin like any other Windows application.

Linux users can run `cat install_manifest.txt | xargs -d '\n' rm` as root from the build directory
to uninstall Dolphin from their system.

macOS users can simply delete Dolphin.app to uninstall it.

Additionally, you'll want to remove the global user directory (see below to
see where it's stored) if you don't plan to reinstall Dolphin.

## Command Line Usage

`Usage: Dolphin [-h] [-d] [-l] [-e <str>] [-b] [-v <str>] [-a <str>]`

* -h, --help Show this help message
* -d, --debugger Show the debugger pane and additional View menu options
* -l, --logger Open the logger
* -e, --exec=<str> Load the specified file (DOL,ELF,WAD,GCM,ISO)
* -b, --batch Exit Dolphin with emulator
* -v, --video_backend=<str> Specify a video backend
* -a, --audio_emulation=<str> Low level (LLE) or high level (HLE) audio

Available DSP emulation engines are HLE (High Level Emulation) and
LLE (Low Level Emulation). HLE is faster but less accurate whereas
LLE is slower but close to perfect. Note that LLE has two submodes (Interpreter and Recompiler)
but they cannot be selected from the command line.

Available video backends are "D3D" and "D3D12" (they are only available on Windows), "OGL", and "Vulkan".
There's also "Null", which will not render anything, and
"Software Renderer", which uses the CPU for rendering and
is intended for debugging purposes only.

## Sys Files

* `wiitdb.txt`: Wii title database from [GameTDB](https://www.gametdb.com/)
* `totaldb.dsy`: Database of symbols (for devs only)
* `GC/font_western.bin`: font dumps
* `GC/font_japanese.bin`: font dumps
* `GC/dsp_coef.bin`: DSP dumps
* `GC/dsp_rom.bin`: DSP dumps
* `Wii/clientca.pem`: Wii network certificate
* `Wii/clientcakey.pem`: Wii network certificate key
* `Wii/rootca.pem`: Wii network certificate issuer / CA

The DSP dumps included with Dolphin have been written from scratch and do not
contain any copyrighted material. They should work for most purposes, however
some games implement copy protection by checksumming the dumps. You will need
to dump the DSP files from a console and replace the default dumps if you want
to fix those issues.

Wii network certificates must be extracted from a Wii IOS. A guide for that can be found [here](https://wiki.dolphin-emu.org/index.php?title=Wii_Network_Guide).

## Folder Structure

These folders are installed read-only and should not be changed:

* `GameSettings`: per-game default settings database
* `GC`: DSP and font dumps
* `Shaders`: post-processing shaders
* `Themes`: icon themes for GUI
* `Resources`: icons that are theme-agnostic
* `Wii`: default Wii NAND contents

## User Folder Structure

List of user folders:

* `Cache`: used to cache the ISO list
* `Config`: configuration files
* `Dump`: anything dumped from Dolphin
* `GameSettings`: additional settings to be applied per-game
* `GBA`: GBA saves
* `GC`: memory cards and system BIOS
* `Load`: graphicmods, riivolution patches, custom textures, wiisdsync, wiiSD
* `Logs`: logs, if enabled
* `ScreenShots`: screenshots taken via Dolphin
* `StateSaves`: save states
* `Wii`: Wii NAND contents

## GraphicMods

GraphicMods have to be placed in the user directory under
`Load/GraphicMods/[GameID]/`. Once you have extracted the graphic mods here, you will need to enable them in settings `Settings > Graphics > Advanced > Graphic Mods`

## Riivolution Patches

Riivolution Patches have to be placed in the user directory under
`Load/Riivolution/[GameID]/`. Once you have extracted the patches here, long press on the game and select `Start with Riivolution Patches`

## Custom Textures

Custom textures have to be placed in the user directory under
`Load/Textures/[GameID]/`. You can find the Game ID by right-clicking a game
in the ISO list and selecting "ISO Properties".



