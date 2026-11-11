package io.github.evgenius09.threshold.registry;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.body.BodyData;
import io.github.evgenius09.threshold.core.LoadState;
import io.github.evgenius09.threshold.core.SkillState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public final class ModAttachments {
	public static final AttachmentType<BodyData> BODY = AttachmentRegistry.<BodyData>create(Threshold.id("body"), b -> b
			.initializer(() -> BodyData.DEFAULT)
			.persistent(BodyData.CODEC)
			.copyOnDeath()
			.syncWith(BodyData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private ModAttachments() {
	}

	public static void init() {
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// alive is true when leaving the End, which is not a death.
			if (alive) return;
			BodyData d = newPlayer.getAttachedOrCreate(BODY);
			newPlayer.setAttached(BODY, new BodyData(d.organs(), d.selected(), LoadState.EMPTY, SkillState.FULL,
					0, d.seenOrgan(), d.damageDealt()));
		});
	}
}
