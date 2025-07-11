import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class OfflineMapDownloader(
    private val context: Context
) {
    private val tileWriter by lazy {
        SqlTileWriter().apply {
            Configuration.getInstance().osmdroidBasePath =
                File(context.getExternalFilesDir(null), "osmdroid").apply { mkdirs() }
        }
    }

    /**
     * Downloads all tiles for a given center, radius (km), and only zoom level 16.
     * Calls onProgress(percentage) as download progresses.
     */
    suspend fun downloadArea(
        center: GeoPoint,
        radiusKm: Int,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val zoom = 16
        val boundingBox = calculateBoundingBox(center, radiusKm)
        val tileSource = TileSourceFactory.MAPNIK
        val tileNE = getTileFromCoordinates(boundingBox.lonEast, boundingBox.latNorth, zoom)
        val tileSW = getTileFromCoordinates(boundingBox.lonWest, boundingBox.latSouth, zoom)
        val minX = tileSW.first
        val maxX = tileNE.first
        val minY = tileNE.second
        val maxY = tileSW.second
        val totalTiles = (maxX - minX + 1) * (maxY - minY + 1)
        var downloadedTiles = 0
        try {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    try {
                        val mapTileIndex = org.osmdroid.util.MapTileIndex.getTileIndex(zoom, x, y)
                        val tileUrl = tileSource.getTileURLString(mapTileIndex)
                        val tileBytes = downloadTile(tileUrl)
                        if (tileBytes != null) {
                            val tileInputStream = ByteArrayInputStream(tileBytes)
                            tileWriter.saveFile(tileSource, mapTileIndex, tileInputStream, null)
                        }
                    } catch (_: Exception) {}
                    downloadedTiles++
                    val progress = (downloadedTiles * 100 / totalTiles).coerceAtMost(100)
                    onProgress(progress)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getTileFromCoordinates(longitude: Double, latitude: Double, zoom: Int): Pair<Int, Int> {
        val x = floor((longitude + 180.0) / 360.0 * (1 shl zoom)).toInt()
        val y = floor((1.0 - ln(kotlin.math.tan(Math.toRadians(latitude)) + 1.0 / cos(Math.toRadians(latitude))) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
        return Pair(x, y)
    }

    private fun calculateBoundingBox(center: GeoPoint, radiusKm: Int): BoundingBox {
        val latPerKm = 1 / 110.574
        val lonPerKm = 1 / (111.320 * cos(Math.toRadians(center.latitude)))
        return BoundingBox(
            center.latitude + radiusKm * latPerKm,
            center.longitude + radiusKm * lonPerKm,
            center.latitude - radiusKm * latPerKm,
            center.longitude - radiusKm * lonPerKm
        )
    }

    private fun downloadTile(tileUrl: String): ByteArray? {
        return try {
            val url = URL(tileUrl)
            val connection = url.openConnection()
            connection.connect()
            val inputStream: InputStream = connection.getInputStream()
            val tileBytes = inputStream.readBytes()
            inputStream.close()
            tileBytes
        } catch (_: Exception) {
            null
        }
    }
}