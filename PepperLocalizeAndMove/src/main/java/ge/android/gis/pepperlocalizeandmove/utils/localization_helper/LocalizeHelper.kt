package ge.android.gis.pepperlocalizeandmove.utils.localization_helper

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.aldebaran.qi.Future
import com.aldebaran.qi.Promise
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.`object`.actuation.*
import com.aldebaran.qi.sdk.`object`.geometry.TransformTime
import com.aldebaran.qi.sdk.`object`.streamablebuffer.StreamableBuffer
import com.aldebaran.qi.sdk.builder.ExplorationMapBuilder
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder
import com.aldebaran.qi.sdk.builder.LocalizeBuilder
import com.aldebaran.qi.sdk.util.FutureUtils
import com.softbankrobotics.dx.pepperextras.ui.ExplorationMapView
import ge.android.gis.pepperlocalizeandmove.utils.constants.HelperVariables
import ge.android.gis.pepperlocalizeandmove.utils.save_in_storage.SaveFileClass
import java.util.concurrent.TimeUnit

class LocalizeHelper() {
    private var TAG = "LOCALIZE_HELPER"

    fun mapSurroundings(qiContext: QiContext): Future<ExplorationMap> {
        // Create a Promise to set the operation state later.
        val promise = Promise<ExplorationMap>().apply {
            // If something tries to cancel the associated Future, do cancel it.
            setOnCancel {
                if (!it.future.isDone) {
                    setCancelled()
                }
            }
        }

        // Create a LocalizeAndMap, run it, and keep the Future.
        val localizeAndMapFuture = LocalizeAndMapBuilder.with(qiContext)
            .buildAsync()
            .andThenCompose { localizeAndMap ->
                // Add an OnStatusChangedListener to know when the robot is localized.
                localizeAndMap.addOnStatusChangedListener { status ->
                    if (status == LocalizationStatus.LOCALIZED) {
                        // Retrieve the map.
                        val explorationMap = localizeAndMap.dumpMap()

                        // Set the Promise state in success, with the ExplorationMap.
                        if (!promise.future.isDone) {
                            promise.setValue(explorationMap)
                        }
                    }
                }

                // Run the LocalizeAndMap.
                localizeAndMap.async().run()

                    .thenConsume {
                        // Remove the OnStatusChangedListener.
                        localizeAndMap.removeAllOnStatusChangedListeners()
                        // In case of error, forward it to the Promise.
                        if (it.hasError() && !promise.future.isDone) {
                            promise.setError(it.errorMessage)
                        }
                    }
            }

        // Return the Future associated to the Promise.
        return promise.future.thenCompose {
            // Stop the LocalizeAndMap.
            localizeAndMapFuture.cancel(true)
            return@thenCompose it
        }
    }


    fun mapToBitmap(explorationMapView: ExplorationMapView, explorationMap: ExplorationMap) {
        explorationMapView.setExplorationMap(explorationMap.topGraphicalRepresentation)
    }

    fun extendMap(
        explorationMap: ExplorationMap,
        qiContext: QiContext,
        updatedMapCallback: (ExplorationMap) -> Unit
    ): Future<Void> {

        Log.i(TAG, "ExtandMap Class")
        val promise = Promise<Void>().apply {
            // If something tries to cancel the associated Future, do cancel it.
            setOnCancel {
                if (!it.future.isDone) {
                    setCancelled()
                }
            }
        }

        // Create a LocalizeAndMap with the initial map, run it, and keep the Future.
        val localizeAndMapFuture = LocalizeAndMapBuilder.with(qiContext)
            .withMap(explorationMap)
            .buildAsync()
            .andThenCompose { localizeAndMap ->
                Log.i(TAG, "localizeandmap Class")

                // Add an OnStatusChangedListener to know when the robot is localized.
                localizeAndMap.addOnStatusChangedListener { status ->
                    if (status == LocalizationStatus.LOCALIZED) {
                        Log.i(TAG, LocalizationStatus.LOCALIZED.toString())
                        // Start the map notification process.
                        HelperVariables.publishExplorationMapFuture =
                            publishExplorationMap(localizeAndMap, updatedMapCallback)
                    }
                }

                // Run the LocalizeAndMap.
                localizeAndMap.async().run()
                    .thenConsume {
                        // Remove the OnStatusChangedListener.
                        localizeAndMap.removeAllOnStatusChangedListeners()
                        // Stop the map notification process.
                        HelperVariables.publishExplorationMapFuture?.cancel(true)
                        // In case of error, forward it to the Promise.
                        if (it.hasError() && !promise.future.isDone) {
                            promise.setError(it.errorMessage)
                        }


                    }
            }

        // Return the Future associated to the Promise.
        return promise.future.thenCompose {
            // Stop the LocalizeAndMap.
            localizeAndMapFuture.cancel(true)
            return@thenCompose it
        }
    }

    private fun publishExplorationMap(
        localizeAndMap: LocalizeAndMap,
        updatedMapCallback: (ExplorationMap) -> Unit
    ): Future<Void> {
        return localizeAndMap.async().dumpMap().andThenCompose {
            Log.i(TAG, "$it Function")
            updatedMapCallback(it)
            FutureUtils.wait(1, TimeUnit.SECONDS)
        }.andThenCompose {
            publishExplorationMap(localizeAndMap, updatedMapCallback)
        }
    }


    fun getStreamableMap(): StreamableBuffer? {
        return HelperVariables.streamableExplorationMap
    }

    fun setStreamableMap(map: StreamableBuffer) {
        HelperVariables.streamableExplorationMap = map
    }

    fun createAttachedFrameFromCurrentPosition(): Future<AttachedFrame>? {

        Log.i(TAG, HelperVariables.actuation.toString())
        return HelperVariables.actuation!!.async()
            .robotFrame()
            .andThenApply { robotFrame: Frame ->
                val mapFrame: Frame = HelperVariables.mapping!!.async().mapFrame().value;


                val transformTime: TransformTime = robotFrame.computeTransform(mapFrame)
                mapFrame.makeAttachedFrame(transformTime.transform)
            }
    }

    fun getMapFrame(): Frame? {
        return HelperVariables.mapping!!.async().mapFrame().value
    }

    fun buildStreamableExplorationMapAndLocalizeRobot(
        explorationMapView: ExplorationMapView,
        activity: Activity,
        context: Context
    ): Future<ExplorationMap>? {


        if (getStreamableMap() == null) {


            val mapData =
                SaveFileClass().readStreamableBufferFromFile(
                    HelperVariables.FILE_DIRECTORY_PATH,
                    HelperVariables.MAP_FILE_NAME
                )


            setStreamableMap(mapData!!)

        }

        Thread {

            try {

                HelperVariables.initialExplorationMap =
                    ExplorationMapBuilder.with(HelperVariables.qiContext)
                        .withStreamableBuffer(HelperVariables.streamableExplorationMap).build()

                mapToBitmap(
                    explorationMapView,
                    HelperVariables.initialExplorationMap!!
                )

                startLocalizing(HelperVariables.qiContext!!, activity, context)

            } catch (e: java.lang.Exception) {

            }

        }.start()


        if (HelperVariables.initialExplorationMap == null) {
            Log.d(
                "TAG",
                "buildStreamableExplorationMap: Building map from StreamableBuffer"
            )
            return ExplorationMapBuilder.with(HelperVariables.qiContext).withStreamableBuffer(
                HelperVariables.streamableExplorationMap
            ).buildAsync()
        }
        return Future.of(HelperVariables.initialExplorationMap)
    }


    fun startLocalizing(qiContext: QiContext, activity: Activity, context: Context) {
        // Create a Localize action.
        HelperVariables.builtLocalize = LocalizeBuilder.with(HelperVariables.qiContext)
            .withMap(HelperVariables.initialExplorationMap)
            .build()

        // Add an on status changed listener on the Localize action to know when the robot is localized in the map.
        HelperVariables.builtLocalize!!.addOnStatusChangedListener { status ->
            if (status == LocalizationStatus.LOCALIZED) {

                activity.runOnUiThread {
                    Toast.makeText(context, "Robot is localized.", Toast.LENGTH_SHORT).show()

                }
                Log.i(TAG, "Robot is localized.")

            }
        }


        activity.runOnUiThread {
            Toast.makeText(context, "Localizing...", Toast.LENGTH_SHORT).show()

        }
        Log.i(TAG, "Localizing...")

        // Execute the Localize action asynchronously.
        val localization = HelperVariables.builtLocalize!!.async().run()

        // Add a lambda to the action execution.
        localization.thenConsume { future ->
            if (future.hasError()) {

                activity.runOnUiThread {

                    Toast.makeText(context, "Localize action finished with error.", Toast.LENGTH_SHORT).show()
                }


            }
        }

    }

}