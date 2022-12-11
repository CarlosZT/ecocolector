package com.example.ecocollector.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.ecocollector.R
import androidx.viewpager.widget.PagerAdapter
import com.example.ecocollector.Entities.Item

class ViewPagerAdapter(var model:ArrayList<Item>,
                       var context: Context ):PagerAdapter() {

    lateinit var layoutInflater:LayoutInflater

    override fun getCount(): Int {
        return model.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view.equals(`object`)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        layoutInflater = LayoutInflater.from(context)
        var view = layoutInflater.inflate(R.layout.item1, container, false)
        var img: ImageView
        var names:TextView
        var description:TextView
        img = view.findViewById(R.id.img1)
        names = view.findViewById(R.id.text1)
        description = view.findViewById(R.id.description)

        img.setImageResource(model.get(position).imageid)
        names.text = model.get(position).names
        description.text = model.get(position).description


        container.addView(view, 0)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View?)
    }


}