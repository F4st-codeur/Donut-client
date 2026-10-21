# Donut Client 🍩

Client Minecraft **PvP / utilitaire** basé sur [Fabric](https://fabricmc.net/), pour **Minecraft 1.21.4** (Java 21).

Le mod contient uniquement des fonctionnalités « légitimes » (HUD, confort, rendu). Il n'y a ni KillAura, ni Fly, ni X-Ray : ces modules te feraient bannir de la plupart des serveurs.

## Fonctionnalités

| Catégorie | Module | Description |
|-----------|--------|-------------|
| HUD | Watermark | Nom du client avec un effet arc-en-ciel |
| HUD | FPS | Images par seconde |
| HUD | Ping | Latence avec le serveur |
| HUD | Coordinates | Position XYZ et direction |
| HUD | CPS | Clics par seconde (gauche / droit) |
| HUD | ArrayList | Liste des modules actifs (en haut à droite) |
| HUD | Keystrokes | Touches ZQSD/WASD, clics et saut (en bas à droite) |
| Render | Fullbright | Luminosité maximale |
| Render | Zoom | Maintiens **C** pour zoomer |
| Movement | ToggleSprint | Sprint automatique |

### Commandes

- **Shift droit** : ouvrir la ClickGUI (touche modifiable dans *Options → Contrôles*)
- Dans la ClickGUI :
  - **clic gauche** sur un module pour l'activer ou le désactiver ;
  - **clic droit** sur un module pour lui assigner une touche (Échap ou Suppr pour l'effacer) ;
  - **glisser l'en-tête** d'un panneau pour le déplacer, **clic droit** dessus pour le replier.

La configuration est sauvegardée dans `.minecraft/config/donutclient.json`.

## Compiler et lancer

Prérequis : un **JDK 21**.

```bash
# Lancer Minecraft avec le client (environnement de dev)
./gradlew runClient

# Construire le .jar
./gradlew build
```

Le jar se trouve ensuite dans `build/libs/donut-client-1.0.0.jar`. Pour l'utiliser dans ton vrai jeu :

1. installe [Fabric Loader](https://fabricmc.net/use/installer/) pour la 1.21.4 ;
2. place `donut-client-1.0.0.jar` et [Fabric API](https://modrinth.com/mod/fabric-api) dans `.minecraft/mods/` ;
3. lance le profil Fabric.

Pour IntelliJ IDEA, ouvre le dossier du projet : Gradle importe tout tout seul, puis utilise la configuration *Minecraft Client*.

## Structure du code

```
src/main/java/com/donutclient/
├── DonutClient.java          # Point d'entrée : touches, événements tick/HUD
├── module/
│   ├── Module.java           # Classe de base d'un module
│   ├── TextHudModule.java    # Module HUD sous forme d'une ligne de texte
│   ├── ModuleManager.java    # Enregistrement des modules et touches
│   ├── hud/ render/ movement/
├── gui/
│   ├── ClickGuiScreen.java   # Interface de gestion des modules
│   └── HudRenderer.java      # Dessin du HUD
├── mixin/                    # Injections dans le code de Minecraft
└── util/                     # Config, CPS, couleurs
```

### Ajouter un module

1. Crée une classe qui étend `Module` (ou `TextHudModule` pour une simple ligne de HUD) :

```java
public class MonModule extends Module {
    public MonModule() {
        super("MonModule", "Ce que fait le module.", Category.RENDER);
    }

    @Override
    public void onTick() {
        // appelé à chaque tick quand le module est actif
    }

    @Override
    public void onRender(DrawContext context) {
        // dessin dans le HUD
    }
}
```

2. Ajoute-la dans le constructeur de `ModuleManager` avec `add(new MonModule());`.

Le module apparaît alors automatiquement dans la ClickGUI et dans la config.
