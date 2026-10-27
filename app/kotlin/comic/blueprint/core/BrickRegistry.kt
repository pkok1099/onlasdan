package comic.blueprint.core

import androidx.compose.runtime.Composable

/**
 * A registry for rendering different brick types.
 *
 * This replaces hardcoded when statements with a centralized, extensible registry.
 * New brick types can be added without modifying screen files.
 *
 * @param B Brick type (enum or sealed class)
 * @param S Screen state type
 */
interface BrickRenderer<B, S> {
    /**
     * Check if this renderer can handle the given brick.
     */
    fun canRender(brick: B): Boolean

    /**
     * Render the brick with the given state and metadata.
     */
    @Suppress("ComposableNaming")
    @Composable
    fun render(brick: B, state: S, metadata: Map<String, Any>)
}

/**
 * Registry for managing brick renderers.
 *
 * Supports:
 * - Multiple renderers per screen
 * - Source-specific renderers
 * - Fallback renderers
 * - Runtime registration
 *
 * @param B Brick type
 * @param S Screen state type
 */
class BrickRendererRegistry<B, S> {
    private val renderers = mutableListOf<BrickRenderer<B, S>>()

    /**
     * Register a brick renderer.
     * Earlier registrations take precedence (first match wins).
     */
    fun register(renderer: BrickRenderer<B, S>) {
        renderers.add(0, renderer) // Add to front for priority
    }

    /**
     * Find a renderer that can handle the given brick.
     * Returns null if no renderer found.
     */
    fun find(brick: B): BrickRenderer<B, S>? = renderers.firstOrNull { it.canRender(brick) }

    /**
     * Render a brick, or invoke fallback if no renderer found.
     */
    @Suppress("ComposableNaming")
    @Composable
    fun render(brick: B, state: S, metadata: Map<String, Any>, onMissing: (B) -> Unit = {}) {
        val renderer = find(brick)
        if (renderer != null) {
            renderer.render(brick, state, metadata)
        } else {
            onMissing(brick)
        }
    }
}

/**
 * Builder for constructing brick renderers inline.
 *
 * Usage:
 * ```kotlin
 * brickRenderer<DetailBrick, DetailState> {
 *     on(DetailBrick.TopBar) { state, metadata ->
 *         // Render TopBar
 *     }
 *     on(DetailBrick.Synopsis) { state, metadata ->
 *         // Render Synopsis
 *     }
 * }
 * ```
 */
class BrickRendererBuilder<B, S> {
    private val cases = mutableMapOf<B, @Composable (S, Map<String, Any>) -> Unit>()

    fun on(brick: B, renderer: @Composable (S, Map<String, Any>) -> Unit) {
        cases[brick] = renderer
    }

    fun build(): BrickRenderer<B, S> = MapBasedBrickRenderer(cases)
}

/**
 * A simple map-based brick renderer for inline definitions.
 */
private class MapBasedBrickRenderer<B, S>(private val cases: Map<B, @Composable (S, Map<String, Any>) -> Unit>) : BrickRenderer<B, S> {
    override fun canRender(brick: B): Boolean = cases.containsKey(brick)

    @Composable
    override fun render(brick: B, state: S, metadata: Map<String, Any>) {
        cases[brick]?.invoke(state, metadata)
    }
}

/**
 * DSL for building a brick renderer inline.
 */
fun <B, S> brickRenderer(build: BrickRendererBuilder<B, S>.() -> Unit): BrickRenderer<B, S> = BrickRendererBuilder<B, S>().apply(build).build()
