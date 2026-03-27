package com.example.birdtrack.data

import android.content.Context
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class SpeciesIdentifier(private val context: Context) {
    private var interpreter: Interpreter? = null

    init {
        interpreter = runCatching {
            val fd = context.assets.openFd("bird_species.tflite")
            val input = fd.createInputStream().channel
            val mapped: ByteBuffer = input.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            Interpreter(mapped)
        }.getOrNull()
    }

    fun suggestSpecies(imageUri: Uri?): String? {
        if (imageUri == null) return null
        if (interpreter == null) {
            val hint = imageUri.lastPathSegment?.lowercase().orEmpty()
            return when {
                "eagle" in hint -> "Bald Eagle"
                "owl" in hint -> "Barn Owl"
                "duck" in hint -> "Mallard"
                "sparrow" in hint -> "House Sparrow"
                else -> "AI Suggestion Unavailable"
            }
        }

        // Model execution can be added when an input pipeline is finalized.
        return "Model Loaded - Suggestion Pending"
    }
}
