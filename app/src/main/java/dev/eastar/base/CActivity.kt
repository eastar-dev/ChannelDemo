package dev.eastar.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.log.Log
import android.log.LogActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dev.eastar.ktx.startMain
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

abstract class CActivity : LogActivity() {
    val mContext by lazy { this }
    val mActivity by lazy { this }

    @SuppressLint("SourceLockedOrientationActivity")
    protected open fun setRequestedOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    override fun setRequestedOrientation(requestedOrientation: Int) {
        kotlin.runCatching { super.setRequestedOrientation(requestedOrientation) }
    }

    protected open fun setSoftInputMode() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    /** onParseExtra() -> onLoadOnce() -> onReload() -> onClear() -> onLoad() -> onUpdateUI() */


    override fun onCreate(savedInstanceState: Bundle?) {
        setRequestedOrientation()
        setSoftInputMode()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun autoDisposable() {
                disposables
                    .filterNot { it.isDisposed }
                    .forEach { it.dispose() }

                progressDismessDisposables()
            }
        })
    }

    var disposables = mutableSetOf<Disposable>()
    fun Disposable.autoDispose() = this.also { disposables.add(it) }

    //progress/////////////////////////////////////////////////////////////////////////////
    private val mProgress: DialogFragment by lazy { createProgress() }
    protected open fun createProgress(): DialogFragment = CProgressDialogFragment()
    class CProgressDialogFragment : DialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            return ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        }
    }

    fun showProgress() {
        progressDismessDisposables()
        if (mProgress.isAdded) {
            return
        }
        mProgress.show(supportFragmentManager, "progress")
    }

    fun dismissProgress() {
        dismessDisposables = Observable.just(false)
            .delay(500L, TimeUnit.MILLISECONDS)
            .subscribe {
                dismissProgressForce()
            }
    }

    private var dismessDisposables: Disposable? = null
    fun progressDismessDisposables(): Unit? {
        return dismessDisposables?.takeUnless { it.isDisposed }?.dispose()
    }

    fun dismissProgressForce() {
        if (mProgress.isAdded) mProgress.dismissAllowingStateLoss()
    }


    //main exit/////////////////////////////////////////////////////////////////
    open fun main() {
        Log.e(javaClass, "public void main()")
        startMain()
    }

    open fun exit() {
        Log.e(javaClass, "public void exit()")
        finish()
    }


    //clicked////////////////////////////////////////////////////////////////////
    protected open fun onBackPressedEx(): Boolean {
        return false
    }

    override fun onBackPressed() {
        if (onBackPressedEx()) return
        super.onBackPressed()
    }

    open fun onHeaderBack(v: View?) {
        Log.d(javaClass, "onHeaderBack")
        onBackPressed()
    }

    open fun onHeaderMain(v: View?) {
        Log.d(javaClass, "onHeaderMain")
        main()
    }

    open fun onHeaderTitle(v: View?) = Log.d(javaClass, "onHeaderTitle")
    open fun onHeaderLogin(v: View?) = Log.d(javaClass, "onHeaderLogin")
    open fun onHeaderMenu(v: View?) = Log.d(javaClass, "onHeaderMenu")
    open fun onHeaderLeft(v: View?) = Log.d(javaClass, "onHeaderLeft")
    open fun onHeaderRight(v: View?) = Log.d(javaClass, "onHeaderRight")

    //fragment/////////////////////////////////////////////////////////////////////////////////////////
    var mFragmentManager = supportFragmentManager
    protected fun setFragmentVisible(resid_fragment: Int, b: Boolean) {
        val fr = mFragmentManager.findFragmentById(resid_fragment)
        setFragmentVisible(fr, b)
    }

    protected fun setFragmentVisible(fr: Fragment?, b: Boolean) {
        val ft = mFragmentManager.beginTransaction()
        if (b == true) ft.show(fr!!) else ft.hide(fr!!)
        ft.commitAllowingStateLoss()
    }

    fun toast(text: Any?) {
        Toast.makeText(mContext, getText(text), Toast.LENGTH_SHORT).show()
    }

    private fun getDrawable(drawable: Any?): Drawable? {
        if (drawable == null) return null
        return if (drawable is Drawable) drawable else ResourcesCompat.getDrawable(mContext.resources, (drawable as Int?)!!, null)
    }

    private fun getText(text: Any?): CharSequence? {
        if (text == null) return null
        if (text is CharSequence) return text
        return if (text is Int) mContext.getString((text as Int?)!!) else text.toString()
    }

    @JvmOverloads
    fun showDialogSticky(
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null,
    ): AlertDialog {
        val dlg = showDialog(
            message, positiveButtonText, positiveListener, negativeButtonText, negativeListener
        )
        dlg.setCancelable(false)
        return dlg
    }

    @JvmOverloads
    fun showDialog(
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null
    ): AlertDialog {
        val context = mContext
        val dlg = getDialog(context, message, positiveButtonText, positiveListener, negativeButtonText, negativeListener)
        dlg.setCanceledOnTouchOutside(false)
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Log.w("activity is Lifecycle destroyed")
            return dlg
        }
        if (isFinishing) {
            Log.w("activity is isFinishing")
            return dlg
        }
        dlg.show()
        return dlg
    }

    private fun newAlertDialogBuilder(context: Context?): AlertDialog.Builder {
        return AlertDialog.Builder(context!!)
    }

    private fun getDialog(
        context: Context?,
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null
    ): AlertDialog {
        val builder = newAlertDialogBuilder(context)
        if (message != null) builder.setMessage(getText(message))
        if (positiveButtonText != null) builder.setPositiveButton(getText(positiveButtonText), positiveListener)
        if (negativeButtonText != null) builder.setNegativeButton(getText(negativeButtonText), negativeListener)
        return builder.create()
    }

    override fun <T : View> findViewById(@IdRes resid: Int): T? {
        if (resid == -1) {
            Log.w("id is NO_ID" + " in the " + javaClass.simpleName)
            return null
        }
        if (resid == 0) {
            Log.w("id is 0" + " in the " + javaClass.simpleName)
            return null
        }

        val v: T? = super.findViewById(resid)
        if (v == null) {
            Log.printStackTrace(Exception("!has not " + resources.getResourceName(resid) + " in the " + javaClass.simpleName))
        }
        return v
    }


}