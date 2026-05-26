package com.craftstudio.launcher.ui.fragment.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.SettingsFragmentControlBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.fragment.CustomMouseFragment
import com.craftstudio.launcher.ui.fragment.FragmentWithAnim
import com.craftstudio.launcher.ui.fragment.settings.wrapper.BaseSettingsWrapper
import com.craftstudio.launcher.ui.fragment.settings.wrapper.SeekBarSettingsWrapper
import com.craftstudio.launcher.ui.fragment.settings.wrapper.SwitchSettingsWrapper
import com.craftstudio.launcher.utils.ZHTools
import fr.spse.gamepad_remapper.Remapper
import com.craftstudio.launcher.fragments.GamepadMapperFragment

class ControlSettingsFragment() : AbstractSettingsFragment(R.layout.settings_fragment_control, SettingCategory.CONTROL) {
    private lateinit var binding: SettingsFragmentControlBinding
    private var parentFragment: FragmentWithAnim? = null

    constructor(parentFragment: FragmentWithAnim?) : this() {
        this.parentFragment = parentFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentControlBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        SwitchSettingsWrapper(
            context,
            AllSettings.disableGestures,
            binding.disableGesturesLayout,
            binding.disableGestures
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.disableDoubleTap,
            binding.disableDoubleTapLayout,
            binding.disableDoubleTap
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.timeLongPressTrigger,
            binding.timeLongPressTriggerLayout,
            binding.timeLongPressTriggerTitle,
            binding.timeLongPressTriggerSummary,
            binding.timeLongPressTriggerValue,
            binding.timeLongPressTrigger,
            "ms"
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.buttonScale,
            binding.buttonscaleLayout,
            binding.buttonscaleTitle,
            binding.buttonscaleSummary,
            binding.buttonscaleValue,
            binding.buttonscale,
            "%"
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.buttonAllCaps,
            binding.buttonAllCapsLayout,
            binding.buttonAllCaps
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.mouseScale,
            binding.mousescaleLayout,
            binding.mousescaleTitle,
            binding.mousescaleSummary,
            binding.mousescaleValue,
            binding.mousescale,
            "%"
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.mouseSpeed,
            binding.mousespeedLayout,
            binding.mousespeedTitle,
            binding.mousespeedSummary,
            binding.mousespeedValue,
            binding.mousespeed,
            "%"
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.virtualMouseStart,
            binding.mouseStartLayout,
            binding.mouseStart
        )

        BaseSettingsWrapper(
            context,
            binding.customMouseLayout
        ) {
            ZHTools.swapFragmentWithAnim(
                this@ControlSettingsFragment,
                CustomMouseFragment::class.java,
                CustomMouseFragment.TAG,
                null
            )
        }

        SwitchSettingsWrapper(
            context,
            AllSettings.enableGyro,
            binding.enableGyroLayout,
            binding.enableGyro
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.gyroSensitivity,
            binding.gyroSensitivityLayout,
            binding.gyroSensitivityTitle,
            binding.gyroSensitivitySummary,
            binding.gyroSensitivityValue,
            binding.gyroSensitivity,
            "%"
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.gyroSampleRate,
            binding.gyroSampleRateLayout,
            binding.gyroSampleRateTitle,
            binding.gyroSampleRateSummary,
            binding.gyroSampleRateValue,
            binding.gyroSampleRate,
            "ms"
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.gyroSmoothing,
            binding.gyroSmoothingLayout,
            binding.gyroSmoothing
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.gyroInvertX,
            binding.gyroInvertXLayout,
            binding.gyroInvertX
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.gyroInvertY,
            binding.gyroInvertYLayout,
            binding.gyroInvertY
        )

        BaseSettingsWrapper(
            context,
            binding.changeControllerBindingsLayout
        ) {
            parentFragment?.apply {
                ZHTools.swapFragmentWithAnim(
                    this,
                    GamepadMapperFragment::class.java,
                    GamepadMapperFragment.TAG,
                    null
                )
            }
        }

        BaseSettingsWrapper(
            context,
            binding.resetControllerBindingsLayout
        ) {
            Remapper.wipePreferences(context)
            Toast.makeText(context, R.string.setting_controller_map_wiped, Toast.LENGTH_SHORT)
                .show()
        }

        SeekBarSettingsWrapper(
            context,
            AllSettings.deadZoneScale,
            binding.gamepadDeadzoneScaleLayout,
            binding.gamepadDeadzoneScaleTitle,
            binding.gamepadDeadzoneScaleSummary,
            binding.gamepadDeadzoneScaleValue,
            binding.gamepadDeadzoneScale,
            "%"
        )

        val mGyroAvailable =
            (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            ) != null
        binding.enableGyroCategory.visibility = if (mGyroAvailable) View.VISIBLE else View.GONE

        computeVisibility()
    }

    override fun onChange() {
        super.onChange()
        computeVisibility()
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }

    private fun computeVisibility() {
        binding.apply {
            setViewVisibility(
                timeLongPressTriggerLayout,
                !AllSettings.disableGestures.getValue()
            )
            setViewVisibility(gyroSensitivityLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroSampleRateLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroInvertXLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroInvertYLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroSmoothingLayout, AllSettings.enableGyro.getValue())
        }
    }

    private fun setViewVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}