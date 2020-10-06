e-Paper devices driver for Android Things [ ![Download](https://api.bintray.com/packages/marcpoppleton/Maven/com.marcpoppleton%3Awsepdkdriver/images/download.svg) ](https://bintray.com/marcpoppleton/Maven/com.marcpoppleton%3Awsepdkdriver/_latestVersion)
============================

This is the source code of the e-Paper devices driver library for Android Things.
This project is under Apache licence 2.0 and comes as is. It is NOT provided by Waveshare.

Introduction
-------------

Currently supported devices:

* Waveshare 7,5" monochrome e-Paper device

Currently available features:

* full display refresh
* clear to white
* clear to black

Usage
--------

Add the dependency to your application's build.gradle file:

```groovy
implementation 'com.marcpoppleton:wsepdkdriver:1.0.1'
```

In your application's code you can use a e-Paper device like a display.
In the following example you can see how a 7,5" Waveshare device is used to display a dashboard updated every 45 seconds.

```kotlin
class MainActivity : Activity() {

    private lateinit var display: Epd7in5v2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        display = Epd7in5v2(layoutInflater, R.layout.activity_dashboard)
    }

    override fun onResume() {
        super.onResume()

        val title = display.findViewById<TextView>(R.id.title) as TextView
        GlobalScope.launch {
            for(i in 0..10){
                title.text = getString(R.string.refresh_title,i)
                display.refresh()
                delay(45000)
            }
        }
    }

    override fun onDestroy() {
        display.close()
        super.onDestroy()
    }

}
```

Calls to the ```refresh``` function must be done from a coroutine since all functions, except ```close```, are suspendable functions.


License
--------

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
