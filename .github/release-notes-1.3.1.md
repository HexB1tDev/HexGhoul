HexKagune 1.3.1 for Forge 1.20.1. Install on client and server (protocol 54). Fix for 1.3.0.

- Fixed the crash when Eto (the Library) came into view: her renderer was missing in some setups. It is now registered twice, and if it is still missing when she loads, it is registered again; failing that she is hidden on that client instead of crashing the game.
