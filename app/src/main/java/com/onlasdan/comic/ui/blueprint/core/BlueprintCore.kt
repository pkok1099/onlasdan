package com.onlasdan.comic.ui.blueprint.core

enum class ScreenSlot {
    Header,
    Aside,
    Body,
    BottomSheet,
    Overlay,
}

data class Placement<S, B>(
    val brick: B,
    val predicate: (S) -> Boolean = { true },
    val metadata: Map<String, Any> = emptyMap(),
)

data class ScreenBlueprint<S, B>(val slots: Map<ScreenSlot, List<Placement<S, B>>>)

class BlueprintBuilder<S, B> {
    private val slots = linkedMapOf<ScreenSlot, MutableList<Placement<S, B>>>()

    fun slot(slot: ScreenSlot, build: SlotBuilder<S, B>.() -> Unit) {
        val list = slots.getOrPut(slot) { mutableListOf() }
        SlotBuilder(list).build()
    }

    fun build(): ScreenBlueprint<S, B> = ScreenBlueprint(slots.mapValues { it.value.toList() })
}

class SlotBuilder<S, B>(private val list: MutableList<Placement<S, B>>) {
    /**
     * Standard place without metadata.
     */
    fun place(brick: B, predicate: (S) -> Boolean = { true }) {
        list.add(Placement(brick, predicate, emptyMap()))
    }

    /**
     * Place with metadata configuration.
     */
    fun place(brick: B, metadata: Map<String, Any>, predicate: (S) -> Boolean = { true }) {
        list.add(Placement(brick, predicate, metadata))
    }
}

fun <S, B> blueprint(build: BlueprintBuilder<S, B>.() -> Unit): ScreenBlueprint<S, B> = BlueprintBuilder<S, B>().apply(build).build()
