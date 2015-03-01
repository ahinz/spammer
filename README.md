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

## License

Copyright © 2015 Adam Hinz

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
