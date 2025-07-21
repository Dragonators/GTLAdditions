package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.recipe.condition.CleanroomCondition
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import net.minecraft.MethodsReturnNonnullByDefault
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.Item
import org.gtlcore.gtlcore.utils.Registries
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class GTLAddRecipeBuilder(id: String, recipeType: GTRecipeType) : GTRecipeBuilder(GTLAdditions.id(id), recipeType) {
    override fun circuitMeta(configuration: Int): GTLAddRecipeBuilder {
        return this.notConsumable(IntCircuitIngredient.circuitInput(configuration)) as GTLAddRecipeBuilder
    }

    @JvmOverloads
    fun inputItemString(input: String, number: Int = 1): GTLAddRecipeBuilder {
        return super.inputItems(Registries.getItemStack(input, number)) as GTLAddRecipeBuilder
    }

    fun InputItems(inputitems: String): GTLAddRecipeBuilder {
        val split: Array<String?> = inputitems.split("x ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return this.inputItemString(split[1]!!, split[0]!!.toInt())
    }

    @JvmOverloads
    fun inputItemsTag(input: String, number: Int = 1): GTLAddRecipeBuilder {
        return super.inputItems(SizedIngredient.create(TagUtil.createItemTag(input), number)) as GTLAddRecipeBuilder
    }

    @JvmOverloads
    fun inputItemsModTag(input: String, number: Int = 1): GTLAddRecipeBuilder {
        return super.inputItems(SizedIngredient.create(TagUtil.createModItemTag(input), number)) as GTLAddRecipeBuilder
    }

    override fun inputItems(orePrefix: TagPrefix, material: Material, count: Int): GTLAddRecipeBuilder {
        val tag = ChemicalHelper.getTag(orePrefix, material)
        return (if (tag == null) this.inputItems(ChemicalHelper.get(orePrefix, material, count)) else this.inputItems(
            tag,
            count
        )) as GTLAddRecipeBuilder
    }

    override fun inputItems(orePrefix: TagPrefix, material: Material): GTLAddRecipeBuilder {
        return this.inputItems(orePrefix, material, 1)
    }

    override fun inputItems(machine: MachineDefinition, count: Int): GTLAddRecipeBuilder {
        return super.inputItems(machine.asStack(count)) as GTLAddRecipeBuilder
    }

    override fun inputItems(machine: MachineDefinition): GTLAddRecipeBuilder {
        return this.inputItems(machine, 1)
    }

    fun chancedInputItems(input: String, chance: Double, tierChanceBoost: Double): GTLAddRecipeBuilder {
        return super.chancedInput(
            Registries.getItemStack(input),
            chance.toInt() * 100,
            tierChanceBoost.toInt() * 100
        ) as GTLAddRecipeBuilder
    }

    override fun inputFluids(input: FluidStack): GTLAddRecipeBuilder {
        return input(
            FluidRecipeCapability.CAP, FluidIngredient.of(
                TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(input.fluid).path),
                input.amount, input.tag
            )
        ) as GTLAddRecipeBuilder
    }

    fun inputFluids(input: String, count: Int): GTLAddRecipeBuilder {
        return super.inputFluids(
            Objects.requireNonNull<Material?>(GTMaterials.get(input)).getFluid(count.toLong())
        ) as GTLAddRecipeBuilder
    }

    @JvmOverloads
    fun outputItems(output: String, number: Int = 1): GTLAddRecipeBuilder {
        return super.outputItems(Registries.getItemStack(output, number)) as GTLAddRecipeBuilder
    }

    fun OutputItems(outputitems: String): GTLAddRecipeBuilder {
        val split: Array<String?> = outputitems.split("x ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return this.outputItems(split[1]!!, split[0]!!.toInt()) as GTLAddRecipeBuilder
    }

    override fun outputItems(orePrefix: TagPrefix, material: Material, count: Int): GTLAddRecipeBuilder {
        return this.outputItems(ChemicalHelper.get(orePrefix, material, count)) as GTLAddRecipeBuilder
    }

    override fun outputItems(orePrefix: TagPrefix, material: Material): GTLAddRecipeBuilder {
        return this.outputItems(orePrefix, material, 1)
    }

    override fun outputItems(machine: MachineDefinition, count: Int): GTLAddRecipeBuilder {
        return super.outputItems(machine.asStack(count)) as GTLAddRecipeBuilder
    }

    override fun outputItems(machine: MachineDefinition): GTLAddRecipeBuilder {
        return this.outputItems(machine, 1)
    }

    override fun chancedOutput(tag: TagPrefix, mat: Material, chance: Int, tierChanceBoost: Int): GTLAddRecipeBuilder {
        return this.chancedOutput(tag, mat, 1, chance, tierChanceBoost)
    }

    override fun chancedOutput(
        tag: TagPrefix,
        mat: Material,
        count: Int,
        chance: Int,
        tierChanceBoost: Int
    ): GTLAddRecipeBuilder {
        return super.chancedOutput(ChemicalHelper.get(tag, mat, count), chance, tierChanceBoost) as GTLAddRecipeBuilder
    }

    fun chancedOutputItems(output: String, chance: Double, tierChanceBoost: Double): GTLAddRecipeBuilder {
        return this.chancedOutputItems(output, 1, chance, tierChanceBoost)
    }

    fun chancedOutputItems(output: String, count: Int, chance: Double, tierChanceBoost: Double): GTLAddRecipeBuilder {
        return super.chancedOutput(
            Registries.getItemStack(output, count),
            (chance * 100).toInt(),
            (tierChanceBoost * 100).toInt()
        ) as GTLAddRecipeBuilder
    }

    override fun outputFluids(output: FluidStack): GTLAddRecipeBuilder {
        return output(FluidRecipeCapability.CAP, FluidIngredient.of(output)) as GTLAddRecipeBuilder
    }

    @JvmOverloads
    fun notConsumable(input: String, count: Int = 1): GTLAddRecipeBuilder {
        return super.notConsumable(Registries.getItemStack(input, count)) as GTLAddRecipeBuilder
    }

    override fun notConsumable(item: Supplier<out Item?>): GTLAddRecipeBuilder {
        val lastChance = this.chance
        this.chance = 0
        this.inputItems(item)
        this.chance = lastChance
        return this
    }

    override fun notConsumableFluid(fluid: FluidStack): GTLAddRecipeBuilder {
        return super.notConsumableFluid(
            FluidIngredient.of(
                TagUtil.createFluidTag(
                    BuiltInRegistries.FLUID.getKey(fluid.fluid).path
                ), fluid.amount
            )
        ) as GTLAddRecipeBuilder
    }

    override fun EUt(eu: Long): GTLAddRecipeBuilder {
        val lastPerTick = this.perTick
        this.perTick = true
        if (eu > 0L) {
            this.tickInput.remove(EURecipeCapability.CAP)
            this.inputEU(eu)
        } else if (eu < 0L) {
            this.tickOutput.remove(EURecipeCapability.CAP)
            this.outputEU(-eu)
        }
        this.perTick = lastPerTick
        return this
    }

    fun TierEUtVA(tier: Int): GTLAddRecipeBuilder {
        return super.EUt(GTValues.VA[tier].toLong()) as GTLAddRecipeBuilder
    }

    override fun duration(duration: Int): GTLAddRecipeBuilder {
        this.duration = duration
        return this
    }

    override fun cleanroom(cleanroomType: CleanroomType): GTLAddRecipeBuilder {
        return this.addCondition(CleanroomCondition(cleanroomType)) as GTLAddRecipeBuilder
    }

    override fun blastFurnaceTemp(blastTemp: Int): GTLAddRecipeBuilder {
        return this.addData("ebf_temp", blastTemp) as GTLAddRecipeBuilder
    }

    override fun save(consumer: Consumer<FinishedRecipe?>) {
        super.save(consumer)
    }
}
