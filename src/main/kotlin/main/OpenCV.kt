package main

import nu.pattern.OpenCV
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

object OpenCV {

    init {
        OpenCV.loadLocally()
    }

    fun ByteArray.toMat(): Mat = Imgcodecs.imdecode(MatOfByte(*this), Imgcodecs.IMREAD_UNCHANGED)

    fun Mat.crop(rect: Rect) = Mat(
        this, rect
    )

    fun Mat.paddingAndResize(targetSize: Int): Mat {
        val width = width()
        val height = height()

        if ((width > targetSize) or (height > targetSize)) {
            val cropSize = targetSize / maxOf(width, height).toDouble()
            val cropTargetWidth = width * cropSize
            val cropTargetHeight = height * cropSize

            try {

                Imgproc.resize(this, this, Size(cropTargetWidth, cropTargetHeight))
            } catch (e: Exception) {
                println("$width  $height  $cropSize  $cropTargetWidth  $cropTargetHeight")
            }
        }

        val padX = targetSize - width()
        val padY = targetSize - height()

        if (padX == 0 && padY == 0) {
            return this
        }

        val padLeft = padX / 2
        val padRight = padX - padLeft
        val padTop = padY / 2
        val padBottom = padY - padTop

        val croppedRegion = Mat()
        try {
            Core.copyMakeBorder(
                this, croppedRegion, padTop, padBottom, padLeft, padRight, Core.BORDER_CONSTANT, Scalar(0.0, 0.0, 0.0)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return croppedRegion
    }
}

/*
resized_cropped_region = cv2.resize(new_cropped_region_rgb, (new_width, new_height))

# Calculate padding to make the image 384x384
pad_x = target_size - new_width
pad_y = target_size - new_height

pad_left = pad_x // 2
pad_right = pad_x - pad_left
pad_top = pad_y // 2
pad_bottom = pad_y - pad_top

# Add padding to the resized cropped region
final_cropped_region = cv2.copyMakeBorder(resized_cropped_region, pad_top, pad_bottom, pad_left, pad_right, cv2.BORDER_CONSTANT, value=[0, 0, 0])

# Verify the final size
final_size = final_cropped_region.shape[:2]

# Display the final 384x384 region
plt.imshow(final_cropped_region)
plt.axis('off')
plt.show()

final_size
 */