package com.jehutyno.yomikata.screens.quizzes

import android.R.id.home
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.flaviofaria.kenburnsview.KenBurnsView
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.YomikataZKApplication
import com.jehutyno.yomikata.databinding.ActivityQuizzesBinding
import com.jehutyno.yomikata.databinding.MenuSwitchBinding
import com.jehutyno.yomikata.databinding.NavHeaderBinding
import com.jehutyno.yomikata.screens.PrefsActivity
import com.jehutyno.yomikata.screens.search.SearchResultActivity
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.view.AppBarStateChangeListener
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.di
import splitties.alertdialog.appcompat.*
import java.util.*


class QuizzesActivity : AppCompatActivity(), DIAware {

    override val di: DI by di()

    private var selectedCategory: Category = Category.HOME
    private lateinit var toolbar: Toolbar
    lateinit var fabMenu: FloatingActionsMenu
    private var recreate = false
    lateinit var quizzesAdapter: QuizzesPagerAdapter
    private lateinit var kenburns: KenBurnsView
    private var menu: Menu? = null

    val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            setImageRandom()
            handler.postDelayed(this, 7000)
        }
    }

    private val homeImages = intArrayOf(R.drawable.pic_04, R.drawable.pic_05, R.drawable.pic_06,
        R.drawable.pic_07, R.drawable.pic_08, R.drawable.pic_21, R.drawable.pic_22,
        R.drawable.pic_23, R.drawable.pic_24, R.drawable.pic_25)

    // View Binding
    private lateinit var binding: ActivityQuizzesBinding


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase, YomikataZKApplication.viewPump))
    }

    fun voicesDownload(level: Int, onSuccess: () -> Unit) {
        launchVoicesDownload(this, level) {
            quizzesAdapter.notifyDataSetChanged()
            onSuccess()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        // super.onSaveInstanceState(outState);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))

        binding = ActivityQuizzesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        kenburns = binding.imageSectionIcon

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_menu)
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        binding.appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                when (state) {
                    State.COLLAPSED -> {
                        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@QuizzesActivity, R.color.toolbarColor)))
                    }
                    else -> {
                        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@QuizzesActivity, R.color.transparent)))
                    }
                }
            }
        })

        selectedCategory = Category.HOME
        displayCategoryTitle(Category.HOME)
        binding.multipleActions.visibility = GONE

        // Set up the navigation drawer.
        binding.drawerLayout.setStatusBarBackground(R.color.colorPrimaryDark)
        setupDrawerContent(binding.navView)

        // keep all fragments loaded for performance reasons
//        binding.pagerQuizzes.offscreenPageLimit = 10

        quizzesAdapter = QuizzesPagerAdapter(this, di)
        binding.pagerQuizzes.adapter = quizzesAdapter
        binding.pagerQuizzes.currentItem = QuizzesPagerAdapter.positionFromCategory(selectedCategory)
        binding.pagerQuizzes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectedCategory = quizzesAdapter.categories[position]
                if (selectedCategory == Category.HOME) {
                    binding.multipleActions.visibility = GONE
                } else {
                    binding.multipleActions.visibility = VISIBLE
                }
                pref.edit().putInt(Prefs.SELECTED_CATEGORY.pref, selectedCategory.index).apply()
                displayCategoryTitle(selectedCategory)
                binding.navView.setCheckedItem(quizzesAdapter.getMenuItemFromPosition(position))
            }

        })

        // set onClick for quiz strategies in floating action button
        fun FloatingActionButton.setLaunchQuizOnClickListener(quizStrategy: QuizStrategy) {
            this.setOnClickListener {
                // Hacky way of finding selected fragment:
                // Viewpager2 uses the tag: "f" + position to store its fragments
                val fragment = supportFragmentManager.findFragmentByTag("f${binding.pagerQuizzes.currentItem}")
                if (fragment is QuizzesFragment) {
                    fragment.launchQuizClick(quizStrategy, null, binding.textTitle.text.toString())
                    binding.multipleActions.collapseImmediately()
                }
            }
        }
        binding.progressivePlay.setLaunchQuizOnClickListener(QuizStrategy.PROGRESSIVE)
        binding.normalPlay.setLaunchQuizOnClickListener(QuizStrategy.STRAIGHT)
        binding.shufflePlay.setLaunchQuizOnClickListener(QuizStrategy.SHUFFLE)

        fabMenu = binding.multipleActions

        binding.anchor.postDelayed({ tutos() }, 500)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerClosed(drawerView: View) {
                if (recreate) {
                    recreate()
                    recreate = false
                }
            }

            override fun onDrawerStateChanged(newState: Int) {

            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {

            }

        })

        fun collapseOrQuit() {
            if (binding.multipleActions.isExpanded)
                binding.multipleActions.collapse()
            else
                alertDialog {
                    titleResource = R.string.app_quit
                    okButton { finishAffinity() }
                    cancelButton()
                    setOnKeyListener { _, keyCode, _ ->
                        if (keyCode == KeyEvent.KEYCODE_BACK)
                            finishAffinity()
                        true
                    }
                }.show()
        }

        // set back button to close floating actions menu or show alertDialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                collapseOrQuit()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                collapseOrQuit()
            }
        }

    }

    private fun tutos() {
        spotlightWelcome(this, binding.anchor, getString(R.string.tuto_yomikataz), getString(R.string.tuto_welcome)
        ) {
            spotlightTuto(this, getNavButtonView(toolbar), getString(R.string.tuto_categories),
                getString(R.string.tuto_categories_message)
            ) {}
        }
    }

    private fun getNavButtonView(toolbar: Toolbar): View? {
        return (0 until toolbar.childCount)
            .firstOrNull { toolbar.getChildAt(it) is ImageButton }
            ?.let { toolbar.getChildAt(it) as ImageButton }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0)
        }

        val navHeaderBinding = NavHeaderBinding.bind(navigationView.getHeaderView(0))
        val navMenuDayNightSwitchBinding = MenuSwitchBinding.bind(
            navigationView.menu.findItem(R.id.day_night_item).actionView!!
        )

        navHeaderBinding.version.text = getString(R.string.yomiakataz_drawer, packageInfo.versionName)
        navHeaderBinding.facebook.setOnClickListener { contactFacebook(this) }
        navHeaderBinding.discord.setOnClickListener { contactDiscord(this) }
        navHeaderBinding.playStore.setOnClickListener { contactPlayStore(this) }
        navHeaderBinding.share.setOnClickListener { shareApp(this) }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.multipleActions.collapse()
            // set smoothScroll to false because scrolling through all of the pages can be bad for performance
            when (menuItem.itemId) {
                R.id.home -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.HOME), false)
                }
                R.id.your_selections_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.SELECTIONS), false)
                }
                R.id.hiragana_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.HIRAGANA), false)
                }
                R.id.katakana_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.KATAKANA), false)
                }
                R.id.kanji_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.KANJI), false)
                }
                R.id.counters_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.COUNTERS), false)
                }
                R.id.jlpt1_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.JLPT_1), false)
                }
                R.id.jlpt2_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.JLPT_2), false)
                }
                R.id.jlpt3_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.JLPT_3), false)
                }
                R.id.jlpt4_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.JLPT_4), false)
                }
                R.id.jlpt5_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.setCurrentItem(QuizzesPagerAdapter.positionFromCategory(Category.JLPT_5), false)
                }
                R.id.day_night_item -> {
                    menuItem.isChecked = !menuItem.isChecked
                    navMenuDayNightSwitchBinding.mySwitch.toggle()
                }
                R.id.settings -> {
                    menuItem.isChecked = false
                    val intent = Intent(this, PrefsActivity::class.java)
                    getResult.launch(intent)
                }
                else -> {
                }
            }
            binding.drawerLayout.closeDrawers()
            true
        }

        val isNightModeOn = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        navMenuDayNightSwitchBinding.mySwitch.isChecked = isNightModeOn
        navigationView.menu.findItem(R.id.day_night_item).isChecked = isNightModeOn
        navMenuDayNightSwitchBinding.mySwitch.setOnCheckedChangeListener {
            _, isChecked ->
            navigationView.menu.findItem(R.id.day_night_item).isChecked = isChecked
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                pref.edit().putInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                pref.edit().putInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_NO).apply()
            }
            binding.drawerLayout.closeDrawers()
            recreate = true
        }
    }

    private fun setImageRandom() {
        // change images randomly
        val ran = Random()
        val i = ran.nextInt(homeImages.size)
        binding.imageSectionIcon.setImageResource(homeImages[i])
    }

    fun displayCategoryTitle(category: Category) {
        when (category) {
            Category.HOME -> {
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.yomi_logo_home))
                binding.textTitle.text = getString(R.string.home_title)
                binding.navView.setCheckedItem(R.id.home)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_24)
                handler.postDelayed(runnable, 7000)
            }
            Category.HIRAGANA -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_hiragana_big))
                binding.textTitle.setText(R.string.drawer_hiragana)
                binding.navView.setCheckedItem(R.id.hiragana_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_miyajima)
            }
            Category.KATAKANA -> {
                handler.removeCallbacks(runnable)
                binding.textTitle.setText(R.string.drawer_katakana)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_katakana_big))
                binding.navView.setCheckedItem(R.id.katakana_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_le_charme)
            }
            Category.KANJI -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_kanji_big))
                binding.textTitle.setText(R.string.drawer_kanji_beginner)
                binding.navView.setCheckedItem(R.id.kanji_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_toit)
            }
            Category.COUNTERS -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_counters_big))
                binding.textTitle.setText(R.string.drawer_counters)
                binding.navView.setCheckedItem(R.id.counters_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_fujiyoshida)
            }
            Category.JLPT_1 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt1_big))
                binding.textTitle.setText(R.string.drawer_jlpt1)
                binding.navView.setCheckedItem(R.id.jlpt1_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_fujisan)
            }
            Category.JLPT_2 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt2_big))
                binding.textTitle.setText(R.string.drawer_jlpt2)
                binding.navView.setCheckedItem(R.id.jlpt2_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_hokusai)
            }
            Category.JLPT_3 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt3_big))
                binding.textTitle.setText(R.string.drawer_jlpt3)
                binding.navView.setCheckedItem(R.id.jlpt3_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_geisha)
            }
            Category.JLPT_4 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt4_big))
                binding.textTitle.setText(R.string.drawer_jlpt4)
                binding.navView.setCheckedItem(R.id.jlpt4_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_monk)
            }
            Category.JLPT_5 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt5_big))
                binding.textTitle.setText(R.string.drawer_jlpt5)
                binding.navView.setCheckedItem(R.id.jlpt5_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_dragon)
            }
            Category.SELECTIONS -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_selections_big))
                binding.textTitle.setText(R.string.drawer_your_selections)
                binding.navView.setCheckedItem(R.id.your_selections_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_hanami)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            home -> {
                // Open the navigation drawer when the home icon is selected from the toolbar.
                binding.drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
            R.id.search -> {
                val intent = Intent(this, SearchResultActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun gotoCategory(category: Category) {
        binding.pagerQuizzes.currentItem = QuizzesPagerAdapter.positionFromCategory(category)
    }

    override fun onResume() {
        super.onResume()
        displayCategoryTitle(selectedCategory)
        quizzesAdapter.notifyDataSetChanged()
    }

    private val getResult =
            registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.also {
                        selectedCategory = it.getSerializableExtraHelper("gotoCategory",
                                                 Category::class.java) ?: selectedCategory
                        displayCategoryTitle(selectedCategory)
                        gotoCategory(selectedCategory)
                    }
                    tutos()
                }
            }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }
}
