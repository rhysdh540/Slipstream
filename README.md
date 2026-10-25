# Slipstream

Slipstream is a client-side Minecraft performance mod that reduces the overhead of getting rendered frames onto your screen.

## Features

- Bypasses the normal frame presentation path, displaying frames directly through macOS's compositor
- Supports OpenGL and Minecraft's Vulkan renderer
- Can improve FPS when frame presentation is a bottleneck; gains depend on your hardware, renderer, and modpack
- Allows the Metal Performance HUD to apply to Minecraft (if the environment variable `MTL_HUD_ENABLED` is set)

## License

[MIT](LICENSE)