package dev.muon.medievalorigins.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;

/**
 * Settings for {@code medievalorigins:modify_duration}.
 *
 * <p>Ported from the 6.7.x line, where it lives at
 * {@code common/.../action/entity/ModifyDurationAction.java} and is written against the
 * Fabric Apoli API. That branch dropped the Forge port of Origins entirely (6.7.0:
 * "Origins (Forge) is no longer supported"), so the code cannot be copied across — only
 * the shape of the data can. This is the Forge-side rewrite: a configuration record plus
 * an {@link dev.muon.medievalorigins.action.ModifyDurationAction} extending
 * {@code EntityAction}, matching how every other action in this module is built.
 *
 * <p>Field names are kept identical to upstream so the power JSON stays interchangeable.
 */
public record ModifyDurationConfiguration(float multiplier, boolean makePermanent)
        implements IDynamicFeatureConfiguration {

    public static final Codec<ModifyDurationConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CalioCodecHelper.optionalField(Codec.FLOAT, "multiplier", 1.0F)
                    .forGetter(ModifyDurationConfiguration::multiplier),
            CalioCodecHelper.optionalField(Codec.BOOL, "make_permanent", false)
                    .forGetter(ModifyDurationConfiguration::makePermanent)
    ).apply(instance, ModifyDurationConfiguration::new));
}
