package com.yiso.renkcarqi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Random;

public class RenkCarkiMod implements ModInitializer {
    public static final String MOD_ID = "renk_carki";
    private static final int TICKS_15_MIN = 15 * 60 * 20;
    private static int timer = TICKS_15_MIN;
    private static int spinTicks = 0;

    private static WheelColor activeTarget = null;
    private static int currentRadiusStep = -30;

    private static final WheelColor[] COLORS = {
            new WheelColor("Kırmızı", "§c"),
            new WheelColor("Turuncu", "§6"),
            new WheelColor("Sarı", "§e"),
            new WheelColor("Lime", "§a"),
            new WheelColor("Yeşil", "§2"),
            new WheelColor("Camgöbeği", "§3"),
            new WheelColor("Mavi", "§9"),
            new WheelColor("Lacivert", "§1"),
            new WheelColor("Mor", "§5"),
            new WheelColor("Pembe", "§d"),
            new WheelColor("Kahverengi", "§4"),
            new WheelColor("Beyaz", "§f")
    };

    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("renkcarki")
                .executes(context -> {
                    startSpin();
                    context.getSource().sendFeedback(() -> Text.literal("§a[Çark] Çark başlatıldı!"), false);
                    return 1;
                }));

            dispatcher.register(CommandManager.literal("wheel")
                .executes(context -> {
                    startSpin();
                    context.getSource().sendFeedback(() -> Text.literal("§a[Çark] Çark başlatıldı!"), false);
                    return 1;
                }));
        });

        ServerTickEvents.END_SERVER_TICK.register(RenkCarkiMod::tick);
    }

    private static void startSpin() {
        if (spinTicks <= 0 && activeTarget == null) {
            timer = TICKS_15_MIN;
            activeTarget = COLORS[RANDOM.nextInt(COLORS.length)];
            spinTicks = 60;
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) return;

        if (spinTicks > 0) {
            spinTicks--;
            WheelColor shown = (spinTicks == 0) ? activeTarget : COLORS[RANDOM.nextInt(COLORS.length)];

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal("§6§l[ ÇARK DÖNÜYOR ] §r" + shown.code + shown.name), true);
            }

            if (spinTicks == 0) {
                Text msg = Text.literal("§6§l[ ÇARK ] §r" + activeTarget.code + activeTarget.name
                        + " §7rengi etrafınızdan siliniyor!");
                server.getPlayerManager().broadcast(msg, false);
                currentRadiusStep = -30;
            }
            return;
        }

        if (activeTarget != null && currentRadiusStep <= 30) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                cleanSliceAroundPlayer(player, activeTarget, currentRadiusStep);
            }
            currentRadiusStep++;
            if (currentRadiusStep > 30) {
                activeTarget = null;
            }
            return;
        }

        timer--;
        if (timer <= 0) {
            startSpin();
        }
    }

    private static void cleanSliceAroundPlayer(ServerPlayerEntity player, WheelColor color, int xOffset) {
        ServerWorld world = player.getServerWorld();
        BlockPos center = player.getBlockPos();

        // Dikey alanı -30 ile +30 yaparak genişlettik
        for (int y = -30; y <= 30; y++) {
            for (int z = -30; z <= 30; z++) {
                BlockPos targetPos = center.add(xOffset, y, z);
                BlockState state = world.getBlockState(targetPos);
                if (!state.isAir() && isTargetColor(state, color)) {
                    world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    private static boolean isTargetColor(BlockState state, WheelColor color) {
        // Bloğun tam kayıtsı kimliğini alıyoruz (Örn: minecraft:red_wool -> red_wool)
        String fullId = Registries.BLOCK.getId(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        String key = color.name.toLowerCase(Locale.ROOT);

        return switch (key) {
            case "kırmızı" -> fullId.contains("red");
            case "turuncu" -> fullId.contains("orange");
            case "sarı" -> fullId.contains("yellow");
            case "lime" -> fullId.contains("lime");
            case "yeşil" -> fullId.contains("green") && !fullId.contains("lime");
            case "camgöbeği" -> fullId.contains("cyan");
            case "mavi" -> (fullId.contains("blue") || fullId.contains("lapis")) && !fullId.contains("light_blue");
            case "lacivert" -> fullId.contains("light_blue");
            case "mor" -> fullId.contains("purple");
            case "pembe" -> fullId.contains("pink");
            case "kahverengi" -> fullId.contains("brown");
            case "beyaz" -> fullId.contains("white");
            default -> false;
        };
    }

    private record WheelColor(String name, String code) {}
}
