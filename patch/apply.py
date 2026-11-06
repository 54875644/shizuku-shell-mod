"""Apply the Shizuku "su 2000 / shell boot start" patches to an apktool work dir.

Usage:  python3 patch/apply.py <workdir>

Run from the repository root (after `apktool d input/Shizuku-orig.apk -o work`).
Every anchor is asserted to exist exactly once, so a silent apktool/version change
fails loudly instead of producing a broken APK.
"""
import os
import shutil
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(p):
    with open(p, 'r', encoding='utf-8') as f:
        return f.read()


def write(p, s):
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        f.write(s)


def patch(path, anchor, replacement):
    s = read(path)
    n = s.count(anchor)
    if n != 1:
        raise SystemExit('anchor count %d in %s:\n%r' % (n, path, anchor[:300]))
    write(path, s.replace(anchor, replacement, 1))
    print('patched', os.path.relpath(path, ROOT))


def main(work):
    global ROOT
    ROOT = os.path.abspath(work)
    if not os.path.isdir(ROOT):
        raise SystemExit('no such work dir: %r' % work)

    # ---------------------------------------------------------------- new class
    src = os.path.join(REPO, 'patch', 'ShellStart.smali')
    dst_dir = os.path.join(ROOT, 'smali', 'moe', 'shizuku', 'manager')
    if not os.path.isdir(dst_dir):
        raise SystemExit('missing smali/moe/shizuku/manager in %s' % ROOT)
    shutil.copyfile(src, os.path.join(dst_dir, 'ShellStart.smali'))
    print('added smali/moe/shizuku/manager/ShellStart.smali')

    # ------------------------------------------------------------ MainActivity: menu item
    main_activity = os.path.join(
        ROOT, 'smali', 'moe', 'shizuku', 'manager', 'MainActivity.smali')

    patch(
        main_activity,
        '.method public final onCreateOptionsMenu(Landroid/view/Menu;)Z\n'
        '    .locals 2\n',
        '.method public final onCreateOptionsMenu(Landroid/view/Menu;)Z\n'
        '    .locals 4\n')

    patch(
        main_activity,
        '    invoke-virtual {v0, v1, p1}, Landroid/view/MenuInflater;->inflate(ILandroid/view/Menu;)V\n',
        '    invoke-virtual {v0, v1, p1}, Landroid/view/MenuInflater;->inflate(ILandroid/view/Menu;)V\n'
        '\n'
        '    # >>> shellmod: "start with su 2000" overflow item\n'
        '    const/4 v0, 0x0\n'
        '\n'
        '    const v1, 0x7f0f00ff\n'
        '\n'
        '    const/4 v2, 0x0\n'
        '\n'
        '    const-string v3, "\u4ee5 su 2000 \u542f\u52a8\u670d\u52a1 (shell)"\n'
        '\n'
        '    invoke-interface {p1, v0, v1, v2, v3}, Landroid/view/Menu;->add(IIILjava/lang/CharSequence;)Landroid/view/MenuItem;\n'
        '\n'
        '    pop\n'
        '    # <<< shellmod\n')

    patch(
        main_activity,
        '    const/4 v2, 0x1\n'
        '\n'
        '    .line 7\n'
        '    sparse-switch v0, :sswitch_data_0\n',
        '    const/4 v2, 0x1\n'
        '\n'
        '    # >>> shellmod: dispatch our overflow item\n'
        '    const v3, 0x7f0f00ff\n'
        '\n'
        '    if-ne v0, v3, :shellmod_not_ours\n'
        '\n'
        '    invoke-static {}, Lmoe/shizuku/manager/ShellStart;->start()V\n'
        '\n'
        '    return v2\n'
        '\n'
        '    :shellmod_not_ours\n'
        '    # <<< shellmod\n'
        '\n'
        '    .line 7\n'
        '    sparse-switch v0, :sswitch_data_0\n')

    # -------------------------------------------------- BootCompleteReceiver: boot switch
    receiver = os.path.join(
        ROOT, 'smali', 'moe', 'shizuku', 'manager', 'receiver',
        'BootCompleteReceiver.smali')

    patch(
        receiver,
        '    :cond_1\n'
        '    sget-object p2, Lrikka/shizuku/xn;->a:Landroid/content/SharedPreferences;\n'
        '\n'
        '    .line 42\n'
        '    .line 43\n'
        '    const-string v2, "mode"\n',
        '    :cond_1\n'
        '    # >>> shellmod: start on boot with su 2000 when enabled\n'
        '    sget-object p2, Lrikka/shizuku/xn;->a:Landroid/content/SharedPreferences;\n'
        '\n'
        '    const-string v2, "start_on_boot_shell"\n'
        '\n'
        '    const/4 v3, 0x0\n'
        '\n'
        '    invoke-interface {p2, v2, v3}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z\n'
        '\n'
        '    move-result v2\n'
        '\n'
        '    if-eqz v2, :shellmod_noboot\n'
        '\n'
        '    invoke-static {}, Lmoe/shizuku/manager/ShellStart;->start()V\n'
        '\n'
        '    return-void\n'
        '\n'
        '    :shellmod_noboot\n'
        '    # <<< shellmod\n'
        '    sget-object p2, Lrikka/shizuku/xn;->a:Landroid/content/SharedPreferences;\n'
        '\n'
        '    .line 42\n'
        '    .line 43\n'
        '    const-string v2, "mode"\n')

    # ------------------------------------------------------------ settings: extra switch
    xml = os.path.join(ROOT, 'res', 'xml', 'APKTOOL_DUMMYVAL_0x7f140004.xml')
    if not os.path.isfile(xml):
        raise SystemExit('missing %s' % xml)
    s = read(xml)
    anchor = ('<rikka.material.preference.MaterialSwitchPreference '
              'android:title="@string/APKTOOL_DUMMYVAL_0x7f110116" '
              'android:key="start_on_boot" '
              'android:summary="@string/APKTOOL_DUMMYVAL_0x7f110117" />')
    if s.count(anchor) != 1:
        raise SystemExit('settings xml anchor not found')
    extra = ('\n'
             '    <rikka.material.preference.MaterialSwitchPreference '
             'android:key="start_on_boot_shell" '
             'android:title="\u5f00\u673a\u542f\u52a8 (shell 2000)" '
             'android:summary="\u5f00\u673a\u65f6\u4ee5 su 2000\uff08shell \u7528\u6237\uff09\u964d\u6743\u542f\u52a8 Shizuku \u670d\u52a1" '
             'android:defaultValue="false" />')
    write(xml, s.replace(anchor, anchor + extra, 1))
    print('patched res/xml/APKTOOL_DUMMYVAL_0x7f140004.xml')

    print('ALL PATCHES APPLIED')


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('usage: python3 patch/apply.py <workdir>')
    main(sys.argv[1])
