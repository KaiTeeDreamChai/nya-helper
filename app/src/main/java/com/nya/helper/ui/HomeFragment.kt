package com.nya.helper.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.nya.helper.MainActivity
import com.nya.helper.databinding.FragmentHomeBinding
import com.nya.helper.engine.ConfigManager
import com.nya.helper.model.NyaConfig
import com.nya.helper.service.NyaAccessibilityService

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMasterSwitch()
        setupEngineStatus()
        setupTriggerMode()
    }

    override fun onResume() {
        super.onResume()
        updateMasterSwitchUI()
        updateEngineStatus()
    }

    private fun setupMasterSwitch() {
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            val currentConfig = ConfigManager.getConfig(requireContext())
            if (currentConfig.isMasterEnabled != isChecked) {
                currentConfig.isMasterEnabled = isChecked
                ConfigManager.saveConfig(requireContext(), currentConfig)

                val msg = if (isChecked) "🐾 助手已开启，萌化功能已恢复！" else "⏸️ 助手已一键暂停，所有萌化已停用"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                updateMasterSwitchUI()
            }
        }
    }

    private fun updateMasterSwitchUI() {
        val config = ConfigManager.getConfig(requireContext())
        binding.switchMaster.isChecked = config.isMasterEnabled
        if (config.isMasterEnabled) {
            binding.tvMasterSwitchHint.text = "萌化功能运行中（LSPosed 与无障碍均生效）"
        } else {
            binding.tvMasterSwitchHint.text = "已暂停 · 点击右侧开关一键重新启用"
        }
    }

    private fun setupEngineStatus() {
        binding.btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun updateEngineStatus() {
        val isLsposed = (activity as? MainActivity)?.isLsposedActiveDirect() ?: false
        if (isLsposed) {
            binding.tvLsposedStatus.text = "运行中 (已挂载并拦截)"
            binding.chipStatus.text = "已激活"
        } else {
            binding.tvLsposedStatus.text = "未激活 (请在 LSPosed 中勾选并重启 QQ)"
            binding.chipStatus.text = "未激活"
        }

        val isAccessRunning = NyaAccessibilityService.isServiceRunning
        if (isAccessRunning) {
            binding.tvAccessibilityStatus.text = "状态：运行中（无障碍服务已开启）"
            binding.btnOpenAccessibility.text = "已开启无障碍"
        } else {
            binding.tvAccessibilityStatus.text = "状态：已关闭（无需 Root 时的备用方案）"
            binding.btnOpenAccessibility.text = "前往开启无障碍"
        }
    }

    private fun setupTriggerMode() {
        val config = ConfigManager.getConfig(requireContext())
        when (config.triggerMode) {
            NyaConfig.MODE_SEND_HOOK -> binding.rbSendHook.isChecked = true
            NyaConfig.MODE_PUNCTUATION -> binding.rbPunctuation.isChecked = true
            NyaConfig.MODE_REALTIME -> binding.rbRealtime.isChecked = true
        }

        binding.rgTriggerMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.rbSendHook.id -> NyaConfig.MODE_SEND_HOOK
                binding.rbPunctuation.id -> NyaConfig.MODE_PUNCTUATION
                binding.rbRealtime.id -> NyaConfig.MODE_REALTIME
                else -> NyaConfig.MODE_SEND_HOOK
            }
            val currentConfig = ConfigManager.getConfig(requireContext())
            currentConfig.triggerMode = mode
            ConfigManager.saveConfig(requireContext(), currentConfig)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
