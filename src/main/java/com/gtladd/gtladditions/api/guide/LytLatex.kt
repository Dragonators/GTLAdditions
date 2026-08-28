package com.gtladd.gtladditions.api.guide

import com.mojang.blaze3d.platform.GlStateManager
import guideme.document.LytRect
import guideme.document.block.LytBlock
import guideme.document.interaction.InteractiveElement
import guideme.layout.LayoutContext
import guideme.render.GuidePageTexture
import guideme.render.RenderContext
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL12C.GL_LINEAR_MIPMAP_LINEAR
import org.lwjgl.opengl.GL12C.GL_TEXTURE_MAX_LEVEL
import org.lwjgl.opengl.GL12C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL30C.glGenerateMipmap
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

private const val SUPER_SAMPLE = 4

class LytLatex(
    private val latexExpression: String,
    private val fontSize: Float = DEFAULT_FONT_SIZE
) : LytBlock(),
    InteractiveElement {
    private var texture = GuidePageTexture.missing()
    private var mipmappedTexture: AbstractTexture? = null

    init {
        convertLatexToTexture()
    }

    private fun convertLatexToTexture() {
        try {
            val icon = TeXFormula(latexExpression)
                .setColor(Color.white)
                .createTeXIcon(TeXConstants.STYLE_DISPLAY, fontSize * SUPER_SAMPLE)
            val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()

            g2.composite = AlphaComposite.Src
            g2.color = Color(0x00FFFFFF, true)
            g2.fillRect(0, 0, icon.iconWidth, icon.iconHeight)
            g2.composite = AlphaComposite.SrcOver

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            icon.paintIcon(null, g2, 0, 0)
            g2.dispose()

            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "png", baos)
            texture = GuidePageTexture.load(ResourceLocation("latex", UUID.randomUUID().toString()), baos.toByteArray())
        } catch (_: Exception) {
            texture = GuidePageTexture.missing()
        }
    }

    private fun enableMipmaps(gpuTexture: AbstractTexture) {
        if (mipmappedTexture === gpuTexture) return

        val maxDimension = maxOf(texture.size.width(), texture.size.height())
        var levels = 0
        var dimension = maxDimension
        while (dimension > 1) {
            dimension = dimension shr 1
            levels++
        }

        gpuTexture.bind()
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels)
        glGenerateMipmap(GL_TEXTURE_2D)
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        mipmappedTexture = gpuTexture
    }

    override fun computeLayout(context: LayoutContext?, x: Int, y: Int, availableWidth: Int): LytRect {
        val size = texture.size
        var width = size.width() / SUPER_SAMPLE
        var height = size.height() / SUPER_SAMPLE
        if (width > availableWidth) {
            val factor = availableWidth.toFloat() / width.toFloat()
            width = (width.toFloat() * factor).toInt()
            height = (height.toFloat() * factor).toInt()
        }

        return LytRect(x, y, width, height)
    }

    override fun onLayoutMoved(i: Int, i1: Int) = Unit

    override fun renderBatch(renderContext: RenderContext, multiBufferSource: MultiBufferSource) = Unit

    override fun render(context: RenderContext) {
        enableMipmaps(texture.use())
        context.fillTexturedRect(bounds, texture)
    }

    companion object {
        const val DEFAULT_FONT_SIZE = 30f
    }
}