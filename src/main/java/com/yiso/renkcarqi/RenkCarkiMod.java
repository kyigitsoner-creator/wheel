package com.yiso.renkcarqi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
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
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class RenkCarkiMod implements ModInitializer {
    public static final String MOD_ID = "renk_carki";
    private static final int TICKS_15_MIN = 15 * 60 * 20;
    private static int timer = TICKS_15_MIN;
    private static int spinTicks = 0;
    
    private static WheelColor targetChoice = null;
    private static final Queue<BlockPosTask> removalQueue = new LinkedList<>();
    private static final int BLOCKS_PER_TICK = 50; // Çökmeyi önlemek için güvenli limit

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

    private static WheelColor banned = null;
    private static final Random RANDOM = new Random();

    private record BlockPosTask(ServerWorld world, BlockPos pos) {}

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

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (banned != null) {
                scanChunkForRemoval(world, chunk, banned);
            }
        });
    }

    private static void startSpin() {
        if (spinTicks <= 0) {
            timer = TICKS_15_MIN;
            targetChoice = COLORS[RANDOM.nextInt(COLORS.length)];
            spinTicks = 60; // 3 saniye
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) return;

        // 1. Kademeli ve Güvenli Blok Silme
        int processed = 0;
        while (!removalQueue.isEmpty() && processed < BLOCKS_PER_TICK) {
            BlockPosTask task = removalQueue.poll();
            if (task.world() != null && task.pos() != null) {
                BlockState current = task.world().getBlockState(task.pos());
                if (!current.isAir()) {
                    task.world().setBlockState(task.pos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
            processed++;
        }

        // 2. Çark Dönme Animasyonu
        if (spinTicks > 0) {
            spinTicks--;
            
            WheelColor shown = (spinTicks == 0) ? targetChoice : COLORS[RANDOM.nextInt(COLORS.length)];
            
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal("§6§l[ ÇARK DÖNÜYOR ] §r" + shown.code + shown.name), true);
            }

            if (spinTicks == 0) {
                applyChoice(server, targetChoice);
            }
            return;
        }

        // 3. Otomatik Zamanlayıcı
        timer--;
        if (timer <= 0) {
            startSpin();
        }
    }

    private static void applyChoice(MinecraftServer server, WheelColor chosen) {
        banned = chosen;

        for (ServerWorld world : server.getWorlds()) {
            for (WorldChunk chunk : getLoadedChunks(world)) {
                scanChunkForRemoval(world, chunk, chosen);
            }
        }

        Text msg = Text.literal("§6§l[ ÇARK ] §r" + chosen.code + chosen.name
                + " §7rengi dünyadan siliniyor!");
        server.getPlayerManager().broadcast(msg, false);
    }

    private static List<WorldChunk> getLoadedChunks(ServerWorld world) {
        List<WorldChunk> result = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            int pcx = player.getChunkPos().x;
            int pcz = player.getChunkPos().z;
            int radius = 2; // Sadece oyuncunun yakını
            for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
                for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                    WorldChunk c = world.getChunkManager().getWorldChunk(cx, cz);
                    if (c != null) result.add(c);
                }
            }
        }
        return result;
    }

    private static void scanChunkForRemoval(ServerWorld world, WorldChunk chunk, WheelColor color) {
        int minX = chunk.getPos().getStartX();
        int minZ = chunk.getPos().getStartZ();

        // Oyuncuların konumunu alıp taramayı sadece oyuncunun etrafındaki Y yüksekliğine sınırlıyoruz
        for (ServerPlayerEntity player : world.getPlayers()) {
            int playerY = player.getBlockPos().getY();
            int minY = Math.max(world.getBottomY(), playerY - 15);
            int maxY = Math.min(world.getTopY(), playerY + 15);

            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(pos);
                        if (!state.isAir() && isTargetColor(state, color)) {
                            removalQueue.add(new BlockPosTask(world, pos));
                        }
                    }
                }
            }
        }
    }

    private static boolean isTargetColor(BlockState state, WheelColor color) {
        String id = Registries.BLOCK.getId(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        String key = color.name.toLowerCase(Locale.ROOT);

        return switch (key) {
            case "kırmızı" -> id.contains("red");
            case "turuncu" -> id.contains("orange");
            case "sarı" -> id.contains("yellow");
            case "lime" -> id.contains("lime");
            case "yeşil" -> id.contains("green") && !id.contains("lime");
            case "camgöbeği" -> id.contains("cyan");
            case "mavi" -> id.contains("blue") && !id.contains("light_blue");
            case "lacivert" -> id.contains("light_blue");
            case "mor" -> id.contains("purple");
            case "pembe" -> id.contains("pink");
            case "kahverengi" -> id.contains("brown");
            case "beyaz" -> id.contains("white");
            default -> false;
        };
    }

    private record WheelColor(String name, String code) {}
}
