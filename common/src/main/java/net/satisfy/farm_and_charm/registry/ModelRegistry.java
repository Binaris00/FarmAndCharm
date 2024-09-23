package net.satisfy.farm_and_charm.registry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.satisfy.farm_and_charm.util.FarmAndCharmIdentifier;

public interface ModelRegistry {
    ModelLayerLocation CART = new ModelLayerLocation(new FarmAndCharmIdentifier("cart"), "main");
    ModelLayerLocation PLOW = new ModelLayerLocation(new FarmAndCharmIdentifier("plow"), "main");
}
