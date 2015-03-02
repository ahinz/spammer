import subprocess
import json
import os
import sys
import tarfile
import urllib2

def p(n,d):
    return int(n * 1000 / d)/10

ipaddr = sys.argv[1]
n = int(sys.argv[2])

scripts = os.path.dirname(os.path.realpath(__file__))
data = os.path.join(scripts, '..', 'data')

spam = os.path.join(data, "spam.tar.gz")
ham = os.path.join(data, "ham.tar.gz")

subprocess.check_call("curl -X POST http://%s/flush" % ipaddr, shell=True)
subprocess.check_call("python %s/post-message-collection.py %s true %s %s" % (scripts, spam, ipaddr, n), shell=True)
subprocess.check_call("python %s/post-message-collection.py %s false %s %s" % (scripts, ham, ipaddr, n), shell=True)

hamtar = tarfile.open(ham)
hamfiles = [(False, hamtar.extractfile(name).read()) for name in hamtar.getnames() if name.startswith("spam/0") or name.startswith("ham/0")]

spamtar = tarfile.open(spam)
spamfiles = [(True, spamtar.extractfile(name).read()) for name in spamtar.getnames() if name.startswith("spam/0") or name.startswith("ham/0")]

spamfiles = spamfiles[n:]
hamfiles = hamfiles[n:]

allfiles = spamfiles + hamfiles

stats = {"spam": {"correct": 0,
                  "incorrect": 0},
         "notspam": {"correct": 0,
                     "incorrect": 0},
         "failures": 0}

i = 1
for (should_be_spam, content) in allfiles:
    sys.stdout.write('%s of %s (%s%%)\r' % (i, len(allfiles), p(i, len(allfiles))))
    sys.stdout.flush()

    try:
        data = json.dumps({"messages": [content]})
        req = urllib2.Request('http://%s/classify' % ipaddr, data, {})

        resp = urllib2.urlopen(req)
        is_spam = json.loads(resp.read())["messages"][0]["is-spam"]

        if should_be_spam:
            key = "spam"
        else:
            key = "notspam"

        if is_spam == should_be_spam:
            stats[key]['correct'] += 1
        else:
            stats[key]['incorrect'] += 1
    except:
        stats["failures"] += 1

    i += 1


def aob(n, d):
    return "%s%% (%s out of %s)" % (p(n, d), n, d)

print
print
print "----------------------------------------------------------"
print "Total Requests Made:            %s" % len(allfiles)
print "Failed Requests:                %s" % aob(stats['failures'], len(allfiles))
print ""
print "Ham Messages Marked Correctly:  %s" % aob(stats['notspam']['correct'],
                                                stats['notspam']['correct'] +
                                                stats['notspam']['incorrect'])
print "Spam Messages Marked Correctly: %s" % aob(stats['spam']['correct'],
                                                 stats['spam']['correct'] +
                                                 stats['spam']['incorrect'])
print "----------------------------------------------------------"
print
