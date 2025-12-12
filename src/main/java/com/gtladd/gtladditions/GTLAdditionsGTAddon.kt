package com.gtladd.gtladditions

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.addon.GTAddon
import com.gregtechceu.gtceu.api.addon.IGTAddon
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate
import com.gtladd.gtladditions.api.registry.GTLAddRegistration
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.material.GTLAddElements
import com.gtladd.gtladditions.common.modify.GTLAddSoundEntries
import com.gtladd.gtladditions.common.modify.SkyTearsAndGregHeart
import com.gtladd.gtladditions.data.recipes.*
import com.gtladd.gtladditions.data.recipes.newmachinerecipe.*
import com.gtladd.gtladditions.data.recipes.process.SocProcess
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.resources.ResourceLocation
import org.gtlcore.gtlcore.config.ConfigHolder
import java.util.function.Consumer

@Suppress("unused")
@GTAddon
class GTLAdditionsGTAddon : IGTAddon {
    override fun getRegistrate(): GTRegistrate {
        return GTLAddRegistration.REGISTRATE
    }

    override fun initializeAddon() {
        GTLAddItems.init()
        GTLAddBlocks.init()
    }

    override fun addonModId(): String {
        return GTLAdditions.MOD_ID
    }

    override fun addRecipes(provider: Consumer<FinishedRecipe?>) {
        PhotonMatrixEtch.init(provider)
        EMResonanceConversionField.init(provider)
        TitansCripEarthbore.init(provider)
        BiologicalSimulation.init(provider)
        VoidfluxReaction.init(provider)
        StellarLgnition.init(provider)
        ChaosWeave.init(provider)
        NightmareCrafting.init(provider)
        GenesisEngine.init(provider)
        HeliofusionExoticizer.init(provider)
        InterStellar.init(provider)
        AE2.init(provider)
        Assembler.init(provider)
        AssemblyLine.init(provider)
        Distort.init(provider)
        ElectricBlastFurnace.init(provider)
        IntegratedOreProcessor.init(provider)
        NewMultiBlockMachineController.init(provider)
        PartMachine.init(provider)
        Qft.init(provider)
        SocProcess.init(provider)
        Misc.init(provider)
        StarGate.init(provider)
        SkyTearsAndGregHeart.init(provider)
    }

    override fun registerSounds() {
        GTLAddSoundEntries.init()
    }

    override fun registerElements() {
        GTLAddElements.init()
    }
}
