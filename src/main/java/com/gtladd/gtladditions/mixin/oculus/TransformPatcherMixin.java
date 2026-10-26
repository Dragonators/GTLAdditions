package com.gtladd.gtladditions.mixin.oculus;

import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;

import com.gtladd.gtladditions.client.render.IrisTerrainShaderCompat;
import com.gtladd.gtladditions.client.render.machine.antichrist.RingBatchShaderSources;
import com.gtladd.gtladditions.client.render.machine.antichrist.RingBatchShaders;
import io.github.douira.glsl_transformer.ast.transform.EnumASTTransformer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumMap;
import java.util.Map;

@Mixin(value = TransformPatcher.class, remap = false)
public abstract class TransformPatcherMixin {

    @Shadow
    @Final
    private static EnumASTTransformer<Parameters, PatchShaderType> transformer;

    @Inject(method = "patchVanilla", at = @At("RETURN"), cancellable = true)
    private static void gtladditions$useModelNormalForFoliageClassification(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
                                                                            net.irisshaders.iris.gl.blending.AlphaTest alpha, boolean lines, boolean chunkOffset,
                                                                            net.irisshaders.iris.gl.state.ShaderAttributeInputs inputs,
                                                                            it.unimi.dsi.fastutil.objects.Object2ObjectMap<?, ?> textureMap,
                                                                            CallbackInfoReturnable<Map<PatchShaderType, String>> cir) {
        var sources = cir.getReturnValue();
        if (sources == null) return;
        // Extra programmable stages need their own varying forwarding and are outside this patch.
        if (sources.get(PatchShaderType.GEOMETRY) != null || sources.get(PatchShaderType.TESS_CONTROL) != null || sources.get(PatchShaderType.TESS_EVAL) != null) {
            if (RingBatchShaderSources.isVariant(name)) throw new IllegalArgumentException("FOA batching requires a vertex/fragment terrain program");
            return;
        }
        var patched = IrisTerrainShaderCompat.INSTANCE.patch(transformer, sources.get(PatchShaderType.VERTEX), sources.get(PatchShaderType.FRAGMENT));
        var result = new EnumMap<PatchShaderType, String>(PatchShaderType.class);
        result.putAll(sources);
        if (patched != null) {
            // Do not mutate Iris's cached map, or assume that future versions return a mutable map.
            result.put(PatchShaderType.VERTEX, patched.getVertex());
            result.put(PatchShaderType.FRAGMENT, patched.getFragment());
        }
        if (RingBatchShaderSources.isVariant(name)) {
            var batch = RingBatchShaderSources.patchIris(transformer, result.get(PatchShaderType.VERTEX), result.get(PatchShaderType.FRAGMENT), RingBatchShaders.capacity());
            result.put(PatchShaderType.VERTEX, batch.getVertex());
            result.put(PatchShaderType.FRAGMENT, batch.getFragment());
        }
        if (patched != null || RingBatchShaderSources.isVariant(name)) cir.setReturnValue(result);
    }
}