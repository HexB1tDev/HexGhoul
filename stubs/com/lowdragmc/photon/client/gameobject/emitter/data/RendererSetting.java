// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / Photon 1.1.17). Never packaged; see verify/api-allowlist.txt.
package com.lowdragmc.photon.client.gameobject.emitter.data;

public class RendererSetting {
    public void setBloomEffect(boolean bloom) { throw new UnsupportedOperationException(); }
    public Cull getCull() { throw new UnsupportedOperationException(); }

    public static class Cull extends ToggleGroup {
    }

    public static class Particle extends RendererSetting {
    }
}
