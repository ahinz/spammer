# Spam

```
Spam is a brand of canned precooked meat products made by Hormel Foods
Corporation. It was first introduced in 1937 and gained popularity
worldwide after its use during World War II.
```

## Running

You should have leiningen installed then you can launch the server with:

```
lein ring server-headless
```

## Current State

A super simple Bayesian filter API for checking if a given email message
is spam or not. Right now the model is held entirely in memory.

## Routes

You can train the model with two built in routes by posting the messages
directly:

```
POST /train/spam
POST /train/not-spam
```

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
