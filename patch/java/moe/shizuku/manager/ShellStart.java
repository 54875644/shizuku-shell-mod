package moe.shizuku.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Starts the Shizuku server as the "shell" user (uid 2000).
 *
 * The starter is a native ELF executable (libshizuku.so in nativeLibraryDir), so it
 * is executed directly - never through "sh".  Root managers such as SuKiSU can be
 * configured so that spawning "/system/bin/su" hands out uid 2000 instead of uid 0;
 * that is the shell identity used here.
 *
 * Release build: no log files, no clipboard, only short toast feedback.
 */
public final class ShellStart {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final long SU_TIMEOUT_MS = 25000L;

    private ShellStart() {
    }

    public static void start() {
        final Context ctx = appContext();
        if (ctx == null) {
            return;
        }
        toast(ctx, "\u6b63\u5728\u4ee5 shell \u8eab\u4efd\u542f\u52a8 Shizuku\u2026");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                doStart(ctx);
            }
        }, "shizuku-shell-start");
        t.setDaemon(true);
        t.start();
    }

    private static void doStart(Context ctx) {
        String result;
        try {
            String cmd = ctx.getApplicationInfo().nativeLibraryDir + "/libshizuku.so"
                    + " --apk=" + ctx.getApplicationInfo().sourceDir;
            String out = runSu(cmd);
            try {
                Thread.sleep(1600L);
            } catch (InterruptedException ignored) {
            }
            if (pingBinder()) {
                result = "\u2705 Shizuku \u5df2\u542f\u52a8\uff08shell \u8eab\u4efd\uff09";
            } else if (out.contains("can't kill")) {
                result = "\u274c \u542f\u52a8\u5931\u8d25\uff1a\u6709\u6b8b\u7559\u7684 Shizuku \u670d\u52a1\u8fdb\u7a0b\uff0c\u8bf7\u91cd\u542f\u624b\u673a\u540e\u91cd\u8bd5";
            } else if (out.contains("non root nor adb user")) {
                result = "\u274c \u542f\u52a8\u5931\u8d25\uff1a\u6ca1\u6709\u62ff\u5230 shell \u8eab\u4efd\uff0c\u8bf7\u68c0\u67e5 SuKiSU/Magisk \u6388\u6743";
            } else if (out.contains("binder")) {
                result = "\u274c \u542f\u52a8\u5931\u8d25\uff1aSELinux \u963b\u6b62\u4e86 binder \u8fde\u63a5";
            } else {
                result = "\u274c \u542f\u52a8\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5 Shizuku \u8fd0\u884c\u72b6\u6001";
            }
        } catch (Throwable t) {
            result = "\u274c \u542f\u52a8\u5931\u8d25\uff1a" + t.getClass().getSimpleName();
        }
        toast(ctx, result);
    }

    /** Runs one command inside "/system/bin/su" and returns what it printed. */
    private static String runSu(String command) {
        StringBuilder out = new StringBuilder();
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/su");
            pb.redirectErrorStream(true);
            p = pb.start();
            final Process proc = p;

            OutputStream os = proc.getOutputStream();
            os.write((command + "\nexit\n").getBytes(UTF8));
            os.flush();
            os.close();

            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream in = proc.getInputStream();
                        byte[] b = new byte[4096];
                        int n;
                        while ((n = in.read(b)) > 0) {
                            buf.write(b, 0, n);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }, "su-reader");
            reader.setDaemon(true);
            reader.start();

            proc.waitFor(SU_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            reader.join(1500L);
            out.append(buf.toString("UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
        return out.toString();
    }

    private static boolean pingBinder() {
        try {
            Class<?> c = Class.forName("rikka.shizuku.Nn");
            java.lang.reflect.Method m = c.getDeclaredMethod("h");
            m.setAccessible(true);
            return Boolean.TRUE.equals(m.invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Context appContext() {
        try {
            Class<?> c = Class.forName("rikka.shizuku.vn");
            java.lang.reflect.Field f = c.getDeclaredField("a");
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof Context) {
                return (Context) v;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void toast(final Context ctx, final String msg) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Throwable ignored) {
        }
    }
}
