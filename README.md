# Spam

```
Spam is a brand of canned precooked meat products made by Hormel Foods
Corporation. It was first introduced in 1937 and gained popularity
worldwide after its use during World War II.
```

## Environment

For local testing you can run vagrant which will build a small
deployment locally with the following:

```
1 Redis Server (v2, so no clustering yet)
2 App Servers
1 Load Balancer
```

To get started you need to build the jar:

```
lein ring uberjar
```

And then:

```
vagrant up
```

## Training data

Data samples can be loaded into the API using the training endpoints:

```
POST /train/spam
POST /train/not-spam
```

There is training data https://spamassassin.apache.org/publiccorpus/ in
the data folder. If you're using vagrant you can load the samples via:

```
# To clear existing data run:
curl -X http://10.111.1.103/flush

# And load it with:
python scripts/post-message-collection.py data/spam.tar.gz true 10.111.1.103
python scripts/post-message-collection.py data/ham.tar.gz false 10.111.1.103
```

## Running

You should have leiningen installed then you can launch the server with:

```
lein ring server-headless
```

## Routes

To test messages POST JSON to:

```
POST /classify
{"messages": ["message1", "message2"]}

Returns:
{:messages [{:prob 0.01, :is-spam false} {:prob 0.99, :is-spam true}]}

```

## Scaling Considerations

There are three main processing parts of this system:
* Messaging parsing
* Probability calculation
* Word count retrieval

The first two bullet points can scale linearly with the number of
compute units and computers. Putting a load balancer (HAProxy, nginx) in
front of these scalable API servers should allow for a very high
throughput.

Word count retrieval is currently done via Redis. This example is using
Redis 2, which doesn't support clustering out of the box. However there
are two obvious strategies for scaling up the word count retrieval
aspect of the system. The easiest is to upgrade to Redis v3 which has
built in clustering support.

If we wanted to stay on Redis 2 one could shard keys based on their
integer values to any number of N servers. The biggest issue would be
support tools for adding/removing nodes from the ring.

Redis also scales horizontally on more servers by adding secondary
nodes.

Right now each request pulls directly from redis. If this becomes a
bottleneck we can add a memory caching layer on each API server. The
question of updating the model becomes important. However, the general
use case will be to add, say millions of emails to this
system. The model classifier will generally change quite slowly in this
case, so as long as the caches have a reasonable timeout, say 5 - 20
minutes, an out of date model shouldn't really effect the outcome.

In writing this API the use case that was considered was testing single
(or small sets of) messages on a streaming basis, thus ruling out any
sort of batch processing.

## Failure Recovery

### Redis

Each Redis master should have at least one fallover secondary server
that can take over.

In the case of a network partition, Redis doesn't guarantee that all
data written will be maintained. It does provide a somewhat weaker
guarantee that a partition lasting less than `node timeout` seconds will
not result in data loss.

See http://redis.io/topics/cluster-tutorial for more info.

### API Servers/Load Balancer

If this system were to be deployed on AWS, using a standard Elastic Load
Balancer we need not fear a node failing. In that case the health check
will fail and the API Load Balancer will self heal.

## Accuracy

There are two data sets that can be used to train and validate the model
in the data directory. To run an empirical simulation you can use the
"test-failure-rates.py" script. The script does three things:
* Flush out any existing data
* Trains with `n` samples from both the spam and ham datasets
* Executes a classify call for `<# of spam training> - n` and `<# of ham
  training> - n` and collates the results

The more you increase `n` the more training is done and the fewer
validations sets that are run.

```
python scripts/test-failure-rates.py localhost:3000 700

----------------------------------------------------------
Total Requests Made:            2496
Failed Requests:                5% (140 out of 2496)

Ham Messages Marked Correctly:  97% (1675 out of 1714)
Spam Messages Marked Correctly: 92% (596 out of 642)
----------------------------------------------------------
```

## License

Copyright © 2015 Adam Hinz

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
