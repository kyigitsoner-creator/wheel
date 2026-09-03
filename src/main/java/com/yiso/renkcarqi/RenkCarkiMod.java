package com.yiso.renkcarqi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class RenkCarkiMod implements ModInitializer {
    public static final String MOD_ID = "renk_carki";
    private static final int TICKS_15_MIN = 15 * 60 * 20;
    private static int timer = TICKS_15_MIN;
    private static int spinTicks = 0;
    private static WheelColor pendingChoice = null;

    // 12 renk: çark sırası
    private static final WheelColor[] COLORS = {
            new WheelColor("Kırmızı", MapColor.RED, "§c"),
            new WheelColor("Turuncu", MapColor.ORANGE, "§6"),
            new WheelColor("Sarı", MapColor.YELLOW, "§e"),
            new WheelColor("Lime", MapColor.LIME, "§a"),
            new WheelColor("Yeşil", MapColor.GREEN, "§2"),
            new WheelColor("Camgöbeği", MapColor.CYAN, "§3"),
            new WheelColor("Mavi", MapColor.BLUE, "§9"),
            new WheelColor("Lacivert", MapColor.PURPLE, "§1"),
            new WheelColor("Mor", MapColor.PURPLE, "§5"),
            new WheelColor("Pembe", MapColor.PINK, "§d"),
            new WheelColor("Kahverengi", MapColor.BROWN, "§4"),
            new WheelColor("Beyaz", MapColor.WHITE, "§f")
    };

    private static WheelColor banned = null;
    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        // /renkcarki ve /wheel komutlarını kaydet
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("renkcarki")
                .executes(context -> {
                    timer = 0; // Zamanlayıcıyı sıfırlayarak çarkı hemen tetikler
                    context.getSource().sendFeedback(() -> Text.literal("§a[Çark] Çark manuel olarak başlatıldı!"), false);
                    return 1;
                }));

            dispatcher.register(CommandManager.literal("wheel")
                .executes(context -> {
                    timer = 0;
                    context.getSource().sendFeedback(() -> Text.literal("§a[Çark] Çark manuel olarak başlatıldı!"), false);
                    return 1;
                }));
        });

        // Oyuncu oyuna girdikten sonra sayaç çalışır. Dünya kaydedildiğinde tekrar 15 dk'dan başlar.
        ServerTickEvents.END_SERVER_TICK.register(RenkCarkiMod::tick);

        // Yeni yüklenen chunk'larda yasak renk de temizlenir.
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (banned != null) {
                removeColorFromChunk(world, chunk, banned);
            }
        });
    }

    private static void tick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) return;

        // Çark animasyonu başladıysa yaklaşık 3 saniye boyunca actionbar'da renkleri döndür.
        if (spinTicks > 0) {
            spinTicks--;
            int index = (spinTicks * 3 + RANDOM.nextInt(3)) % COLORS.length;
            WheelColor shown = COLORS[index];
            for (var player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal("§6§l[ ÇARK DÖNÜYOR ] §r" + shown.code + shown.name), true);
            }

            if (spinTicks == 0) {
                applyChoice(server, pendingChoice);
                pendingChoice = null;
            }
            return;
        }

        timer--;
        if (timer <= 0) {
            timer = TICKS_15_MIN;
            pendingChoice = COLORS[RANDOM.nextInt(COLORS.length)];
            spinTicks = 60; // 3 saniye
        }
    }

    private static void applyChoice(MinecraftServer server, WheelColor chosen) {
        banned = chosen;

        for (ServerWorld world : server.getWorlds()) {
            for (WorldChunk chunk : getLoadedChunks(world)) {
                removeColorFromChunk(world, chunk, chosen);
            }
        }

        Text msg = Text.literal("§6§l[ ÇARK ] §r" + chosen.code + chosen.name
                + " §7rengi dünyadan silindi!");
        server.getPlayerManager().broadcast(msg, false);
    }

    private static List<WorldChunk> getLoadedChunks(ServerWorld world) {
        List<WorldChunk> result = new ArrayList<>();
        for (var player : world.getPlayers()) {
            int pcx = player.getChunkPos().x;
            int pcz = player.getChunkPos().z;
            int radius = Math.max(2, world.getServer().getPlayerManager().getViewDistance());
            for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
                for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                    WorldChunk c = world.getChunkManager().getWorldChunk(cx, cz);
                    if (c != null) result.add(c);
                }
            }
        }
        return result;
    }

    private static void removeColorFromChunk(ServerWorld world, WorldChunk chunk, WheelColor color) {
        int minX = chunk.getPos().getStartX();
        int minZ = chunk.getPos().getStartZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int bottom = world.getBottomY();
        int top = world.getTopY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = bottom; y < top; y++) {
                    BlockState state = world.getBlockState(new net.minecraft.util.math.BlockPos(x, y, z));
                    if (isTargetColor(state, color)) {
                        world.setBlockState(new net.minecraft.util.math.BlockPos(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }
    }

    private static boolean isTargetColor(BlockState state, WheelColor color) {
        MapColor map = state.getMapColor(null, null);
        if (map == color.mapColor) return true;

        String id = Registries.BLOCK.getId(state.getBlock()).getPath();
        String key = color.name.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "kırmızı" -> id.contains("red_") || id.contains("_red") || id.equals("red_wool") || id.equals("red_concrete");
            case "turuncu" -> id.contains("orange");
            case "sarı" -> id.contains("yellow");
            case "lime" -> id.contains("lime");
            case "yeşil" -> id.contains("green");
            case "camgöbeği" -> id.contains("cyan");
            case "mavi" -> id.contains("blue");
            case "lacivert" -> id.contains("light_blue") || id.contains("blue");
            case "mor" -> id.contains("purple");
            case "pembe" -> id.contains("pink");
            case "kahverengi" -> id.contains("brown");
            case "beyaz" -> id.contains("white");
            default -> false;
        };
    }

    private record WheelColor(String name, MapColor mapColor, String code) {}
}
