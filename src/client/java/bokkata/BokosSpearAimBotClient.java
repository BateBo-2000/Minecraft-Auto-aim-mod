package bokkata;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.stream.StreamSupport;

public class BokosSpearAimBotClient implements ClientModInitializer {

	private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
			Identifier.fromNamespaceAndPath("auto_aim_mod", "main")
	);

	private static KeyMapping holdKey;
	private static KeyMapping toggleKey;

	private static boolean isToggledOn = false;

	@Override
	public void onInitializeClient() {

		holdKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.auto_aim_mod.hold",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				CATEGORY
		));

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.auto_aim_mod.toggle",
				InputConstants.UNKNOWN.getValue(),
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null || client.player == null) return;

			while (toggleKey.consumeClick()) {
				isToggledOn = !isToggledOn;
				String status = isToggledOn ? "§aENABLED" : "§cDISABLED";
				// Text.literal is now Component.literal
				client.player.sendOverlayMessage(Component.literal("Auto-Aim: " + status));
			}

			if (holdKey.isDown() || isToggledOn) {
				LivingEntity target = findClosestTarget(client);
				if (target != null) {
					snapToTarget(client.player, target);
				}
			}
		});
	}

	private LivingEntity findClosestTarget(Minecraft client) {
		if (client.level == null) return null;

		return StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
				.filter(entity -> entity instanceof Player)
				.map(entity -> (LivingEntity) entity)
				.filter(entity -> entity != client.player && entity.isAlive())
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(client.player)))
				.orElse(null);
	}

	private void snapToTarget(LocalPlayer player, LivingEntity target) {
		// Vec3d is now Vec3, getEyePos is now getEyePosition
		Vec3 targetPos = target.getBoundingBox().getCenter();
		Vec3 playerPos = player.getEyePosition();

		double dx = targetPos.x - playerPos.x;
		double dy = targetPos.y - playerPos.y;
		double dz = targetPos.z - playerPos.z;
		double dh = Math.sqrt(dx * dx + dz * dz);

		float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float targetPitch = (float) (-(Math.atan2(dy, dh) * 180.0D / Math.PI));

		// getYaw/getPitch are now getYRot/getXRot
		float smoothYaw = lerpAngle(player.getYRot(), targetYaw, 0.45f);
		float smoothPitch = lerpAngle(player.getXRot(), targetPitch, 0.45f);

		player.setYRot(smoothYaw);
		player.setXRot(smoothPitch);
	}

	private float lerpAngle(float start, float end, float pct) {
		float d = end - start;
		while (d < -180) d += 360;
		while (d >= 180) d -= 360;
		return start + d * pct;
	}
}