// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / Photon 1.1.17). Never packaged; see verify/api-allowlist.txt.
package com.lowdragmc.photon.client.gameobject.emitter.particle;

import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.PhysicsSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.RendererSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;

public class ParticleConfig {
    public final EmissionSetting emission = null;
    public final MaterialSetting material = null;
    public final RendererSetting.Particle renderer = null;
    public final PhysicsSetting physics = null;

    public void setDuration(int duration) { throw new UnsupportedOperationException(); }
    public void setLooping(boolean looping) { throw new UnsupportedOperationException(); }
    public void setMaxParticles(int maxParticles) { throw new UnsupportedOperationException(); }
    public void setParallelUpdate(boolean parallel) { throw new UnsupportedOperationException(); }
    public void setParallelRendering(boolean parallel) { throw new UnsupportedOperationException(); }
    public void setStartLifetime(NumberFunction lifetime) { throw new UnsupportedOperationException(); }
    public void setStartSpeed(NumberFunction speed) { throw new UnsupportedOperationException(); }
    public void setStartSize(NumberFunction3 size) { throw new UnsupportedOperationException(); }
}
