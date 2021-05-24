package ge.android.gis.localizepepper

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.actuation.AttachedFrame
import com.aldebaran.qi.sdk.`object`.actuation.ExplorationMap
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.`object`.streamablebuffer.StreamableBuffer
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.util.FutureUtils
import ge.android.gis.localizepepper.databinding.ActivityMainBinding
import ge.android.gis.localizepepper.databinding.ProgressBarBinding
import ge.android.gis.pepperlocalizeandmove.utils.constants.HelperVariables
import ge.android.gis.pepperlocalizeandmove.utils.localization_helper.LocalizeHelper
import ge.android.gis.pepperlocalizeandmove.utils.robot_helper.RobotHelper
import ge.android.gis.pepperlocalizeandmove.utils.save_in_storage.SaveFileClass
import ge.android.gis.pepperlocalizeandmove.utils.save_in_storage_helper.Vector2theta
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {

    private lateinit var binding: ActivityMainBinding

    var progressBarForMapDialog: Dialog? = null

    var robotHelper: RobotHelper = RobotHelper()
    private var localizeHelper: LocalizeHelper = LocalizeHelper()
    var saveInStorage: SaveFileClass = SaveFileClass()

    lateinit var spinnerAdapter: ArrayAdapter<String>
    private var selectedLocation: String? = null
    var savedLocations: MutableMap<String, AttachedFrame> = mutableMapOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        QiSDK.register(this, this)

        if (!hasPermissions(this, *HelperVariables.PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, HelperVariables.PERMISSIONS, HelperVariables.PERMISSION_ALL);
        }

        binding.localizationView.startMappingButton.setOnClickListener {
            Log.i(HelperVariables.TAG, "startMappingStep clicked")

            val qiContext = HelperVariables.qiContext ?: return@setOnClickListener
            startMappingStep(qiContext)
        }

        binding.localizationView.extendMapButton.setOnClickListener {
            Log.i(HelperVariables.TAG, "extendMapButton clicked")
            // Check that an initial map is available.
            val initialExplorationMap = HelperVariables.initialExplorationMap
            if (initialExplorationMap != null) {

                // Check that the Activity owns the focus.
                val qiContext = HelperVariables.qiContext ?: return@setOnClickListener
                // Start the map extension step.
                startMapExtensionStep(initialExplorationMap, qiContext)
            } else {
                Toast.makeText(this, "Please First Make a Map", Toast.LENGTH_LONG).show()
            }

        }


        binding.localizationView.stopLocalization.setOnClickListener {

            robotHelper.releaseAbilities()

            showProgress(this, "Saving Map ...")

            HelperVariables.publishExplorationMapFuture!!.cancel(true)

            Thread {

                localizeHelper.setStreamableMap(HelperVariables.toSaveUpdatedExplorationMap!!.serializeAsStreamableBuffer())

                val mapData: StreamableBuffer? = localizeHelper.getStreamableMap()
                saveInStorage.writeStreamableBufferToFile(
                        HelperVariables.FILE_DIRECTORY_PATH,
                        HelperVariables.MAP_FILE_NAME,
                        mapData!!
                )

                runOnUiThread {
                    hideProgress()
                }

            }.start()

        }


        binding.localizationView.saveButton.setOnClickListener {
            val location: String = binding.localizationView.addItemEdit.text.toString()
            // Save location only if new.
            if (location.isNotEmpty() && !savedLocations.containsKey(location)) {
                spinnerAdapter.add(location)
                saveInStorage.saveLocation(location, savedLocations)
                binding.localizationView.addItemEdit.text.clear()
            } else {
                Toast.makeText(this, "Enter the location name", Toast.LENGTH_LONG).show()
            }
        }

        binding.localizationView.loadMap.setOnClickListener {

            showProgress(HelperVariables.qiContext, "Loading map ...")
            localizeHelper.buildStreamableExplorationMapAndLocalizeRobot(binding.localizationView.explorationMapView, MainActivity(), this)!!.andThenConsume {
                hideProgress()
            }

        }

        binding.localizationView.spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    selectedLocation = parent.getItemAtPosition(position) as String
                    Log.i(HelperVariables.TAG, "onItemSelected: $selectedLocation")
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedLocation = null
                    Log.i(HelperVariables.TAG, "onNothingSelected")
                }
            }

        binding.localizationView.get.setOnClickListener {

            loadLocations()

        }


        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.localizationView.spinner.adapter = spinnerAdapter



    }





    private fun loadLocations(): Future<Boolean>? {

        return FutureUtils.futureOf<Boolean> {
            val file =
                File(HelperVariables.FILE_DIRECTORY_PATH, HelperVariables.LOCATION_FILE_NAME)
            if (file.exists()) {
                val vectors: MutableMap<String?, Vector2theta?>? =
                    saveInStorage.getLocationsFromFile(
                        HelperVariables.FILE_DIRECTORY_PATH,
                        HelperVariables.LOCATION_FILE_NAME
                    )

                // Clear current savedLocations.
                savedLocations = TreeMap()
                val mapFrame: Frame? = localizeHelper.getMapFrame()


                Log.i("yvelaa", vectors.toString())

                vectors!!.forEach { (key1, value1) ->

                    val t = value1!!.createTransform()
                    Log.d("TAG", "loadLocations: $key1")

                    runOnUiThread {
                        spinnerAdapter.add(key1)
                    }


                    val attachedFrame =
                        mapFrame!!.async().makeAttachedFrame(t).value

                    // Store the FreeFrame.
                    savedLocations[key1!!] = attachedFrame

                }

                HelperVariables.loadLocationSuccess.set(true)


                Log.d("TAG", "loadLocations: Done")
                Log.d("TAG", savedLocations.toString())
                if (HelperVariables.loadLocationSuccess.get()) return@futureOf Future.of(
                    true
                ) else throw Exception("Empty file")
            } else {
                throw Exception("No file")
            }
        }
    }


    private fun startMapExtensionStep(initialExplorationMap: ExplorationMap, qiContext: QiContext) {
        Log.i(HelperVariables.TAG, "StartMapEXTENSION Class")
        binding.localizationView.extendMapButton.isEnabled = false
        robotHelper.holdAbilities(qiContext, true)!!.andThenConsume {
            robotHelper.animationToLookInFront(qiContext)!!.andThenConsume {
                localizeHelper.extendMap(initialExplorationMap, qiContext) { updatedMap ->
                    binding.localizationView.explorationMapView.setExplorationMap(
                            initialExplorationMap.topGraphicalRepresentation
                    )
                    HelperVariables.toSaveUpdatedExplorationMap = updatedMap
                    localizeHelper.mapToBitmap(
                            binding.localizationView.explorationMapView,
                            updatedMap
                    )

                }.thenConsume { future ->
                    // If the operation is not a success, re-enable "extend map" button.
                    if (!future.isSuccess) {
                        runOnUiThread {
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                binding.localizationView.extendMapButton.isEnabled = true
                            }
                        }
                    }
                }
            }
        }

    }

    private fun startMappingStep(qiContext: QiContext) {

        binding.localizationView.startMappingButton.isEnabled = false

        Log.i(HelperVariables.TAG, "startMappingStep Class")

        // show progress dialog
        showProgress(HelperVariables.qiContext, "Creating Map ...")

        // Map the surroundings and get the map.
        localizeHelper.mapSurroundings(qiContext).thenConsume { future ->
            if (future.isSuccess) {
                Log.i(HelperVariables.TAG, "FUTURE Success")

                val explorationMap = future.get()
                // Store the initial map.
                HelperVariables.initialExplorationMap = explorationMap
                // Convert the map to a bitmap.
                localizeHelper.mapToBitmap(
                        binding.localizationView.explorationMapView,
                        explorationMap
                )
                // Display the bitmap and enable "extend map" button.

                // hide progress dialog
                hideProgress()

                runOnUiThread {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        Log.i(HelperVariables.TAG, "awdawdawdawdawds")
                        binding.localizationView.extendMapButton.isEnabled = true
                    }
                }
            } else {
                // hide progress dialog
                hideProgress()
                // If the operation is not a success, re-enable "start mapping" button.
                runOnUiThread {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        Log.i(HelperVariables.TAG, "awdawaawdwdawdawdawds")
                        binding.localizationView.startMappingButton.isEnabled = true
                    }
                }
            }
        }
    }


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

    private fun showProgress(mContext: Context?, progresBarText: String) {

        val dialogBinding: ProgressBarBinding = ProgressBarBinding.inflate(LayoutInflater.from(this))
        dialogBinding.progressText.text = progresBarText
        progressBarForMapDialog = Dialog(mContext!!)
        progressBarForMapDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressBarForMapDialog!!.setContentView(dialogBinding.root)
        progressBarForMapDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressBarForMapDialog!!.setCancelable(false)
        progressBarForMapDialog!!.setCanceledOnTouchOutside(false)
        progressBarForMapDialog!!.show()
    }

    private fun hideProgress() {


        if (progressBarForMapDialog != null) {
            progressBarForMapDialog!!.dismiss()
            progressBarForMapDialog = null;
        }
    }


    fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}