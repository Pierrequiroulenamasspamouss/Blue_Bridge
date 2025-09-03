# FCM Image Chunks System Documentation

## Overview

Le système de chunks d'images FCM permet d'envoyer des images via Firebase Cloud Messaging en les divisant en petits morceaux (chunks) pour contourner les limitations de taille des messages FCM.

## Architecture

### Composants principaux

1. **ImageChunkManager** - Gère le stockage temporaire des chunks
2. **FirebaseMessagingService** - Reçoit et traite les chunks
3. **ImageUtils** - Utilitaires pour la manipulation d'images
4. **ChatRepository** - Gère la sauvegarde des messages d'images

### Flux de données

```
Image → Base64 → Chunks → FCM → Reception → Reconstruction → Sauvegarde
```

## Implémentation

### 1. Envoi d'images

#### ChatRepositoryImpl.sendImageMessage()
- Convertit l'image en Base64
- Divise en chunks de 3.5KB
- Envoie chaque chunk via FCM
- Sauvegarde le message localement

#### ImageUtils.splitImageToChunks()
- Divise une string Base64 en chunks
- Taille par défaut : 3.5KB par chunk
- Retourne une liste d'objets ImageChunk

### 2. Réception d'images

#### FirebaseMessagingService.handleImageChunk()
- Reçoit les chunks FCM
- Stocke temporairement via ImageChunkManager
- Vérifie si tous les chunks sont reçus
- Reconstruit l'image quand complet

#### ImageChunkManager
- Stockage temporaire en mémoire
- Gestion des chunks par imageId
- Nettoyage automatique après reconstruction

### 3. Reconstruction d'images

#### ImageUtils.reconstructImageFromChunks()
- Trie les chunks par index
- Vérifie l'intégrité (tous les chunks présents)
- Reconstruit la string Base64
- Sauvegarde l'image sur le disque

## Utilisation

### Dans l'application

1. **Envoi d'image** :
```kotlin
chatRepository.sendImageMessage(imageUri, receiverId)
```

2. **Réception automatique** :
- Les chunks sont reçus automatiquement via FCM
- L'image est reconstruite et sauvegardée
- Un message de chat est créé

### Test avec l'outil Python

```bash
# Envoyer une image en chunks
python Tools/fcm_messaging_tool.py \
  --server-key "YOUR_FCM_SERVER_KEY" \
  --token "DEVICE_TOKEN" \
  --image "path/to/image.jpg" \
  --sender-name "John" \
  --sender-id "user123" \
  --receiver-id "user456"

# Envoyer un message de chat normal
python Tools/fcm_messaging_tool.py \
  --server-key "YOUR_FCM_SERVER_KEY" \
  --token "DEVICE_TOKEN" \
  --chat \
  --sender-name "John" \
  --body "Hello!"
```

### Écran de debug

L'écran de debug permet de :
- Tester l'envoi d'images
- Simuler la réception de chunks
- Visualiser les images reçues
- Surveiller l'état du ChunkManager

## Configuration

### Taille des chunks
- Taille par défaut : 3.5KB
- Configurable dans `ImageUtils.MAX_PAYLOAD_SIZE`
- Doit être inférieure à la limite FCM (4KB)

### Notifications
- Notifications de progression pendant la réception
- Notification finale quand l'image est reçue
- IDs uniques pour éviter les conflits

## Sécurité

### Validation des données
- Vérification des types de données
- Validation des tailles de chunks
- Nettoyage automatique des chunks temporaires

### Gestion d'erreurs
- Timeout sur les chunks manquants
- Nettoyage en cas d'erreur
- Logs détaillés pour le debugging

## Limitations

1. **Taille maximale** : ~1MB par image (limitation FCM)
2. **Latence** : Délai entre les chunks pour éviter le rate limiting
3. **Mémoire** : Stockage temporaire des chunks en mémoire
4. **Fiabilité** : Dépend de la réception de tous les chunks

## Debugging

### Logs utiles
```
BlueBridgeFCM: Handling image chunk
BlueBridgeFCM: Stored chunk 1/5 for image 1234567890
BlueBridgeFCM: All chunks received for image 1234567890, reconstructing...
BlueBridgeFCM: Image saved successfully: /path/to/image.jpg
```

### Outils de test
- `Tools/fcm_messaging_tool.py` - Envoi de chunks
- Écran de debug dans l'app - Test et visualisation
- Logs détaillés dans Logcat

## Évolutions futures

1. **Compression** : Réduction de la taille des images
2. **Retry** : Nouvelle tentative en cas d'échec
3. **Chiffrement** : Sécurisation des données
4. **Cache** : Mise en cache des images fréquentes
5. **Progression** : Indicateur de progression en temps réel 