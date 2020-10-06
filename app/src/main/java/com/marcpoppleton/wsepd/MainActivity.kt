/**
WS EPD Driver
Copyright (C) 2020 Marc Poppleton
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.marcpoppleton.wsepd

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.marcpoppleton.wsepdkdriver.Epd7in5v2
import com.marcpoppleton.wsepdkdriver.Orientation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.io.IOException


const val TAG = "WSEPD"

class MainActivity : Activity() {

    private lateinit var display: Epd7in5v2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        display = Epd7in5v2(layoutInflater, R.layout.activity_dashboard,Orientation.PORTRAIT_RIGHT)
    }

    override fun onResume() {
        super.onResume()

        val title = display.findViewById<TextView>(R.id.title) as TextView
        val orientations = Orientation.values()
        GlobalScope.launch {
            display.clear()
            for(i in 0..10){
                title.text = getString(R.string.refresh_title,i)
                display.orientation = orientations[i%4]
                Log.d(TAG,"orientation is ${orientations[i%4]}")
                display.refresh()
                delay(10000)
            }
        }
    }

    override fun onDestroy() {
        GlobalScope.launch {
            display.clear()
            display.close()
            super.onDestroy()
        }
    }

}
