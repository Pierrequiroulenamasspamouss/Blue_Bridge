# Well Monitoring App (Jetpack Compose + ESP32)

This Android app allows users to monitor and configure well data coming from ESP32 servers on the local network. It supports dynamic data updates, persistent local storage, and rich UI built with Jetpack Compose.

## Features

✅ View and configure individual well entries  
✅ Automatically fetch data from ESP32 (HTTP servers)  
✅ Display dynamic fields sent by the server
✅ Save and persist well configurations with DataStore  
✅ Reorder, edit, delete, and refresh wells  
✅ Gracefully handle internet connection loss and errors  

---

## 📦 Project Structure

- `data/WellData.kt` — Model class for wells, supports dynamic fields via `extraData`
- `data/WellDataStore.kt` — Handles saving/loading wells using Jetpack DataStore
- `network/RetrofitBuilder.kt` — Creates API client to communicate with ESP32 HTTP servers
- `ui/` — UI components and screens using Jetpack Compose
- `viewmodel/WellViewModel.kt` — Main ViewModel managing state and logic

---

## 📡 ESP32 Server Simulation

Use `esp32_simulator.py` (Python) to simulate ESP32 devices:

```bash
python esp32_simulator.py
```
