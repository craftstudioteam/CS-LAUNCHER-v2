package com.craftstudio.launcher.anim.animations

import com.craftstudio.launcher.anim.animations.bounce.BounceEnlargeAnimator
import com.craftstudio.launcher.anim.animations.bounce.BounceInDownAnimator
import com.craftstudio.launcher.anim.animations.bounce.BounceInLeftAnimator
import com.craftstudio.launcher.anim.animations.bounce.BounceInRightAnimator
import com.craftstudio.launcher.anim.animations.bounce.BounceInUpAnimator
import com.craftstudio.launcher.anim.animations.bounce.BounceShrinkAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeInAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeInDownAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeInLeftAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeInRightAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeInUpAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeOutAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeOutDownAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeOutLeftAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeOutRightAnimator
import com.craftstudio.launcher.anim.animations.fade.FadeOutUpAnimator
import com.craftstudio.launcher.anim.animations.other.PulseAnimator
import com.craftstudio.launcher.anim.animations.other.ShakeAnimator
import com.craftstudio.launcher.anim.animations.other.WobbleAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideInDownAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideInLeftAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideInRightAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideInUpAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideOutDownAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideOutLeftAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideOutRightAnimator
import com.craftstudio.launcher.anim.animations.slide.SlideOutUpAnimator

enum class Animations(val animator: BaseAnimator) {
    //Bounce
    BounceInDown(BounceInDownAnimator()),
    BounceInLeft(BounceInLeftAnimator()),
    BounceInRight(BounceInRightAnimator()),
    BounceInUp(BounceInUpAnimator()),
    BounceEnlarge(BounceEnlargeAnimator()),
    BounceShrink(BounceShrinkAnimator()),

    //Fade in
    FadeIn(FadeInAnimator()),
    FadeInLeft(FadeInLeftAnimator()),
    FadeInRight(FadeInRightAnimator()),
    FadeInUp(FadeInUpAnimator()),
    FadeInDown(FadeInDownAnimator()),

    //Fade out
    FadeOut(FadeOutAnimator()),
    FadeOutLeft(FadeOutLeftAnimator()),
    FadeOutRight(FadeOutRightAnimator()),
    FadeOutUp(FadeOutUpAnimator()),
    FadeOutDown(FadeOutDownAnimator()),

    //Slide in
    SlideInLeft(SlideInLeftAnimator()),
    SlideInRight(SlideInRightAnimator()),
    SlideInUp(SlideInUpAnimator()),
    SlideInDown(SlideInDownAnimator()),

    //Slide out
    SlideOutLeft(SlideOutLeftAnimator()),
    SlideOutRight(SlideOutRightAnimator()),
    SlideOutUp(SlideOutUpAnimator()),
    SlideOutDown(SlideOutDownAnimator()),

    //Other
    Pulse(PulseAnimator()),
    Wobble(WobbleAnimator()),
    Shake(ShakeAnimator())
}