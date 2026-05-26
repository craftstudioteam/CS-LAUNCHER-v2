package com.craftstudio.launcher.ui.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentAboutBinding

import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.stringutils.StringUtils

class AboutFragment : FragmentWithAnim(R.layout.fragment_about) {
    companion object {
        const val TAG: String = "AboutFragment"
        private const val SECTION_DELAY = 150L
        private const val DEFAULT_ACCENT = "#24B538"
        private const val PREFS_NAME = "appearance_prefs"
        private const val KEY_ACCENT_COLOR = "app_accent_color"

        fun getAccentColor(context: Context): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT) ?: DEFAULT_ACCENT
        }
    }

    private lateinit var binding: FragmentAboutBinding
    private val animatedViews = mutableSetOf<View>()
    private var accentColor: String = DEFAULT_ACCENT
    private var accentColorInt: Int = Color.parseColor(DEFAULT_ACCENT)

    // Role icon mapping
    private data class Member(val name: String, val role: String, val iconRes: Int, val iconTintGold: Boolean = false)

    private val founder = Member("NOT DANGER", "Founder & Owner", R.drawable.ic_crown, iconTintGold = true)

    private val developers = listOf(
        Member("ROHIT", "Lead Developer", R.drawable.ic_role_dev),
        Member("ENDER WARRIOR", "Developer", R.drawable.ic_code),
        Member("MINER ADI", "Developer", R.drawable.ic_code),
        Member("ONIZ.EXE", "Developer", R.drawable.ic_code)
    )

    private val staff = listOf(
        Member("RKMC", "Main Provider", R.drawable.ic_role_provider),
        Member("REALONESKY", "Second Provider", R.drawable.ic_role_provider),
        Member("BLIND GAMERRZ", "Head Moderator", R.drawable.ic_role_staff),
        Member("NOTERRORX", "Head Administrator", R.drawable.ic_role_staff)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        accentColor = getAccentColor(requireContext())
        accentColorInt = Color.parseColor(accentColor)

        applyAccentToHeaders()
        setupVersionInfo()
        setupButtons()
        populateTeam()
        setupScrollReveal()
        playCascadeAnimation()
    }

    private fun applyAccentToHeaders() {
        applyAccentToStrip(binding.founderSectionStrip)
        applyAccentToStrip(binding.devSectionStrip)
        applyAccentToStrip(binding.staffSectionStrip)
        tintSectionIcon(binding.founderSectionIcon)
        tintSectionIcon(binding.devSectionIcon)
        tintSectionIcon(binding.staffSectionIcon)
    }

    private fun applyAccentToStrip(view: View) {
        val bg = view.background?.mutate() as? GradientDrawable
        bg?.setColor(accentColorInt)
    }

    private fun tintSectionIcon(container: android.widget.FrameLayout) {
        val icon = (container.getChildAt(0) as? ImageView) ?: return
        icon.setColorFilter(accentColorInt)
    }
    private fun setupVersionInfo() {
        binding.aboutVersionInfo.text = StringUtils.insertSpace(
            getString(R.string.about_version_name),
            ZHTools.getVersionName()
        ) + "  \u2022  " + StringUtils.insertSpace(
            getString(R.string.about_version_code),
            ZHTools.getVersionCode()
        )
    }

    private fun setupButtons() {
        binding.returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        binding.creditsGithubButton.setOnClickListener {
            ZHTools.openLink(requireActivity(), "https://github.com/PojavLauncherTeam/PojavLauncher")
        }
        binding.footerDiscordButton.setOnClickListener {
            ZHTools.openLink(requireActivity(), "https://discord.gg/MqNT9j46Tg")
        }
        binding.footerWebsiteButton.setOnClickListener {
            ZHTools.openLink(requireActivity(), "https://cs-launcher.netlify.app/")
        }
    }

    // ═══════════════════════════════════════════════════
    //  TEAM CARD BUILDER
    // ═══════════════════════════════════════════════════

    private fun populateTeam() {
        // Founder - single prominent card centered
        addFounderCard()

        // Developers - 2-column grid
        addDeveloperGrid()

        // Staff - balanced list
        addStaffList()
    }

    private fun addFounderCard() {
        val container = binding.founderCardsContainer
        val card = createMemberCard(founder, isFounder = true)
        container.addView(card)
    }

    private fun addDeveloperGrid() {
        val container = binding.devCardsContainer
        var currentRow: LinearLayout? = null

        developers.forEachIndexed { index, member ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    container.addView(this)
                }
            }

            val card = createMemberCard(member, isFounder = false)
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            val marginEnd = if (index % 2 == 0) 4 else 0
            val marginStart = if (index % 2 != 0) 4 else 0
            params.setMargins(marginStart, 0, marginEnd, 8)
            card.layoutParams = params
            currentRow?.addView(card)
        }
    }

    private fun addStaffList() {
        val container = binding.staffCardsContainer
        staff.forEach { member ->
            val card = createMemberCard(member, isFounder = false)
            container.addView(card)
        }
    }

    private fun createMemberCard(member: Member, isFounder: Boolean): View {
        val card = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_about_member, binding.founderCardsContainer, false)

        val avatar = card.findViewById<ImageView>(R.id.member_avatar)
        if (member.iconTintGold) {
            avatar.setImageResource(member.iconRes)
            // Keep gold crown as-is (no tint override)
        } else {
            avatar.setImageResource(member.iconRes)
            avatar.setColorFilter(accentColorInt)
        }

        card.findViewById<TextView>(R.id.member_name).text = member.name
        card.findViewById<TextView>(R.id.member_role).text = member.role

        // Apply accent-colored border to the card
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f * resources.displayMetrics.density
            setColor(Color.parseColor("#0F0F0F"))
            setStroke((1 * resources.displayMetrics.density).toInt(), accentColorInt)
        }
        card.background = bg

        if (isFounder) {
            val params = card.layoutParams as? ViewGroup.MarginLayoutParams
            params?.setMargins(0, 0, 0, 8)
            card.layoutParams = params
        }

        return card
    }
    // ═══════════════════════════════════════════════════
    //  CASCADING ENTRANCE ANIMATION
    // ═══════════════════════════════════════════════════

    private fun playCascadeAnimation() {
        // 1. Header (title image + logo) - no delay
        animateSectionIn(binding.headerSection, 0)

        // 2. Credits - 150ms after header
        animateSectionIn(binding.creditsSection, SECTION_DELAY)

        // 3. Founder card - 150ms after credits
        animateSectionIn(binding.teamFounderSection, SECTION_DELAY * 2)

        // 4. Developers grid - 150ms after founder
        animateSectionIn(binding.teamDevSection, SECTION_DELAY * 3)

        // 5. Staff - 150ms after devs
        animateSectionIn(binding.teamStaffSection, SECTION_DELAY * 4)

        // 6. Footer - 150ms after staff
        animateSectionIn(binding.footerSection, SECTION_DELAY * 5)

        // Start continuous breathing animations after cascade settles
        binding.headerSection.postDelayed({
            startTitleBreathing()
            startLogoShimmer()
        }, SECTION_DELAY * 6 + 200)
    }

    private fun animateSectionIn(view: View, delay: Long) {
        view.postDelayed({
            view.alpha = 0f
            view.translationY = 50f

            val translateY = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

            AnimatorSet().apply {
                playTogether(translateY, alpha)
                duration = 500
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
            animatedViews.add(view)
        }, delay)
    }

    // ═══════════════════════════════════════════════════
    //  TITLE IMAGE BREATHING ANIMATION
    // ═══════════════════════════════════════════════════

    private fun startTitleBreathing() {
        val floatUp = ObjectAnimator.ofFloat(binding.aboutTitleImage, "translationY", -5f, 5f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val alpha = ObjectAnimator.ofFloat(binding.aboutTitleImage, "alpha", 0.85f, 1f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(floatUp, alpha)
            start()
        }
    }

    // ═══════════════════════════════════════════════════
    //  LOGO SHIMMER ANIMATION (kept from original)
    // ═══════════════════════════════════════════════════

    private fun startLogoShimmer() {
        val floatUp = ObjectAnimator.ofFloat(binding.aboutLogo, "translationY", -6f, 6f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val rotate = ObjectAnimator.ofFloat(binding.aboutLogo, "rotation", -2f, 2f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val scaleX = ObjectAnimator.ofFloat(binding.aboutLogo, "scaleX", 0.95f, 1.05f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val scaleY = ObjectAnimator.ofFloat(binding.aboutLogo, "scaleY", 0.95f, 1.05f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }

        AnimatorSet().apply {
            playTogether(floatUp, rotate, scaleX, scaleY)
            start()
        }
    }

    // ═══════════════════════════════════════════════════
    //  SCROLL-BASED CARD REVEAL
    // ═══════════════════════════════════════════════════

    private fun setupScrollReveal() {
        binding.aboutScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, _, _, _ ->
                val scrollY = v.scrollY
                val screenHeight = v.height
                revealCardsOnScroll(binding.devCardsContainer, scrollY, screenHeight)
                revealCardsOnScroll(binding.founderCardsContainer, scrollY, screenHeight)
                revealCardsOnScroll(binding.staffCardsContainer, scrollY, screenHeight)
            }
        )
    }

    private fun revealCardsOnScroll(container: ViewGroup, scrollY: Int, screenHeight: Int) {
        for (i in 0 until container.childCount) {
            val card = container.getChildAt(i)
            if (animatedViews.contains(card)) continue

            val location = IntArray(2)
            card.getLocationOnScreen(location)
            val cardTop = location[1]

            if (cardTop < scrollY + screenHeight - (50 * resources.displayMetrics.density).toInt()) {
                animatedViews.add(card)
                card.alpha = 0f
                card.translationY = 30f

                card.postDelayed({
                    ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
                        duration = 300
                        start()
                    }
                    ObjectAnimator.ofFloat(card, "translationY", 30f, 0f).apply {
                        duration = 350
                        interpolator = OvershootInterpolator(1.0f)
                        start()
                    }
                }, i * 120L)
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  FRAGMENT ANIMATIONS
    // ═══════════════════════════════════════════════════

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutScroll, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutScroll, Animations.FadeOutUp))
    }
}
