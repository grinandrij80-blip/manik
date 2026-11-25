package com.grinandrij80blip.manik;

import net.minecraft.util.Mth;

/** Shared easing helpers for the lively maniac animations. */
final class ManiacAnim {
    static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** Arm angle during a swing (negative = raised forward): wind-up, fast slam, recovery. */
    static float armAngle(float t, float raised, float slammed) {
        if (t <= 0.0F) return 0.0F;
        if (t < 0.25F) return Mth.lerp(ease(t / 0.25F), 0.0F, raised);
        if (t < 0.5F) return Mth.lerp(ease((t - 0.25F) / 0.25F), raised, slammed);
        return Mth.lerp(ease((t - 0.5F) / 0.5F), slammed, 0.0F);
    }

    /** Torso lean during a swing: leans back on the wind-up, lunges on the slam. */
    static float lean(float t, float back, float forward) {
        if (t <= 0.0F) return 0.0F;
        if (t < 0.25F) return Mth.lerp(ease(t / 0.25F), 0.0F, back);
        if (t < 0.5F) return Mth.lerp(ease((t - 0.25F) / 0.25F), back, forward);
        return Mth.lerp(ease((t - 0.5F) / 0.5F), forward, 0.0F);
    }

    private ManiacAnim() {}
}
