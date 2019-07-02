import os
from os.path import join, basename
import shutil

d = "../original_apks/"
t = "./experiments/"

try:
    os.mkdir(t)
except:
    pass

files = [join(d, f) for f in os.listdir(d)]

for apk in filter(lambda x: x.endswith("apk"), files):
    json = apk.replace("-instrumented", "").replace(".apk", ".apk.json")
    apk_dir = join(t, basename(apk).replace("-instrumented.apk", "/"))
    try:
        os.mkdir(apk_dir)
    except:
        pass
    apk_dir = join(t, basename(apk).replace("-instrumented.apk", "/"), "apks/")
    try:
        os.mkdir(apk_dir)
    except:
        pass
    d_apk = join(apk_dir, basename(apk))
    d_json = join(apk_dir, basename(json))
    try:
        print("copying %s to %s" % (apk, d_apk))
        shutil.copy(apk, d_apk)
    except:
        pass
    try:
        print("copying %s to %s" % (json, d_json))
        shutil.copy(json, d_json)
    except:
        pass

