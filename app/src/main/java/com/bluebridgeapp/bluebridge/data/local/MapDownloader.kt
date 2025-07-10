import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import java.io.File
import java.net.URL
import java.io.InputStream
import java.io.ByteArrayInputStream
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow

class OfflineMapDownloader(
    private val context: Context,
    private val mapView: MapView
) {
    private val tileWriter by lazy {
        SqlTileWriter().apply {
            // Set cache location
            Configuration.getInstance().osmdroidBasePath =
                File(context.getExternalFilesDir(null), "osmdroid").apply { mkdirs() }
        }
    }

    suspend fun downloadArea(
        center: GeoPoint,
        radiusKm: Int,
        minZoom: Int = 12,
        maxZoom: Int = 18,
        onProgress: (Int) -> Unit = {}
    ) {
        val boundingBox = calculateBoundingBox(center, radiusKm)
        val totalTiles = calculateTotalTiles(boundingBox, minZoom, maxZoom)
        var downloadedTiles = 0

        val tileSource = TileSourceFactory.MAPNIK

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (zoom in minZoom..maxZoom) {
                    val tileNE = getTileFromCoordinates(boundingBox.lonEast, boundingBox.latNorth, zoom)
                    val tileSW = getTileFromCoordinates(boundingBox.lonWest, boundingBox.latSouth, zoom)

                    val minX = tileSW.first
                    val maxX = tileNE.first
                    val minY = tileNE.second
                    val maxY = tileSW.second

                    for (x in minX..maxX) {
                        for (y in minY..maxY) {
                            try {
                                val mapTileIndex = org.osmdroid.util.MapTileIndex.getTileIndex(zoom, x, y)
                                val tileUrl = tileSource.getTileURLString(mapTileIndex)
                                downloadTile(tileUrl, x, y, zoom)
                                downloadedTiles++
                                val progress = (downloadedTiles * 100 / totalTiles).coerceAtMost(100)
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            } catch (e: Exception) {
                                // Skip failed tiles and continue
                                e.printStackTrace()
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Map downloaded successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun downloadTile(tileUrl: String, x: Int, y: Int, zoom: Int) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(tileUrl)
                val connection = url.openConnection()
                connection.connect()

                val inputStream: InputStream = connection.getInputStream()
                val tileBytes = inputStream.readBytes()
                inputStream.close()

                // Save tile using SqlTileWriter
                val mapTileIndex = org.osmdroid.util.MapTileIndex.getTileIndex(zoom, x, y)
                val tileInputStream = ByteArrayInputStream(tileBytes)
                tileWriter.saveFile(TileSourceFactory.MAPNIK, mapTileIndex, tileInputStream, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getTileFromCoordinates(longitude: Double, latitude: Double, zoom: Int): Pair<Int, Int> {
        val x = floor((longitude + 180.0) / 360.0 * (1 shl zoom)).toInt()
        val y = floor((1.0 - kotlin.math.ln(kotlin.math.tan(Math.toRadians(latitude)) + 1.0 / cos(Math.toRadians(latitude))) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
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

    private fun calculateTotalTiles(boundingBox: BoundingBox, minZoom: Int, maxZoom: Int): Int {
        var total = 0
        for (zoom in minZoom..maxZoom) {
            val tileNE = getTileFromCoordinates(boundingBox.lonEast, boundingBox.latNorth, zoom)
            val tileSW = getTileFromCoordinates(boundingBox.lonWest, boundingBox.latSouth, zoom)

            val tilesX = tileNE.first - tileSW.first + 1
            val tilesY = tileSW.second - tileNE.second + 1
            total += tilesX * tilesY
        }
        return total
    }
}