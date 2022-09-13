package com.mmp.sdfs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    public static void copy(InputStream is, OutputStream os, int len) throws IOException {
        int total = len;
        byte[] arr = new byte[1024 * 256];
        while (total > 0) {
            int read = is.read(arr);
            if(read < 1)
                break;
            os.write(arr, 0, read);
            total -= read;
        }
    }

}
