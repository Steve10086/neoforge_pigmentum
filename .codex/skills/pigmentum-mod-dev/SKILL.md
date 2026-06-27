---
name: pigmentum-mod-dev
description: Develop, integrate, or extend the Pigmentum/Painter NeoForge mod API with minimal decompilation. Use when working in any Minecraft NeoForge mod environment that depends on Pigmentum/Painter, when adding custom paint brushes, paint patterns, blend/effect logic, canvas image providers, canvas pixel renderers, canvas data access, item-frame canvas display behavior, networking hooks, mixin-sensitive behavior, or when needing a module-by-module map of this mod without reading the whole source tree.
---

# Pigmentum Mod Development

Use this skill before decompiling Pigmentum internals. Prefer public API classes, `API.md`, and the reference files here; inspect implementation classes only when the task directly touches rendering, networking, block replacement, stroke undo/redo, piston movement, or mixin behavior.

## Workflow

1. Identify whether the request is integration-only or internal modification.
2. For integration-only tasks, read `references/modules.md` and use public types under `com.astune.painter.api`.
3. For unfamiliar behavior, read `references/decompile-map.md` to choose the smallest source/decompile set.
4. Read `API.md` from this repository when examples or method details are needed.
5. Only inspect implementation packages after the public API and references cannot answer the task.
6. Validate code with `./gradlew compileTestJava` for compile-only changes or `./gradlew test` when tests exist or behavior changed.

## Dependency Coordinates

In a consuming NeoForge mod, Pigmentum is published through Modrinth Maven:

```groovy
repositories {
    maven { url "https://api.modrinth.com/maven" }
}

dependencies {
    implementation "maven.modrinth:pigmentum:${painter_version}+neoforge"
}
```

Use the version expected by the target workspace. This repository currently targets Minecraft `1.21.1`, NeoForge `21.1.228`, Java `21`, mod id `painter`, and base package `com.astune.painter`.

## Public API First

Use these packages before reading internal code:

- `com.astune.painter.api`: brush providers, canvas data, faces, pixels, blend modes, paint patterns.
- `com.astune.painter.api.blend`: custom blend functions and effect writes.
- `com.astune.painter.api.imageProvider`: texture/image generation from canvas faces.
- `com.astune.painter.api.render`: custom face renderer registration.

Avoid depending on these internal packages in downstream mods unless the task requires Pigmentum itself to change:

- `com.astune.painter.block`
- `com.astune.painter.client`
- `com.astune.painter.network`
- `com.astune.painter.mixin`
- `com.astune.painter.registry`

## Common Integrations

Register a custom brush with `PaintProviders.register(Item, IPaintProvider)`.

Implement `IPaintProvider.getColor(...)` for simple ARGB painting. Return `null` to erase a pixel. Implement `getPattern(...)` and return `PaintPattern(width, height, PixelProvider)` for area or image-like brushes.

Use `PixelProvider.getEffectValues(dx, dy)` together with a custom `BlendFunction` when painting extra per-pixel effect layers such as glow, roughness, or metallic.

Register `CanvasImageProviderRegistry.register(provider, priority)` to create one or more `NativeImage` textures from a `CanvasFace`.

Register `CanvasRendererRegistry.registerPixelRenderer(renderer, priority)` to replace or augment the in-world rendering of a canvas face.

Read `CanvasData` and `CanvasFace` for canvas storage and hit-face logic; use `CanvasData.getOrCreateCanvasFace(...)` only when deliberately creating/retrieving a face from block hit context.

## Reference Files

- Read `references/modules.md` when the user asks what each module does or how to use a module.
- Read `references/decompile-map.md` when working in a different mod development environment and deciding what must be decompiled or inspected.

## Guardrails

- Keep downstream integrations on public API whenever possible.
- Do not copy internal mixin or client classes into another mod as an integration strategy.
- Treat client-only renderer, texture, screen, and input classes as client-dist code.
- Treat packet handlers and canvas mutation as server-authoritative unless the existing API explicitly documents client-only behavior.
- After changing serialization, networking, data components, attachments, or `CanvasFace`/`CanvasData`, compile the project and review compatibility with saved data and network sync.
- When changing canvas block replacement or stroke undo/redo, preserve `CanvasBlockSetController` semantics: normal external `setBlock` calls proxy to `mimickedState`, while scoped internal replacements may restore the real block.
