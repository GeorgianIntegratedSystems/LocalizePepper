package ge.android.gis.localizepepper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object Constants {
    var TAG: String = "MOVE_PEPPER"

    @SuppressLint("SdCardPath")
    const val FILE_DIRECTORY_PATH = "/sdcard/Maps"
    const val MAP_FILE_NAME = "mapData.txt"
    const val LOCATION_FILE_NAME = "points.json"
    var PERMISSION_ALL = 1
    var PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }


}