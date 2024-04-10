package satisfy.farm_and_charm.block;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import satisfy.farm_and_charm.entity.CookingPotBlockEntity;
import satisfy.farm_and_charm.registry.SoundEventRegistry;
import satisfy.farm_and_charm.registry.TagRegistry;
import satisfy.farm_and_charm.util.GeneralUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public class CookingPotBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty COOKING = BooleanProperty.create("cooking");
    public static final BooleanProperty NEEDS_SUPPORT = BooleanProperty.create("needs_support");

    public CookingPotBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(COOKING, false).setValue(NEEDS_SUPPORT, false));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, COOKING, NEEDS_SUPPORT);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), Shapes.empty());
    }

    private static final Map<Direction, VoxelShape> SHAPES = Util.make(new HashMap<>(), map -> {
        Supplier<VoxelShape> voxelShapeSupplier = () -> Shapes.or(Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.5, 0.8125), Shapes.box(0.0625, 0.3125, 0.25, 0.9375, 0.4375, 0.75));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            map.put(direction, GeneralUtil.rotateShape(Direction.NORTH, direction, voxelShapeSupplier.get()));
        }
    });

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState belowState = world.getBlockState(pos.below());
        boolean needsSupport = belowState.is(BlockTags.CAMPFIRES);
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(NEEDS_SUPPORT, needsSupport);
    }


    @Override
    public @NotNull InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof MenuProvider) {
                player.openMenu((MenuProvider)entity);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(COOKING) && !state.getValue(LIT)) return;

        double d = pos.getX() + 0.5;
        double e = pos.getY() + 0.7;
        double f = pos.getZ() + 0.5;

        if (random.nextDouble() < 0.1) {
            world.playLocalSound(d, e, f, SoundEventRegistry.COOKING_POT_BOILING.get(), SoundSource.BLOCKS, 0.05F, 0.05F, false);
        }

        double h = random.nextDouble() * 0.6 - 0.3;
        double i = h * (random.nextBoolean() ? 1 : -1);
        double j = random.nextDouble() * 0.5625;
        double k = h * (random.nextBoolean() ? 1 : -1);

        world.addParticle(ParticleTypes.SMOKE, d + i, e + j, f + k, 0.0, 0.0, 0.0);
        world.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, d + i, e + j, f + k, 0.0, 0.0, 0.0);
        world.addParticle(ParticleTypes.BUBBLE, d + i, e + j, f + k, 0.0, 0.0, 0.0);
        world.addParticle(ParticleTypes.BUBBLE_POP, d + i, e + j, f + k, 0.0, 0.0, 0.0);
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CookingPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (!world.isClientSide) {
            return (lvl, pos, blkState, t) -> {
                if (t instanceof CookingPotBlockEntity cookingPot) {
                    cookingPot.tick(lvl, pos, blkState, cookingPot);
                }
            };
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.farm_and_charm.canbeplaced").withStyle(ChatFormatting.GRAY));
    }
}
