package dev.rdh.slipstream.macos;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.FFIType;

import static dev.rdh.slipstream.macos.Macos.SEND;
import static dev.rdh.slipstream.macos.Macos.send;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.libffi.LibFFI.*;
import static org.lwjgl.system.macosx.ObjCRuntime.*;

final class MacosHud {
    private static final long BGRA8 = 80, CLEAR = 2, STORE = 1;

    private final long parent, device, queue, layer, attachment, pass;
    private boolean hidden = true;

    MacosHud(long parent) {
        this.parent = parent;
        device = invokeP(Macos.symbol(Macos.framework("Metal"), "MTLCreateSystemDefaultDevice"));
        if (device == 0) throw new IllegalStateException("No Metal device for the HUD overlay");
        queue = send(device, "newCommandQueue");
        long[] made = new long[1];
        Macos.onMain(() -> {
            long created = send(objc_getClass("CAMetalLayer"), "new");
            send(created, "setDevice:", device);
            send(created, "setPixelFormat:", BGRA8);
            Macos.set(created, "setFramebufferOnly:", true);
            Macos.set(created, "setDisplaySyncEnabled:", false);
            Macos.set(created, "setOpaque:", false);
            Macos.set(created, "setHidden:", true);
            invokePPV(created, sel_getUid("setAutoresizingMask:"), 18L, SEND);
            Macos.resize(created, parent);
            send(parent, "addSublayer:", created);
            made[0] = created;
        });
        layer = made[0];
        pass = send(send(objc_getClass("MTLRenderPassDescriptor"), "renderPassDescriptor"), "retain");
        attachment = send(send(pass, "colorAttachments"), "objectAtIndexedSubscript:", 0);
        send(attachment, "setLoadAction:", CLEAR);
        send(attachment, "setStoreAction:", STORE);
        clearToNothing();
    }

    private void copy(String key) {
        long name = Macos.string(key);
        invokePPPPV(layer, sel_getUid("setValue:forKey:"), send(parent, "valueForKey:", name), name, SEND);
    }

    private void drawableSize(int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long value = invokePPPPP(objc_getClass("NSValue"), sel_getUid("valueWithBytes:objCType:"),
                    memAddress(stack.doubles(width, height)), memAddress(stack.ASCII("{CGSize=dd}")), SEND);
            long name = Macos.string("drawableSize");
            invokePPPPV(layer, sel_getUid("setValue:forKey:"), value, name, SEND);
        }
    }

    private void clearToNothing() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FFIType colour = FFIType.calloc(stack).type(FFI_TYPE_STRUCT).elements(stack.pointers(
                    ffi_type_double.address(), ffi_type_double.address(),
                    ffi_type_double.address(), ffi_type_double.address(), 0L));
            FFICIF cif = FFICIF.calloc(stack);
            var types = stack.pointers(ffi_type_pointer.address(), ffi_type_pointer.address(), colour.address());
            if (ffi_prep_cif(cif, FFI_DEFAULT_ABI, ffi_type_void, types) != FFI_OK) {
                throw new IllegalStateException("Could not bind setClearColor:");
            }
            var arguments = stack.pointers(memAddress(stack.pointers(attachment)),
                    memAddress(stack.pointers(sel_getUid("setClearColor:"))),
                    memAddress(stack.doubles(0, 0, 0, 0)));
            ffi_call(cif, SEND, stack.calloc(8), arguments);
        }
    }

    void present() {
        Macos.pooled(() -> {
            long drawable = send(layer, "nextDrawable");
            if (drawable == 0) return;
            send(attachment, "setTexture:", send(drawable, "texture"));
            long buffer = send(queue, "commandBuffer");
            send(send(buffer, "renderCommandEncoderWithDescriptor:", pass), "endEncoding");
            send(buffer, "presentDrawable:", drawable);
            send(buffer, "commit");
        });
        if (hidden) setHidden(false);
    }

    void resize(int width, int height) {
        Macos.onMain(() -> {
            Macos.resize(layer, parent);
            copy("contentsScale"); // the HUD draws itself at the layer's scale
            drawableSize(width, height);
        });
    }

    void hide() {
        if (!hidden) setHidden(true);
    }

    private void setHidden(boolean hide) {
        Macos.onMain(() -> Macos.set(layer, "setHidden:", hide));
        hidden = hide;
    }

    void close() {
        Macos.onMain(() -> {
            send(layer, "removeFromSuperlayer");
            send(layer, "release");
        });
        send(pass, "release");
        send(queue, "release");
        send(device, "release");
    }
}
