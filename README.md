# D-Complex GPT

Application Android Java servant d’interface personnelle vers un backend d’assistant compatible OpenAI + MCP.

Le dépôt ne stocke aucune clé OpenAI dans l’APK. Le client Android appelle un backend HTTPS configurable (route recommandée `/dcomplex/*`).

## Objectifs V1
- assistant intégré dans l’application ;
- test de connexion backend/MCP ;
- ouverture de l’application officielle ChatGPT ;
- réglages persistants ;
- mise à jour APK via GitHub Releases ;
- CI Android et workflow de release signée.

## Limite d’architecture
Android ne peut pas donner des « droits sur ChatGPT » à une application tierce. L’intégration fiable se fait via l’API OpenAI côté serveur et les outils MCP autorisés par ce serveur. L’ouverture de l’application ChatGPT est séparée.
