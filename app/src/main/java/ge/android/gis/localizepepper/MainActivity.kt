package ge.android.gis.localizepepper

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.actuation.ExplorationMap
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import ge.android.gis.localizepepper.databinding.ActivityMainBinding
import ge.android.gis.localizepepper.databinding.ProgressBarBinding
import ge.android.gis.pepperlocalizeandmove.utils.localization_helper.LocalizeHelper
import ge.android.gis.pepperlocalizeandmove.utils.robot_helper.RobotHelper

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {

    private lateinit var binding: ActivityMainBinding

    var progressBarForMapDialog: Dialog? = null

    var robotHelper: RobotHelper = RobotHelper()
    var localizeHelper: LocalizeHelper = LocalizeHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        QiSDK.register(this, this)

        if (!Constants.hasPermissions(this, *Constants.PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_ALL);
        }

        binding.localizationView.startMappingButton.setOnClickListener {
            Log.i(Constants.TAG, "startMappingStep clicked")

            val qiContext = robotHelper.qiContext ?: return@setOnClickListener
            startMappingStep(qiContext)
        }

        binding.localizationView.extendMapButton.setOnClickListener {
            Log.i(Constants.TAG, "extendMapButton clicked")
            // Check that an initial map is available.
            val initialExplorationMap = localizeHelper.initialExplorationMap
            if (initialExplorationMap != null) {

                // Check that the Activity owns the focus.
                val qiContext = robotHelper.qiContext ?: return@setOnClickListener
                // Start the map extension step.
                startMapExtensionStep(initialExplorationMap, qiContext)
            } else {
                Toast.makeText(this, "Please First Make a Map", Toast.LENGTH_LONG).show()
            }

        }


    }


    private fun startMapExtensionStep(initialExplorationMap: ExplorationMap, qiContext: QiContext) {
        Log.i(Constants.TAG, "StartMapEXTENSION Class")
        binding.localizationView.extendMapButton.isEnabled = false
        robotHelper.holdAbilities(qiContext, true)!!.andThenConsume {
            robotHelper.animationToLookInFront(qiContext)!!.andThenConsume {
                localizeHelper.extendMap(initialExplorationMap, qiContext) { updatedMap ->
                    binding.localizationView.explorationMapView.setExplorationMap(
                            initialExplorationMap.topGraphicalRepresentation
                    )
                    localizeHelper.toSaveUpdatedExplorationMap = updatedMap
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

        Log.i(Constants.TAG, "startMappingStep Class")

        // show progress dialog
        showProgress(robotHelper.qiContext, "Creating Map ...")

        // Map the surroundings and get the map.
        localizeHelper.mapSurroundings(qiContext).thenConsume { future ->
            if (future.isSuccess) {
                Log.i(Constants.TAG, "FUTURE Success")

                val explorationMap = future.get()
                // Store the initial map.
                localizeHelper.initialExplorationMap = explorationMap
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
                        Log.i(Constants.TAG, "awdawdawdawdawds")
                        binding.localizationView.extendMapButton.isEnabled = true
                    }
                }
            } else {
                // hide progress dialog
                hideProgress()
                // If the operation is not a success, re-enable "start mapping" button.
                runOnUiThread {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        Log.i(Constants.TAG, "awdawaawdwdawdawdawds")
                        binding.localizationView.startMappingButton.isEnabled = true
                    }
                }
            }
        }
    }


    override fun onRobotFocusGained(qiContext: QiContext?) {
        Log.i(Constants.TAG, "onRobotFocusGained: ")

        robotHelper.qiContext = qiContext
        localizeHelper.actuation = qiContext!!.actuation
        localizeHelper.mapping = qiContext.mapping
        runOnUiThread {
            binding.localizationView.startMappingButton.isEnabled = true
        }

    }

    override fun onRobotFocusLost() {
        Log.i(Constants.TAG, "onRobotFocusLost: ")
        robotHelper.qiContext = null
        QiSDK.unregister(this)
    }

    override fun onRobotFocusRefused(reason: String?) {
        Log.i(Constants.TAG, "onRobotFocusRefused")
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
}