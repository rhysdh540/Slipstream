package dev.rdh.slipstream.macos;

import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.libffi.FFICIF;

import java.lang.invoke.MethodHandles;

import static org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow;
import static org.lwjgl.sdl.SDLProperties.SDL_GetPointerProperty;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.APIUtil.apiCreateCIF;
import static org.lwjgl.system.libffi.LibFFI.*;
import static org.lwjgl.system.macosx.CoreFoundation.CFRelease;
import static org.lwjgl.system.macosx.ObjCRuntime.*;

public final class Macos {
    private static final SharedLibrary SURFACE = framework("IOSurface");
    private static final SharedLibrary OPENGL = framework("OpenGL");
    private static final SharedLibrary DISPATCH = Library.loadNative(Macos.class, "slipstream", "/usr/lib/system/libdispatch.dylib");
    private static final SharedLibrary SYSTEM = Library.loadNative(Macos.class, "slipstream", "/usr/lib/libSystem.B.dylib");
    private static final long SEND = getLibrary().getFunctionAddress("objc_msgSend");
    private static final long CREATE = symbol(SURFACE, "IOSurfaceCreate");
    private static final long IN_USE = symbol(SURFACE, "IOSurfaceIsInUse");
    private static final long TEX_IMAGE = symbol(OPENGL, "CGLTexImageIOSurface2D");
    private static final long CURRENT_CONTEXT = symbol(OPENGL, "CGLGetCurrentContext");
    private static final long MAIN_THREAD = symbol(SYSTEM, "pthread_main_np");
    private static final long MAIN_QUEUE = symbol(DISPATCH, "_dispatch_main_q");
    private static final long SYNC = symbol(DISPATCH, "dispatch_sync_f");

    private Macos() {}

    private static SharedLibrary framework(String name) {
        return Library.loadNative(Macos.class, "slipstream", "/System/Library/Frameworks/" + name + ".framework/" + name);
    }

    private static long symbol(SharedLibrary library, String name) {
        long address = library.getFunctionAddress(name);
        if (address == 0) throw new IllegalStateException("Missing macOS symbol: " + name);
        return address;
    }

    static long send(long object, String selector) {
        return invokePPP(object, sel_getUid(selector), SEND);
    }

    static long send(long object, String selector, long value) {
        return invokePPPP(object, sel_getUid(selector), value, SEND);
    }

    private static void set(long object, String selector, boolean value) {
        invokePPV(object, sel_getUid(selector), value, SEND);
    }

    private static long string(String text) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return send(objc_getClass("NSString"), "stringWithUTF8String:", memAddress(stack.UTF8(text)));
        }
    }

    private static void put(long dictionary, String key, int value) {
        long number = invokePPP(objc_getClass("NSNumber"), sel_getUid("numberWithInt:"), value, SEND);
        invokePPPPV(dictionary, sel_getUid("setObject:forKey:"), number, string(key), SEND);
    }

    static long surface(int width, int height) {
        long pool = send(objc_getClass("NSAutoreleasePool"), "new");
        try {
            long dictionary = send(objc_getClass("NSMutableDictionary"), "dictionary");
            put(dictionary, "IOSurfaceWidth", width);
            put(dictionary, "IOSurfaceHeight", height);
            put(dictionary, "IOSurfaceBytesPerElement", 4);
            put(dictionary, "IOSurfacePixelFormat", 0x42475241);
            long surface = invokePP(dictionary, CREATE);
            if (surface == 0) throw new IllegalStateException("IOSurfaceCreate failed");
            return surface;
        } finally {
            send(pool, "drain");
        }
    }

    static long currentContext() {
        return invokeP(CURRENT_CONTEXT);
    }

    static boolean inUse(long surface) {
        return invokePZ(surface, IN_USE);
    }

    static void release(long surface) {
        CFRelease(surface);
    }

    static int texture(long context, long surface, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FFICIF cif = FFICIF.calloc(stack);
            var types = stack.pointers(ffi_type_pointer.address(), ffi_type_uint32.address(), ffi_type_uint32.address(),
                    ffi_type_sint32.address(), ffi_type_sint32.address(), ffi_type_uint32.address(),
                    ffi_type_uint32.address(), ffi_type_pointer.address(), ffi_type_uint32.address());
            if (ffi_prep_cif(cif, FFI_DEFAULT_ABI, ffi_type_sint32, types) != FFI_OK) {
                throw new IllegalStateException("Could not bind CGLTexImageIOSurface2D");
            }
            var arguments = stack.pointers(memAddress(stack.pointers(context)), memAddress(stack.ints(0x84F5)),
                    memAddress(stack.ints(0x8058)), memAddress(stack.ints(width)), memAddress(stack.ints(height)),
                    memAddress(stack.ints(0x80E1)), memAddress(stack.ints(0x8367)),
                    memAddress(stack.pointers(surface)), memAddress(stack.ints(0)));
            var result = stack.calloc(8);
            ffi_call(cif, TEX_IMAGE, result, arguments);
            return result.getInt(0);
        }
    }

    public static long cocoaWindow(long window, boolean sdl) {
		if(sdl) {
            return SDL_GetPointerProperty(SDL_GetWindowProperties(window), SDL_PROP_WINDOW_COCOA_WINDOW_POINTER, 0);
        } else {
            return glfwGetCocoaWindow(window);
        }
	}

    static long attach(long window) {
        long[] result = new long[1];
        onMain(() -> {
            long view = send(window, "contentView");
            set(view, "setWantsLayer:", true);
            long parent = send(view, "layer");
            if (parent == 0) throw new IllegalStateException("Window has no backing layer");
            long layer = send(objc_getClass("CALayer"), "new");
            set(layer, "setOpaque:", true);
            set(layer, "setHidden:", true);
            invokePPV(layer, sel_getUid("setAutoresizingMask:"), 18L, SEND);
            resize(layer, parent);
            send(parent, "addSublayer:", layer);
            result[0] = layer;
        });
        return result[0];
    }

    private static void resize(long layer, long parent) {
        long bounds = send(parent, "valueForKey:", string("bounds"));
        invokePPPPV(layer, sel_getUid("setValue:forKey:"), bounds, string("frame"), SEND);
    }

    static void commit(long layer, long surface) {
        pooled(() -> {
            long transaction = objc_getClass("CATransaction");
            send(transaction, "begin");
            try {
                set(transaction, "setDisableActions:", true);
                send(layer, "setContents:", surface);
                set(layer, "setHidden:", surface == 0);
            } finally {
                send(transaction, "commit");
            }
        });
    }

    static void detach(long layer) {
        onMain(() -> {
            send(layer, "setContents:", 0);
            send(layer, "removeFromSuperlayer");
            send(layer, "release");
        });
    }

    private static void onMain(Runnable action) {
        if (invokeI(MAIN_THREAD) != 0) {
            pooled(action);
            return;
        }
        Throwable[] failure = new Throwable[1];
        DispatchFunction callback = ignored -> {
            try {
                pooled(action);
            } catch (Throwable t) {
                failure[0] = t;
            }
        };
        long address = callback.address();
        try {
            invokePPPV(MAIN_QUEUE, 0, address, SYNC);
        } finally {
            Callback.free(address);
        }
        if (failure[0] != null) throw new IllegalStateException("Main-thread presentation failed", failure[0]);
    }

    @FunctionalInterface
    private interface DispatchFunction extends CallbackI {
        Callback.Descriptor DESCRIPTOR = descriptor();

        private static Callback.Descriptor descriptor() {
            FFICIF cif = apiCreateCIF(ffi_type_void, ffi_type_pointer);
            var constructor = Callback.Descriptor.class.getConstructors()[0];
            try {
                // LWJGL 3.4.3 added the callback interface to the descriptor constructor.
                return (Callback.Descriptor) constructor.newInstance(constructor.getParameterCount() == 2
                        ? new Object[]{MethodHandles.lookup(), cif}
                        : new Object[]{DispatchFunction.class, MethodHandles.lookup(), cif});
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not bind the main-thread callback", failure);
            }
        }

        @Override
        default Callback.Descriptor getDescriptor() {
            return DESCRIPTOR;
        }

        @Override
        default void callback(long result, long arguments) {
            invoke(memGetAddress(memGetAddress(arguments)));
        }

        void invoke(long context);
    }

    private static void pooled(Runnable action) {
        long pool = send(objc_getClass("NSAutoreleasePool"), "new");
        try {
            action.run();
        } finally {
            send(pool, "drain");
        }
    }
}
