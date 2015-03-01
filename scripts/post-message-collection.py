from multiprocessing import Pool
import datetime
import tarfile
import urllib2
import sys

tarpath = sys.argv[1]
isspam = sys.argv[2] == "true"
ipaddr = sys.argv[3]

if len(sys.argv) > 4:
    count = int(sys.argv[4])
else:
    count = 10000

def doit(r):
    urllib2.urlopen(r)

tar = tarfile.open(tarpath)
files = [name for name in tar.getnames() if name.startswith("spam/0") or name.startswith("ham/0")]
count = min(count, len(files))

requests = []
for afile in files[:count]:
    data = tar.extractfile(afile).read()

    if isspam:
        urlsfx = "spam"
    else:
        urlsfx = "not-spam"

    r = urllib2.Request('http://%s/train/%s' % (ipaddr, urlsfx), data, {})
    requests.append(r)

print "Executing requests..."
start = datetime.datetime.now()
p = Pool(6)
p.map(doit, requests)
end = datetime.datetime.now()
print "Done"

n_req = len(requests)
total_secs = (end - start).total_seconds()

print "%s requests in %s seconds (%s requests/sec)" % (n_req, total_secs, n_req/total_secs)
