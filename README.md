# D-Complex GPT

Application Android Java orientée **MCP uniquement**.

## Architecture

- aucune clé API OpenAI dans l’application ;
- aucune connexion directe à l’API OpenAI ;
- connexion à un serveur MCP distant en HTTPS ;
- test MCP réel avec `initialize`, `notifications/initialized` et `tools/list` ;
- affichage du nombre et du nom des outils MCP disponibles ;
- réglages MCP persistants ;
- mise à jour APK intégrée via GitHub Releases ;
- compilation APK automatisée avec GitHub Actions.

## Mise à jour Android

L’application vérifie la dernière GitHub Release, télécharge le nouvel APK puis lance l’installation Android.

Pour conserver les données entre versions, toutes les releases doivent garder :
- le même `applicationId` ;
- la même clé de signature Android.

## Important

MCP donne accès uniquement aux outils, ressources et actions réellement exposés et autorisés par le serveur MCP. Il ne donne pas automatiquement un contrôle total sur l’application officielle ChatGPT ni sur le modèle.
