package dev.muon.medievalorigins.action;

import dev.muon.medievalorigins.configuration.ModifyDurationConfiguration;
import dev.muon.medievalorigins.entity.ISummon;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;

/**
 * Extends (or removes) the lifetime of a summon.
 *
 * <p>{@code ISummon} already carries {@code getTicksLeft} / {@code setLifeTicks} /
 * {@code setIsLimitedLife} on this branch, so only the action itself was missing.
 *
 * <p>Pairs with the {@code duration} field of {@code medievalorigins:summon_entity}:
 * the summon starts with a limited life, and Putrid Communion multiplies it or lifts the
 * limit entirely.
 */
public class ModifyDurationAction extends EntityAction<ModifyDurationConfiguration> {

    public ModifyDurationAction() {
        super(ModifyDurationConfiguration.CODEC);
    }

    @Override
    public void execute(ModifyDurationConfiguration configuration, Entity entity) {
        if (!(entity instanceof ISummon summon)) return;
        if (configuration.makePermanent()) {
            summon.setIsLimitedLife(false);
            return;
        }
        summon.setLifeTicks((int) (summon.getTicksLeft() * configuration.multiplier()));
    }
}
