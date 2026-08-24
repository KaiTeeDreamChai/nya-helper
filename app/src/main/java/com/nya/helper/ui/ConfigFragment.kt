package com.nya.helper.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.nya.helper.databinding.FragmentConfigBinding
import com.nya.helper.engine.ConfigManager
import com.nya.helper.engine.RuleEngine
import com.nya.helper.model.NyaConfig

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private var currentConfig = NyaConfig()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadConfig()
        setupListeners()
        updateLivePreview()
    }

    override fun onResume() {
        super.onResume()
        loadConfig()
        updateLivePreview()
    }

    private fun loadConfig() {
        currentConfig = ConfigManager.getConfig(requireContext())

        binding.switchSentenceNya.isChecked = currentConfig.enableSentenceNya
        binding.switchReplaceI.isChecked = currentConfig.enableReplaceI
        binding.switchReplaceYou.isChecked = currentConfig.enableReplaceYou
        binding.switchKaomoji.isChecked = currentConfig.enableKaomoji
        binding.switchFumoKaomoji.isChecked = currentConfig.enableFumoKaomoji
        binding.switchMoodKaomoji.isChecked = currentConfig.enableMoodKaomoji

        binding.etCustomFumoKaomoji.setText(currentConfig.customFumoKaomojis)
        binding.etCustomKaomoji.setText(currentConfig.customKaomojis)
        binding.etCustomReplacements.setText(currentConfig.customReplacements)
    }

    private fun setupListeners() {
        val changeListener = {
            syncConfigFromUI()
            updateLivePreview()
        }

        binding.switchSentenceNya.setOnCheckedChangeListener { _, _ -> changeListener() }
        binding.switchReplaceI.setOnCheckedChangeListener { _, _ -> changeListener() }
        binding.switchReplaceYou.setOnCheckedChangeListener { _, _ -> changeListener() }
        binding.switchKaomoji.setOnCheckedChangeListener { _, _ -> changeListener() }
        binding.switchFumoKaomoji.setOnCheckedChangeListener { _, _ -> changeListener() }
        binding.switchMoodKaomoji.setOnCheckedChangeListener { _, _ -> changeListener() }

        binding.etCustomFumoKaomoji.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { changeListener() }
        })

        binding.etCustomKaomoji.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { changeListener() }
        })

        binding.etCustomReplacements.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { changeListener() }
        })

        binding.etTestInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateLivePreview() }
        })

        binding.btnSaveConfig.setOnClickListener {
            syncConfigFromUI()
            ConfigManager.saveConfig(requireContext(), currentConfig)
            Snackbar.make(binding.root, "🐾 配置已保存并即刻生效！", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun syncConfigFromUI() {
        val baseConfig = ConfigManager.getConfig(requireContext())
        currentConfig.triggerMode = baseConfig.triggerMode
        currentConfig.enableSentenceNya = binding.switchSentenceNya.isChecked
        currentConfig.enableReplaceI = binding.switchReplaceI.isChecked
        currentConfig.enableReplaceYou = binding.switchReplaceYou.isChecked
        currentConfig.enableKaomoji = binding.switchKaomoji.isChecked
        currentConfig.enableFumoKaomoji = binding.switchFumoKaomoji.isChecked
        currentConfig.enableMoodKaomoji = binding.switchMoodKaomoji.isChecked
        currentConfig.customFumoKaomojis = binding.etCustomFumoKaomoji.text?.toString() ?: ""
        currentConfig.customKaomojis = binding.etCustomKaomoji.text?.toString() ?: ""
        currentConfig.customReplacements = binding.etCustomReplacements.text?.toString() ?: ""
    }

    private fun updateLivePreview() {
        syncConfigFromUI()
        val input = binding.etTestInput.text?.toString() ?: ""
        val output = if (input.isNotBlank()) {
            RuleEngine.transform(input, currentConfig)
        } else {
            "等待输入测试文字..."
        }
        binding.tvTestOutput.text = output
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
