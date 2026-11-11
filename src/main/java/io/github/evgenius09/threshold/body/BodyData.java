package io.github.evgenius09.threshold.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.evgenius09.threshold.core.Body;
import io.github.evgenius09.threshold.core.BodySlot;
import io.github.evgenius09.threshold.core.Element;
import io.github.evgenius09.threshold.core.LoadState;
import io.github.evgenius09.threshold.core.Organ;
import io.github.evgenius09.threshold.core.Skill;
import io.github.evgenius09.threshold.core.SkillState;
import io.github.evgenius09.threshold.item.OrganItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record BodyData(List<ItemStack> organs, Element selected, LoadState load, SkillState skills,
		int haloTicks, boolean seenOrgan, Map<Element, Double> damageDealt) {
	private static final int SLOTS = BodySlot.values().length;

	public BodyData {
		List<ItemStack> fixed = new ArrayList<>(SLOTS);
		for (int i = 0; i < SLOTS; i++) fixed.add(i < organs.size() ? organs.get(i) : ItemStack.EMPTY);
		organs = Collections.unmodifiableList(fixed);
		damageDealt = Map.copyOf(damageDealt);
	}

	public static final BodyData DEFAULT = new BodyData(List.of(), Element.STEEL, LoadState.EMPTY, SkillState.FULL, 0, false, Map.of());

	// The save file is untrusted (hand edits, renamed enums later): bad values are repaired, never a reason to wipe organs.
	private static final Codec<Element> ELEMENT_CODEC = Codec.STRING.xmap(
			name -> {
				Element element = parse(Element.class, name);
				return element == null ? Element.STEEL : element;
			},
			BodyData::key);

	private static final Codec<LoadState> LOAD_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("load", 0.0).forGetter(LoadState::load),
			Codec.INT.optionalFieldOf("rejection_ticks", 0).forGetter(LoadState::rejectionTicks)
	).apply(i, (load, ticks) -> new LoadState(clamp(load, LoadState.MAX), Math.clamp(ticks, 0, LoadState.REJECTION_TICKS))));

	private static final Codec<SkillState> SKILLS_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("energy", SkillState.MAX_ENERGY).forGetter(SkillState::energy),
			enumMap(Skill.class, Codec.INT).optionalFieldOf("cooldowns", Map.of()).forGetter(SkillState::cooldowns)
	).apply(i, (energy, cooldowns) -> {
		Map<Skill, Integer> positive = new EnumMap<>(Skill.class);
		cooldowns.forEach((skill, left) -> {
			if (left > 0) positive.put(skill, left);
		});
		return new SkillState(clamp(energy, SkillState.MAX_ENERGY), positive);
	}));

	public static final Codec<BodyData> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("organs", List.of()).forGetter(BodyData::organs),
			ELEMENT_CODEC.optionalFieldOf("selected", Element.STEEL).forGetter(BodyData::selected),
			LOAD_CODEC.optionalFieldOf("load", LoadState.EMPTY).forGetter(BodyData::load),
			SKILLS_CODEC.optionalFieldOf("skills", SkillState.FULL).forGetter(BodyData::skills),
			Codec.INT.optionalFieldOf("halo_ticks", 0).forGetter(BodyData::haloTicks),
			Codec.BOOL.optionalFieldOf("seen_organ", false).forGetter(BodyData::seenOrgan),
			enumMap(Element.class, Codec.DOUBLE).optionalFieldOf("damage_dealt", Map.of()).forGetter(BodyData::damageDealt)
	).apply(i, (organs, selected, load, skills, halo, seen, damage) ->
			new BodyData(organs, selected, load, skills, Math.max(0, halo), seen, damage)));

	// StreamCodec.composite stops at 6 fields and BodyData has 7; one codec also keeps save and sync in step.
	public static final StreamCodec<RegistryFriendlyByteBuf, BodyData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public Body body() {
		Map<BodySlot, Organ> result = new EnumMap<>(BodySlot.class);
		for (int i = 0; i < SLOTS; i++)
			if (organs.get(i).getItem() instanceof OrganItem item) result.put(BodySlot.values()[i], item.organ());
		return new Body(result);
	}

	private static double clamp(double value, double max) {
		return Double.isNaN(value) ? 0.0 : Math.clamp(value, 0.0, max);
	}

	private static String key(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	private static <E extends Enum<E>> E parse(Class<E> type, String name) {
		try {
			return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static <E extends Enum<E>, V> Codec<Map<E, V>> enumMap(Class<E> type, Codec<V> values) {
		return Codec.unboundedMap(Codec.STRING, values).xmap(
				raw -> {
					Map<E, V> result = new EnumMap<>(type);
					raw.forEach((name, value) -> {
						E key = parse(type, name);
						if (key != null) result.put(key, value);
					});
					return result;
				},
				map -> {
					Map<String, V> result = new HashMap<>();
					map.forEach((key, value) -> result.put(key(key), value));
					return result;
				});
	}
}
