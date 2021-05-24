package ge.android.gis.pepperlocalizeandmove.utils.robot_helper

import android.provider.SyncStateContract
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.`object`.actuation.AttachedFrame
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.`object`.actuation.OrientationPolicy
import com.aldebaran.qi.sdk.util.FutureUtils
import com.softbankrobotics.dx.pepperextras.actuation.StubbornGoToBuilder
import ge.android.gis.pepperlocalizeandmove.utils.constants.HelperVariables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class GotoHelper {

    fun goToRandomLocation(setGoToRandom: Boolean) {

        if (setGoToRandom) {
            HelperVariables.goToRandomFuture = FutureUtils.wait(40, TimeUnit.SECONDS)
                .andThenConsume {
                    Log.d(HelperVariables.TAG, "PEPPER GOTO RANDOM LOCATION STARTED ")
                    HelperVariables.job.cancel()
                    goToRandomLocation(
                        setGoToRandom
                    )
                }


            HelperVariables.nextLocation = pickRandomLocation()

            Log.d(HelperVariables.TAG, "PEPPER NEXT LOCATION IS ${HelperVariables.nextLocation} ")

            thread {

                goToLocation(HelperVariables.nextLocation!!)
            }

        } else {
            try {
                HelperVariables.goToRandomFuture!!.thenConsume {

                    Log.d(HelperVariables.TAG, "PEPPER GOTO RANDOM LOCATION STOPPED ")

                }
                HelperVariables.goToRandomFuture!!.requestCancellation()
                HelperVariables.goToRandomFuture!!.cancel(true)


            } catch (e: Exception) {
                Log.d(HelperVariables.TAG, "PEPPER STOPPED GOTO RANDOM LOCATION HAS ERROR $e ")
            }
        }
    }
    private fun goToLocation(location: String) {

        Log.d(HelperVariables.TAG, " PEPPER STARTED GOING ON THIS LOCATION $location ")


        val freeFrame: AttachedFrame? = HelperVariables.savedLocations[location]
        val frameFuture: Frame = freeFrame!!.frame()

        HelperVariables.job = CoroutineScope(Dispatchers.IO).launch {

            HelperVariables.goto = StubbornGoToBuilder.with(HelperVariables.qiContext!!)
                .withFinalOrientationPolicy(OrientationPolicy.ALIGN_X)
                .withMaxRetry(10)
                .withMaxSpeed(0.25f)
                .withMaxDistanceFromTargetFrame(0.3)
                .withWalkingAnimationEnabled(true)
                .withFrame(frameFuture).build()
            HelperVariables.currentGoToAction = HelperVariables.goto!!.async().run()

        }

//        waitForInstructions()
    }
//    fun waitForInstructions() {
//        Log.i("TAG", "Waiting for instructions...")
//        runOnUiThread {
//            binding.localizationView.saveButton.isEnabled = true
//            binding.localizationView.goToRandom.isEnabled = true
//        }
//    }

    private fun pickRandomLocation(): String {


        val keysAsArray: MutableList<String> = java.util.ArrayList(HelperVariables.savedLocations.keys)
        val r = Random()
        val location = keysAsArray[r.nextInt(keysAsArray.size)]
        return if (location != HelperVariables.nextLocation) {
            location
        } else pickRandomLocation()
    }

    fun checkAndCancelCurrentGoto(): Future<Boolean>? {
        if (HelperVariables.currentGoToAction == null) {

            return Future.of(null)
        }

        HelperVariables.job.cancel()

        HelperVariables.currentGoToAction!!.requestCancellation()

        Log.d(HelperVariables.TAG, "PEPPER CHECK AND CANCEL GOTO  " )

        return HelperVariables.currentGoToAction
    }


}