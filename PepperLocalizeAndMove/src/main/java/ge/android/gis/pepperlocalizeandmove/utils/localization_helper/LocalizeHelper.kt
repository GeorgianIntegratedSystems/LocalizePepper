package ge.android.gis.pepperlocalizeandmove.utils.localization_helper

import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.Promise
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.`object`.actuation.*
import com.aldebaran.qi.sdk.`object`.geometry.TransformTime
import com.aldebaran.qi.sdk.`object`.streamablebuffer.StreamableBuffer
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder
import com.aldebaran.qi.sdk.util.FutureUtils
import com.softbankrobotics.dx.pepperextras.ui.ExplorationMapView
import java.util.concurrent.TimeUnit

class LocalizeHelper() {
    private var TAG = "LOCALIZE_HELPER"

    var streamableExplorationMap: StreamableBuffer? = null

    var actuation: Actuation? = null
    var mapping: Mapping? = null

    var publishExplorationMapFuture: Future<Void>? = null

    var toSaveUpdatedExplorationMap: ExplorationMap? = null

    var initialExplorationMap: ExplorationMap? = null


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
                        // Start the map notification process.
                        publishExplorationMapFuture =
                            publishExplorationMap(localizeAndMap, updatedMapCallback)
                    }
                }

                // Run the LocalizeAndMap.
                localizeAndMap.async().run()
                    .thenConsume {
                        // Remove the OnStatusChangedListener.
                        localizeAndMap.removeAllOnStatusChangedListeners()
                        // Stop the map notification process.
                        publishExplorationMapFuture?.cancel(true)
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
        return streamableExplorationMap
    }

    fun setStreamableMap(map: StreamableBuffer) {
        streamableExplorationMap = map
    }

    fun createAttachedFrameFromCurrentPosition(): Future<AttachedFrame>? {
        return actuation!!.async()
            .robotFrame()
            .andThenApply { robotFrame: Frame ->
                val mapFrame: Frame = mapping!!.async().mapFrame().value;


                val transformTime: TransformTime = robotFrame.computeTransform(mapFrame)
                mapFrame.makeAttachedFrame(transformTime.transform)
            }
    }

    fun getMapFrame(): Frame? {
        return mapping!!.async().mapFrame().value
    }


}