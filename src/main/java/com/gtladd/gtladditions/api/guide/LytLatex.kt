package com.gtladd.gtladditions.api.guide

import guideme.document.LytRect
import guideme.document.block.LytBlock
import guideme.document.interaction.InteractiveElement
import guideme.layout.LayoutContext
import guideme.render.GuidePageTexture
import guideme.render.RenderContext
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

class LytLatex(
    private val latexExpression: String,
    private val fontSize: Float = DEFAULT_FONT_SIZE
) : LytBlock(),
    InteractiveElement {
    private var texture = GuidePageTexture.missing()

    init {
        convertLatexToTexture()
    }

    private fun convertLatexToTexture() {
        try {
            val icon = TeXFormula(latexExpression).setColor(Color.white).createTeXIcon(0, fontSize)
            val image = BufferedImage(icon.iconWidth * 4, icon.iconHeight * 4, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

            g2.scale(4.0, 4.0)
            icon.paintIcon(null, g2, 0, 0)
            g2.dispose()

            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "png", baos)
            texture = GuidePageTexture.load(ResourceLocation("latex", UUID.randomUUID().toString()), baos.toByteArray())
        } catch (_: Exception) {
            texture = GuidePageTexture.missing()
        }
    }

    override fun computeLayout(context: LayoutContext?, x: Int, y: Int, availableWidth: Int): LytRect {
        val size = texture.size
        var width = size.width() / 4
        var height = size.height() / 4
        if (width > availableWidth) {
            val factor = (availableWidth.toFloat() / width.toFloat()) * 0.8f
            width = (width.toFloat() * factor).toInt()
            height = (height.toFloat() * factor).toInt()
        }

        return LytRect(x, y, width, height)
    }

    override fun onLayoutMoved(i: Int, i1: Int) = Unit

    override fun renderBatch(renderContext: RenderContext, multiBufferSource: MultiBufferSource) = Unit

    override fun render(context: RenderContext) = context.fillTexturedRect(bounds, texture)

    companion object {
        const val DEFAULT_FONT_SIZE = 48f
    }
}