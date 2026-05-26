package com.craftstudio.launcher.ui.fragment.about

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.craftstudio.launcher.InfoCenter
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentAboutInfoPageBinding
import com.craftstudio.launcher.ui.subassembly.about.AboutItemBean
import com.craftstudio.launcher.ui.subassembly.about.AboutItemBean.AboutItemButtonBean
import com.craftstudio.launcher.ui.subassembly.about.AboutRecyclerAdapter
import com.craftstudio.launcher.utils.ZHTools

class AboutInfoPageFragment() : Fragment(R.layout.fragment_about_info_page) {
    private lateinit var binding: FragmentAboutInfoPageBinding
    private val mAboutData: MutableList<AboutItemBean> = ArrayList()
    private var parentPager2: ViewPager2? = null

    constructor(parentPager: ViewPager2): this() {
        this.parentPager2 = parentPager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutInfoPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadAboutData(requireContext().resources)

        val context = requireActivity()

        binding.apply {
            dec1.text = InfoCenter.replaceName(context, R.string.about_dec1)
            dec2.text = InfoCenter.replaceName(context, R.string.about_dec2)
            dec3.text = InfoCenter.replaceName(context, R.string.about_dec3)

            githubButton.setOnClickListener {
                ZHTools.openLink(requireActivity(), "https://cs.launcher.netlify.app/")
            }
            licenseButton.setOnClickListener {
                ZHTools.openLink(requireActivity(), "https://www.gnu.org/licenses/gpl-3.0.html")
            }

            qqGroupButton.visibility = View.GONE

            discordButton.setOnClickListener {
                ZHTools.openLink(requireActivity(), "https://discord.gg/C98VH7DWU")
            }

            val aboutAdapter = AboutRecyclerAdapter(this@AboutInfoPageFragment.mAboutData)
            aboutRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = aboutAdapter
            }

            sponsor.setOnClickListener {
                parentPager2?.currentItem = 1
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadAboutData(resources: Resources) {
        mAboutData.clear()

        // ── Craft Studio Team ──
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_about, requireContext().theme),
                "NOD DANGER",
                "Owner — Craft Studio Launcher",
                AboutItemButtonBean(
                    requireActivity(),
                    "Website",
                    "https://cs.launcher.netlify.app/"
                )
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_about, requireContext().theme),
                "Rohit",
                "Developer — Craft Studio Launcher",
                AboutItemButtonBean(
                    requireActivity(),
                    "Discord",
                    "https://discord.gg/C98VH7DWU"
                )
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_about, requireContext().theme),
                "miner.adi",
                "Developer — Craft Studio Launcher",
                AboutItemButtonBean(
                    requireActivity(),
                    "Discord",
                    "https://discord.gg/C98VH7DWU"
                )
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_about, requireContext().theme),
                "ONICHAA",
                "Developer — Craft Studio Launcher",
                AboutItemButtonBean(
                    requireActivity(),
                    "Discord",
                    "https://discord.gg/C98VH7DWU"
                )
            )
        )

        // ── Original Credits (GPL v3 Required) ──
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_pojav_full, requireContext().theme),
                "PojavLauncherTeam",
                getString(R.string.about_PojavLauncher_desc),
                AboutItemButtonBean(
                    requireActivity(),
                    "Github",
                    "https://github.com/PojavLauncherTeam/PojavLauncher"
                )
            )
        )
    }
}
