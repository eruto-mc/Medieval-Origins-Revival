package dev.muon.medievalorigins.action;

import dev.muon.medievalorigins.Constants;
import dev.muon.medievalorigins.entity.ISummon;
import dev.muon.medievalorigins.entity.SummonTracker;
import dev.muon.medievalorigins.entity.SummonedSkeleton;
import dev.muon.medievalorigins.entity.SummonedWitherSkeleton;
import dev.muon.medievalorigins.entity.SummonedZombie;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import io.github.apace100.apoli.util.MiscUtil;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import dev.muon.medievalorigins.configuration.SummonEntityConfiguration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class SummonEntityAction extends EntityAction<SummonEntityConfiguration> {

    /** How many summons one Revenant may keep at once. Back-ported from the 6.7.x line. */
    private static final int MAX_SUMMONS = 5;

    public SummonEntityAction() {
        super(SummonEntityConfiguration.CODEC);
    }
    @Override
    public void execute(SummonEntityConfiguration configuration, Entity caster) {
        if (caster.level().isClientSide())
            return;
        ServerLevel serverWorld = (ServerLevel) caster.level();

        Optional<Entity> opt$entityToSpawn = MiscUtil.getEntityWithPassengers(
                serverWorld,
                configuration.entityType(),
                configuration.tag(),
                caster.position(),
                caster.getYRot(),
                caster.getXRot()
        );

        if (opt$entityToSpawn.isEmpty()) return;
        Entity entityToSpawn = opt$entityToSpawn.get();

        if (entityToSpawn instanceof Mob mob) {
            DifficultyInstance difficulty = serverWorld.getCurrentDifficultyAt(mob.blockPosition());
            MobSpawnType spawnType = MobSpawnType.MOB_SUMMONED;
            // but why
            ForgeEventFactory.onFinalizeSpawn(mob, serverWorld, difficulty, spawnType, null, configuration.tag());
            // mob.finalizeSpawn(serverWorld, difficulty, spawnType, null, configuration.tag());
            mob.setPersistenceRequired();
        }

        serverWorld.tryAddFreshEntityWithPassengers(entityToSpawn);
        ConfiguredEntityAction.execute(configuration.action(), entityToSpawn);

        if (entityToSpawn instanceof ISummon summon) {
;
            if (caster instanceof LivingEntity livingCaster) {
                configuration.duration().ifPresent(duration -> {
                    summon.setLifeTicks(duration);
                    summon.setIsLimitedLife(true);
                });

                if (configuration.duration().isEmpty()) {
                    summon.setIsLimitedLife(false);
                }

                summon.setOwner(livingCaster);
                summon.setOwnerID(livingCaster.getUUID());
                // The owner has to be set before the tracker sees it, so the cap is
                // applied here rather than at spawn time.
                SummonTracker.trackSummon(summon);
                manageSummonLimit(livingCaster);
            }
        }

        ConfiguredBiEntityAction.execute(configuration.biEntityAction(), caster, entityToSpawn);

        configuration.weapon().ifPresent(weapon -> {
            if (entityToSpawn instanceof ISummon summon) {
                summon.setWeapon(weapon);
            }
        });
    }

    /**
     * Keeps a Revenant to {@value #MAX_SUMMONS} summons at once.
     *
     * <p>Back-ported from the 6.7.x line. Rather than refusing the new summon, the
     * longest-spent one is dismissed, so the player always gets what they just paid bones
     * for. Which one goes is decided in this order:
     *
     * <ol>
     *   <li>a summon that was going to expire anyway, before a permanent one;</li>
     *   <li>among those, the one with the least time left;</li>
     *   <li>otherwise the weaker type (zombie before skeleton before wither skeleton).</li>
     * </ol>
     *
     * <p>The player is told which one left and where, so a summon does not simply vanish.
     */
    private static void manageSummonLimit(Entity owner) {
        Collection<ISummon> existing = SummonTracker.getSummonsForOwner(owner.getUUID());
        if (existing.size() <= MAX_SUMMONS) return;

        List<ISummon> sorted = existing.stream().sorted(SUMMON_ORDER).toList();
        ISummon toRemove = sorted.get(0);
        Mob mob = toRemove.getSelfAsMob();
        if (mob == null) {
            SummonTracker.untrackSummon(toRemove);
            return;
        }
        if (owner instanceof Player player) {
            player.displayClientMessage(
                    Component.translatable("message.medievalorigins.summon_limit_reached")
                            .append(" ")
                            .append(mob.getDisplayName()),
                    true);
        }
        mob.remove(Entity.RemovalReason.DISCARDED);
        SummonTracker.untrackSummon(toRemove);
    }

    private static final Comparator<ISummon> SUMMON_ORDER = (a, b) -> {
        LivingEntity ea = a.getLivingEntity();
        LivingEntity eb = b.getLivingEntity();
        if (ea == null || eb == null) {
            return ea == null ? -1 : 1;
        }
        if (a.isLimitedLife() != b.isLimitedLife()) {
            return a.isLimitedLife() ? -1 : 1;
        }
        if (a.isLimitedLife()) {
            return Integer.compare(a.getTicksLeft(), b.getTicksLeft());
        }
        return Integer.compare(priorityOf(ea), priorityOf(eb));
    };

    private static int priorityOf(LivingEntity entity) {
        // Higher is kept longer.
        if (entity instanceof SummonedWitherSkeleton) return 3;
        if (entity instanceof SummonedSkeleton) return 2;
        if (entity instanceof SummonedZombie) return 1;
        return 0;
    }
}
