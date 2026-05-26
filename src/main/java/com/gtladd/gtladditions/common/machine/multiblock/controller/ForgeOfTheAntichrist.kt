package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.data.RecursiveReverseBuffState
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RecursiveReverseArray
import com.gtladd.gtladditions.common.machine.trait.StarRitualTrait
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.CommonUtils.createObfuscatedRainbowComponent
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gtladd.gtladditions.utils.RecipeCalculationHelper.appendSpecificInput
import com.gtladd.gtladditions.utils.RecipeCalculationHelper.isRecipeCycleContainerContent
import com.gtladd.gtladditions.utils.StarGradient
import com.gtladd.gtladditions.utils.antichrist.AntichristPosHelper
import com.gtladd.gtladditions.utils.antichrist.ServerMachineManager
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

class ForgeOfTheAntichrist(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.FORGE_OF_THE_ANTICHRIST,
        *args
    ),
    IModularMachineHost<ForgeOfTheAntichrist>,
    IWirelessBindableSource<RecursiveReverseArray> {
    private val modules: Set<IModularMachineModule<ForgeOfTheAntichrist, *>> =
        ReferenceOpenHashSet<IModularMachineModule<ForgeOfTheAntichrist, *>>()

    @field:Persisted
    @field:DescSynced
    var runningSecs: Long = 0
        private set
    private var runningSecSubs: TickableSubscription? = null
    private var mam = 0
    private var cachedRecursiveReverseBuffState: RecursiveReverseBuffState? = null
    private var cachedRecursiveReverseBuffTick: Long = -1

    @field:DescSynced
    @field:Persisted
    private var recursiveReverseArrayPos: BlockPos? = null
    private var recursiveReverseArray: RecursiveReverseArray? = null

    @field:Persisted
    @field:DescSynced
    val starRitual: StarRitualTrait = StarRitualTrait(this)

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = ForgeOfTheAntichristLogic(this)

    override fun getRecipeLogic(): ForgeOfTheAntichristLogic = super.getRecipeLogic() as ForgeOfTheAntichristLogic

    override fun needConfirmMEStock(): Boolean = true

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!isFormed) return

        textList.add(
            "gtladditions.multiblock.forge_of_the_antichrist.running_sec".toComponent(createRainbowComponent(FormattingUtil.DECIMAL_FORMAT_2F.format(runningSecs / 3600.0)))
        )

        val recursiveReverseBoostPreview = getRecursiveReverseBoostPreview()
        textList.add(
            if (runningSecs >= MAX_EFFICIENCY_SEC) {
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.achieve_max_efficiency".toComponent
                ).apply {
                    recursiveReverseBoostPreview.outputMultiplier
                        .takeIf { it > 1.0 }
                        ?.roundToLong()
                        ?.let {
                            append(Component.literal(" (x$it)"))
                        }
                }
            } else {
                "gtladditions.multiblock.forge_of_the_antichrist.output_multiplier".toComponent(createRainbowComponent(FormattingUtil.DECIMAL_FORMAT_2F.format(recipeOutputMultiplyPreview)))
            }
        )

        textList.add(
            "gtceu.multiblock.blast_furnace.max_temperature".toComponent(createObfuscatedRainbowComponent(Long.MAX_VALUE.toString()))
        )
        textList.add("tooltip.gtlcore.installed_module_count".toComponent(getMAM()))

        val recursiveReverseActive = recursiveReverseBoostPreview.commonActive
        textList.add(
            "gtladditions.multiblock.forge_of_the_antichrist.recursive_reverse_array".toComponent(
                Component.literal(if (recursiveReverseActive) "✓" else "x")
                    .withStyle(if (recursiveReverseActive) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
        textList.add(
            "gtladditions.multiblock.threads".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<ForgeOfTheAntichrist, *>> = modules
    override fun getModuleScanPositions(): Array<out BlockPos> = AntichristPosHelper.calculateModulePositions(pos, frontFacing)

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(
            BlockPos(-13, 14, 0),
            Direction.EAST,
            Direction.UP,
            Direction.UP,
            Direction.NORTH,
            MultiBlockMachine.HELIOTHERMAL_PLASMA_FABRICATOR
        ),
        ModuleRenderInfo(
            BlockPos(-13, 0, -14),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.NORTH,
            MultiBlockMachine.HELIOFLARE_POWER_FORGE
        ),
        ModuleRenderInfo(
            BlockPos(-13, -14, 0),
            Direction.EAST,
            Direction.UP,
            Direction.DOWN,
            Direction.NORTH,
            MultiBlockMachine.HELIOFUSION_EXOTICIZER
        ),
        ModuleRenderInfo(
            BlockPos(-13, 0, 14),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.NORTH,
            MultiBlockMachine.HELIOFLUIX_MELTING_CORE
        )
    )

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        recursiveReverseArray?.unbindForge(clearBinding = false)
        recursiveReverseArray = null
        safeClearModules()
        unregisterFromServerManager()
    }

    override fun onMachineRemoved() {
        recursiveReverseArray?.unbindSource()
        safeClearModules()
        unregisterFromServerManager()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        safeClearModules()
        scanAndConnectModules()
        registerToServerManager()
        reconnectRecursiveReverseArray()
    }

    // ========================================
    // Rrf connection
    // ========================================

    override val bindingType: ResourceLocation = BINDING_TYPE

    override fun onBound(target: RecursiveReverseArray) {
        if (recursiveReverseArray != target) {
            recursiveReverseArray?.unbindSource()
        }
        recursiveReverseArray = target
        recursiveReverseArrayPos = target.pos
    }

    override fun onUnbound(target: RecursiveReverseArray?) {
        disconnectRecursiveReverseArray(target, clearBinding = true)
    }

    fun disconnectRecursiveReverseArray(target: RecursiveReverseArray?, clearBinding: Boolean) {
        if (target != null && recursiveReverseArray != target && recursiveReverseArrayPos != target.pos) return
        recursiveReverseArray = null
        if (clearBinding) recursiveReverseArrayPos = null
    }

    // ========================================
    // Life Cycle
    // ========================================

    override fun onLoad() {
        super.onLoad()
        (level as? ServerLevel)?.server?.tell(TickTask(0, ::updateRunningSecSubscription))
    }

    override fun onUnload() {
        super.onUnload()
        recursiveReverseArray?.unbindForge(clearBinding = false)
        recursiveReverseArray = null
        if (!isRemote) unregisterFromServerManager()
    }

    override fun onWorking(): Boolean {
        if (this.runningSecs == 0L) {
            this.runningSecs = 1
            this.updateRunningSecSubscription()
        }
        starRitual.handleStarRitualLogic()
        return super.onWorking()
    }

    private fun updateRunningSecSubscription() {
        if (this.runningSecs > 0) {
            this.runningSecSubs = this.subscribeServerTick(this.runningSecSubs, ::updateRunningSecs)
        } else if (this.runningSecSubs != null) {
            this.runningSecSubs!!.unsubscribe()
            this.runningSecSubs = null
        }
    }

    private fun updateRunningSecs() {
        if (this.offsetTimer % 20 == 0L) {
            if (this.recipeLogic.isWorking) {
                this.runningSecs = max(runningSecs + 1, 0)
            } else if (getRecursiveReverseBuffState().spacetimeStasisActive) {
                this.runningSecs = max(runningSecs, 0)
            } else {
                this.runningSecs = max(runningSecs - 16, 0)
            }
        }

        this.updateRunningSecSubscription()
    }

    // ========================================
    // Utils
    // ========================================

    private fun registerToServerManager() {
        (level as? ServerLevel)?.let { serverLevel ->
            ServerMachineManager.registerMachine(serverLevel, pos, frontFacing)
        }
    }

    private fun unregisterFromServerManager() {
        (level as? ServerLevel)?.let { serverLevel ->
            ServerMachineManager.unregisterMachine(serverLevel, pos)
        }
    }

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }

    val radiusMultiplier: Float
        get() = (1 + 1.7 * (1.0 - exp(-runningSecs.toDouble() / MAX_EFFICIENCY_SEC))).toFloat()

    val rgbFromTime: Int
        get() = StarGradient.getRGBFromTime(
            max(
                0.0,
                min(
                    1.0,
                    1.0 - exp(-runningSecs.toDouble() / MAX_EFFICIENCY_SEC)
                )
            )
        )

    val recipeOutputMultiply: Double
        // 1 -> MAX_MULTIPLIER
        get() {
            return baseRecipeOutputMultiply() * getRecursiveReverseBuffState().outputMultiplier
        }

    val recipeOutputMultiplyPreview: Double
        get() = baseRecipeOutputMultiply() * getRecursiveReverseBoostPreview().outputMultiplier

    private fun baseRecipeOutputMultiply(): Double {
        val addition: Double = (MAX_OUTPUT_RATIO - 1) * (
            min(
                this.runningSecs,
                MAX_EFFICIENCY_SEC.toLong()
            ).toDouble() / MAX_EFFICIENCY_SEC
            ).pow(2.0)
        return 1 + addition
    }

    fun canStarRitualStart(): Boolean = getRecursiveReverseArray()?.isStarRitualGateActive() == true && runningSecs >= MAX_EFFICIENCY_SEC

    // ========================================
    // Utils - RRF
    // ========================================

    private fun getRecursiveReverseBuffState(): RecursiveReverseBuffState {
        cachedRecursiveReverseBuffState?.takeIf { cachedRecursiveReverseBuffTick == offsetTimer }?.let { return it }
        val array = resolveRecursiveReverseArray() ?: return RecursiveReverseBuffState()
        return array.getBuffState().also {
            cachedRecursiveReverseBuffState = it
            cachedRecursiveReverseBuffTick = offsetTimer
        }
    }

    private fun getRecursiveReverseBoostPreview(): RecursiveReverseArray.Companion.BoostPreview =
        getRecursiveReverseArray()?.getBoostPreview() ?: RecursiveReverseArray.Companion.BoostPreview(false, 0.0, 1.0, emptyList(), emptyList())

    private fun getRecursiveReverseArray(): RecursiveReverseArray? = resolveRecursiveReverseArray()

    private fun resolveRecursiveReverseArray(): RecursiveReverseArray? {
        val cached = recursiveReverseArray
        if (cached != null && cached.isFormed()) return cached
        recursiveReverseArray = null
        if (!reconnectRecursiveReverseArray()) return null
        return recursiveReverseArray
    }

    private fun reconnectRecursiveReverseArray(): Boolean {
        if (!isFormed) return false
        val arrayPos = recursiveReverseArrayPos ?: return false
        val machine = ((level as? ServerLevel)?.getBlockEntity(arrayPos) as? MetaMachineBlockEntity)
            ?.metaMachine as? RecursiveReverseArray
            ?: return false
        if (!machine.isFormed()) return false
        return machine.bindResolvedSource(this).isSuccess
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        // 1 -> 1 / MAX_MULTIPLIER
        fun getEuReduction(machine: ForgeOfTheAntichrist): Double = 1 - (1 - MIN_EU_RATIO) * (
            min(
                machine.runningSecs,
                MAX_EFFICIENCY_SEC.toLong()
            ).toDouble() / MAX_EFFICIENCY_SEC
            ).pow(2.5)

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ForgeOfTheAntichrist::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )

        const val MAX_EFFICIENCY_SEC = 14400
        private const val MAX_OUTPUT_RATIO = 15
        private const val MIN_EU_RATIO = 0.2
        val BINDING_TYPE: ResourceLocation = ResourceLocation("gtladditions", "forge_of_the_antichrist")

        class ForgeOfTheAntichristLogic(parallel: ForgeOfTheAntichrist) : GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): ForgeOfTheAntichrist = super.getMachine() as ForgeOfTheAntichrist

            override fun getEuMultiplier(): Double = super.getEuMultiplier() *
                getEuReduction(getMachine()) *
                getMachine().getRecursiveReverseBuffState().euMultiplier

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                val focus = getMachine().getRecursiveReverseBuffState().magnetorheologicalConvergenceActive
                val modifier = ContentModifier.multiplier(getMachine().recipeOutputMultiply)

                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel,
                    modifyRecipe = { recipe -> copyAndModifyRecipe(recipe, modifier, focus) },
                    useModifiedRecipe = true
                )
            }
        }

        private fun copyAndModifyRecipe(recipe: GTRecipe, modifier: ContentModifier, focus: Boolean): GTRecipe {
            val copy = GTRecipe(
                recipe.recipeType,
                recipe.id,
                modifyInputContents(recipe.inputs, modifier, recipe.id),
                modifyOutputContents(recipe.outputs, modifier, focus),
                recipe.tickInputs,
                recipe.tickOutputs,
                recipe.inputChanceLogics,
                recipe.outputChanceLogics,
                recipe.tickInputChanceLogics,
                recipe.tickOutputChanceLogics,
                recipe.conditions,
                recipe.ingredientActions,
                recipe.data,
                recipe.duration,
                recipe.isFuel
            )
            IGTRecipe.of(copy).realParallels = IGTRecipe.of(recipe).realParallels
            copy.ocTier = recipe.ocTier
            return copy
        }

        private fun modifyOutputContents(
            before: Map<RecipeCapability<*>, List<Content>>,
            modifier: ContentModifier,
            focus: Boolean
        ): Map<RecipeCapability<*>, List<Content>> {
            val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
            for (entry in before) {
                val cap = entry.key
                val contentList = entry.value
                val copyList = ObjectArrayList<Content>(contentList.size)

                if (cap == ItemRecipeCapability.CAP) {
                    for (content in contentList) {
                        if (isRecipeCycleContainerContent(content)) {
                            copyList.add(content)
                        } else {
                            copyList.add(content.copy(ItemRecipeCapability.CAP, modifier))
                        }
                    }
                } else {
                    if (focus && cap == FluidRecipeCapability.CAP) {
                        if (!focusFluidContents(contentList, copyList, modifier)) {
                            for (content in contentList) {
                                copyList.add(content.copy(cap, modifier))
                            }
                        }
                    } else {
                        for (content in contentList) {
                            copyList.add(content.copy(cap, modifier))
                        }
                    }
                }
                after[cap] = copyList
            }
            return after
        }

        private fun modifyInputContents(
            before: Map<RecipeCapability<*>, List<Content>>,
            modifier: ContentModifier,
            id: ResourceLocation
        ): Map<RecipeCapability<*>, List<Content>> {
            if (!before.containsKey(ItemRecipeCapability.CAP)) return before

            val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
            for (entry in before) {
                val cap = entry.key
                val contentList = entry.value

                if (cap == ItemRecipeCapability.CAP) {
                    val copyList = ObjectArrayList<Content>(contentList.size)
                    for (content in contentList) {
                        if (isRecipeCycleContainerContent(content)) {
                            copyList.add(content.copy(ItemRecipeCapability.CAP, modifier))
                        } else {
                            copyList.add(content)
                        }
                    }
                    appendSpecificInput(copyList, id, modifier)
                    after[cap] = copyList
                } else {
                    after[cap] = contentList
                }
            }
            return after
        }

        private fun focusFluidContents(
            contentList: List<Content>,
            copyList: MutableList<Content>,
            modifier: ContentModifier
        ): Boolean {
            if (contentList.size <= 1) return false

            val first = contentList[0]
            val firstFluid = first.content as? FluidIngredient ?: return false
            var totalAmount: Long = firstFluid.amount

            for (i in 1..<contentList.size) {
                val fluid = contentList[i].content as? FluidIngredient ?: return false
                totalAmount += fluid.amount
            }

            val copy = first.copy(FluidRecipeCapability.CAP, null)
            val fluid = copy.content as? FluidIngredient ?: return false
            fluid.amount = totalAmount
            copyList.add(copy.copy(FluidRecipeCapability.CAP, modifier))
            return true
        }
    }
}