package de.ueen.morphbutton

import android.animation.*
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlin.math.min


class MorphButton(context: Context, attrs: AttributeSet): MaterialButton(context, attrs) {

    var currentId: String

    companion object {const val FRIST = "first"}

    private var prevMorph: MorphParams? = null

    private var initialMorph: MorphParams? = null

    private var currentColor = 0

    private var animating = false

    init {
        val tv = TypedValue()
        context.theme.resolveAttribute(R.attr.colorPrimary, tv, true)
        currentColor = tv.data

        context.theme.obtainStyledAttributes(attrs, R.styleable.MorphButton,0,0).apply {
            try {
                currentColor = getColor(R.styleable.MorphButton_backgroundColor, currentColor)
            } finally { recycle() }
        }

        currentId = FRIST
        this@MorphButton.setBackgroundColor(currentColor)
        this@MorphButton.iconPadding = 0
        this@MorphButton.iconGravity = ICON_GRAVITY_TEXT_START
}



    fun setOnClickListener(onClick: (it: MorphButton, morphId: String) -> Unit) {
        super.setOnClickListener {
            if (!animating) {onClick(this@MorphButton,currentId)}
        }
    }
    fun setOnLongClickListener(onLongClick: (it: MorphButton, morphId: String) -> Unit) {
        super.setOnLongClickListener {
            if (!animating) {onLongClick(this@MorphButton,currentId)}

            true
        }
    }

    fun reverse(onEnd: (morphId: String) -> Unit = {}) {
        prevMorph?.let { morph(it,onEnd) }
    }

    fun backToFirst(durationMS: Int? = null, onEnd: (morphId: String) -> Unit = {}) {
        initialMorph?.let {
            morph(it.apply { duration = durationMS },onEnd)
        }
    }

    fun morph(morphParams: MorphParams, onEnd: (morphId: String) -> Unit = {}) {
        if (initialMorph == null) {
            initialMorph = MorphParams(FRIST, currentColor,this@MorphButton.cornerRadius,this@MorphButton.width,this@MorphButton.height, this@MorphButton.icon, this@MorphButton.iconSize, text = this@MorphButton.text.toString(), textSize = this@MorphButton.textSize)
        }

        val durationMS = (morphParams.duration)?: resources.getInteger(android.R.integer.config_shortAnimTime)

        prevMorph = MorphParams(currentId, currentColor,this@MorphButton.cornerRadius,this@MorphButton.width,this@MorphButton.height, this@MorphButton.icon, this@MorphButton.iconSize, text = this@MorphButton.text.toString(), duration = durationMS, keepText=morphParams.keepText, textSize = this@MorphButton.textSize)

        currentId = morphParams.morphId

        val anim = AnimatorSet()

        if (morphParams.circle) {
            val dia = min(this@MorphButton.width, this@MorphButton.height)
            morphParams.width = dia
            morphParams.height = dia
            morphParams.radius = dia/2
        }
        morphParams.radius?.let{r ->
            anim.play(ObjectAnimator.ofInt(this@MorphButton, "cornerRadius", r))
        }
        anim.play(ValueAnimator.ofInt(this@MorphButton.width, (morphParams.width)?:this@MorphButton.width).apply {
            addUpdateListener {
                val valu = it.animatedValue
                if (valu is Int) {
                    this@MorphButton.layoutParams = this@MorphButton.layoutParams.apply { width = valu }
                }
            }
        })
        anim.play(ValueAnimator.ofInt(this@MorphButton.height, (morphParams.height)?:this@MorphButton.height).apply {
            addUpdateListener {
                val valu = it.animatedValue
                if (valu is Int) {
                    this@MorphButton.layoutParams = this@MorphButton.layoutParams.apply { height = valu }
                }
            }
        })
        morphParams.color?.let {c ->
            anim.play(ValueAnimator.ofInt(currentColor, c).apply {
                setEvaluator(ArgbEvaluator())
                addUpdateListener {
                    val valu = it.animatedValue
                    if (valu is Int) {
                        this@MorphButton.setBackgroundColor(valu)
                    }
                }

            })
            currentColor = c
        }
        morphParams.icon?.let { ic ->
            this@MorphButton.iconSize = morphParams.iconSize?: (morphParams.height?: this@MorphButton.height)
            anim.play(ValueAnimator.ofInt(0, 255).apply {
                addUpdateListener {
                    val valu = it.animatedValue
                    if (valu is Int) {
                        this@MorphButton.icon = ic.apply{alpha = valu}
                    }
                }
                startDelay = (durationMS - resources.getInteger(android.R.integer.config_shortAnimTime)).toLong()
            })
        }

        if (!morphParams.keepText) {
            this@MorphButton.text = ""
        } else {
            morphParams.text = null
        }
        morphParams.textSize?.let {this@MorphButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, it)}
        this@MorphButton.icon = null

        anim.apply {
            duration = durationMS.toLong()
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    animating = true
                }
                override fun onAnimationEnd(animation: Animator?) {
                    animating = false
                    morphParams.text?.let { t ->
                        this@MorphButton.text = t
                        ValueAnimator.ofInt(Color.TRANSPARENT, this@MorphButton.currentTextColor).apply {
                            setEvaluator(ArgbEvaluator())
                            duration = (resources.getInteger(android.R.integer.config_shortAnimTime) / 2).toLong()
                            addUpdateListener {
                                val valu = it.animatedValue
                                if (valu is Int) {
                                    this@MorphButton.setTextColor(valu)
                                }
                            }
                            start()
                        }
                    }
                    onEnd(currentId)
                }
            })
            start()
        }
    }

    class MorphParams(val morphId: String, var color: Int? = null, var radius: Int? = null, var width: Int? = null, var height: Int? = null, var icon: Drawable? = null, var iconSize: Int? = null , var text: String? = null, var textSize: Float? = null, var circle: Boolean = false, var duration: Int? = null, var keepText: Boolean = false )
}

