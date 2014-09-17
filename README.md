#Keypan#

This is a simple tool to find OpenPGP keys associated with a public
profile on Twitter, Github or Google Plus. It achieves one of the
things a site like [keybase.io](https://keybase.io) offers, but
without requiring a separate service.

```
$ java -jar keypan-cli.jar github.com/kbsriram
Found key for 'github.com/kbsriram'
PGP fingerprint: BF71 A5E8 E8CD 553B DE86  0969 62F4 63C6 73F6 C01F
linked on Github by kbsriram <https://github.com/kbsriram>
    from this gist <https://gist.github.com/c05dca103a252ac0d6ac>
linked on Twitter by kbsriram <https://twitter.com/kbsriram>
    from their profile <https://twitter.com/kbsriram>
linked on Google+ by KB Sriram <https://plus.google.com/+KBSriram>
    from their profile <https://plus.google.com/+KBSriram/about>
Save this key? (y/N)
```

There's also a locally runnable web-server, which offers a
nicer-looking interface to the search.

![keypan search](https://github.com/kbsriram/keypan/raw/master/etc/keypan-web.png "Keypan Search")

##How it works

I call the approach "key panning", as it lets clients sift through
keys published against your public profiles, while adding a basic
level of assurance.

1. Add your various profile URLs as user-ids on your public key, and
   push them to key-servers as usual.
2. Publish your fingerprint in some 'well-understood' way on each such
   profile site.

A client can now lookup your key by your profile URL on a
key-server. For each valid user id that matches a public profile, the
client looks for a confirming fingerprint published at the profile
site. It's not an authoritative proof of identity, but it indicates
that someone in control of this key was also able to publish to the
associated account.

As a secondary benefit, it consolidates your various public profiles
directly on your key, and uses the distributed OpenPGP key-servers to
propagate your keys.

## Running `keypan`

The `keypan` client is written in Java and needs at least Java 1.6.

To run the command-line client, download
[keypan-cli.jar](https://github.com/kbsriram/keypan/raw/master/bin/keypan-cli.jar)
and simply run it with a suitable query.

```
$ java -jar keypan-cli.jar github.com/kbsriram
```

To run the local webserver, download
[keypan-web.jar](https://github.com/kbsriram/keypan/raw/master/bin/keypan-web.jar)
and run it as

```
$ java -jar keypan-web.jar
```

and then visit http://localhost:8014

## Adding a profile to your key

Here's how to use `gpg` to add your social media profiles to your key.

1. First add each profile url as a new uid. For example,
<pre>
$ gpg --allow-freeform-uid --edit-key john@example.com adduid
[...]
Real name: <b>https://github.com/mygithubid</b>
Email address: <b>&lt;CR&gt;</b>
Comment: <b>&lt;CR&gt;</b>
You selected this USER-ID:
    "https://github.com/myrealname"
Change (N)ame, (C)omment, (E)mail or (O)kay/(Q)uit? <b>o</b>
[...]
Enter passphrase: <b>&lt;password&gt;</b>
[...]
gpg&gt; <b>save</b>
$
</pre>
2. Repeat this for each profile URL you want to associate with your
key. Then, publish it to the keyservers.
<pre>
$ gpg --send-keys &lt;yourkeyid&gt;
gpg: sending key 12F3C45F to hkps server hkps.pool.sks-keyservers.net
$
</pre>
3. Finally, publish your key fingerprint on each of your profiles.

## Publishing your fingerprint on a profile

The `keypan` tool can search for fingerprints from three types of
profiles, Github, Twitter and Google+. To publish your fingerprint on
each of these sites, do the following.

1. Github - publish a public gist containing your fingerprint. An
   example can be found here - https://gist.github.com/kbsriram/c05dca103a252ac0d6ac
2. Twitter - go to your [profile settings](https://twitter.com/settings/profile) and add your fingerprint to your bio. An example can be found here - https://twitter.com/kbsriram
3. Google+ - go to your profile about page and [edit your basic information](https://support.google.com/plus/answer/1355890) to include your fingerprint. An example can be found here - https://plus.google.com/+KBSriram/about
