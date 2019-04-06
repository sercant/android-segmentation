# Mobile Segmenter

[![](https://jitpack.io/v/sercant/android-segmentation.svg)](https://jitpack.io/#sercant/android-segmentation)

This repository holds an example of how to use Tensorflow Lite to run segmentation on an Android phone. It also serves as a library which you can use in your application for a simple segmentation job.

## Adding as a dependency

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```gradle
dependencies {
    implementation 'com.github.sercant:android-segmentation:-SNAPSHOT'
}
```

## How to use

Put the model config file and the tflite model under the assets folder.

An example `model_config.json`:

```json
{
  "input": {
    "width": 225,
    "height": 225,
    "channelCount": 3
  },
  "output": {
    "width": 225,
    "height": 225,
    "channelCount": 1
  },
  "model": {
    "filePath": "model.tflite",
    "classCount": 19
  }
}
```

In your code, initialize the configuration by reading the json file or filling the configuration by hand.

```kotlin
val config: Config? = context.assets
    .open("model_config.json")
    .bufferedReader()
    .use {
        val json = it.readText()
        Segmenter.Config.fromJson(json)
    }
// config can return null so be sure to check the result.

// Initialize the image segmentation
val segmenter = Segmenter(context, config)

// ...

// Segment a frame (Bitmap should be in `Bitmap.Config.RGBA_8888` format)
val result: IntArray = segmenter.segment(image)

// ...

// To uninitialize the image segmentation simply call
segmenter.close()

// After this point the class instance shouldn't be used again.
```
