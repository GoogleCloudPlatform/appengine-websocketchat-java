App Engine Java VM Runtime Websocket Chat
Copyright (C) 2010-2014 Google Inc.

## Sample websocket chat application for use with App Engine Java VM Runtime.

Requires [Apache Maven](http://maven.apache.org) 3.0 or greater, and
JDK 7+ in order to run.  This application needs to be deployed to the
[App Engine VM Runtime][1].

Make sure that you are invited to the [VM Runtime Trusted Tester
Program][2], and have [downloaded the SDK](http://commondatastorage.googleapis.com/gae-vm-runtime-tt/vmruntime_sdks.html).

In order to run this application, you also need to configure the
Compute Engine firewall to allow incoming connections to the port 65080
by default.

Here is how to configure the Compute Engine firewall.

1. Go to the [cloud console][3].
2. Select your project which is under the VM Runtime TT program.
3. Select `Compute Engine`
4. Click the `Network` menu then click the `default` network.
5. Click `Create new` button in the `Firewalls` section.
6. Type `chatservice` in the `Name` field and `tcp:65080` in the
`Protocols & Ports` field, then click `Create` button.

Now you're good to go!

To build:

1. Rewrite the value of the `application` element in your `appengine-web.xml` to your app id.
2. Run `mvn package`
3. Run `appcfg.sh` of the SDK as follows:

        $ $SDK_DIR/bin/appcfg.sh -s preview.appengine.google.com update target/websocketchat-1.0-SNAPSHOT

4. Visit `http://chat.your-app-id.appspot.com/`.

For further information, consult the [Java App
Engine](https://developers.google.com/appengine/docs/java/overview)
documentation.

To see all the available goals for the App Engine plugin, run

    mvn help:describe -Dplugin=appengine

[1]: https://docs.google.com/document/d/1VH1oVarfKILAF_TfvETtPPE3TFzIuWqsa22PtkRkgJ4
[2]: https://groups.google.com/forum/?fromgroups#!topic/google-appengine/gRZNqlQPKys
[3]: https://cloud.google.com/console
