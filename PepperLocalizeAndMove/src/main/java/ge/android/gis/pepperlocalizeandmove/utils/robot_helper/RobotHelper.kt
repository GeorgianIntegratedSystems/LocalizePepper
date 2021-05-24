package ge.android.gis.pepperlocalizeandmove.utils.robot_helper

import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.`object`.actuation.Animate
import com.aldebaran.qi.sdk.`object`.actuation.Animation
import com.aldebaran.qi.sdk.`object`.holder.AutonomousAbilitiesType
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.HolderBuilder
import ge.android.gis.pepperlocalizeandmove.utils.constants.HelperVariables

class RobotHelper() {
    private var TAG = "ROBOT_HELPER_CLASS"




    fun holdAbilities(qiContext: QiContext, withBackgroundMovement: Boolean): Future<Void?>? {
        return releaseAbilities()!!.thenCompose<Void> {

            Log.d(TAG, "starting holdAbilities")
            HelperVariables.holder = if (withBackgroundMovement) {
                HolderBuilder.with(qiContext)
                    .withAutonomousAbilities(
                        AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                        AutonomousAbilitiesType.BASIC_AWARENESS
                    )
                    .build()
            } else {
                HolderBuilder.with(qiContext)
                    .withAutonomousAbilities(
                        AutonomousAbilitiesType.BASIC_AWARENESS
                    )
                    .build()
            }
            HelperVariables.holder!!.async().hold()
        }
    }

    fun releaseAbilities(): Future<Void?>? {
        return if (HelperVariables.holder != null) {

            Log.d(TAG, "releaseAbilities");
            HelperVariables.holder!!.async().release()
                .andThenConsume { _: Void? ->
                    HelperVariables.holder = null
                    Log.d(TAG, "stoped holdAbilities start releaseAbilities")

                }
        } else {
            Log.d(TAG, "releaseAbilities: No holder to release");
            Future.of(null)
        }

    }

    fun animationToLookInFront(qiContext: QiContext): Future<Void?>? {
        Log.d(TAG, "animationToLookInFront: started")
        return AnimationBuilder.with(qiContext) // Create the builder with the context.
            .withAssets("idle.qianim") // Set the animation resource.
            .buildAsync()
            .andThenCompose { animation: Animation? ->
                AnimateBuilder.with(qiContext)
                    .withAnimation(animation)
                    .buildAsync()
                    .andThenCompose { animate: Animate -> animate.async().run() }
            }
    }

}