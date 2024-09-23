package net.satisfy.farm_and_charm.client.recipebook.group;

import com.google.common.collect.ImmutableList;
import de.cristelknight.doapi.client.recipebook.IRecipeBookGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.satisfy.farm_and_charm.registry.ObjectRegistry;

import java.util.List;

@Environment(EnvType.CLIENT)
public enum StoveRecipeBookGroup implements IRecipeBookGroup {
    SEARCH(new ItemStack(Items.COMPASS)),
    BREAD(new ItemStack(ObjectRegistry.FARMERS_BREAD.get())),
    BEEF(new ItemStack(ObjectRegistry.STUFFED_CHICKEN.get())),
    CAKE(new ItemStack(ObjectRegistry.GRANDMOTHERS_STRAWBERRY_CAKE.get()));

    public static final List<IRecipeBookGroup> STOVE_GROUPS = ImmutableList.of(SEARCH, BREAD, CAKE, BEEF);

    private final List<ItemStack> icons;

    StoveRecipeBookGroup(ItemStack... entries) {
        this.icons = ImmutableList.copyOf(entries);
    }

    public boolean fitRecipe(Recipe<? extends Container> recipe, RegistryAccess registryAccess) {
        return switch (this) {
            case SEARCH, BEEF -> true;
            case BREAD ->
                    recipe.getIngredients().stream().anyMatch((ingredient) -> ingredient.test(ObjectRegistry.DOUGH.get().getDefaultInstance()));
            case CAKE ->
                    recipe.getIngredients().stream().anyMatch((ingredient) -> ingredient.test(Items.SUGAR.getDefaultInstance()));
        };
    }

    @Override
    public List<ItemStack> getIcons() {
        return this.icons;
    }

}
