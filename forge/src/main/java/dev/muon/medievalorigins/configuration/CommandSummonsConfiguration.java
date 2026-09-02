package dev.muon.medievalorigins.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;

/**
 * Settings for {@code medievalorigins:command_summons}.
 *
 * <p>Back-ported from the 6.7.x line, which writes it against the Fabric Apoli API and so
 * cannot be copied across. The field name ({@code command}) and its accepted values
 * ({@code sit} / {@code follow} / {@code come}) are kept identical to upstream, so the
 * power JSON stays interchangeable.
 */
public record CommandSummonsConfiguration(String command) implements IDynamicFeatureConfiguration {

    public static final Codec<CommandSummonsConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SerializableDataTypes.STRING.fieldOf("command")
                    .forGetter(CommandSummonsConfiguration::command)
    ).apply(instance, CommandSummonsConfiguration::new));
}
