# 26.2 compatibility regressions

## Custom payloads through Velocity

Upstream commit `99e08069d` restored the identifier encoding described below. The
encoding regression check protects that behavior; this change additionally addresses
channel advertisement after a backend switch.

Keep the namespace when encoding identifiers with both `FriendlyByteBuf.writeIdentifier`
and `Identifier.STREAM_CODEC`. Sending `minecraft:register` as `register` makes Velocity's
legacy-channel normalization forward it as `legacy:register`. NeoForge consequently
does not register the advertised channels. A later MTR edit or drive packet then fails
the client's channel validation with `Payload ... may not be sent to the server!`.

This repair restores vanilla identifier encoding; it does not disable channel checks,
alter proxy forwarding settings, or change MTR packets. Explicit mod namespaces remain
unchanged. Vanilla decoding continues to accept default-namespace identifiers.

### Switching backends on an existing client connection

Correct identifier encoding alone is insufficient when a proxy switches a NeoForge
26.2.0.88 client between backends. `ClientNetworkRegistry` initializes its negotiated
channel table once per socket. A lobby without MTR channels can therefore leave the
client with an empty/stale table even after the next backend negotiates MTR channels.
Previously, a NeoForge backend advertised only built-in channels at the end of
configuration, assuming the client had applied the new negotiated table. The real
client send check then rejected `mtr:packet_update_depot` and `mtr:packet_drive_train`.

Youer now advertises its optional play channels to NeoForge connections as well as
other connections. It uses the existing `minecraft:register` protocol and the existing
optional/protocol/direction filters. Required channels still require negotiation;
unknown payloads remain forbidden, and `minecraft:unregister` still revokes ad-hoc
permission. This is an optional-payload compatibility repair, not general support for
switching arbitrary incompatible modpacks or bypassing channel/version validation.

Update old Youer lobby backends too: they can still emit shortened identifiers. Test
the full lobby-to-game route as well as a fresh game-server connection; reconnecting
through the same old lobby does not isolate the game backend.

## FAWE and dynamically added materials

Commodore rewrites modern Bukkit plugins' `Material.values()` and `Material.ordinal()`
calls to `CraftLegacy.modern_values()` and `CraftLegacy.modern_ordinal()`. Previously,
the list stopped at `LEGACY_AIR`, but mod materials retained ordinals after the entire
legacy range. FAWE's material-indexed arrays therefore excluded mod items and failed
with an out-of-bounds access when a player held one.

The modern list now includes every non-legacy material, and its ordinals skip the
fixed legacy range. Vanilla indices remain unchanged, lookup stays constant-time,
and each enumeration returns a fresh array. No FAWE JAR modification is required.

## Verification

Run with the project's JDK 25 toolchain. After changing Minecraft patches, regenerate
the common Minecraft sources before compiling (this overwrites generated sources;
preserve any direct edits to those sources first):

```text
./gradlew :neoforge:setupCommon
./gradlew :neoforge:checkIdentifierEncoding :neoforge:checkModernMaterials :neoforge:checkProxyChannels
./gradlew :neoforge:youerJar
```

`checkIdentifierEncoding` exercises both real encoders and round trips. To additionally
invoke an installed Velocity JAR's real channel conversion without starting the proxy,
pass `-PvelocityJar=/absolute/path/to/velocity.jar`. This also checks the old shortened
`register` value as a negative control.

`checkProxyChannels` replays initial configuration and a second backend on the same
Netty connection through the real client registry. It reproduces the depot-send
exception before the advertisement repair, then tests fresh NeoForge/other connections,
the proxy switch, editing/driving payload validation, direction/protocol filtering,
required/unknown payload rejection, and channel withdrawal. No live server or proxy
is started. Its fixture deliberately retains NeoForge 26.2.0.88's once-per-socket
behavior so the server-side fallback cannot accidentally be tested against a repaired
client instead. The real proxy channel-name conversion is covered separately above.

`checkModernMaterials` injects real mod item/block enum constants, verifies the complete
non-legacy enumeration/index correspondence, preserves legacy rejection and vanilla
indices, and runs a material-indexed plugin fixture through the actual Commodore
bytecode transformer. These are headless compatibility checks, not a full FAWE edit
session or an in-game multiplayer test.

For runtime acceptance, restart a separate test backend with the rebuilt server and
reconnect the client through Velocity. Check opening/saving depot and platform editors,
boarding/driving trains, and FAWE interaction while holding both vanilla and mod items.
Confirm that `legacy:register`/`legacy:unregister` decode failures and material-cache
out-of-bounds errors no longer appear. Also verify ordinary FAWE edits and undo in a
disposable world. Do not deploy to a running server or test destructive edits on a live
world. Passenger-position desynchronization and vanilla chunk-update reports require
their own runtime evidence; these checks do not establish that those issues are fixed.
