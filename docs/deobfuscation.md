# Deobfuscation and verification

## Source artifact

- File: `micsaltmanager-rel (8).jar`
- SHA-256: `77BB5CEA319DBE12229558B74E7DA54F1581D5D77C3D2098B3AA437504142C63`
- Minecraft: 1.8.9
- Forge: 11.15.1.2318

The original binary is deliberately not part of the repository-ready project tree.

## Reconstruction process

1. Inventory the JAR and distinguish mod-owned classes from the bundled JNA file chooser.
2. Decompile independently with Vineflower and CFR.
3. Map Minecraft SRG members through MCP stable_22 where mappings exist.
4. Assign semantic names to all 67 classes in the original `f75` package.
5. Rename mod-owned fields, methods, enum constants, parameters, and recovered helpers.
6. Reconstruct lambda bodies and control flow lost by the member-renaming decompiler pass.
7. Compile under Java 8 and package through ForgeGradle `reobfJar`.
8. Compare class descriptors, constants, resources, and artifact structure with the source JAR.
9. Launch an isolated Minecraft client and verify completion of the Forge loading lifecycle.

The complete class map is recorded in [class-mapping.md](class-mapping.md).

## Verification results

- 78 class files in both original and reconstructed artifacts
- 63 of 67 mapped classes with identical normalized descriptor multisets
- Four expected descriptor differences caused by making compiler-generated switch/access helpers readable source-level classes
- Matching user-visible string constants, aside from renamed enum identifiers and normal Java constant folding
- No `f75` package entries in the reconstructed artifact
- No Vineflower placeholders or mod-owned `class_N`, `field_N`, or `method_N` identifiers in source
- Successful Forge construction, initialization, post-initialization, and load-complete events

## Remaining SRG names

Two Minecraft members have no names in MCP stable_22 and therefore remain in their SRG form:

- `GuiSlot.field_148163_i`
- `S00PacketDisconnect.func_149603_c`

These identifiers belong to Minecraft, not to the mod.

## Scope of runtime testing

The smoke test used an isolated `APPDATA` directory and no real account credentials. Authentication-dependent behavior was preserved and audited from bytecode but was not exercised against live personal accounts.
