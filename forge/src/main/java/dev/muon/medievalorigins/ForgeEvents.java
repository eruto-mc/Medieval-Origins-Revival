package dev.muon.medievalorigins;

import dev.muon.medievalorigins.entity.ISummon;
import dev.muon.medievalorigins.entity.SummonTracker;
import dev.muon.medievalorigins.entity.SummonedSkeleton;
import dev.muon.medievalorigins.entity.SummonedWitherSkeleton;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MedievalOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof SummonedSkeleton || entity instanceof SummonedWitherSkeleton) {
            ItemStack itemstack = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemstack.isEmpty()) {
                event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), itemstack));
                entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }

    /*
     * Keeping SummonTracker in step with the world.
     *
     * The tracker is platform-agnostic (it only knows ISummon), so the wiring lives here.
     * Upstream's 6.7.x line does the same from its own platform layer.
     *
     * Joins are tracked rather than only registered at summon time, so summons that were
     * saved with the world are commandable again after a restart.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ISummon summon) {
            SummonTracker.onEntityLoad(summon);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ISummon summon) {
            SummonTracker.untrackSummon(summon);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SummonTracker.onServerTick();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            SummonTracker.onWorldUnload();
        }
    }
}
