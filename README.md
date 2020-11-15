# DecentChat
<p align="center">
<img align="center" width="120" height="120" style="-right: 40px" src="https://decentchat.ishaanraja.com/assets/icon.png">
</p>
<p align="center">
The official reference implementation for the DecentChat network.
</p>

![Screenshot](https://decentchat.ishaanraja.com/assets/screenshot.png)

## Table of Contents
- [What is DecentChat?](#what-is-decentchat)
- [Features](#features)
- [Whitepaper](#whitepaper)
- [License](#license)
- [Downloads](#downloads)
- [Getting Started](#getting-started)
- [Sending Messages](#sending-messages)
- [Seed Nodes](#seed-nodes)
- [Configuration](#configuration)
- [Identification](#identification)
- [Commands](#commands)
- [Ignoring](#ignoring)
- [Building](#building)
- [Issues](#issues)
- [Contributing](#contributing)

## What is DecentChat?
DecentChat is global, peer-to-peer, decentralized chatroom. It does not rely on a moderator or central server and can continue to exist perpetually as long as there are nodes online. 

Using RSA, users are able to create consistent forms of identification on the network. All users are identified via their public key and a human readable username. 

Peers forward messages to other peers, allowing messages to propagate across the network in an anonymous fashion. 

The network relies on a proof of work system to deter spam attacks. The difficulty of this proof of work is constantly adjusted to ensure that it is one step ahead of any prospective message spammers. 

## Features
Some notable features include:
-  Semi-anonymous private/public key identity system
-  Dynamically adjusted SHA-256 proof of work system to deter spam attacks
-  Automated peer finding
-  Lightweight Swing GUI (and an optional headless mode)
-  UPnP support (configured in  `config.json`)
-  No IP/identifier link, there is no definitive way to find out the IP Address of anyone on the network

## Whitepaper
The whitepaper can be found [here](https://decentchat.ishaanraja.com/decentchat.pdf).

## License
This project is licensed under the terms of the [MIT license](https://github.com/IshaanRaja/DecentChat/blob/master/LICENSE).

## Downloads
Stable releases are available for download in [Releases](https://github.com/IshaanRaja/DecentChat/releases). 

## Getting Started
Before running DecentChat, ensure Java 8 or above is installed.

Then, download the [latest stable release](https://github.com/IshaanRaja/DecentChat/releases) and run it. 

If running DecentChat on a headless machine, make sure that `headlessMode` is set to `true`in `config.json`.

When started, DecentChat will look for other peers, ask for their peers, and try to establish connections to up to the configured maximum amount of peers (see `config.json`). 

All peers are stored in `peers.txt`

## Sending Messages
Messages can be between 1 and 256 characters. To send a message, simply type in the box at the bottom of the client, and press the "Send" button or the Enter key to send it to the network. 

Keep in mind that due to the proof-of-work, messages can take between 1 and 10 seconds to send, sometimes longer on slower computers. 

## Seed Nodes
When a client connects to the network for the first time ever, it automatically resolves peers from the project's domain via DNS. The network needs seed peers run by different people to reduce the threat of centralization. 

Running a seed node is great away of contributing back to the network.

If you would like to run a seed peer, please fill out [this form](https://docs.google.com/forms/d/e/1FAIpQLSdnGIncQQ2shWVVY9ZmFhafWhcIC2_W5RCazKGxpHm4co4Q5g/viewform?usp=sf_link).

## Configuration
Configuration is primarily done through the GUI and the `config.json` file. This file is generated after DecentChat is first run.

The default `config.json` looks like this:
```
{
	"maximumConnections": 256,
	"username": "Newbie",
	"upnpEnabled": true,
	"headlessMode": false
}
```
- `maximumConnections` is the maximum amount of peers the client can have. 
- `username` is the human readable username that is sent along with every chat message.
- `upnpEnabled` is an optional setting that determines if the client should uses Universal-Plug-and-Play (UPNP). If UPNP is not available on the network, this option does nothing.
- `headlessMode` determines whether the client should use a GUI or a command line interface.

## Identification
There are three parts to a chat message, the username, the 10 character key identifier, and the message itself. An example message looks like this:

```
bob@a044a6ae59: Hello,world!
```

-   In the above example,  `bob`  is the username. Usernames can be changed in  `config.json`, or through the /changeusername command.
-   `a044a6ae59`  is the 10 character key identifier. This is how identity verification can be done, anyone can change their name to anything, but key identifier relies on public/private key encryption so it cannot be forged. To change key identifiers, delete the  `public.pem`  and  `private.pem`  files and DecentChat will generate new ones.
-   `Hello,world!`  is the message that the user sent.

## Commands
In order for every user to easily be able to use DecentChat’s various features, the client provides a commands system.

To use a command, type a “/” at the beginning of your message, to indicate that you want to use a command rather than broadcast a message to the rest of the network.

Available commands are as follows:
-   “/help” - Provides a list of available commands
-   “/addpeer [address]” - Allows a user to manually add a peer by specifying its IP Address   
-   “/peerinfo” - Shows a list of connected peers  
-   “/difficultyinfo” - Shows the client’s current difficulty    
-   “/ignore [identifier]” - Ignores a specified identifier. See above for how to get another user's identifier.
-   “/ignorelist” - Shows the list of currently ignored identifiers
-   “/unignore [identifier]” - Removes a specified identifier from the ignore list    
-   “/changeusername [new_username]” - Changes the client’s human readable username for all following messages (see section 4).   

Command arguments are specified in brackets (“[]”).
More commands may be added in the future to accommodate additional features or functionality

## Ignoring
Inevitably, there will be users that you do not want to see messages from. To assist with this, DecentChat provides an ignoring functionality. To ignore a user, use the /ignore command with the user you wish to ignore's 10 character key identifier. This will stop the client from displaying messages from that user.

To stop ignoring a user, use the /unignore command.

All ignored identifiers are stored in `ignorelist.txt`

## Building
To build DecentChat from source, use the following commands:
```
git clone https://github.com/IshaanRaja/DecentChat
cd DecentChat
./gradlew fatJar
```

This will automatically download all dependencies, compile, and build a runnable .jar file. 

DecentChat uses the following dependencies:
- [Gson](https://github.com/google/gson/)
- [WaifUPnP](https://github.com/adolfintel/WaifUPnP)

## Issues
Please attach your `debug.log` file and explain the steps that led up to the issue when reporting a bug/glitch on GitHub.

## Contributing
Pull Requests are always welcomed! Please ensure that any code contributions have proper Javadoc comments included.
