package com.mohistmc.youer.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface Kernel32 extends Library {
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

    // Gets the current process handle
    Pointer GetCurrentProcess();

    // Set the process working set size
    boolean SetProcessWorkingSetSize(
            Pointer hProcess,
            int dwMinimumWorkingSetSize,
            int dwMaximumWorkingSetSize
    );
}