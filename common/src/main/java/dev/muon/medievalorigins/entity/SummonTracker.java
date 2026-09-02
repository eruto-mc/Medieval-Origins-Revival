package dev.muon.medievalorigins.entity;

import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which summons belong to which owner, so a power can address all of them at once.
 *
 * <p>Back-ported from the 6.7.x line. That branch targets Fabric Apoli under Sinytra
 * Connector and cannot be copied wholesale, but this class touches neither — it only
 * knows {@link ISummon} — so it is carried across as-is. The platform hooks
 * ({@link #onEntityLoad}, {@link #onServerTick}, {@link #onWorldUnload}) are wired from
 * Forge events in {@code ModEvents}.
 *
 * <p>Entries are dropped for dead or owner-less summons every {@value #CLEANUP_INTERVAL}
 * ticks, so a summon that expires or is killed stops being commanded.
 */
public class SummonTracker {
    private static final Map<UUID, Set<ISummon>> OWNER_TO_SUMMONS = new ConcurrentHashMap<>();
    private static final int CLEANUP_INTERVAL = 200;
    private static int cleanupTicks = 0;

    public static void onEntityLoad(ISummon summon) {
        trackSummon(summon);
    }

    public static void onServerTick() {
        if (++cleanupTicks >= CLEANUP_INTERVAL) {
            cleanupTicks = 0;
            cleanupInvalidSummons();
        }
    }

    public static void onWorldUnload() {
        OWNER_TO_SUMMONS.clear();
    }

    public static void trackSummon(ISummon summon) {
        UUID ownerID = summon.getOwnerUUID();
        if (ownerID != null) {
            OWNER_TO_SUMMONS.computeIfAbsent(ownerID, k -> ConcurrentHashMap.newKeySet())
                    .add(summon);
        }
    }

    public static void untrackSummon(ISummon summon) {
        UUID ownerID = summon.getOwnerUUID();
        if (ownerID == null) return;
        Set<ISummon> summons = OWNER_TO_SUMMONS.get(ownerID);
        if (summons == null) return;
        summons.remove(summon);
        if (summons.isEmpty()) {
            OWNER_TO_SUMMONS.remove(ownerID);
        }
    }

    public static Collection<ISummon> getSummonsForOwner(UUID ownerID) {
        return OWNER_TO_SUMMONS.getOrDefault(ownerID, Collections.emptySet());
    }

    public static void cleanupInvalidSummons() {
        OWNER_TO_SUMMONS.values().forEach(summons ->
                summons.removeIf(summon -> {
                    LivingEntity entity = summon.getLivingEntity();
                    return entity == null || !entity.isAlive() || summon.getOwnerUUID() == null;
                })
        );
    }
}
