package com.lowdragmc.mbd2.common.item;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SearchComponentWidget;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.network.MBD2Network;
import com.lowdragmc.mbd2.common.network.packets.SPatternErrorPosPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MBDGadgetsItem extends Item implements HeldItemUIFactory.IHeldItemUIHolder {

    public MBDGadgetsItem() {
        super(new Item.Properties()
                .fireResistant()
                .stacksTo(1));
    }

    public boolean isMultiblockBuilder(ItemStack stack) {
        return stack.getDamageValue() == 0;
    }

    public boolean isRecipeDebugger(ItemStack stack) {
        return stack.getDamageValue() == 1;
    }

    public boolean isMultiblockDebugger(ItemStack stack) {
        return stack.getDamageValue() == 2;
    }

    @Nullable
    public ResourceLocation getRecipe(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null ? (tag.contains("recipe") ? new ResourceLocation(tag.getString("recipe")) : null) : null;
    }

    public void setRecipe(ItemStack stack, ResourceLocation recipe) {
        stack.getOrCreateTag().putString("recipe", recipe.toString());
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        var id = super.getDescriptionId(pStack);
        if (isMultiblockBuilder(pStack)) {
            return id + ".multiblock_builder";
        } else if (isRecipeDebugger(pStack)) {
            return id + ".recipe_debugger";
        } else if (isMultiblockDebugger(pStack)) {
            return id + ".multiblock_debugger";
        }
        return id;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, components, isAdvanced);
        components.add(Component.translatable(getDescriptionId() + ".tooltip"));
        var id = getDescriptionId(stack);
        if (isMultiblockBuilder(stack))
            components.add(Component.translatable(id + ".tooltip"));
        else if (isRecipeDebugger(stack)) {
            components.add(Component.translatable(id + ".tooltip.0"));
            components.add(Component.translatable(id + ".tooltip.1"));
            var recipe = getRecipe(stack);
            if (recipe != null) {
                components.add(Component.translatable(id + ".tooltip.2", recipe.toString()));
            }
        } else if (isMultiblockDebugger(stack)) {
            components.add(Component.translatable(id + ".tooltip"));
        }
    }

    private boolean isUsed;

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        var stack = pPlayer.getItemInHand(pUsedHand);
        if (isUsed) {
            isUsed = false;
            return InteractionResultHolder.success(stack);
        }
        if (pPlayer.isCrouching()) {
            if (isMultiblockBuilder(stack)) {
                stack.setDamageValue(1);
                return InteractionResultHolder.success(stack);
            } else if (isRecipeDebugger(stack)) {
                stack.setDamageValue(2);
                return InteractionResultHolder.success(stack);
            } else if (isMultiblockDebugger(stack)) {
                stack.setDamageValue(0);
                return InteractionResultHolder.success(stack);
            }
        } else if (pPlayer instanceof ServerPlayer serverPlayer && isRecipeDebugger(stack)) {
            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, pUsedHand);
            return InteractionResultHolder.success(stack);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCrouching()) {
            if (isMultiblockBuilder(stack)) {
                var controller = IMultiController.ofController(player.level(), context.getClickedPos()).orElse(null);
                if (controller != null) {
                    controller.getPattern().autoBuild(player,
                            new MultiblockState(player.level(), context.getClickedPos()));
                    isUsed = true;
                    return InteractionResult.SUCCESS;
                }
            } else if (isMultiblockDebugger(stack)) {
                var controller = IMultiController.ofController(player.level(), context.getClickedPos()).orElse(null);
                if (controller != null) {
                    if (controller.isFormed()) {
                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.is_formed"));
                    } else if (controller.checkPatternWithLock()) {
                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.success"));
                        if (controller instanceof MBDMultiblockMachine multiblock && multiblock.getDefinition().multiblockSettings().catalyst().isEnable()) {
                            if (!multiblock.getDefinition().multiblockSettings().catalyst().getFilterItems().isEmpty()) {
                                var items = Component.literal("[");
                                for (ItemStack filterItem : multiblock.getDefinition().multiblockSettings().catalyst().getFilterItems()) {
                                    items.append(filterItem.getDisplayName()).append(Component.literal(", "));
                                }
                                items.append(Component.literal("]"));
                                serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.catalyst.items", items));
                            }
                            if (!multiblock.getDefinition().multiblockSettings().catalyst().getFilterTags().isEmpty()) {
                                var tags = Component.literal("[");
                                for (ResourceLocation filterTag : multiblock.getDefinition().multiblockSettings().catalyst().getFilterTags()) {
                                    tags.append(Component.literal(filterTag.toString())).append(Component.literal(", "));
                                }
                                tags.append(Component.literal("]"));
                                serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.catalyst.tags", tags));
                            }
                        }
                    } else {
                        var error = controller.getMultiblockState().error;
                        if (error != null) {
                            MBD2Network.NETWORK.sendToPlayer(new SPatternErrorPosPacket(error.getPos()), serverPlayer);
                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.failure.error.pos", error.getPos()));
                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.failure.error.info", error.getErrorInfo()));
                            serverPlayer.sendSystemMessage(error.getErrorInfo());
                        } else {
                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.failure.no_error"));
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (isRecipeDebugger(stack) && getRecipe(stack) != null && serverPlayer.getServer() != null) {
                var machine = IMachine.ofMachine(player.level(), context.getClickedPos()).orElse(null);
                if (machine != null) {
                    var recipe = getRecipe(stack);
                    var recipeManager = serverPlayer.getServer().getRecipeManager();
                    for (MBDRecipeType recipeType : MBDRegistries.RECIPE_TYPES) {
                        for (MBDRecipe mbdRecipe : recipeManager.getAllRecipesFor(recipeType)) {
                            if (Objects.equals(mbdRecipe.id, recipe)) {
                                if (machine.getRecipeType() != recipeType) {
                                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.warning.recipe_type",
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                    Component.literal(machine.getRecipeType().toString())))),
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                    Component.literal(mbdRecipe.id.toString()))))
                                    ));
                                }
                                var result = mbdRecipe.matchRecipe(machine);
                                if (result.isSuccess()) {
                                    result = mbdRecipe.matchTickRecipe(machine);
                                    if (result.isSuccess()) {
                                        result = mbdRecipe.checkConditions(machine.getRecipeLogic());
                                    }
                                }
                                if (result.isSuccess()) {
                                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.raw.success",
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                            Component.literal(mbdRecipe.id.toString()))))));
                                    var modifiedRecipe = machine.doModifyRecipe(mbdRecipe);
                                    if (modifiedRecipe == mbdRecipe) {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.empty"));
                                    } else if (modifiedRecipe == null) {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.failure.0"));
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.failure.1"));
                                    } else {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.has"));
                                        result = modifiedRecipe.matchRecipe(machine);
                                        if (result.isSuccess()) {
                                            result = modifiedRecipe.matchTickRecipe(machine);
                                            if (result.isSuccess()) {
                                                result = modifiedRecipe.checkConditions(machine.getRecipeLogic());
                                            }
                                        }
                                        if (result.isSuccess()) {
                                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.success"));
                                        } else {
                                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.failure.0"));
                                            if (result.reason() != null) {
                                                serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.failure.reason").append(result.reason().get()));
                                            }
                                        }
                                    }
                                    isUsed = true;
                                    return InteractionResult.SUCCESS;
                                } else {
                                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.raw.failure.0",
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                    Component.literal(mbdRecipe.id.toString()))))));
                                    if (result.reason() != null) {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.failure.reason").append(result.reason().get()));
                                    }
                                }
                                isUsed = true;
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                    isUsed = true;
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public ModularUI createUI(Player entityPlayer, HeldItemUIFactory.HeldItemHolder holder) {
        var x = (200 - 150) / 2;
        var y = (50 - 10) / 2;
        var searchComponent = new SearchComponentWidget<>(x, y, 150, 10,
                new SearchComponentWidget.IWidgetSearch<ResourceLocation>() {
                    @Override
                    public String resultDisplay(ResourceLocation value) {
                        return value.toString();
                    }

                    @Override
                    public void selectResult(ResourceLocation value) {
                        setRecipe(holder.getHeld(), value);
                    }

                    @Override
                    public void search(String word, Consumer<ResourceLocation> find) {
                        if (Platform.getMinecraftServer() != null) {
                            var recipeManager = Platform.getMinecraftServer().getRecipeManager();
                            for (MBDRecipeType recipeType : MBDRegistries.RECIPE_TYPES) {
                                if (Thread.currentThread().isInterrupted()) return;
                                for (var recipe : recipeManager.getAllRecipesFor(recipeType)) {
                                    if (recipe.id.toString().contains(word.toLowerCase())) {
                                        find.accept(recipe.id);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void serialize(ResourceLocation value, FriendlyByteBuf buf) {
                        buf.writeUtf(value.toString());
                    }

                    @Override
                    public ResourceLocation deserialize(FriendlyByteBuf buf) {
                        return new ResourceLocation(buf.readUtf());
                    }
                }, true);
        var currentRecipe = getRecipe(holder.getHeld());
        searchComponent.setShowUp(true);
        searchComponent.setCapacity(5);
        var textFieldWidget = searchComponent.textFieldWidget;
        textFieldWidget.setCurrentString(currentRecipe == null ? "" : currentRecipe.toString());
        return new ModularUI(200, 50, holder, entityPlayer)
                .background(ResourceBorderTexture.BORDERED_BACKGROUND)
                .widget(searchComponent)
                .widget(new ImageWidget(x, y - 12, 150, 10, new TextTexture("item.mbd2.mbd_gadgets.recipe_debugger.recipe_id").setWidth(150)));
    }
}
