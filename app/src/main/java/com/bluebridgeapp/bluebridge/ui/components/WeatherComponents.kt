package com.bluebridgeapp.bluebridge.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WeatherData
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EmptyWeatherState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(id = R.string.no_weather_data_available),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(id = R.string.load_weather_content_description),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(stringResource(id = R.string.load_weather))
        }
    }
}

@Composable
fun WeatherContent(
    groupedWeather: Map<String, List<WeatherData>>,
    userData: UserData?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show location
        item {
            LocationCard(userData)
        }

        // Show weather for each day
        groupedWeather.forEach { (date, weatherItems) ->
            item {
                DayForecast(date, weatherItems)
            }
        }
    }
}

@Composable
fun LocationCard(userData: UserData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = stringResource(id = R.string.location_icon_content_description),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            val context = LocalContext.current
            Text(
                text = if (userData != null) {
                    context.getString(R.string.weather_for_user_location, userData.username)
                } else {
                    stringResource(id = R.string.current_location)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun DayForecast(date: String, weatherItems: List<WeatherData>) {
    val context = LocalContext.current
    // Parse the date for display
    val displayDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern(context.getString(R.string.date_format_yyyy_mm_dd))
        val displayFormatter = java.time.format.DateTimeFormatter.ofPattern(context.getString(R.string.date_format_eeee_mmmm_d), Locale.getDefault())
        val parsedDate = java.time.LocalDate.parse(date, dateFormatter)
        parsedDate.format(displayFormatter)
    } else {
        val inputFormat = SimpleDateFormat(context.getString(R.string.date_format_yyyy_mm_dd), Locale.getDefault())
        val outputFormat = SimpleDateFormat(context.getString(R.string.date_format_eeee_mmmm_d), Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        parsedDate?.let { outputFormat.format(it) } ?: date
    }

    // Weather for current day
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Get min and max temperatures for the day
            val minTemp = weatherItems.minByOrNull { it.minTemperature }?.minTemperature ?: 0.0
            val maxTemp = weatherItems.maxByOrNull { it.maxTemperature }?.maxTemperature ?: 0.0

            // Get the first weather item for this day for a summary
            val firstWeather = weatherItems.firstOrNull()

            firstWeather?.let { weather ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weather icon
                    AsyncImage(
                        model = weather.icon,
                        contentDescription = weather.description,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )

                    Column(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = weather.description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row {
                            Text(
                                text = stringResource(id = R.string.min_temp, minTemp.toInt()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                            Text(
                                text = stringResource(id = R.string.max_temp, maxTemp.toInt()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = stringResource(id = R.string.humidity_icon_content_description),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.humidity_value, weather.humidity),
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                            Icon(
                                Icons.Default.Air,
                                contentDescription = stringResource(id = R.string.wind_icon_content_description),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.wind_speed_value, weather.windSpeed),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hourly forecast
            Text(
                text = stringResource(id = R.string.hourly_forecast),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weatherItems) { weather ->
                    HourlyWeatherItem(weather)
                }
            }
        }
    }
}

@Composable
fun HourlyWeatherItem(weather: WeatherData) {
    Card(
        modifier = Modifier
            .size(100.dp, 150.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = weather.time,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            AsyncImage(
                model = weather.icon,
                contentDescription = weather.description,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = stringResource(id = R.string.temperature_value_degrees_celsius, weather.temperature.toInt()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            if (weather.rainAmount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = stringResource(id = R.string.rain_icon_content_description),
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF4FC3F7)
                    )
                    Text(
                        text = stringResource(id = R.string.rain_amount_mm, weather.rainAmount),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
