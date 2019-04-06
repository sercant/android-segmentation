package com.sercanturkmen.mobilesegmenter

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Segmenter(
    context: Context,
    val config: Config
) {
    companion object {
        const val BYTES_PER_CHANNEL = 4
    }

    private val lock = Any()

    private val tfliteOptions: Interpreter.Options = Interpreter.Options()
    private val tfliteModel: MappedByteBuffer = context.assets.openFd(config.modelPath).use { fd ->
        fd.createInputStream().use { fis ->
            fis.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    private val interpreter: Interpreter = Interpreter(tfliteModel, tfliteOptions)
    var isClosed = false
        private set

    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(
            config.inputWidth *
                    config.inputHeight *
                    config.inputChannelCount *
                    BYTES_PER_CHANNEL
        ).apply {
            order(ByteOrder.nativeOrder())
        }

    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(
            config.outputWidth *
                    config.outputHeight *
                    BYTES_PER_CHANNEL
        ).apply {
            order(ByteOrder.nativeOrder())
        }

    private val inputIntArray: IntArray = IntArray(config.inputWidth * config.inputHeight)
    private val outputIntArray: IntArray = IntArray(config.outputWidth * config.outputHeight)

    private fun prepareInputBuffer(bitmap: Bitmap) {
        bitmap.getPixels(inputIntArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        inputBuffer.rewind()
        for (i in 0 until inputIntArray.size) {
            val pixelValue = inputIntArray[i]
            inputBuffer.apply {
                putFloat((pixelValue shr 16 and 0xFF).toFloat())
                putFloat((pixelValue shr 8 and 0xFF).toFloat())
                putFloat((pixelValue and 0xFF).toFloat())
            }
        }
    }

    fun segment(bitmap: Bitmap): IntArray {
        synchronized(lock) {
            if (isClosed) throw Exception("Trying to use a closed segmenter!")

            prepareInputBuffer(bitmap)
            outputBuffer.rewind()

            interpreter.run(inputBuffer, outputBuffer)

            outputBuffer.position(0)
            var i = 0
            while (outputBuffer.hasRemaining())
                outputIntArray[i++] = outputBuffer.int

            return outputIntArray
        }
    }

    fun close() {
        synchronized(lock) {
            tfliteModel.clear()
            interpreter.close()
            isClosed = true
        }
    }

    data class Config(
        val modelPath: String,
        val inputWidth: Int,
        val inputHeight: Int,
        val outputWidth: Int,
        val outputHeight: Int,
        val classCount: Int,
        val inputChannelCount: Int = 3
    ) {
        companion object {
            @JvmStatic
            fun fromJson(json: String): Config? {
                return try {
                    val obj = JSONObject(json)
                    val input = obj.getJSONObject("input")
                    val output = obj.getJSONObject("output")
                    val model = obj.getJSONObject("model")

                    Config(
                        model.getString("filePath"),
                        input.getInt("width"),
                        input.getInt("height"),
                        output.getInt("width"),
                        output.getInt("height"),
                        model.getInt("classCount"),
                        input.getInt("channelCount")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            @JvmStatic
            fun fromJsonFile(context: Context, filePath: String): Config? {
                return try {
                    context.assets
                        .open(filePath)
                        .bufferedReader()
                        .use {
                            val json = it.readText()
                            fromJson(json)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }
}