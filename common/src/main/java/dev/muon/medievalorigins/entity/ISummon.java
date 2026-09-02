package dev.muon.medievalorigins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public interface ISummon extends OwnableEntity {
    /*
    * Originally based off of Ars Nouveau, which is under the LGPL-v3.0 license
    */

    int getTicksLeft();

    default @Nullable LivingEntity getLivingEntity() {
        return this instanceof LivingEntity ? (LivingEntity) this : null;
    }
    void setLifeTicks(int lifeTicks);
    void setIsLimitedLife(boolean bool);
    void setWeapon(ItemStack item);
    void setOwner(LivingEntity owner);
    void setOwnerID(UUID uuid);
    void reassessWeaponGoal();

    /*
     * Sit / follow, back-ported from the 6.7.x line.
     *
     * FollowSummonerGoal already had a "don't follow while sitting" branch, but it asked
     * TamableAnimal — and none of these summons are tameable, so the branch never fired.
     * Upstream moved the question onto ISummon; this does the same.
     */
    void setOrderedToSit(boolean sit);

    boolean isOrderedToSit();

    /** The summon as a Mob, for navigation and teleport. Null if it somehow isn't one. */
    default Mob getSelfAsMob() {
        return this instanceof Mob ? (Mob) this : null;
    }

    @Nullable
    default UUID getOwnerUUID(){
        return null;
    }
    @Nullable
    default LivingEntity getOwner(){
        if(this instanceof LivingEntity && ((Entity) this).getCommandSenderWorld() instanceof ServerLevel serverLevel){
            return (LivingEntity) this.getOwner(serverLevel);
        }
        return null;
    }
    LivingEntity getOwnerFromID();
    @Deprecated(forRemoval = true) // Use getOwner
    default @Nullable Entity getOwner(ServerLevel world) {
        return getOwnerUUID() != null ? world.getEntity(getOwnerUUID()) : null;
    }
}