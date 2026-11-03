## 2.0.19 (HexGhoul): fix the crash while the game loads
HexKagune 2.0.19 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### Fix from your report

- **The crash while loading** ("QuinqueClient tried to access private method ItemProperties.m_174570_").
  - The quinques register their open and close model frames through a Minecraft method that is private in vanilla. Forge normally makes it public, but in your launch it wasn't.
  - This call hasn't changed since 1.3.25, so it worked in your earlier loads. I can't tell from the crash report what was different in this launch.
  - The method is now reached through Forge's own reflection helper, which works whether or not Forge made it public.
  - If even that fails, the quinques are only drawn without their open and close frames, and the game keeps loading.
- **Why the build didn't catch it:** the build checks every Minecraft name the mod uses against the official mappings, but not whether each method is public. It also can't start the game. This one compiled because the build's stand-in for Minecraft lists the method as public.
