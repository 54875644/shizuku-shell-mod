package moe.shizuku.manager;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Starts the Shizuku server as the "shell" user (uid 2000) by spawning
 * "/system/bin/su" (no arguments, so root managers such as SuKiSU can apply their
 * "app == shell" template) and feeding the bundled libshizuku.so starter command to
 * its stdin.
 *
 * The starter is a native ELF executable, so it must be executed directly - never
 * through "sh".  Command form:  <nativeLibraryDir>/libshizuku.so --apk=<base.apk>
 *
 * Everything the starter prints is collected, written to
 *   /sdcard/Android/data/moe.shizuku.privileged.api/files/shizuku-shell-start.log
 * and copied to the clipboard, so failures can be diagnosed without a PC.
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
        toast(ctx, "正在通过 /system/bin/su 获取 shell 身份并启动 Shizuku\u2026");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                finish(ctx, doStart(ctx));
            }
        }, "shizuku-shell-start");
        t.setDaemon(true);
        t.start();
    }

    private static void finish(Context ctx, String text) {
        copyToClipboard(ctx, text);
        saveLog(ctx, text);
        int nl = text.indexOf('\n');
        String head = nl > 0 ? text.substring(0, nl) : text;
        toast(ctx, head);
    }

    private static String doStart(Context ctx) {
        StringBuilder log = new StringBuilder();
        String headline;
        try {
            String starter = ctx.getApplicationInfo().nativeLibraryDir + "/libshizuku.so";
            String apk = ctx.getApplicationInfo().sourceDir;
            String cmd = starter + " --apk=" + apk;

            log.append("time: ").append(new Date()).append('\n');
            log.append("cmd : ").append(cmd).append('\n');
            log.append("su  : /system/bin/su (no args, command fed on stdin)\n\n");

            String out = runSu(cmd, log);
            log.append("---- su output ----\n").append(out).append("\n-------------------\n");

            Thread.sleep(1500L);
            boolean up = pingBinder(log);
            if (up) {
                headline = "\u2705 Shizuku 已启动（shell 身份），日志见剪贴板";
            } else if (out.contains("can't kill")) {
                headline = "\u274c 有残留的旧 Shizuku 服务（root 身份）杀不掉，请重启手机后再试";
            } else if (out.contains("non root nor adb user")) {
                headline = "\u274c 拿到的不是 shell 身份（su 没套用模板），请检查 SuKiSU 模板";
            } else if (out.contains("binder")) {
                headline = "\u274c SELinux 拦住了 binder（su 域问题），日志已复制";
            } else {
                headline = "\u274c Shizuku 未能启动，日志已复制到剪贴板";
            }
        } catch (Throwable t) {
            log.append("fatal: ").append(t).append('\n');
            headline = "\u274c 启动异常: " + t.getClass().getSimpleName() + "，日志已复制";
        }
        return headline + "\n\n" + log;
    }

    /** Runs one command inside "/system/bin/su" and returns everything it printed. */
    private static String runSu(String command, StringBuilder log) {
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

            boolean done = proc.waitFor(SU_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!done) {
                log.append("su: timeout ").append(SU_TIMEOUT_MS).append("ms\n");
            }
            reader.join(1500L);
            out.append(buf.toString("UTF-8"));
        } catch (Throwable t) {
            out.append("runSu error: ").append(t).append('\n');
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

    private static boolean pingBinder(StringBuilder log) {
        try {
            Class<?> c = Class.forName("rikka.shizuku.Nn");
            java.lang.reflect.Method m = c.getDeclaredMethod("h");
            m.setAccessible(true);
            boolean ok = Boolean.TRUE.equals(m.invoke(null));
            log.append("pingBinder: ").append(ok).append('\n');
            return ok;
        } catch (Throwable t) {
            log.append("pingBinder error: ").append(t).append('\n');
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

    @SuppressLint("NewApi")
    private static void copyToClipboard(Context ctx, String text) {
        try {
            ClipboardManager cm =
                    (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("ShizukuShellStart", text));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void saveLog(Context ctx, String text) {
        try {
            File dir = ctx.getExternalFilesDir(null);
            if (dir != null) {
                File f = new File(dir, "shizuku-shell-start.log");
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(text.getBytes(UTF8));
                fo.close();
            }
        } catch (Throwable ignored) {
        }
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
