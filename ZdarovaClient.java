package dev.zdarova;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.zdarova.gui.ClickGui;
import dev.zdarova.module.BlockEsp;
import dev.zdarova.module.Module;
import dev.zdarova.module.ModuleManager;
import dev.zdarova.util.ChunkScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ZdarovaClient implements ClientModInitializer {
    private static final String PREFIX = "§7[§aZdarova§7] §f";

    @Override
    public void onInitializeClient() {
        KeyBinding guiKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.zdarova.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, "category.zdarova"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (guiKey.wasPressed()) {
                mc.setScreen(new ClickGui());
            }
            if (mc.world == null || mc.player == null) return;
            ChunkScanner.tick(mc);
            for (Module m : ModuleManager.ALL) {
                if (m.enabled) m.tick(mc);
            }
        });

        WorldRenderEvents.LAST.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            for (Module m : ModuleManager.ALL) {
                if (m.enabled) m.render(ctx, mc);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.player == null) return;
            ctx.drawTextWithShadow(mc.textRenderer, "Zdarova", 4, 4, 0xFF55FF55);
            int y = 15;
            for (Module m : ModuleManager.ALL) {
                if (!m.enabled) continue;
                ctx.drawTextWithShadow(mc.textRenderer, m.name, 4, y, 0xFFFFFFFF);
                y += 10;
            }
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (ModuleManager.anyScan()) ChunkScanner.enqueue(chunk.getPos());
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ChunkScanner.remove(chunk.getPos()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> resetAll());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> resetAll());

        registerCommands();
    }

    private static void resetAll() {
        ChunkScanner.clear();
        for (Module m : ModuleManager.ALL) m.reset();
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("zd")
                        .then(ClientCommandManager.literal("toggle")
                                .then(ClientCommandManager.argument("module", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "module");
                                            Module m = ModuleManager.byId(id);
                                            if (m == null) {
                                                reply(ctx.getSource(), "Unknown module. Ids: " + ids());
                                            } else {
                                                m.toggle();
                                                reply(ctx.getSource(), m.name + (m.enabled ? " §aenabled" : " §cdisabled"));
                                            }
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("blockesp")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("block", StringArgumentType.word())
                                                .executes(ctx -> editBlocks(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "block"), true))))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("block", StringArgumentType.word())
                                                .executes(ctx -> editBlocks(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "block"), false))))
                                .then(ClientCommandManager.literal("list").executes(ctx -> {
                                    StringBuilder sb = new StringBuilder();
                                    synchronized (BlockEsp.TARGETS) {
                                        for (Block b : BlockEsp.TARGETS) {
                                            if (sb.length() > 0) sb.append(", ");
                                            sb.append(Registries.BLOCK.getId(b).getPath());
                                        }
                                    }
                                    reply(ctx.getSource(), "Block ESP: " + sb);
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("reset").executes(ctx -> {
                                    BlockEsp.resetTargets();
                                    ChunkScanner.rescanAll();
                                    reply(ctx.getSource(), "Block ESP list reset");
                                    return 1;
                                })))));
    }

    private static int editBlocks(FabricClientCommandSource src, String raw, boolean add) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            reply(src, "Unknown block: " + raw);
            return 0;
        }
        Block block = Registries.BLOCK.get(id);
        if (add) BlockEsp.TARGETS.add(block);
        else BlockEsp.TARGETS.remove(block);
        ChunkScanner.rescanAll();
        reply(src, (add ? "Added " : "Removed ") + id.getPath());
        return 1;
    }

    private static String ids() {
        StringBuilder sb = new StringBuilder();
        for (Module m : ModuleManager.ALL) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(m.id);
        }
        return sb.toString();
    }

    private static void reply(FabricClientCommandSource src, String msg) {
        src.sendFeedback(Text.literal(PREFIX + msg));
    }
}
