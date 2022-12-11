package com.example.ecocollector

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.ecocollector.Adapters.ViewPagerAdapter
import com.example.ecocollector.Entities.Item
import kotlinx.android.synthetic.main.activity_tutorial_pager.*

class TutorialPager : AppCompatActivity(), OnPageChangeListener {
    private lateinit var adapter: ViewPagerAdapter
    private lateinit var listItems:ArrayList<Item>
    private var page = 0
    private var max_page = 0

    companion object{
        var instances = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_pager)
        instances += 1
        if(instances>1)
            finish()
        supportActionBar?.hide()

        listItems = ArrayList<Item>()
        setItems()
        viewPager.addOnPageChangeListener(this)

        adapter = ViewPagerAdapter(listItems, this)
        viewPager.adapter = adapter
        viewPager.setPadding(50, 0, 50, 0)
        max_page = listItems.size - 1

        btnNext.setOnClickListener {
            if (page < max_page){
                page+=1
                viewPager.setCurrentItem(page, true)
            }else{
                finish()
            }
        }
        btnBack.isEnabled = false
        btnBack.setTextColor(Color.GRAY)
        btnBack.setStrokeColorResource(androidx.appcompat.R.color.material_grey_600)
        btnBack.setOnClickListener {
            if (page > 0){
                page-=1
                viewPager.setCurrentItem(page, true)
            }
        }

    }

    private fun setItems(){
        listItems.add(Item("", "Welcome to Eco-collector!", R.drawable.eco_cinves_2))

        listItems.add(Item("You can use this options to measure the environment quality:",
            "You only can choose one of them each time!",
            R.drawable.screen_tuto_1))

        listItems.add(Item("Press start to begin the measure. You need at least 30 seconds to save it.",
            "Also you can cancel the process pressing back button.",
            R.drawable.screen_tuto_2))

        listItems.add(Item("When you press stop automatically we'll upload the data to Ecodata server.",
            "Data will keep on your storage until the upload were done.",
            R.drawable.screen_tuto_3))

        listItems.add(Item("We need the following permissions to collect the data. Otherwise, the app won't works correctly:",
            "",
            R.drawable.screen_tuto_4))

        listItems.add(Item("Please, keep the screen on and don't use other apps while you are measuring!",
            "",
            R.drawable.screen_tuto_5))
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//        Log.d("COUT", "on page scrolled; position: ${position}, positionOffset: ${positionOffset}, positionOffsetPixels: ${positionOffsetPixels}")
    }

    override fun onPageSelected(position: Int) {
        page = position
        if (page > 0) {
            btnBack.isEnabled = true
            btnBack.setTextColor(Color.WHITE)
            btnBack.setStrokeColorResource(R.color.white)
            if (page == max_page) {
                btnNext.text = "OK, got it!"
            } else {
                btnNext.text = "Next"
            }
        }else{
            btnBack.isEnabled = false
            btnBack.setTextColor(Color.GRAY)
            btnBack.setStrokeColorResource(androidx.appcompat.R.color.material_grey_600)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}
}