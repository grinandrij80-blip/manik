package com.grinandrij80blip.manik;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Per-frame animation inputs for the maniac model. */
public final class ManiacBodyRenderState extends LivingEntityRenderState {
    public float walkPos;
    public float walkSpeed;
    /** Progress of the current claw swing, 0 when idle. */
    public float attack;
    /** 0 standing .. 1 fully crouched. */
    public float crouch;
    /** True when the local player is this maniac and looks through their own eyes. */
    public boolean hiddenFromOwner;
}
