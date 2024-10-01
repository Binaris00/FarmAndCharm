package net.satisfy.farm_and_charm.registry;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.satisfy.farm_and_charm.FarmAndCharm;
import net.satisfy.farm_and_charm.effect.*;
import net.satisfy.farm_and_charm.util.FarmAndCharmIdentifier;

import java.util.function.Supplier;

public class MobEffectRegistry {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(FarmAndCharm.MOD_ID, Registries.MOB_EFFECT);
    private static final Registrar<MobEffect> MOB_EFFECTS_REGISTRAR = MOB_EFFECTS.getRegistrar();

    public static final DeferredSupplier<MobEffect> SWEETS;
    public static final DeferredSupplier<MobEffect> HORSE_FODDER;
    public static final DeferredSupplier<MobEffect> DOG_FOOD;
    public static final DeferredSupplier<MobEffect> CLUCK;
    public static final DeferredSupplier<MobEffect> GRANDMAS_BLESSING;
    public static final DeferredSupplier<MobEffect> RESTED;
    public static final DeferredSupplier<MobEffect> FARMERS_BLESSING;
    public static final DeferredSupplier<MobEffect> SUSTENANCE;
    public static final DeferredSupplier<MobEffect> SATIATION;
    public static final DeferredSupplier<MobEffect> FEAST;

    private static RegistrySupplier<MobEffect> registerEffect(String name, Supplier<MobEffect> effect) {
        if (Platform.isNeoForge()) {
            return MOB_EFFECTS.register(name, effect);
        }
        return MOB_EFFECTS_REGISTRAR.register(FarmAndCharmIdentifier.of(name), effect);
    }

    public static void init() {
        FarmAndCharm.LOGGER.debug("Registering MobEffects for " + FarmAndCharm.MOD_ID);
        MOB_EFFECTS.register();
    }

    public static Holder<MobEffect> getHolder(DeferredSupplier<MobEffect> effect) {
        return MOB_EFFECTS_REGISTRAR.getHolder(effect.getId());
    }

    static {
        SWEETS = registerEffect("sweets", SweetsEffect::new);
        HORSE_FODDER = registerEffect("horse_fodder", HorseFodderEffect::new);
        DOG_FOOD = registerEffect("dog_food", DogFoodEffect::new);
        CLUCK = registerEffect("cluck", ChickenEffect::new);
        GRANDMAS_BLESSING = registerEffect("grandmas_blessing", GrandmasBlessingEffect::new);
        RESTED = registerEffect("rested", RestedEffect::new);
        FARMERS_BLESSING = registerEffect("farmers_blessing", FarmersBlessingEffect::new);
        SUSTENANCE = registerEffect("sustenance", SustenanceEffect::new);
        SATIATION = registerEffect("satiation", SatiationEffect::new);
        FEAST = registerEffect("feast", FeastEffect::new);
    }
}
