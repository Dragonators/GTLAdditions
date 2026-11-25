package com.gtladd.gtladditions.api.machine.logic.chemical

import com.google.common.collect.HashBasedTable
import com.gregtechceu.gtceu.api.capability.recipe.*
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputHatchPartMachine
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.hepdd.gtmthings.common.block.machine.multiblock.part.appeng.MEOutputPartMachine
import com.lowdragmc.lowdraglib.syncdata.ISubscription
import it.unimi.dsi.fastutil.objects.ReferenceSet
import it.unimi.dsi.fastutil.objects.ReferenceSets
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.machine.trait.*
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEFilterIOPartMachine
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEPatternPartMachine
import org.gtlcore.gtlcore.utils.datastructure.FirstFlagRecipePartSet
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Stable machine view for one RecipeLogic. Only the physical handlers change on reformation.
 *
 * Remaining `IRecipeLogicMachine` Java defaults (`notifyStatusChanged`, `IWorkable`/`IControllable`, etc.)
 * intentionally stay unit-scoped and must not be forwarded to `host`, or a single unit stopping would
 * toggle host active blocks or control the whole coordinator.
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
internal class ExoticChemicalUnitMachine(private val host: ExoticChemicalComplex) :
    IRecipeLogicMachine by host,
    IGTLAddMultiRecipeMachine by host,
    IRecipeCapabilityMachine,
    ISuspendableMachine {

    lateinit var unitLogic: ExoticChemicalUnitLogic
    var isOnline: Boolean = false
        private set

    private val proxy = HashBasedTable.create<IO, RecipeCapability<*>, MutableList<IRecipeHandler<*>>>()
    private val inputs = ArrayList<RecipeHandlePart>()
    private val outputs = ArrayList<RecipeHandlePart>()
    private val patterns = ArrayList<MEPatternRecipeHandlePart>()
    private val meOutputs = ArrayList<MEIORecipeHandlePart<*>>()
    private var shared: RecipeHandlePart? = null

    private val cache = HashMap<GTRecipe, FirstFlagRecipePartSet>()
    private val subscriptions = IdentityHashMap<Any, ISubscription>()
    private val wake = Runnable { unitLogic.updateTickSubscription() }

    private var distinct = false
    private var itemBus = false
    private var fluidHatch = false
    private var alwaysItem = false
    private var alwaysFluid = false

    // ========================================
    // PART BINDING & LIFECYCLE
    // ========================================

    fun refresh(parts: List<IMultiPart>, isDistinct: Boolean) {
        clearHandlers()
        distinct = isDistinct

        val sources = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val common = ArrayList<IRecipeHandler<*>>()

        for (part in parts) {
            if (part is MEOutputBusPartMachine || part is MEOutputPartMachine) itemBus = true
            if (part is MEOutputHatchPartMachine || part is MEOutputPartMachine) fluidHatch = true

            val partDistinct = part is IDistinctPart && part.isDistinct
            val local = ArrayList<IRecipeHandler<*>>()
            val out = ArrayList<IRecipeHandler<*>>()

            for (handler in part.recipeHandlers) {
                if (handler.isProxy || handler.capability != ItemRecipeCapability.CAP && handler.capability != FluidRecipeCapability.CAP) {
                    continue
                }

                val io = handler.handlerIO
                proxy.row(io).getOrPut(handler.capability) { ArrayList() }.add(handler)
                if (part is IMEFilterIOPartMachine) continue

                sources.add(handler)
                subscriptions.getOrPut(handler) { handler.addChangedListener(wake) }

                if (io == IO.IN || io == IO.BOTH) {
                    if (distinct || partDistinct && handler.capability != FluidRecipeCapability.CAP) {
                        local += handler
                    } else {
                        common += handler
                    }
                }
                if (io == IO.OUT || io == IO.BOTH) {
                    out += handler
                }
            }

            if (local.isNotEmpty()) inputs += RecipeHandlePart.of(IO.IN, local)
            if (out.isNotEmpty()) outputs += RecipeHandlePart.of(IO.OUT, out)

            if (part is IMEFilterIOPartMachine) {
                val handlers = part.meRecipeHandlerTraits
                for (handler in listOf(handlers.left(), handlers.right())) {
                    sources.add(handler)
                    subscriptions.getOrPut(handler) { handler.addBufferChangedListener(wake) }
                }

                val me = if (part is IMEPatternPartMachine) {
                    MEPatternRecipeHandlePart.of(part).also {
                        patterns += it
                        it.restoreMachineCache(::tryAddAndActiveRhp)
                    }
                } else {
                    MEIORecipeHandlePart.of(part)
                }

                if (part.meTrait.io == IO.OUT || part.meTrait.io == IO.BOTH) {
                    meOutputs += me
                }
            }
        }

        if (common.isNotEmpty()) {
            shared = RecipeHandlePart.of(IO.IN, common).also { part ->
                for (group in inputs) {
                    group.addSharedFluidHandlers(part.getCapability(FluidRecipeCapability.CAP))
                }
            }
        }

        subscriptions.entries.removeIf { (source, subscription) ->
            if (source in sources) return@removeIf false
            subscription.unsubscribe()
            true
        }

        sortMEOutput()
        outputs.sortWith(RecipeHandlePart.COMPARATOR.reversed())
        isOnline = true
    }

    fun clear() {
        isOnline = false
        subscriptions.values.forEach(ISubscription::unsubscribe)
        subscriptions.clear()
        clearHandlers()
    }

    private fun clearHandlers() {
        itemBus = false
        fluidHatch = false
        alwaysItem = false
        alwaysFluid = false
        inputs.clear()
        outputs.clear()
        patterns.clear()
        meOutputs.clear()
        clearRecipeCache()
        proxy.clear()
        shared = null
    }

    // ========================================
    // MACHINE VIEW OVERRIDES
    // ========================================

    override fun self(): ExoticChemicalComplex = host

    override fun getRecipeLogic(): ExoticChemicalUnitLogic = unitLogic

    override fun getRecipeType(): GTRecipeType = unitLogic.recipeType

    override fun isRecipeLogicAvailable(): Boolean = isOnline && host.isRecipeLogicAvailable

    override fun keepSubscribing(): Boolean = false

    override fun getChanceTier(): Int = host.tier

    override fun needConfirmMEStock(): Boolean = host.needConfirmMEStock()

    override fun beforeWorking(recipe: GTRecipe?): Boolean = true

    override fun onWorking(): Boolean = true

    override fun onWaiting() = Unit

    override fun afterWorking() = Unit

    override fun getCapabilitiesProxy(): HashBasedTable<IO, RecipeCapability<*>, MutableList<IRecipeHandler<*>>> = proxy

    override fun hasProxies(): Boolean = !emptyHandlePart()

    override fun getMaintenanceMachine(): IMaintenanceMachine? = host.getMaintenanceMachine()

    // ========================================
    // RECIPE HANDLE PARTS
    // ========================================

    override fun getNormalRecipeHandlePart(io: IO): ArrayList<RecipeHandlePart> = if (io == IO.IN) inputs else outputs

    override fun getSharedRecipeHandlePart(): RecipeHandlePart? = shared

    override fun emptyRecipeHandlePart(): Boolean = shared == null && inputs.isEmpty() && patterns.isEmpty()

    override fun emptyHandlePart(): Boolean = emptyRecipeHandlePart() && outputs.isEmpty() && meOutputs.isEmpty()

    override fun getMEPatternRecipeHandleParts(): ArrayList<MEPatternRecipeHandlePart> = patterns

    override fun getMEOutputRecipeHandleParts(): ArrayList<MEIORecipeHandlePart<*>> = meOutputs

    override fun sortMEOutput() {
        meOutputs.sortWith(MEIORecipeHandlePart.COMPARATOR.reversed())
        alwaysItem = itemBus || meOutputs.any { !it.hasItemFilter() }
        alwaysFluid = fluidHatch || meOutputs.any { !it.hasFluidFilter() }
    }

    // ========================================
    // OUTPUT MATCHING & DISTINCT MODE
    // ========================================

    override fun itemOutPutAlwaysMatch(): Boolean = alwaysItem

    override fun fluidOutPutAlwaysMatch(): Boolean = alwaysFluid

    override fun isRecipeOutputAlwaysMatch(recipe: GTRecipe): Boolean =
        (alwaysItem || recipe.getOutputContents(ItemRecipeCapability.CAP).isEmpty()) &&
            (alwaysFluid || recipe.getOutputContents(FluidRecipeCapability.CAP).isEmpty())

    override fun isDistinct(): Boolean = distinct

    override fun setDistinct(isDistinct: Boolean) = Unit

    override fun upDate() = Unit

    // ========================================
    // RECIPE HANDLE CACHE
    // ========================================

    fun clearRecipeCache() = cache.clear()

    override fun getActiveRecipeHandle(recipe: GTRecipe): IRecipeHandlePart? = cache[recipe]?.active

    override fun getAllCachedRecipeHandlesIter(recipe: GTRecipe): MutableIterator<IRecipeHandlePart> =
        cache[recipe]?.reverseIterator ?: Collections.emptyIterator()

    override fun getAllCachedRecipeHandles(recipe: GTRecipe): ReferenceSet<IRecipeHandlePart> =
        cache[recipe]?.all ?: ReferenceSets.emptySet()

    override fun tryAddAndActiveRhp(recipe: GTRecipe, part: IRecipeHandlePart?) {
        if (part != null) {
            cache.getOrPut(recipe) { FirstFlagRecipePartSet() }.addOrSetActive(part)
        }
    }

    override fun tryAddAndActiveMERhp(part: MEPatternRecipeHandlePart, recipe: GTRecipe, slot: Int) {
        part.setLastRecipe2Slot(recipe, slot)
        tryAddAndActiveRhp(recipe, part)
    }

    // ========================================
    // SUSPEND CONTROL
    // ========================================

    override fun `gtlcore$isSuspendAfterFinish`(): Boolean = unitLogic.suspendAfterFinish

    override fun `gtlcore$setSuspendAfterFinish`(value: Boolean) {
        unitLogic.setSuspendAfterFinish(value)
    }
}
