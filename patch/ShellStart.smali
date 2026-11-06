.class public final Lmoe/shizuku/manager/ShellStart;
.super Ljava/lang/Object;
.source "SourceFile"


# direct methods
.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static start()V
    .locals 6

    :try_start_p
    invoke-static {}, Lrikka/shizuku/xh;->p()Lrikka/shizuku/Gn;

    move-result-object v0
    :try_end_p
    .catch Lrikka/shizuku/Zi; {:try_start_p .. :try_end_p} :catch_noroot

    iget v1, v0, Lrikka/shizuku/Gn;->a:I

    if-nez v1, :cond_shell

    # ---------------------------------------------------------------- root shell
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "su 2000 "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v2, Lrikka/shizuku/Qo;->b:Ljava/lang/String;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    const-string v2, "已获取 root，正在以 su 2000 降权启动 Shizuku 服务..."

    invoke-static {v2}, Lmoe/shizuku/manager/ShellStart;->toast(Ljava/lang/CharSequence;)V

    goto :build_job

    # ------------------------------------------- already demoted shell (adb-like)
    :cond_shell
    sget-object v1, Lrikka/shizuku/Qo;->b:Ljava/lang/String;

    const-string v2, "当前 shell 已降权，直接以 shell 身份启动 Shizuku 服务..."

    invoke-static {v2}, Lmoe/shizuku/manager/ShellStart;->toast(Ljava/lang/CharSequence;)V

    :build_job
    filled-new-array {v1}, [Ljava/lang/String;

    move-result-object v2

    new-instance v1, Lrikka/shizuku/Sj;

    invoke-direct {v1}, Lrikka/shizuku/Sj;-><init>()V

    new-instance v3, Lrikka/shizuku/H6;

    invoke-direct {v3, v2}, Lrikka/shizuku/H6;-><init>([Ljava/lang/String;)V

    iget-object v2, v1, Lrikka/shizuku/Sj;->d:Ljava/util/ArrayList;

    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v2, Lrikka/shizuku/Rj;

    const/4 v4, 0x0

    invoke-direct {v2, v1, v4}, Lrikka/shizuku/Rj;-><init>(Lrikka/shizuku/Sj;I)V

    iput-object v2, v1, Lrikka/shizuku/Sj;->i:Ljava/lang/Runnable;

    new-instance v2, Lrikka/shizuku/Re;

    invoke-direct {v2}, Ljava/lang/Object;-><init>()V

    iput-object v2, v1, Lrikka/shizuku/Sj;->h:Lrikka/shizuku/yn;

    const/4 v2, 0x0

    iput-object v2, v1, Lrikka/shizuku/Sj;->g:Lrikka/shizuku/Cr;

    :try_start_b
    invoke-virtual {v0, v1}, Lrikka/shizuku/Gn;->b(Lrikka/shizuku/Sj;)V
    :try_end_b
    .catch Ljava/io/IOException; {:try_start_b .. :try_end_b} :catch_io

    return-void

    :catch_io
    const-string v1, "启动失败：IOException"

    invoke-static {v1}, Lmoe/shizuku/manager/ShellStart;->toast(Ljava/lang/CharSequence;)V

    return-void

    :catch_noroot
    const-string v0, "无法启动 shell，请先在 SuKiSU/Magisk 中给 Shizuku 授权"

    invoke-static {v0}, Lmoe/shizuku/manager/ShellStart;->toast(Ljava/lang/CharSequence;)V

    return-void
.end method

.method private static toast(Ljava/lang/CharSequence;)V
    .locals 2

    sget-object v0, Lrikka/shizuku/vn;->a:Lmoe/shizuku/manager/ShizukuApplication;

    const/4 v1, 0x0

    invoke-static {v0, p0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    move-result-object v0

    invoke-virtual {v0}, Landroid/widget/Toast;->show()V

    return-void
.end method
