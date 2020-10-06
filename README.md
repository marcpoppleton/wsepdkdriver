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
implementation 'com.marcpoppleton:wsepdkdriver:1.2.0'
```

In your application's code you can use a e-Paper device like a display.
In the following example you can see how a 7,5" Waveshare device is used to display a layout in which we update the content of a TextView.

```kotlin
class MainActivity : Activity() {

    private lateinit var display: Epd7in5v2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        display = Epd7in5v2(layoutInflater, R.layout.epd_hello)
    }

    override fun onResume() {
        super.onResume()

        val title = display.findViewById<TextView>(R.id.title) as TextView
        GlobalScope.launch {
                title.text = getString(R.string.hello_world)
                display.refresh()
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

Orientation
--------

By default the display renders the layout in landscape mode, the serial port on the bottom. Orientation can either be set when creating a Epd device or at any moment in your code.

In the following example the orientation is set to ```PORTRAIT_RIGHT``` when the Epd device is created:
```kotlin
display = Epd7in5v2(layoutInflater, R.layout.activity_dashboard,Orientation.PORTRAIT_RIGHT)
```

On the other hand, in the next example the orientation is set at some moment of the code:
```kotlin
display.orientation = Orientation.LANDSCAPE_TOP
display.refresh()
```

The possible values are:

* ```Orientation.LANDSCAPE_BOTTOM```(the default orientation, with the serial port on the bottom)
* ```Orientation.PORTRAIT_RIGHT```(portrait mode, with the serial port on the right)
* ```Orientation.PORTRAIT_LEFT```(portrait mode, with the serial port on the left)
* ```Orientation.PORTRAIT_TOP```(landscape mode, with the serial port on the top)


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
