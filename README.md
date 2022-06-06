# CSModLoader
The goal of this project is to provide a universal mod loading system for Minecraft (specifically on servers). This allows mods to be loaded on clients which do not traditionally support mod loading (Lunar, Badlion, etc.).

## Concept
The functional principle behind this project is to open a TCP Server and set the Minecraft client to connect to it. The program can then initiate a connection to the target Minecraft server, complete the handshaking protocol, and then get full read/write access to the packet stream. These messages can then be interfaced to loaded mods. Obviously, without direct access to the client code, not all features can be used which could be utilized by a mod focused client such as Forge, but because I (initially) only plan to use this for simple chat based mods, it should be fine. That being said, if someone wants to make the loader more feature rich, feel free to open a PR.

## Restrictions
Unfortunately, while the design of the mod loader allows it to work regardless of the client used, this comes with a few trade offs:
1. The loader works by intercepting the packet stream between a client and server, so it only works on servers (if you want to play singleplayer, you can run a local Minecraft server. Keep in mind that by default the loader uses port `25565` on the local machine, so you would have to set the server to an alternate port.)
2. The loader is not a client, so any mods designed for other clients Fabric/Forge etc. will not work immediately with this loader. The full reason for this is two part: 1) Because the loader acts as a server to the client, it can only perform actions which can be done by a regular server. Thus, many GUI overlays and non-vanilla features are impossible without great forethought as to how to implement them using vanilla features 2) The APIs exposed by existing loaders would take me a long time to replicate well and is unjustified for my use case (simple chat-based utility mods). If there are any mods you would like to use, my strategy has been to use a site such as [jdec.app](https://jdec.app) to decompile the mod file and then to manually rewrite the relevant code to use this api. (I've done this with [CSAutoGG](https://github.com/CoolSpy3/CSAutoGG) which is a port of [Sk1er's AutoGG Mod](https://sk1er.club/mods/autogg))
3. For the moment, I have set the mod loader up to use 1.8. It *looks* like newer versions of Minecraft don't make any changes to the part of the specification used by this loader, but I have not tested this. (Note: just because the loader works on multiple versions does not mean that individual mods do)

## Installation
Each release of the mod loader ships with two files `CSModLoader.jar` and `config.zip`. In order to authenticate with Mojang's servers it is launched by the [vanilla Minecraft launcher](https://www.minecraft.net/en-us/download).

1. Extract `config.zip`. It should contain a single folder named `config` (we will use this later)

2. Go to your [.minecraft](https://minecraft.fandom.com/wiki/.minecraft#Locating_.minecraft) directory

3. Copy `cslogging-config.xml` from the `config` folder to your `.minecraft` directory

4. Go into the `versions` directory

5. Create a new directory called `CSModLoader`

6. Copy the `CSModLoader.json` file from `config` and the `CSModLoader.jar` file into the `CSModLoader` directory

7. Launch the Minecraft launcher

8. Go to Installations > New installation

9. Set the version to `release CSModLoader`

10. Create the installation, select it, and press play

## Usage
To use the loader, simply go to your [.minecraft](https://minecraft.fandom.com/wiki/.minecraft#Locating_.minecraft) directory and place any mods you wish to load in a directory called `csmods`. Then restart the loader to reload all mods.

To add a server press the `Add Server` button in the mod loader. Then press the `Edit` button next to the new server. This will open a window where you can set an optional name and the server's ip. IP addresses are inputted the same way as the Minecraft client: `<ip>:<port>` (basically just enter whatever you would into a regular Minecraft client).

To connect to a server, press the `Connect` button in the loader and then connect to `localhost` in your Minecraft client.

### Auto-Starting servers
A server can be marked as `Auto Start`. In this case, it will automatically be available when the loader is launched. The `Restart Servers` button terminates all running servers and restarts just those marked as `Auto Start`.

### Changing the local server port
The port on which the local server is hosted can be changed in the server settings. By default, this is `25565`, the default Minecraft server port. If it is changed, you will have to specify the new port in the Minecraft client (`localhost:<port>`). If two servers are run on different local ports, they may be run simultaneously. Additionally, multiple servers may be set as `Auto Start` provided that they are configured for different ports. In the case where many servers are running and need to be stopped, the `Stop All Servers` button may be used to terminate all running servers.

### Changing your .minecraft directory
If for whatever reason you want to change the minecraft directory the loader uses, just change the directory in the launcher (as you would for a normal profile) and move or copy the `cslogging-config.xml` file and `csmods` folder into the new directory.

### Log files
The loader creates log files in the `<game directory>/cslogs` folder. The logging level can be changed to one of any: `error`, `warning`, `info`, `debug`, or `trace`. `info` is selected by default. To change the level, open the `cslogging-config.xml` file and change line 12 (`<root level="info">`) to reflect your desired level.

## Developing Mods
To develop mods for this API, create a Gradle or Maven project and then add `com.github.coolspy3:CSModLoader:<version>` as a compile-only dependency. Keep in mind that the loader is distributed through JitPack, so you will have to add `maven { url 'https://jitpack.io' }` to your repositories.

In order to be more version independent, the bulk of the API is not contained in the loader, but individual mods can define packet types. I have already created the [CSPackets](https://github.com/CoolSpy3/CSPackets) mod which registers most of the packets used in 1.8. It can be added to a project in the same way as the loader through the `com.coolspy3:CSPackets:<version>` package. You can also look at my other repos for example mods. I recommend [CSShortCommands](https://github.com/CoolSpy3/CSShortCommands) (ported from [ShortCommands](https://github.com/CoolSpy3/ShortCommands)) because it is a fairly simple use of how the API works. I've also create the repo [CSModTemplate](https://github.com/CoolSpy3/CSModTemplate) which features a basic template for a compatible mod.

## A quick note
Because this loader requires a direct interface with the packet stream, it's helpful to have a copy of the protocol open for reference. [Here's a link to the specification I've been using thus far](https://wiki.vg/index.php?title=Protocol&oldid=7368). Happy Coding!
