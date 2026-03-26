package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import us.potatoboy.invview.gui.SavingPlayerDataGui;
import us.potatoboy.invview.gui.UnmodifiableSlot;
import us.potatoboy.invview.mixin.EntityAccessor;

import java.util.Optional;

public class ViewCommand {
    private static final MinecraftServer minecraftServer = InvView.getMinecraftServer();

    private static final String permProtected = "invview.protected";
    private static final String permModify = "invview.can_modify";
    private static final String msgProtected = "Requested inventory is protected";

    public static int inv(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        ServerPlayer requestedPlayer = getRequestedPlayer(context);

        boolean canModify = Permissions.check(context.getSource(), permModify, true);

        Permissions.check(requestedPlayer.getUUID(), permProtected, false).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendFailure(Component.literal(msgProtected));
            } else {
                SimpleGui gui = new SavingPlayerDataGui(MenuType.GENERIC_9x5, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                addBackground(gui);
                for (int i = 0; i < requestedPlayer.getInventory().getContainerSize(); i++) {
                    gui.setSlot(i, canModify ? new Slot(requestedPlayer.getInventory(), i, 0, 0)
                            : new UnmodifiableSlot(requestedPlayer.getInventory(), i));
                }

                gui.open();
            }
        });

        return 1;
    }

    public static int eChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        ServerPlayer requestedPlayer = getRequestedPlayer(context);
        PlayerEnderChestContainer requestedEchest = requestedPlayer.getEnderChestInventory();

        boolean canModify = Permissions.check(context.getSource(), permModify, true);

        Permissions.check(requestedPlayer.getUUID(), permProtected, false).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendFailure(Component.literal(msgProtected));
            } else {
                MenuType<?> screenHandlerType = switch (requestedEchest.getContainerSize()) {
                    case 9 -> MenuType.GENERIC_9x1;
                    case 18 -> MenuType.GENERIC_9x2;
                    case 36 -> MenuType.GENERIC_9x4;
                    case 45 -> MenuType.GENERIC_9x5;
                    case 54 -> MenuType.GENERIC_9x6;
                    default -> MenuType.GENERIC_9x3;
                };
                SimpleGui gui = new SavingPlayerDataGui(screenHandlerType, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                addBackground(gui);
                for (int i = 0; i < requestedEchest.getContainerSize(); i++) {
                    gui.setSlot(i,
                            canModify ? new Slot(requestedEchest, i, 0, 0) : new UnmodifiableSlot(requestedEchest, i));
                }

                gui.open();
            }
        });

        return 1;
    }

//    public static int trinkets(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
//        ServerPlayerEntity player = context.getSource().getPlayer();
//        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);
//        TrinketComponent requestedComponent = TrinketsApi.getTrinketComponent(requestedPlayer).get();
//
//        boolean canModify = Permissions.check(context.getSource(), permModify, true);
//
//        Permissions.check(requestedPlayer.getUuid(), permProtected, false).thenAcceptAsync(isProtected -> {
//            if (isProtected) {
//                context.getSource().sendError(Text.literal(msgProtected));
//            } else {
//                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X2, player, requestedPlayer);
//                addBackground(gui);
//                gui.setTitle(requestedPlayer.getName());
//                int index = 0;
//                for (Map<String, TrinketInventory> group : requestedComponent.getInventory().values()) {
//                    for (TrinketInventory inventory : group.values()) {
//                        for (int i = 0; i < inventory.size(); i++) {
//                            gui.setSlotRedirect(index, canModify ? new Slot(inventory, i, 0, 0) : new UnmodifiableSlot(inventory, i));
//                            index += 1;
//                        }
//                    }
//                }
//
//                gui.open();
//            }
//        });
//
//        return 1;
//    }

//    public static int apoli(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
//        ServerPlayerEntity player = context.getSource().getPlayer();
//        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);
//
//        boolean canModify = Permissions.check(context.getSource(), permModify, true);
//
//        Permissions.check(requestedPlayer.getUuid(), permProtected, false).thenAcceptAsync(isProtected -> {
//            if (isProtected) {
//                context.getSource().sendError(Text.literal(msgProtected));
//            } else {
//                List<InventoryPower> inventories = PowerHolderComponent.getPowers(requestedPlayer,
//                        InventoryPower.class);
//                if (inventories.isEmpty()) {
//                    context.getSource().sendError(Text.literal("Requested player has no inventory power"));
//                } else {
//                    SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X5, player, requestedPlayer);
//                    gui.setTitle(requestedPlayer.getName());
//                    addBackground(gui);
//                    int index = 0;
//                    for (InventoryPower inventory : inventories) {
//                        for (int i = 0; i < inventory.size(); i++) {
//                            gui.setSlotRedirect(index, canModify ? new Slot(inventory, i, 0, 0) : new UnmodifiableSlot(inventory, i));
//                            index += 1;
//                        }
//                    }
//
//                    gui.open();
//                }
//            }
//        });
//
//        return 1;
//    }

    private static ServerPlayer getRequestedPlayer(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        NameAndId playerConfigEntry = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        ServerPlayer requestedPlayer = minecraftServer.getPlayerList().getPlayerByName(playerConfigEntry.name());

        // If player is not currently online
        if (requestedPlayer == null) {
            requestedPlayer = new ServerPlayer(minecraftServer, minecraftServer.overworld(), new GameProfile(playerConfigEntry.id(), playerConfigEntry.name()),
                    ClientInformation.createDefault());
            Optional<ValueInput> readViewOpt = minecraftServer.getPlayerList()
                .loadPlayerData(playerConfigEntry).map(playerData -> TagValueInput.create(new ProblemReporter.ScopedCollector(LogUtils.getLogger()), minecraftServer.registryAccess(), playerData));
            readViewOpt.ifPresent(requestedPlayer::load);

            // Avoids player's dimension being reset to the overworld
            if (readViewOpt.isPresent()) {
                ValueInput readView = readViewOpt.get();
                Optional<String> dimension = readView.getString("Dimension");
                
                if (dimension.isPresent()) {
                    ServerLevel world = minecraftServer.getLevel(
                            ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(dimension.get())));

                    if (world != null) {
                        ((EntityAccessor) requestedPlayer).callSetLevel(world);
                    }
                }
            }
        }

        return requestedPlayer;
    }

    private static void addBackground(SimpleGui gui) {
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setSlot(i, new GuiElementBuilder(Items.BARRIER).setName(Component.literal("")).build());
        }
    }
}
