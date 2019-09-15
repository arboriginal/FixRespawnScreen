# FixRespawnScreen

FixRespawnScreen is a small plugin which fixes 2 bugs with Spigot servers (version 1.14) during the respawn screen:
* [Endless chunk loading and unloading](https://hub.spigotmc.org/jira/browse/SPIGOT-5227)
* Map becomes transparent letting caves, mobs, ... around visible to the dead player 

## How to install
There is no dependencies, simply drop the jar file into your plugin directory, then restart (or reload) your server. All configuration parameters are explained in this [config.yml](https://github.com/arboriginal/FixRespawnScreen/blob/master/src/config.yml).

## Permissions
All permissions are listed with a short description in this [plugin.yml](https://github.com/arboriginal/FixRespawnScreen/blob/master/src/plugin.yml).

## Commands
* **/frs-reload** will reload the configuration
