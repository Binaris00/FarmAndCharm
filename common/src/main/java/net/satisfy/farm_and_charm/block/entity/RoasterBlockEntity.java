package net.satisfy.farm_and_charm.block.entity;

import de.cristelknight.doapi.common.world.ImplementedInventory;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.block.RoasterBlock;
import net.satisfy.farm_and_charm.client.gui.handler.RoasterGuiHandler;
import net.satisfy.farm_and_charm.item.food.EffectFood;
import net.satisfy.farm_and_charm.item.food.EffectFoodHelper;
import net.satisfy.farm_and_charm.recipe.RoasterRecipe;
import net.satisfy.farm_and_charm.registry.EntityTypeRegistry;
import net.satisfy.farm_and_charm.registry.RecipeTypeRegistry;
import net.satisfy.farm_and_charm.registry.TagRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class RoasterBlockEntity extends BlockEntity implements BlockEntityTicker<RoasterBlockEntity>, ImplementedInventory, MenuProvider {
    private static final int MAX_CAPACITY = 8, CONTAINER_SLOT = 6, OUTPUT_SLOT = 7, INGREDIENTS_AREA = 2 * 3;
    private static final int[] SLOTS_FOR_UP = new int[]{0, 1, 2, 3, 4, 5, 6};
    private static final int MAX_ROASTING_TIME = 900;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(MAX_CAPACITY, ItemStack.EMPTY);
    private int roastingTime;
    private boolean isBeingBurned;
    private final ContainerData delegate = new ContainerData() {
        public int get(int index) {
            return switch (index) {
                case 0 -> roastingTime;
                case 1 -> isBeingBurned ? 1 : 0;
                default -> 0;
            };
        }

        public void set(int index, int value) {
            switch (index) {
                case 0 -> roastingTime = value;
                case 1 -> isBeingBurned = value != 0;
            }
        }

        public int getCount() {
            return 2;
        }
    };

    public RoasterBlockEntity(BlockPos pos, BlockState state) {
        super(EntityTypeRegistry.ROASTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static int getMaxRoastingTime() {
        return MAX_ROASTING_TIME;
    }

    public int @NotNull [] getSlotsForFace(Direction side) {
        return switch (side) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> new int[]{OUTPUT_SLOT};
            default -> new int[]{CONTAINER_SLOT};
        };
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        ContainerHelper.loadAllItems(compoundTag, inventory, provider);
        roastingTime = compoundTag.getInt("RoastingTime");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, inventory, provider);
        compoundTag.putInt("RoastingTime", roastingTime);
    }

    public boolean isBeingBurned() {
        if (level == null) throw new IllegalStateException("Null world not allowed");
        BlockState belowState = level.getBlockState(worldPosition.below());
        return belowState.is(TagRegistry.ALLOWS_COOKING);
    }

    private boolean canCraft(Recipe<?> recipe, RegistryAccess access) {
        if (recipe == null || recipe.getResultItem(access).isEmpty()) return false;
        if (recipe instanceof RoasterRecipe roastingRecipe) {
            ItemStack outputSlotStack = getItem(OUTPUT_SLOT), containerSlotStack = getItem(CONTAINER_SLOT);
            boolean isContainerCorrect = containerSlotStack.is(roastingRecipe.getContainer().getItem()), isOutputSlotCompatible = outputSlotStack.isEmpty() || ItemStack.isSameItemSameComponents(outputSlotStack, generateOutputItem(recipe, access)) && outputSlotStack.getCount() < outputSlotStack.getMaxStackSize();
            return isContainerCorrect && isOutputSlotCompatible;
        }
        return false;
    }

    private void craft(Recipe<?> recipe, RegistryAccess access) {
        if (!canCraft(recipe, access)) return;
        ItemStack recipeOutput = generateOutputItem(recipe, access), outputSlotStack = getItem(OUTPUT_SLOT);
        if (outputSlotStack.isEmpty()) {
            setItem(OUTPUT_SLOT, recipeOutput);
        } else {
            outputSlotStack.grow(recipeOutput.getCount());
        }
        recipe.getIngredients().forEach(ingredient -> {
            for (int slot = 0; slot < INGREDIENTS_AREA; slot++) {
                ItemStack stack = getItem(slot);
                if (ingredient.test(stack)) {
                    ItemStack remainderStack = stack.getItem().hasCraftingRemainingItem() ? new ItemStack(Objects.requireNonNull(stack.getItem().getCraftingRemainingItem())) : ItemStack.EMPTY;
                    stack.shrink(1);
                    if (!remainderStack.isEmpty()) setItem(slot, remainderStack);
                    break;
                }
            }
        });
        ItemStack containerSlotStack = getItem(CONTAINER_SLOT);
        if (!containerSlotStack.isEmpty()) {
            containerSlotStack.shrink(1);
            if (containerSlotStack.isEmpty()) setItem(CONTAINER_SLOT, ItemStack.EMPTY);
        }
    }

    private ItemStack generateOutputItem(Recipe<?> recipe, RegistryAccess access) {
        ItemStack outputStack = recipe.getResultItem(access);
        if (outputStack.getItem() instanceof EffectFood) {
            recipe.getIngredients().forEach(ingredient -> {
                for (int slot = 0; slot < INGREDIENTS_AREA; slot++) {
                    ItemStack stack = getItem(slot);
                    if (ingredient.test(stack)) {
                        EffectFoodHelper.getEffects(stack).forEach(effect -> EffectFoodHelper.addEffect(outputStack, effect));
                        break;
                    }
                }
            });
        }
        return outputStack;
    }

    public void tick(Level world, BlockPos pos, BlockState state, RoasterBlockEntity blockEntity) {
        if (world.isClientSide()) return;
        boolean wasBeingBurned = isBeingBurned;
        isBeingBurned = isBeingBurned();
        if (wasBeingBurned != isBeingBurned || state.getValue(RoasterBlock.LIT) != isBeingBurned) {
            world.setBlock(pos, state.setValue(RoasterBlock.LIT, isBeingBurned), Block.UPDATE_ALL);
        }

        if (!isBeingBurned) {
            return;
        }

        if (level == null) throw new IllegalStateException("Null world not allowed");

        RecipeManager recipeManager = level.getRecipeManager();
        List<RecipeHolder<RoasterRecipe>> recipes = recipeManager.getAllRecipesFor(RecipeTypeRegistry.ROASTER_RECIPE_TYPE.get());
        Optional<RoasterRecipe> recipe = Optional.ofNullable(getRecipe(recipes, inventory));
        RegistryAccess access = level.registryAccess();

        if (recipe.isPresent() && canCraft(recipe.get(), access)) {
            if (++roastingTime >= MAX_ROASTING_TIME) {
                roastingTime = 0;
                craft(recipe.get(), access);
            }
            if (!state.getValue(RoasterBlock.ROASTING)) {
                world.setBlock(pos, state.setValue(RoasterBlock.ROASTING, true), Block.UPDATE_ALL);
            }
        } else {
            roastingTime = 0;
            if (state.getValue(RoasterBlock.ROASTING)) {
                world.setBlock(pos, state.setValue(RoasterBlock.ROASTING, false), Block.UPDATE_ALL);
            }
        }
    }


    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public @NotNull Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new RoasterGuiHandler(syncId, inv, this, delegate);
    }

    private RoasterRecipe getRecipe(List<RecipeHolder<RoasterRecipe>> recipes, NonNullList<ItemStack> inventory) {
        recipeLoop:
        for (RecipeHolder<RoasterRecipe> recipeHolder : recipes) {
            RoasterRecipe recipe = recipeHolder.value();
            for (Ingredient ingredient : recipe.getIngredients()) {
                boolean ingredientFound = false;
                for (int slotIndex = 1; slotIndex < inventory.size(); slotIndex++) {
                    ItemStack slotItem = inventory.get(slotIndex);
                    if (ingredient.test(slotItem)) {
                        ingredientFound = true;
                        break;
                    }
                }
                if (!ingredientFound) {
                    continue recipeLoop;
                }
            }
            return recipe;
        }
        return null;
    }
}
