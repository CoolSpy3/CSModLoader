# CSModLoader
The goal of this project is to provide a universal mod loading system for Minecraft (specifically on servers). This allows mods to be loaded on clients which do not traditionally support mod loading (Lunar, Badlion, etc.).

## Concept
The functional principle behind this project is to open a TCP Server and set the Minecraft client to connect to it. The program can then initiate a connection to the target Minecraft server, complete the handshaking protocol, and then get full read/write access to the packet stream. These messages can then be interfaced to loaded mods. Obviously, without direct access to the client code, not all features can be used which could be utilized by a mod focused client such as Forge, but because I (initially) only plan to use this for simple chat based mods, it should be fine. That being said, if someone wants to make the loader more feature rich, feel free to open a PR.

## A quick note
Because this loader requires a direct interface with the packet stream, it's helpful to have a copy of the protocol open for reference. [Here's a link to the specification I've been using thus far](https://wiki.vg/index.php?title=Protocol&oldid=7368). Happy Coding!
