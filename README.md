# Radio Programmée V1
Application Android native pour Redmi 9 (Android 11). Chérie FM par défaut, radio modifiable, horaires hebdomadaires multiples, lecture manuelle, prise jack.

## Compiler sans PC
Déposer le contenu de ce ZIP à la racine du dépôt GitHub (pas le dossier parent), puis ouvrir **Actions > Construire APK > Run workflow**. Télécharger l'artefact `Radio-Programmee-V1-APK`, décompresser puis installer `app-debug.apk`.

## IMPORTANT
Prototype V1 à tester : le flux de Chérie FM et les autres URLs peuvent changer. L'automatisation dépend des autorisations MIUI (démarrage automatique, batterie sans restriction) et des alarmes Android. La lecture manuelle peut être interrompue par un événement programmé. Les heures de fin avant les heures de début signifient passage à minuit. La V1 utilise targetSdk 28 pour compatibilité Android 11, non destinée au Play Store.

## Sécurité
Ne pas utiliser pour des alarmes critiques. L'application ne collecte aucune donnée personnelle.
