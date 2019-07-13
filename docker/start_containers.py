import os
from os.path import join, basename

d = "/home/nataniel.borges/droidgram/docker/experiments/"
dirs = [join(d, f) for f in os.listdir(d)]

for d in filter(lambda x: os.path.isdir(x), dirs):
    c = 'docker run --privileged -d -v %s:/test/experiment --name nataniel.%s droidgram bash -c "./runExperimentRQ2.sh"' % (d, basename(d))
    print(c)
    os.system(c)

