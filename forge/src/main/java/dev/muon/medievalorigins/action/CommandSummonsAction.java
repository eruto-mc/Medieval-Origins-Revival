package dev.muon.medievalorigins.action;

import dev.muon.medievalorigins.configuration.CommandSummonsConfiguration;
import dev.muon.medievalorigins.entity.ISummon;
import dev.muon.medievalorigins.entity.SummonTracker;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

/**
 * Tells every summon of one owner to sit, follow again, or teleport to them.
 *
 * <p>Which summons belong to whom comes from {@link SummonTracker}. Sitting is honoured by
 * {@code FollowSummonerGoal}, which asks {@link ISummon#isOrderedToSit()}.
 *
 * <p>The message is shown above the hotbar so it does not fill the chat when a player
 * toggles repeatedly.
 */
public class CommandSummonsAction extends EntityAction<CommandSummonsConfiguration> {

    public CommandSummonsAction() {
        super(CommandSummonsConfiguration.CODEC);
    }

    @Override
    public void execute(CommandSummonsConfiguration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        Collection<ISummon> summons = SummonTracker.getSummonsForOwner(living.getUUID());
        if (summons.isEmpty()) return;

        switch (configuration.command().toLowerCase()) {
            case "sit" -> {
                for (ISummon summon : summons) {
                    summon.setOrderedToSit(true);
                    Mob mob = summon.getSelfAsMob();
                    if (mob != null) {
                        mob.getNavigation().stop();
                    }
                }
                say(entity, "message.medievalorigins.summon.sit");
            }
            case "follow" -> {
                for (ISummon summon : summons) {
                    summon.setOrderedToSit(false);
                }
                say(entity, "message.medievalorigins.summon.follow");
            }
            case "come" -> {
                for (ISummon summon : summons) {
                    Mob mob = summon.getSelfAsMob();
                    if (mob != null) {
                        mob.teleportTo(entity.getX(), entity.getY(), entity.getZ());
                    }
                }
                say(entity, "message.medievalorigins.summon.come");
            }
            default -> {
                // Unknown command: do nothing rather than guess.
            }
        }
    }

    private static void say(Entity entity, String key) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }
}
