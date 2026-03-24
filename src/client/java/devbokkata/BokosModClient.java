package devbokkata;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.stream.StreamSupport;

public class BokosModClient implements ClientModInitializer {

	//Keybinds category
	private static final KeyBinding.Category CATEGORY = new KeyBinding.Category(
			Identifier.of("auto_aim_mod", "main")
	);

	//KeyBinds
	private static KeyBinding holdKey;
	private static KeyBinding toggleKey;

	private static boolean isToggledOn = false;

	@Override
	public void onInitializeClient() {

		holdKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auto_aim_mod.hold",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				CATEGORY
		));

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auto_aim_mod.toggle",
				InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(),
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null || client.player == null) return;

			while (toggleKey.wasPressed()) {
				isToggledOn = !isToggledOn;
				// Send a small message above the hotbar
				String status = isToggledOn ? "§aENABLED" : "§cDISABLED";
				client.player.sendMessage(Text.literal("Auto-Aim: " + status), true);
			}

			if (holdKey.isPressed() || isToggledOn) {
				LivingEntity target = findClosestTarget(client);
				if (target != null) {
					snapToTarget(client.player, target);
				}
			}
		});
	}

	private LivingEntity findClosestTarget(MinecraftClient client) {
		if (client.world == null) return null;

		return StreamSupport.stream(client.world.getEntities().spliterator(), false)
				.filter(entity -> entity instanceof LivingEntity)

				/*
				* For testing on everything:
				* .filter(entity -> entity instanceof net.minecraft.entity.player.PlayerEntity)
				* */
				.map(entity -> (LivingEntity) entity)
				.filter(entity -> entity != client.player && entity.isAlive())
				.min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(client.player)))
				.orElse(null);
	}

	private void snapToTarget(ClientPlayerEntity player, LivingEntity target) {
		Vec3d targetPos = target.getBoundingBox().getCenter();
		Vec3d playerPos = player.getEyePos();

		double dx = targetPos.x - playerPos.x;
		double dy = targetPos.y - playerPos.y;
		double dz = targetPos.z - playerPos.z;
		double dh = Math.sqrt(dx * dx + dz * dz);

		float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float targetPitch = (float) (-(Math.atan2(dy, dh) * 180.0D / Math.PI));

		// 0.45f is the "Stickiness". 1.0f is instant snap, 0.1f is very slow.
		// This allows the spear to "track" the target during the jab animation.
		float smoothYaw = lerpAngle(player.getYaw(), targetYaw, 0.45f);
		float smoothPitch = lerpAngle(player.getPitch(), targetPitch, 0.45f);

		player.setYaw(smoothYaw);
		player.setPitch(smoothPitch);
	}

	// Helper method to handle the 360-degree wrap-around (prevents the head-spin bug)
	private float lerpAngle(float start, float end, float pct) {
		float d = end - start;
		while (d < -180) d += 360;
		while (d >= 180) d -= 360;
		return start + d * pct;
	}
}