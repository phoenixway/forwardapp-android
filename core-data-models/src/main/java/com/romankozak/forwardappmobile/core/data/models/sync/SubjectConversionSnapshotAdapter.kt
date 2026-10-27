package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Conversion-aware wire generation, independent of H1/Planning versions.
 * Older SnapshotBundle decoders require an integer snapshotVersion and therefore
 * reject this token before they can discard history or selectively import rows.
 */
class SubjectConversionSnapshotAdapter : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != SnapshotBundle::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        val trees = gson.getAdapter(JsonElement::class.java)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                if (value == null) { out.nullValue(); return }
                val tree = delegate.toJsonTree(value).asJsonObject
                if ((value as SnapshotBundle).managedSubjectConversions.orEmpty().isNotEmpty()) {
                    tree.add("canonicalSnapshotBaseVersion", tree.get("snapshotVersion"))
                    tree.addProperty("snapshotVersion", if (value.managedSubjectConversions.orEmpty().any { it.formatVersion == 2 }) KIND_GENERATION else GENERATION)
                }
                trees.write(out, tree)
            }

            override fun read(input: JsonReader): T? {
                val tree = trees.read(input)
                if (tree == null || tree.isJsonNull) return null
                val root = tree.asJsonObject
                val version = root.get("snapshotVersion") ?: root.get("version")
                val marker = version?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                val facts = root.get("managedSubjectConversions")
                if (marker == GENERATION || marker == KIND_GENERATION) {
                    require(facts != null && facts.isJsonArray) { "Conversion generation requires explicit history" }
                    if (marker == GENERATION) require(facts.asJsonArray.none { it.asJsonObject.get("formatVersion")?.asInt == 2 }) { "Kind changes require conversion generation 2" }
                    val base = requireNotNull(root.get("canonicalSnapshotBaseVersion")) { "Missing base snapshot version" }
                    require(base.isJsonPrimitive && base.asJsonPrimitive.isNumber && base.asString in setOf("1", "2")) { "Unsupported base snapshot version" }
                    root.add("snapshotVersion", base)
                    root.remove("version")
                    root.remove("canonicalSnapshotBaseVersion")
                } else {
                    require(marker == null || !marker.startsWith("semantic-subject-conversion/")) { "Unsupported semantic conversion generation" }
                    require(facts == null || facts.isJsonNull || (facts.isJsonArray && facts.asJsonArray.size() == 0)) {
                        "Conversion history requires conversion-aware wire generation"
                    }
                }
                return delegate.fromJsonTree(root)
            }
        }
    }

    companion object {
        const val GENERATION = "semantic-subject-conversion/1"
        const val KIND_GENERATION = "semantic-subject-conversion/2"
    }
}

/** Do not let a desktop subset adapter silently discard this Android canonical generation. */
fun Gson.decodeCanonicalSnapshotRoot(root: JsonObject): SnapshotBundle? {
    val version = root.get("snapshotVersion") ?: root.get("version")
    val marked = version?.let { it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.startsWith("semantic-subject-conversion/") } == true
    return if (marked || root.has("snapshotVersion") || root.has("managedSubjectConversions")) fromJson(root, SnapshotBundle::class.java) else null
}
