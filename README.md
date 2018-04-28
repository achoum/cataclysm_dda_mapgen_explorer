# Mapgen Explorer for Cataclysm DDA

**Mapgen Explorer for Cataclysm DDA** in an explorer, visualizer and editor of *mapgens* (or **prefabs** i.e. the building templates for used by the world generator) for the [Cataclysm: Dark Days Ahead](https://github.com/CleverRaven/Cataclysm-DDA) game.

**Mapgen Explorer** was made by reverse engineering the Cataclysm-DDA prefab format, and it is likely that not all (and there are lot) variations and caveats are not yet covered. Join the discussion about Mapgen Explorer on the [Cataclysm-DDA forum](https://discourse.cataclysmdda.org/t/prefab-explorer/15347).

![Imgur](https://i.imgur.com/R0oR6r0.png)

![Imgur](https://i.imgur.com/3uvPtPu.png)

![Imgur](https://i.imgur.com/pyIAq12.png)

## Usage

Download and run [Prefab Explorer's jar](https://github.com/achoum/cataclysm_dda_mapgen_explorer/releases). For most people, *double clicking* on the jar file will just work. In the case *double clicking* does not work, run the following command in a console:

~~~~
java -jar cataclysm_dda_mapgen_explorer_<version>.jar
~~~~

Mapgen Explorer will first ask you to select the Cataclysm DDA directory. After scanning for prefab and tilesets, you will be able to select prefabs to vizualize.

## Requirement
- [Java](https://java.com/en/download/)
- [Cataclysm: Dark Days Ahead](https://github.com/CleverRaven/Cataclysm-DDA) with tiles

## Supported Platforms
Anything that can run Java.

- Window (tested on Window 10).
- Linux (tested on Debian 9).
- MacOS.

## Features

- Scans and index all the mapgens automatically.
- "Linked" edition i.e. editing the json code updates the render automatically, and editing the rendered map using the edit tools updates the json code automatically.
- Instantaneous switch between mapgens. 
- Instantaneous switch between tiles. 