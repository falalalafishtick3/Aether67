package dev.aether.mixin;

import dev.aether.module.impl.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow private boolean thirdPerson;

    @Inject(method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void aether$freecam(World area, Entity focused, boolean tp, boolean inverse, float tickProgress, CallbackInfo ci) {
        if (!Freecam.active) return;
        Vec3d p = Freecam.prevPos.lerp(Freecam.pos, tickProgress);
        setPos(p);
        thirdPerson = true; // draws your body and unlocks the view
    }
}
