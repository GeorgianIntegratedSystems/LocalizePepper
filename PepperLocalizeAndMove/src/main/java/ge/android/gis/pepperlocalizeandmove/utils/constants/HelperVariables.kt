package ge.android.gis.pepperlocalizeandmove.utils.constants

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.actuation.ExplorationMap
import com.aldebaran.qi.sdk.`object`.actuation.Mapping
import com.aldebaran.qi.sdk.`object`.holder.Holder
import com.aldebaran.qi.sdk.`object`.streamablebuffer.StreamableBuffer
import java.util.concurrent.atomic.AtomicBoolean

object HelperVariables {


    var TAG: String = "MOVE_PEPPER"

    @SuppressLint("SdCardPath")
    const val FILE_DIRECTORY_PATH = "/sdcard/Maps"
    const val MAP_FILE_NAME = "mapData.txt"
    const val LOCATION_FILE_NAME = "points.json"

    var actuation: Actuation? = null

    var mapping: Mapping? = null

    var streamableExplorationMap: StreamableBuffer? = null

    var publishExplorationMapFuture: Future<Void>? = null

    var toSaveUpdatedExplorationMap: ExplorationMap? = null

    var initialExplorationMap: ExplorationMap? = null

    var holder: Holder? = null

    var qiContext: QiContext? = null

    val loadLocationSuccess = AtomicBoolean(false)

    var PERMISSION_ALL = 1

    var PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

}