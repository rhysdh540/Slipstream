# Slipstream

Slipstream is a client-side Minecraft performance mod that reduces the overhead of getting rendered frames onto your screen.

## Features

- Bypasses the normal frame presentation path, displaying frames directly through macOS's compositor
- Supports OpenGL and Minecraft's Vulkan renderer
- Can improve FPS when frame presentation is a bottleneck; gains depend on your hardware, renderer, and modpack

Slipstream changes how frames are displayed, not how worlds are rendered. Only works when vsync is off. macOS only (for now).

## License

[MIT](LICENSE)