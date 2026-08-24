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
        setupEngineStatus()
        setupMultiUserActivator()
        setupTriggerMode()
    }

    override fun onResume() {
        super.onResume()
        updateEngineStatus()
    }

    private fun setupEngineStatus() {
        binding.btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setupMultiUserActivator() {
        binding.btnActivateMultiUser.setOnClickListener {
            Thread {
                try {
                    val pkgName = requireContext().packageName
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm list users"))
                    val reader = process.inputStream.bufferedReader()
                    val output = reader.readText()
                    process.waitFor()

                    val regex = Regex("UserInfo\\{(\\d+):")
                    val userIds = regex.findAll(output).map { it.groupValues[1] }.filter { it != "0" }.toList()

                    if (userIds.isEmpty()) {
                        // 尝试直接安装至默认 999 分身空间
                        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm install-existing --user 999 $pkgName"))
                        p.waitFor()
                        requireActivity().runOnUiThread {
                            if (p.exitValue() == 0) {
                                Snackbar.make(binding.root, "🐾 已为用户空间 999 安装模块！请在 LSPosed 勾选并重启分身", Snackbar.LENGTH_LONG).show()
                            } else {
                                Snackbar.make(binding.root, "未检测到分身用户空间，请确保已开启应用双开", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                        return@Thread
                    }

                    var successCount = 0
                    for (uid in userIds) {
                        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm install-existing --user $uid $pkgName"))
                        p.waitFor()
                        if (p.exitValue() == 0) {
                            successCount++
                        }
                    }

                    requireActivity().runOnUiThread {
                        Snackbar.make(
                            binding.root,
                            "🐾 已成功为 $successCount 个分身空间激活模块！LSPosed 即可识别并勾选",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        Snackbar.make(binding.root, "激活失败，请在 KernelSU 中允许 Root 权限", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }.start()
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
