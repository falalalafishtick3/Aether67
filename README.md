# Aether (Fabric 1.21.11 client)

Push this repo to GitHub. The workflow in `.github/workflows/build.yml` builds on every push;
download the jar from the run's **Artifacts** section (use the one WITHOUT `-sources`).

Right Shift opens the ClickGUI. Right-click a module to expand its settings.
Config saves to `config/aether.json`.

If the build fails on dependency versions, update `gradle.properties` from https://fabricmc.net/develop.
Files marked `VERSION-SENSITIVE` (EspRenderer, AetherClient, ClickGuiScreen input) may need small mapping tweaks.

## Modules
Combat: Aimbot, Triggerbot, Reach, Velocity, AutoArmor, AutoTotem, CrystalMacro, AnchorMacro, StunSlam. Movement: Freecam, Flight, Sprint, NoFall. World also has FastPlace.
Render: StorageFinder, SpawnerFinder, Visuals (fullbright / FOV / entity ESP). World: TuffChunkFinder, BaseFinder.

**Keybinds:** middle-click a module in the GUI, press a key (Backspace clears, Esc cancels).

## New: HUD + sound
- **SpotifyHud** (HUD panel): shows the song from the Spotify *desktop app*. Settings: Show (While Playing / On Song Change / Always),
  Show Duration, Position + offsets, Scale, Background Opacity, Progress, Equalizer, Accent color.
  Windows reads the Spotify window title (no progress bar). Linux needs `playerctl`. macOS uses AppleScript.
- **Startup sound**: plays on the title screen once per launch (voice + chime). Toggle/volume in ClickGUI module settings.
- Other HUD: ActiveMods, InfoHud, TargetHud, Radar, Keystrokes. More gameplay: AutoClicker, Speed, Step, Zoom.
