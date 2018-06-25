package com.ehanoc.menuwheel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ehanoc.menuwheel.view.WheelMenuLayout
import su.levenetc.android.badgeview.BadgeView

class MainActivity : AppCompatActivity(), WheelMenuLayout.WheelChangeListener {

    override fun onSelectionChange(selectedPosition: Int) {
        val badge: BadgeView = findViewById(R.id.lense_badgeview)
        badge.setValue(selectedPosition)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        var menu: WheelMenuLayout = findViewById(R.id.wheelmenu)
        menu.setWheelChangeListener(this)
    }
}
