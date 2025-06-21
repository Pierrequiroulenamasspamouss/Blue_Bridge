package com.bluebridge.bluebridgeapp.ui.screens.miscscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Point(val x: Int, val y: Int) {
    // Equals and hashCode implemented for easier comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Point) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return 31 * x + y
    }
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

const val GRID_SIZE = 15 // Smaller grid for better visibility
const val CELL_SIZE = 20 // Fixed size in dp

// Generate food ensuring it doesn't overlap with the snake
fun generateFood(snake: List<Point>): Point {
    var food: Point
    do {
        food = Point(
            x = Random.nextInt(GRID_SIZE),
            y = Random.nextInt(GRID_SIZE)
        )
    } while (snake.contains(food))
    return food
}

// Different types of food for bonus points
enum class FoodType(val color: Color, val points: Int) {
    NORMAL(Color.Red, 1),
    BONUS(Color.Yellow, 3),
    SUPER(Color.Magenta, 5)
}

@Composable
fun EasterEgg() {
    var snake by remember { mutableStateOf(listOf(Point(x = GRID_SIZE / 2, y = GRID_SIZE / 2))) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var food by remember { mutableStateOf(generateFood(snake)) }
    var foodType by remember { mutableStateOf(FoodType.NORMAL) }
    var isPlaying by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var gameSpeed by remember { mutableIntStateOf(200) } // ms delay between moves
    var gameOver by remember { mutableStateOf(false) }
    var level by remember { mutableIntStateOf(1) }

    // Create special food at random intervals
    LaunchedEffect(isPlaying) {
        while (isPlaying && !gameOver) {
            delay(5000L) // 5 seconds
            if (!gameOver && isPlaying) {
                foodType = FoodType.entries.toTypedArray().random()
            }
        }
    }

    // Main game loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (!gameOver) {
                delay(gameSpeed.toLong())
                val head = snake.first()
                val newHead = when (direction) {
                    Direction.UP -> Point(head.x, (head.y - 1 + GRID_SIZE) % GRID_SIZE)
                    Direction.DOWN -> Point(head.x, (head.y + 1) % GRID_SIZE)
                    Direction.LEFT -> Point((head.x - 1 + GRID_SIZE) % GRID_SIZE, head.y)
                    Direction.RIGHT -> Point((head.x + 1) % GRID_SIZE, head.y)
                }

                // Check for collision with self
                if (snake.contains(newHead)) {
                    gameOver = true
                } else {
                    val newSnake = mutableListOf(newHead)
                    newSnake.addAll(snake)

                    if (newHead.x == food.x && newHead.y == food.y) {
                        // Increase score based on food type
                        score += foodType.points

                        // Level up every 10 points
                        if (score / 10 > level - 1) {
                            level = score / 10 + 1
                            gameSpeed = maxOf(50, 200 - (level - 1) * 15) // Speed up gradually
                        }

                        food = generateFood(newSnake)
                        foodType = if (Random.nextInt(10) < 7) FoodType.NORMAL else FoodType.entries.toTypedArray()
                            .random()
                    } else {
                        newSnake.removeAt(newSnake.lastIndex)
                    }
                    snake = newSnake
                }
            }
            // Update high score on game over
            if (score > highScore) {
                highScore = score
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game title
            Text(
                text = "Snake Game",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Score display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Score", fontWeight = FontWeight.Bold)
                        Text("$score", fontSize = 18.sp)
                    }
                }

                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("High Score", fontWeight = FontWeight.Bold)
                        Text("$highScore", fontSize = 18.sp)
                    }
                }

                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Level", fontWeight = FontWeight.Bold)
                        Text("$level", fontSize = 18.sp)
                    }
                }
            }

            if (gameOver) {
                // Game over screen
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Game Over!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your score: $score",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            snake = listOf(Point(x = GRID_SIZE / 2, y = GRID_SIZE / 2))
                            direction = Direction.RIGHT
                            food = generateFood(snake)
                            foodType = FoodType.NORMAL
                            score = 0
                            level = 1
                            gameSpeed = 200
                            gameOver = false
                            isPlaying = true
                        }) {
                            Text("Play Again")
                        }
                    }
                }
            } else {
                // Game board
                Box(
                    modifier = Modifier
                        .size((GRID_SIZE * CELL_SIZE).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                ) {
                    // Draw game board
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Draw grid lines
                        val strokeWidth = 1f
                        for (i in 0..GRID_SIZE) {
                            drawLine(
                                color = Color(0xFF333333),
                                start = Offset(x = i * CELL_SIZE.dp.toPx(), y = 0f),
                                end = Offset(x = i * CELL_SIZE.dp.toPx(), y = size.height),
                                strokeWidth = strokeWidth
                            )
                            drawLine(
                                color = Color(0xFF333333),
                                start = Offset(x = 0f, y = i * CELL_SIZE.dp.toPx()),
                                end = Offset(x = size.width, y = i * CELL_SIZE.dp.toPx()),
                                strokeWidth = strokeWidth
                            )
                        }

                        // Draw snake head
                        if (snake.isNotEmpty()) {
                            val head = snake.first()
                            drawRect(
                                color = Color(0xFF4CAF50), // Bright green for head
                                topLeft = Offset(
                                    x = head.x * CELL_SIZE.dp.toPx(),
                                    y = head.y * CELL_SIZE.dp.toPx()
                                ),
                                size = Size(
                                    width = CELL_SIZE.dp.toPx(),
                                    height = CELL_SIZE.dp.toPx()
                                )
                            )
                        }

                        // Draw snake body
                        snake.drop(1).forEach { point ->
                            drawRect(
                                color = Color(0xFF81C784), // Lighter green for body
                                topLeft = Offset(
                                    x = point.x * CELL_SIZE.dp.toPx(),
                                    y = point.y * CELL_SIZE.dp.toPx()
                                ),
                                size = Size(
                                    width = CELL_SIZE.dp.toPx(),
                                    height = CELL_SIZE.dp.toPx()
                                )
                            )
                        }

                        // Draw food
                        drawRect(
                            color = foodType.color,
                            topLeft = Offset(
                                x = food.x * CELL_SIZE.dp.toPx(),
                                y = food.y * CELL_SIZE.dp.toPx()
                            ),
                            size = Size(
                                width = CELL_SIZE.dp.toPx(),
                                height = CELL_SIZE.dp.toPx()
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { if (direction != Direction.DOWN) direction = Direction.UP },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (direction == Direction.UP)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Up",
                            modifier = Modifier.size(40.dp),
                            tint = if (direction == Direction.UP)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { if (direction != Direction.RIGHT) direction = Direction.LEFT },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (direction == Direction.LEFT)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Left",
                                modifier = Modifier.size(40.dp),
                                tint = if (direction == Direction.LEFT)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(64.dp))

                        IconButton(
                            onClick = { if (direction != Direction.LEFT) direction = Direction.RIGHT },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (direction == Direction.RIGHT)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Right",
                                modifier = Modifier.size(40.dp),
                                tint = if (direction == Direction.RIGHT)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (direction != Direction.UP) direction = Direction.DOWN },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (direction == Direction.DOWN)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Down",
                            modifier = Modifier.size(40.dp),
                            tint = if (direction == Direction.DOWN)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Game control buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { isPlaying = !isPlaying }) {
                        Text(if (isPlaying) "Pause" else "Start")
                    }

                    Button(onClick = {
                        snake = listOf(Point(x = GRID_SIZE / 2, y = GRID_SIZE / 2))
                        direction = Direction.RIGHT
                        food = generateFood(snake)
                        foodType = FoodType.NORMAL
                        score = 0
                        level = 1
                        gameSpeed = 200
                        gameOver = false
                    }) {
                        Text("Reset")
                    }
                }

                // Instructions
                Card(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Use the arrow buttons to change direction.\n" +
                                "Red food = 1 point\n" +
                                "Yellow food = 3 points\n" +
                                "Purple food = 5 points\n",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}