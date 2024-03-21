package com.edotassi.amazmod.ui

import amazmod.com.transport.util.ImageUtils
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.view.LayoutInflaterCompat
import com.edotassi.amazmod.R
import com.edotassi.amazmod.util.FilesUtil
import com.mikepenz.iconics.context.IconicsLayoutInflater2
import kotlinx.android.synthetic.main.activity_image_compression_test.buttonCompressJPG
import kotlinx.android.synthetic.main.activity_image_compression_test.buttonCompressPNG
import kotlinx.android.synthetic.main.activity_image_compression_test.buttonCompressWebp
import kotlinx.android.synthetic.main.activity_image_compression_test.imageAfter0
import kotlinx.android.synthetic.main.activity_image_compression_test.imageAfter1
import kotlinx.android.synthetic.main.activity_image_compression_test.imageAfter2
import kotlinx.android.synthetic.main.activity_image_compression_test.imageBefore0
import kotlinx.android.synthetic.main.activity_image_compression_test.imageBefore1
import kotlinx.android.synthetic.main.activity_image_compression_test.imageBefore2
import kotlinx.android.synthetic.main.activity_image_compression_test.seekBarCompressionLevel
import kotlinx.android.synthetic.main.activity_image_compression_test.textViewCompressinPercent
import kotlinx.android.synthetic.main.activity_image_compression_test.textViewSizeAfter
import kotlinx.android.synthetic.main.activity_image_compression_test.textViewSizeBefore
import org.tinylog.kotlin.Logger

class ImageCompressionActivity : BaseAppCompatActivity() {

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LayoutInflaterCompat.setFactory2(layoutInflater, IconicsLayoutInflater2(delegate))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_compression_test)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setTitle("Image Compression Test")
        } catch (exception: NullPointerException) {
            Logger.error(exception, "AboutActivity onCreate exception: {}", exception.message)
        }

        val drawable0 = this.resources.getDrawable(R.drawable.test_image)
        val drawable1 = this.resources.getDrawable(R.drawable.art_material_motion)
        val drawable2 = this.resources.getDrawable(R.drawable.dummy_icon)

        imageBefore0.setImageDrawable(drawable0);
        imageBefore1.setImageDrawable(drawable1);
        imageBefore2.setImageDrawable(drawable2);

        val drawableToBitmap0 = ImageUtils.drawableToBitmap(drawable0)
        val drawableToBitmap1 = ImageUtils.drawableToBitmap(drawable1)
        val drawableToBitmap2 = ImageUtils.drawableToBitmap(drawable2)

        textViewSizeBefore.setText(
            "Size 1: " + FilesUtil.formatBytes(drawableToBitmap0.byteCount.toLong()) + ", " +
            "Size 2: " + FilesUtil.formatBytes(drawableToBitmap1.byteCount.toLong()) + ", " +
                    "Size 3: " + FilesUtil.formatBytes(drawableToBitmap2.byteCount.toLong())
        )

        seekBarCompressionLevel.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                textViewCompressinPercent.setText(p1.toString())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        buttonCompressPNG.setOnClickListener(View.OnClickListener {
            val compressedBytes0 =
                ImageUtils.bitmap2bytes(drawableToBitmap0, seekBarCompressionLevel.progress)
            val compressedBytes1 =
                ImageUtils.bitmap2bytes(drawableToBitmap1, seekBarCompressionLevel.progress)
            val compressedBytes2 =
                ImageUtils.bitmap2bytes(drawableToBitmap2, seekBarCompressionLevel.progress)

            imageAfter0.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes0))
            imageAfter1.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes1))
            imageAfter2.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes2))

            textViewSizeAfter.setText(
                "Size 1: " + FilesUtil.formatBytes(compressedBytes0.size.toLong()) + ", " +
                "Size 2: " + FilesUtil.formatBytes(compressedBytes1.size.toLong()) + ", " +
                        "Size 3: " + FilesUtil.formatBytes(compressedBytes2.size.toLong())
            )
        })

        buttonCompressJPG.setOnClickListener(View.OnClickListener {
            val compressedBytes0 =
                ImageUtils.bitmap2bytesJpeg(drawableToBitmap0, seekBarCompressionLevel.progress)
            val compressedBytes1 =
                ImageUtils.bitmap2bytesJpeg(drawableToBitmap1, seekBarCompressionLevel.progress)
            val compressedBytes2 =
                ImageUtils.bitmap2bytesJpeg(drawableToBitmap2, seekBarCompressionLevel.progress)

            imageAfter0.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes0))
            imageAfter1.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes1))
            imageAfter2.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes2))

            textViewSizeAfter.setText(
                "Size 1: " + FilesUtil.formatBytes(compressedBytes0.size.toLong()) + ", " +
                "Size 2: " + FilesUtil.formatBytes(compressedBytes1.size.toLong()) + ", " +
                        "Size 3: " + FilesUtil.formatBytes(compressedBytes2.size.toLong())
            )
        })

        buttonCompressWebp.setOnClickListener(View.OnClickListener {
            val compressedBytes0 =
                ImageUtils.bitmap2bytesWebp(drawableToBitmap0, seekBarCompressionLevel.progress)
            val compressedBytes1 =
                ImageUtils.bitmap2bytesWebp(drawableToBitmap1, seekBarCompressionLevel.progress)
            val compressedBytes2 =
                ImageUtils.bitmap2bytesWebp(drawableToBitmap2, seekBarCompressionLevel.progress)

            imageAfter0.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes0))
            imageAfter1.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes1))
            imageAfter2.setImageBitmap(ImageUtils.bytes2Bitmap(compressedBytes2))

            textViewSizeAfter.setText(
                "Size 1: " + FilesUtil.formatBytes(compressedBytes0.size.toLong()) + ", " +
                "Size 2: " + FilesUtil.formatBytes(compressedBytes1.size.toLong()) + ", " +
                        "Size 3: " + FilesUtil.formatBytes(compressedBytes2.size.toLong())
            )
        })
    }
}