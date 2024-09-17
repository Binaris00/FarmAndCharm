package net.satisfy.farm_and_charm.block.entity;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.satisfy.farm_and_charm.block.MincerBlock;
import net.satisfy.farm_and_charm.recipe.MincerRecipe;
import net.satisfy.farm_and_charm.registry.EntityTypeRegistry;
import net.satisfy.farm_and_charm.registry.RecipeTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class MincerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, BlockEntityTicker<MincerBlockEntity> {
    public final int SLOT_COUNT = 2;
    public final int INPUT_SLOT = 0;
    public final int OUTPUT_SLOT = 1;

    private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public MincerBlockEntity(BlockPos position, BlockState state) {
        super(EntityTypeRegistry.MINCER_BLOCK_ENTITY.get(), position, state);
    }

    public static void spawnItem(Level world, ItemStack stack, int speed, Direction side, Position pos) {
        double d = pos.x();
        double e = pos.y();
        double f = pos.z();
        if (side.getAxis() == Direction.Axis.Y) {
            e -= 0.125;
        } else {
            e -= 0.15625;
        }

        ItemEntity itemEntity = new ItemEntity(world, d, e, f, stack);
        double g = world.random.nextDouble() * 0.1 + 0.2;
        itemEntity.setDeltaMovement(world.random.triangle((double) side.getStepX() * g, 0.0172275 * (double) speed), world.random.triangle(0.2, 0.0172275 * (double) speed), world.random.triangle((double) side.getStepZ() * g, 0.0172275 * (double) speed));
        world.addFreshEntity(itemEntity);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        if (!this.tryLoadLootTable(compoundTag))
            this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.stacks, provider);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        if (!this.trySaveLootTable(compoundTag))
            ContainerHelper.saveAllItems(compoundTag, this.stacks, provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.stacks)
            if (!itemstack.isEmpty())
                return false;
        return true;
    }

    @Override
    public @NotNull Component getDefaultName() {
        return Component.literal("mincer");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory);
    }

    private void dropItemsInOutputSlot(Level level, BlockPos pos, BlockState state, MincerBlockEntity mincer) {

        Direction direction = state.getValue(MincerBlock.FACING).getClockWise();

        if (!level.isClientSide() && !this.stacks.get(OUTPUT_SLOT).isEmpty()) {
            ItemStack droppedStack = new ItemStack(mincer.stacks.get(OUTPUT_SLOT).getItem());
            droppedStack.setCount(mincer.stacks.get(OUTPUT_SLOT).getCount());
            this.stacks.set(OUTPUT_SLOT, ItemStack.EMPTY);

            Vec3 vec3d = Vec3.atCenterOf(pos);
            Vec3 vec3d2 = vec3d.relative(direction, 0.7);
            ((ServerLevel) level).sendParticles(ParticleTypes.SPIT, vec3d2.x(), vec3d2.y(), vec3d2.z(), 3, 0.2, 0.1, 0, 0.1);
            spawnItem(level, droppedStack, 6, direction, vec3d2);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        ItemStack inputSlotItemStack = this.stacks.get(INPUT_SLOT);
        return (index == INPUT_SLOT) && inputSlotItemStack.isEmpty() || (stack.is(inputSlotItemStack.getItem()) && (inputSlotItemStack.getCount() < inputSlotItemStack.getMaxStackSize()));
    }

    @Override
    public @NotNull ItemStack getItem(int index) {
        return this.getItems().get(index);
    }

    @Override
    public @NotNull NonNullList<ItemStack> getItems() {
        return this.stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public int @NotNull [] getSlotsForFace(Direction side) {
        return IntStream.range(0, this.getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return (direction == Direction.UP) && canPlaceItem(index, stack);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, MincerBlockEntity mincer) {

        dropItemsInOutputSlot(level, pos, state, mincer);

        if (!level.isClientSide() && level.getBlockState(pos).getBlock() instanceof MincerBlock) {

            int crank = state.getValue(MincerBlock.CRANK);
            int cranked = state.getValue(MincerBlock.CRANKED);

            if (crank > 0) {
                if (cranked < MincerBlock.CRANKS_NEEDED) {
                    cranked++;
                }
                crank -= 1;
                if (cranked >= MincerBlock.CRANKS_NEEDED) {
                    cranked = 0;

                    RecipeManager recipeManager = level.getRecipeManager();
                    List<RecipeHolder<MincerRecipe>> recipes = recipeManager.getAllRecipesFor(RecipeTypeRegistry.MINCER_RECIPE_TYPE.get());
                    Optional<MincerRecipe> recipe = Optional.ofNullable(getRecipe(recipes, stacks));


                    if (recipe.isPresent()) {

                        ItemStack inputStack = this.stacks.get(INPUT_SLOT);

                        String recipe_type = recipe.get().getRecipeType();
                        int recipe_difficulty = 5;

                        switch (recipe_type) {
                            case "MEAT" -> recipe_difficulty = 1;
                            case "WOOD" -> recipe_difficulty = 2;
                            case "STONE" -> recipe_difficulty = 3;
                            case "METAL" -> recipe_difficulty = 4;
                        }

                        AABB searched_area = new AABB(pos);
                        searched_area.inflate(4.0D);
                        List<ServerPlayer> playersNearby = level.getEntitiesOfClass(ServerPlayer.class, searched_area, Player::isAlive); // checking for nearby living player around the block

                        if (!playersNearby.isEmpty()) {
                            for (Player player : playersNearby) {
                                if (player != null && player.getOffhandItem().is(inputStack.getItem())) {
                                    recipe_difficulty -= 1;
                                }
                            }
                        }

                        if (recipe_difficulty > 0) {
                            inputStack.shrink(1);
                            inputStack = inputStack.isEmpty() ? ItemStack.EMPTY : inputStack;
                            mincer.setItem(INPUT_SLOT, inputStack);
                            mincer.setItem(OUTPUT_SLOT, recipe.get().getResultItem(level.registryAccess()));

                            level.setBlock(pos, state.setValue(MincerBlock.CRANK, crank).setValue(MincerBlock.CRANKED, cranked), Block.UPDATE_ALL);
                            return;
                        }

                    }
                }
                level.setBlock(pos, state.setValue(MincerBlock.CRANK, crank).setValue(MincerBlock.CRANKED, cranked), Block.UPDATE_ALL);
            } else if (cranked > 0 && cranked < MincerBlock.CRANKS_NEEDED) {
                level.setBlock(pos, state.setValue(MincerBlock.CRANKED, 0), Block.UPDATE_ALL);
            }
        }
    }

    private MincerRecipe getRecipe(List<RecipeHolder<MincerRecipe>> recipes, NonNullList<ItemStack> inventory) {
        recipeLoop:
        for (RecipeHolder<MincerRecipe> recipeHolder : recipes) {
            MincerRecipe recipe = recipeHolder.value();
            for (Ingredient ingredient : recipe.getIngredients()) {
                boolean ingredientFound = false;
                for (ItemStack slotItem : inventory) {
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
