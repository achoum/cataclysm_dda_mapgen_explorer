# Prefab Explorer for Cataclysm DDA

**Prefab Explorer for Cataclysm DDA** in an explorer and a visualizer of *prefabs* (i.e. building templates for used by the world generator) for the [Cataclysm: Dark Days Ahead](https://github.com/CleverRaven/Cataclysm-DDA) game.

**Prefab Explorer** was made by reverse engineering the Cataclysm-DDA prefab format, and it is likely that not all (and there are lot) variations and caveats are not yet covered. Please submit missing features (with example) in the [Cataclysm-DDA forum](). If you're not able to, send an email to `achoum@gmail.com`.

![Imgur](https://i.imgur.com/rdXzFXX.png)

## Usage

Download and run [Prefab Explorer's jar](https://github.com/achoum/cataclysm_dda_prefab_explorer/releases). For most people, *double clicking* on the jar file will just work. In the case *double clicking* does not work, run the following command in a console:

~~~~
java -jar cataclysm_dda_prefab_explorer_0.1.jar
~~~~

Prefab Explorer will first ask you to select the Cataclysm DDA directory. After scanning for prefab and tilesets, you will be able to select prefabs to vizualize.

## Requirement
- [Java](https://java.com/en/download/)
- [Cataclysm: Dark Days Ahead](https://github.com/CleverRaven/Cataclysm-DDA) with tiles

## Supported Platforms
Anything that can run Java.

- Window (tested on Window 10).
- Linux (tested on Debian 9).
- MacOS.
