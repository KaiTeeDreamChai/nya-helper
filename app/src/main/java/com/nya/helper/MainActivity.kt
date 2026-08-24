package com.nya.helper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.nya.helper.databinding.ActivityMainBinding
import com.nya.helper.ui.AboutFragment
import com.nya.helper.ui.ConfigFragment
import com.nya.helper.ui.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment by lazy { HomeFragment() }
    private val configFragment by lazy { ConfigFragment() }
    private val aboutFragment by lazy { AboutFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, insets.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, insets.bottom)
            windowInsets
        }

        if (savedInstanceState == null) {
            switchFragment(homeFragment)
        }

        setupBottomNavigation()
    }

    /**
     * 该方法由 LSPosed Hook 覆写为返回 true（纯内存级 Hook 检测）
     */
    fun isLsposedActiveDirect(): Boolean {
        return false
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.toolbar.title = "🐾 QQ 喵喵助手"
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_config -> {
                    binding.toolbar.title = "⚙️ 规则配置"
                    switchFragment(configFragment)
                    true
                }
                R.id.nav_about -> {
                    binding.toolbar.title = "ℹ️ 关于与帮助"
                    switchFragment(aboutFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
