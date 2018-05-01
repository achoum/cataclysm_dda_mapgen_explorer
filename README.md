# Mapgen Explorer for Cataclysm DDA

**Mapgen Explorer for Cataclysm DDA** in an explorer, visualizer and editor of **mapgens** (or **prefabs** i.e. the building templates for used by the world generator) for the [Cataclysm: Dark Days Ahead](https://github.com/CleverRaven/Cataclysm-DDA) game.

Join the discussion about Mapgen Explorer on its [forum thread](https://discourse.cataclysmdda.org/t/prefab-explorer/15347).

## Usage

Download and run [Mapgen Explorer's .jar or .exe](https://github.com/achoum/cataclysm_dda_mapgen_explorer/releases). If you choose the .jar, and if _double clicking_ on the jar does not start Mapgen Explorer, run the following command line in a console:

~~~~
java -jar cataclysm_dda_mapgen_explorer_<version>.jar
~~~~

*Note: Mapgen Explorer will first ask you to specify the Cataclysm DDA directory in order to scan the prefab, tilesets and palettes.*

## Requirement & Supported Platforms
- [Java 1.8 or higher](https://java.com/en/download/)
- [Cataclysm: Dark Days Ahead](https://github.com/CleverRaven/Cataclysm-DDA) with tiles


Mapgen Explorer runs on:
- Window (tested on Window 10).
- Linux (tested on Debian 9).
- MacOS.

## Screen shots

_The prefab explorer._

![Imgur](https://i.imgur.com/R0oR6r0.png)

_Rendering with iso tileset_

![Imgur](https://raw.githubusercontent.com/achoum/cataclysm_dda_mapgen_explorer/master/screen/6.png)

_The integrated Editor_

![Imgur](https://i.imgur.com/pyIAq12.png)

_How to use your own editor_

![Imgur](https://raw.githubusercontent.com/achoum/cataclysm_dda_mapgen_explorer/master/screen/tutorial.png)


## Features

- Scans and index all the mapgens automatically.
- "Linked" edition i.e. editing the json code updates the render automatically, and editing the rendered map using the edit tools updates the json code automatically.
- Instantaneous switch between mapgens. 
- Instantaneous switch between tiles. 