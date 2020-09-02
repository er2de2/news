package co.appreactor.nextcloud.news

import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.squareup.picasso.Picasso

class PicassoImageGetter(private val textView: TextView) : Html.ImageGetter {

    override fun getDrawable(source: String): Drawable {
        val drawable = LazyTextViewDrawable(textView) // TODO figure out better approach

        Picasso.get()
            .load(source)
            .resize(textView.width, 0)
            .into(drawable)

        return drawable
    }
}

