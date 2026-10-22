# Donut Client 🍩

Client Minecraft **PvP / utilitaire** basé sur [Fabric](https://fabricmc.net/), pour **Minecraft 1.21.11** (Java 21).

Le mod contient surtout des fonctionnalités de confort (HUD, rendu). Le module BaseFinder fonctionne partout, mais sur un serveur, vérifie que les règles l'autorisent.

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
| Render | ChestESP | Surligne coffres, ender chests, shulkers, barils (à travers les murs) |
| Render | PlayerESP | Montre les autres joueurs à travers les murs (contour et/ou boîte) |
| World | BaseFinder | Surligne le kelp qui a poussé et l'améthyste |
| World | SchemDigger | Bot qui mine tout seul le schéma Litematica chargé |
| World | SchemBuilder | Bot qui construit tout seul le schéma Litematica chargé |
| Movement | ToggleSprint | Sprint automatique |
| Misc | FakeMoney | Affiche un faux solde sur ton écran (visuel uniquement) |

### Commandes

- **Shift droit** : ouvrir la ClickGUI (touche modifiable dans *Options → Contrôles*)
- Dans la ClickGUI :
  - **clic gauche** sur un module pour l'activer ou le désactiver ;
  - **clic droit** sur un module pour afficher ses réglages et sa touche ;
  - sur un réglage : clic gauche pour l'activer, le désactiver ou augmenter sa valeur, clic droit pour la diminuer ;
  - clic sur **Touche** puis appui sur une touche pour l'assigner (Échap ou Suppr pour l'effacer) ;
  - **glisser l'en-tête** d'un panneau pour le déplacer, **clic droit** dessus pour le replier.

### ChestESP et PlayerESP

**ChestESP** surligne les conteneurs chargés autour de toi, chacun avec sa couleur :
- coffres en orange, coffres piégés en rouge ;
- ender chests en violet ;
- shulkers en rose ;
- barils en marron ;
- en option, fours, hoppers et dispensers en gris.

Chaque type s'active séparément dans les réglages, et l'ArrayList affiche le nombre trouvé.

**PlayerESP** montre les autres joueurs. Réglage **Mode** :
- *Contour* : contour lumineux rouge, comme l'effet Glowing ;
- *Boîte* : boîte rouge translucide ;
- *Les deux*.

Le réglage **À travers les murs** est aussi disponible dans BaseFinder, ChestESP et PlayerESP.

### FakeMoney

FakeMoney remplace les montants en `$` affichés sur **ton** écran (scoreboard, tab, textes du HUD) par un faux montant. Exemple : `Money: $1.25M` devient `Money: $100M`.

C'est **uniquement visuel** : ton vrai solde ne change pas, le serveur et les autres joueurs voient toujours la vraie valeur, et l'argent n'est pas utilisable.

Réglages :
- **Montant** et **Unité** (aucune, K, M, B, T) : la valeur affichée ;
- **Où** :
  - *Lignes argent* : seulement les lignes qui contiennent « money », « balance », « argent », « solde » ou « coins » ;
  - *Tous les $* : tous les montants en `$`.

Les messages du chat ne sont pas modifiés.

### BaseFinder

Les plantes ne poussent que dans les chunks chargés, donc près d'un joueur. Plus elles ont grandi, plus quelqu'un est resté longtemps à côté.

- **Kelp** (vert) : à la génération du monde, le haut d'un kelp a un âge de 20 à 23. Le module surligne toute la colonne de kelp dont le haut a atteint l'âge maximum (25), qui a donc forcément poussé après la génération. *Kelp min (blocs)* filtre les colonnes trop courtes.
- **Amethyst** (violet) : surligne les grappes d'améthyste (stade final). *Gros bourgeons* (rose) ajoute aussi l'avant-dernier stade.
- **Rayon (chunks)** : distance de scan autour de toi (2 à 16 chunks).

Le nombre de résultats s'affiche dans l'ArrayList. Les surbrillances ne se voient pas à travers les blocs.

### SchemDigger et SchemBuilder

Ces deux bots travaillent sur le schéma chargé dans [Litematica](https://modrinth.com/mod/litematica) (avec MaLiLib). Il faut installer Litematica pour la 1.21.11 dans `mods/`. Sans lui, les modules affichent « Litematica absent ».

1. Charge et place ton schéma avec Litematica, comme d'habitude.
2. Active **SchemDigger** ou **SchemBuilder** dans la ClickGUI.
3. Le bot scanne la zone du schéma, marche jusqu'au bloc le plus proche et agit. L'ArrayList affiche le nombre de blocs restants.

**SchemDigger** mine en partant de la couche du haut. Il choisit tout seul le meilleur outil de la hotbar et évite les blocs qui touchent de la lave. Réglage **Mode** :
- *Différents* : mine les blocs qui ne correspondent pas au schéma (pour creuser une forme ou préparer le terrain) ;
- *Identiques* : mine les blocs qui correspondent au schéma (pour démonter un build) ;
- *Tout* : vide toute la zone.

**SchemBuilder** construit en partant de la couche du bas, avec les blocs de ton inventaire. Il les ramène dans la hotbar si besoin et affiche « manque X » s'il lui manque un bloc. **Délai (ticks)** règle la vitesse de pose.

**Orientation** : avant chaque pose, SchemBuilder simule la pose sous les 16 directions, en regardant devant, en haut ou en bas, sur chaque face d'appui et à deux hauteurs de clic. Il garde la combinaison qui donne la même orientation que dans le schéma : escaliers, bûches, fours, coffres, observateurs, pistons, hoppers, dalles haut/bas, trappes, leviers, boutons, panneaux, têtes, torches murales, etc. Il envoie ensuite cette rotation au serveur avant de poser. Les portes et les lits sont posés par leur moitié du bas ou leur pied, et l'autre moitié suit toute seule. Avec **Orientation stricte** (activé par défaut), un bloc impossible à orienter correctement est mis de côté au lieu d'être posé de travers.

**Pathfinding** : pour se déplacer, les bots utilisent l'algorithme A*. Ils calculent un chemin qui contourne les obstacles : marcher (y compris en diagonale), monter d'un bloc en sautant, descendre jusqu'à 3 blocs. Ils évitent la lave, le feu, les cactus, les toiles d'araignée et les autres blocs dangereux. Si la cible est trop loin pour un seul calcul, le bot avance vers le point le plus proche trouvé puis recalcule depuis là.

Limites actuelles :
- le bot ne nage pas et ne casse ni ne pose de blocs pour se frayer un chemin. Si aucun chemin n'existe, il passe à un autre bloc et réessaie plus tard ;
- les dalles doubles sont posées comme des dalles simples ;
- n'active pas les deux bots en même temps ;
- ouvrir un menu (inventaire, chat...) met le bot en pause.

### Mode legit

En haut de la ClickGUI, la case **Mode legit** n'affiche que les modules legit, ceux qui ne donnent pas d'avantage de triche :
- tout le HUD ;
- Fullbright, Zoom et ToggleSprint.

Quand tu la coches, les autres modules (ESP, BaseFinder, bots, Misc) sont **désactivés et cachés**, et leurs touches ne marchent plus. Décoche-la pour tout retrouver. Ce choix est sauvegardé.

La configuration est sauvegardée dans `.minecraft/config/donutclient.json`.

## Télécharger le .jar

À chaque push, GitHub Actions compile le mod. Le `.jar` est publié dans les **Releases** du dépôt (build-1, build-2...) et dans l'onglet **Actions**, en artefact `donut-client`.

## Compiler et lancer

Prérequis : un **JDK 21**.

```bash
# Lancer Minecraft avec le client (environnement de dev)
./gradlew runClient

# Construire le .jar
./gradlew build
```

Le jar se trouve ensuite dans `build/libs/donut-client-1.1.0.jar`. Pour l'utiliser dans ton vrai jeu :

1. installe [Fabric Loader](https://fabricmc.net/use/installer/) pour la 1.21.11 ;
2. place `donut-client-1.1.0.jar` et [Fabric API](https://modrinth.com/mod/fabric-api) dans `.minecraft/mods/` ;
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
│   ├── setting/              # Réglages (on/off, nombres, modes)
│   ├── hud/ render/ world/ movement/
├── gui/
│   ├── ClickGuiScreen.java   # Interface de gestion des modules
│   └── HudRenderer.java      # Dessin du HUD
├── mixin/                    # Injections dans le code de Minecraft
└── util/                     # Config, CPS, couleurs, pont Litematica
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
