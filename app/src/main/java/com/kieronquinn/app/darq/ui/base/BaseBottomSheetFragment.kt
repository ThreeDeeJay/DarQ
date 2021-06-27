package com.kieronquinn.app.darq.ui.base

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.core.view.*
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.providers.blur.BlurProvider
import com.kieronquinn.app.darq.ui.utils.autoCleared
import com.kieronquinn.app.darq.utils.extensions.isDarkTheme
import com.kieronquinn.monetcompat.core.MonetCompat
import org.koin.android.ext.android.inject

abstract class BaseBottomSheetFragment<T: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T): BottomSheetDialogFragment() {

    internal val monet by lazy {
        MonetCompat.getInstance()
    }

    internal var binding by autoCleared<T>()

    private val blurProvider by inject<BlurProvider>()
    private var isBlurShowing = false
    private val bottomSheetCallback = object: BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            showBlurAnimation?.cancel()
            applyBlur(1f + slideOffset)
        }

    }

    open val cancelable: Boolean = true
    private var behavior: BottomSheetBehavior<*>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = object : BottomSheetDialog(requireContext(), theme) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()

                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                }

                findViewById<View>(com.google.android.material.R.id.container)?.apply {
                    fitsSystemWindows = false
                    val topMargin = marginTop
                    ViewCompat.setOnApplyWindowInsetsListener(this){ view, insets ->
                        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            updateMargins(top = topMargin + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
                        }
                        insets
                    }
                }

                findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
            }
        }
        dialog.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.navigationBarColor = Color.TRANSPARENT
            ViewCompat.setOnApplyWindowInsetsListener(it.decorView) { view, insets ->
                val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updatePadding(left = navigationInsets.left, right = navigationInsets.right)
                insets
            }
        }
        dialog.setOnShowListener {
            (binding.root.parent as View).backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
            behavior = BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet)!!).apply {
                isDraggable = cancelable
                state = BottomSheetBehavior.STATE_EXPANDED
                addBottomSheetCallback(bottomSheetCallback)
            }
        }
        isCancelable = cancelable
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        behavior?.removeBottomSheetCallback(bottomSheetCallback)
        behavior = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = inflate.invoke(layoutInflater, container, false)
        dialog?.setOnShowListener {
            (binding.root.parent as View).backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
        }
        return binding.root
    }

    private var showBlurAnimation: ValueAnimator? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showBlurAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250L
            addUpdateListener {
                applyBlur(it.animatedValue as Float)
            }
            addListener(onEnd = {
                isBlurShowing = true
            })
            start()
        }
    }

    private fun applyBlur(ratio: Float){
        val dialogWindow = dialog?.window ?: return
        val appWindow = activity?.window ?: return
        dialogWindow.decorView.post {
            blurProvider.applyBlur(dialogWindow, appWindow, ratio)
        }
    }

    override fun onResume() {
        super.onResume()
        if(isBlurShowing){
            view?.post {
                applyBlur(1f)
            }
        }
    }

    override fun getTheme(): Int {
        return if(requireContext().isDarkTheme){
            R.style.BaseBottomSheetDialog_Dark
        }else{
            R.style.BaseBottomSheetDialog
        }
    }

}