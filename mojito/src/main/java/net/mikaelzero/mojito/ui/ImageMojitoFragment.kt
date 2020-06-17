package net.mikaelzero.mojito.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_image.*
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.Mojito.Companion.contentLoader
import net.mikaelzero.mojito.Mojito.Companion.imageLoader
import net.mikaelzero.mojito.Mojito.Companion.imageViewFactory
import net.mikaelzero.mojito.MojitoView
import net.mikaelzero.mojito.R
import net.mikaelzero.mojito.bean.ContentViewOriginModel
import net.mikaelzero.mojito.interfaces.IMojitoFragment
import net.mikaelzero.mojito.interfaces.ImageViewLoadFactory
import net.mikaelzero.mojito.interfaces.OnMojitoViewCallback
import net.mikaelzero.mojito.loader.*
import net.mikaelzero.mojito.tools.ScreenUtils
import java.io.File


class ImageMojitoFragment : Fragment(), IMojitoFragment, OnMojitoViewCallback {
    var contentViewOriginModel: ContentViewOriginModel? = null
    var originUrl: String? = null
    var showView: View? = null
    var position = 0
    var showImmediately = false
    private var mImageLoader: ImageLoader? = null
    private var mViewLoadFactory: ImageViewLoadFactory? = null
    private var contentLoader: ContentLoader? = null
    private var mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (context == null || activity == null) {
            return
        }
        mImageLoader = imageLoader()
        mViewLoadFactory = imageViewFactory()
        if (arguments != null) {
            originUrl = arguments!!.getString("originUrl")
            position = arguments!!.getInt("position")
            showImmediately = arguments!!.getBoolean("showImmediately")
            contentViewOriginModel = arguments!!.getParcelable("model")
        }
        contentLoader = contentLoader()?.newInstance(viewLifecycleOwner)
        mojitoView?.setContentLoader(contentLoader)
        showView = contentLoader?.providerRealView()


        mojitoView?.setOnMojitoViewCallback(this)

        contentLoader?.onTapCallback(object : OnTapCallback {
            override fun onTap(view: View, x: Float, y: Float) {
                mojitoView?.backToMin()
                Mojito.instance.mojitoListener()?.onClick(view, x, y, position)
            }
        })
        contentLoader?.onLongTapCallback(object : OnLongTapCallback {
            override fun onLongTap(view: View, x: Float, y: Float) {
                Mojito.instance.mojitoListener()?.onLongClick(activity, view, x, y, position)
            }
        })
        mImageLoader?.loadImage(showView.hashCode(), Uri.parse(originUrl), object : DefaultImageCallback() {
            override fun onSuccess(image: File) {
                mViewLoadFactory?.loadSillContent(showView!!, Uri.fromFile(image))
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(image.absolutePath, options)
                var h = options.outHeight
                var w = options.outWidth
                val isLongImage = contentLoader?.isLongImage(w, h)
                if (isLongImage != null && isLongImage) {
                    w = ScreenUtils.getScreenWidth(context)
                    h = ScreenUtils.getScreenHeight(context)
                }
                mojitoView?.putData(
                    contentViewOriginModel!!.getLeft(), contentViewOriginModel!!.getTop(),
                    contentViewOriginModel!!.getWidth(), contentViewOriginModel!!.getHeight(),
                    w, h
                )
                mojitoView?.show(showImmediately && !ImageMojitoActivity.showImmediatelyFlag)
                ImageMojitoActivity.showImmediatelyFlag = false
            }
        })
    }

    override fun backToMin() {
        mojitoView?.backToMin()
    }

    override fun replaceImageUrl(url: String) {

    }

    companion object {
        fun newInstance(originUrl: String?, position: Int, shouldShowAnimation: Boolean, contentViewOriginModel: ContentViewOriginModel?): ImageMojitoFragment {
            val args = Bundle()
            args.putString("originUrl", originUrl)
            args.putInt("position", position)
            args.putBoolean("showImmediately", shouldShowAnimation)
            args.putParcelable("model", contentViewOriginModel)
            val fragment = ImageMojitoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onMojitoViewFinish() {
        Mojito.instance.mojitoListener()?.onMojitoViewFinish()
        if (context is ImageMojitoActivity) {
            (context as ImageMojitoActivity).finishView()
        }
    }

    override fun onDrag(view: MojitoView, moveX: Float, moveY: Float) {
        Mojito.iIndicator?.move(moveX, moveY)
        Mojito.coverLayoutLoader?.move(moveX, moveY)
        Mojito.instance.mojitoListener()?.onDrag(view, moveX, moveY)
    }

    override fun onRelease(isToMax: Boolean, isToMin: Boolean) {
        Mojito.iIndicator?.fingerRelease(isToMax, isToMin)
        Mojito.coverLayoutLoader?.fingerRelease(isToMax, isToMin)
    }

    override fun showFinish(mojitoView: MojitoView, showImmediately: Boolean) {
        Mojito.instance.mojitoListener()?.onShowFinish(mojitoView, showImmediately)
    }

    override fun onLock(isLock: Boolean) {
        if (context is ImageMojitoActivity) {
            (context as ImageMojitoActivity).setViewPagerLock(isLock)
        }
    }


}