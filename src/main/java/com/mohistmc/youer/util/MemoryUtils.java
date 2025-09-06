package com.mohistmc.youer.util;

import com.sun.jna.Pointer;

public class MemoryUtils {

    public static String setProcessWorkingSetSize(int minSizeMB, int maxSizeMB) {
        // Gets the current process handle
        Pointer processHandle = Kernel32.INSTANCE.GetCurrentProcess();

        // Call the API to set the working set size
        boolean success = Kernel32.INSTANCE.SetProcessWorkingSetSize(
                processHandle,
                minSizeMB,
                maxSizeMB
        );

        return !success ? I18n.as("memoryutils.failed") : I18n.as("memoryutils.success");
    }
}
