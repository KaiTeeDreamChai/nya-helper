package com.nya.helper.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.nya.helper.databinding.FragmentAboutBinding
import com.nya.helper.util.DebugLogger

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDebugLogs()
        setupGitHubLink()
    }

    override fun onResume() {
        super.onResume()
        updateLogsDisplay()
    }

    private fun setupGitHubLink() {
        binding.btnOpenGitHub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/KaiTeeDreamChai/nya-helper"))
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "无法打开浏览器: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDebugLogs() {
        binding.tvDebugLogs.movementMethod = ScrollingMovementMethod()

        binding.btnClearLogs.setOnClickListener {
            DebugLogger.clear()
            updateLogsDisplay()
            Snackbar.make(binding.root, "日志已清空", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnCopyLogs.setOnClickListener {
            val logs = DebugLogger.getAllLogsText()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("nya_debug_logs", logs)
            clipboard?.setPrimaryClip(clip)
            Snackbar.make(binding.root, "日志已复制到剪贴板 📋", Snackbar.LENGTH_SHORT).show()
        }

        DebugLogger.setOnLogUpdatedListener {
            activity?.runOnUiThread {
                updateLogsDisplay()
            }
        }
    }

    private fun updateLogsDisplay() {
        if (_binding != null) {
            binding.tvDebugLogs.text = DebugLogger.getAllLogsText()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        DebugLogger.setOnLogUpdatedListener(null)
        _binding = null
    }
}
