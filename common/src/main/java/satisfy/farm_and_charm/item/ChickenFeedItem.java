package satisfy.farm_and_charm.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.NotNull;
import satisfy.farm_and_charm.registry.MobEffectRegistry;

public class ChickenFeedItem extends Item {
    public ChickenFeedItem(Properties properties) {
       super(properties.food(new FoodProperties.Builder().nutrition(0).saturationMod(0f).build()));
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!player.level().isClientSide && entity.getType() == EntityType.CHICKEN) {
            Chicken chicken = (Chicken) entity;
            chicken.addEffect(new MobEffectInstance(MobEffectRegistry.CHICKEN.get(), 45 * 20));
            player.level().playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(), SoundEvents.CHICKEN_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            chicken.level().addParticle(ParticleTypes.HEART, chicken.getX(), chicken.getY() + 0.5, chicken.getZ(), 0.0D, 0.0D, 0.0D);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entityLiving) {
        if (entityLiving instanceof Player player && !world.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffectRegistry.CHICKEN.get(), 10 * 20));
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return super.finishUsingItem(stack, world, entityLiving);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.EAT;
    }
}
