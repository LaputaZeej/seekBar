package com.laputa.seekbar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sb.onProgressSelectedListener = { _, progress, max ->
            tv_info.text = "${progress}ml/${max}  ml"
        }

        sb.onProgressChanged = { _, progress, max ->
            //tv_info.text = "$progress/$max"
        }

        tv_info.setOnClickListener {
            sb.changeSize(20)
        }

    }
}