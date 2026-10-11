package dev.rdh.slipstream.macos;

import java.util.function.IntPredicate;

final class MacosSurfacePool {
    static final int SIZE = 3;
    final long[] surfaces = new long[SIZE];
    private final long layer;
    private int shown = -1;

    MacosSurfacePool(long window) {
        layer = Macos.attach(window);
    }

    void allocate(int width, int height) {
        for (int i = 0; i < SIZE; i++) surfaces[i] = Macos.surface(width, height);
    }

    int free(IntPredicate ready) {
        for (int offset = 1; offset <= SIZE; offset++) {
            int i = (shown + offset) % SIZE;
            if (i != shown && ready.test(i) && !Macos.inUse(surfaces[i])) return i;
        }
        return -1;
    }

    void show(int index) {
        Macos.commit(layer, surfaces[index]);
        shown = index;
    }

    void hide() {
        Macos.commit(layer, 0);
        shown = -1;
    }

    void release() {
        for (int i = 0; i < SIZE; i++) {
            if (surfaces[i] != 0) Macos.release(surfaces[i]);
            surfaces[i] = 0;
        }
        shown = -1;
    }

    void close() {
        Macos.detach(layer);
        release();
    }
}
