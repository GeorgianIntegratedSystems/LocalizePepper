# Add the library as a dependency
You can use Jitpack (https://jitpack.io/) to add the library as a gradle dependency.

Step 1) add JitPack repository to your build file:

Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  ```
Step 2) Add the dependency to your app build.gradle in the dependencies section:

```gradle
dependencies {
...
    implementation'com.github.GeorgianIntegratedSystems:LocalizePepper:v1.0.0'
}
  ```
# Usage
 You can look at the sample [activity](https://github.com/GeorgianIntegratedSystems/LocalizePepper/blob/main/app/src/main/java/ge/android/gis/localizepepper/MainActivity.kt) for an example of how to use the library.
  ### Initialize helper classes
  ``` kotlin 
class MainActivity : RobotActivity(), RobotLifecycleCallbacks {

    private lateinit var binding: ActivityMainBinding

    var robotHelper: RobotHelper = RobotHelper()
    private var localizeHelper: LocalizeHelper = LocalizeHelper()
    var saveInStorage: SaveFileClass = SaveFileClass()
    
    // These are for just to demonstrate how this app works

    var progressBarForMapDialog: Dialog? = null
    lateinit var spinnerAdapter: ArrayAdapter<String>
    private var selectedLocation: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        QiSDK.register(this, this)

}

   ```
### **Add this in your Manifest file**




 ``` kotlin

<uses-permission android:name="android.permission.INTERNET" />

<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    }

 ```
 
 
### Request Permissions

 ``` kotlin

        if (!hasPermissions(this, *HelperVariables.PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, HelperVariables.PERMISSIONS, HelperVariables.PERMISSION_ALL);
        }
    }

 ```

  ### Then initialize Robot variables in onRobotFocusGained method
 ``` kotlin
    override fun onRobotFocusGained(qiContext: QiContext?) {
        Log.i(HelperVariables.TAG, "onRobotFocusGained: ")

        HelperVariables.qiContext = qiContext
        HelperVariables.actuation = qiContext!!.actuation
        HelperVariables.mapping = qiContext.mapping

        runOnUiThread {
            binding.localizationView.startMappingButton.isEnabled = true
        }

    }

    override fun onRobotFocusLost() {
        Log.i(HelperVariables.TAG, "onRobotFocusLost: ")
        HelperVariables.qiContext = null
        QiSDK.unregister(this)
    }

    override fun onRobotFocusRefused(reason: String?) {
        Log.i(HelperVariables.TAG, "onRobotFocusRefused")
    }
   }
 ```

 ``` kotlin

    override fun onRobotFocusLost() {
        Log.i(HelperVariables.TAG, "onRobotFocusLost: ")
        HelperVariables.qiContext = null
        QiSDK.unregister(this)
    }

 ```

 ``` kotlin

    override fun onRobotFocusRefused(reason: String?) {
        Log.i(HelperVariables.TAG, "onRobotFocusRefused")
    }

 ```
