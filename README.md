App Engine Java VM Runtime Websocket Chat
Copyright (C) 2010-2015 Google Inc.

## Sample websocket chat application for use with App Engine Java VM Runtime.

Requires [Apache Maven](http://maven.apache.org) 3.1 or greater, and
JDK 7+ in order to run.  This application needs to be deployed to the
[App Engine Managed VMs][1].


In order to run this application, you also need to configure the
Compute Engine firewall to allow incoming connections to the port 65080
by default.

Here is how to configure the Compute Engine firewall.

1. Go to the [cloud console][https://cloud.google.com/console].
2. Select your Cloud project.
3. Select `Compute Engine`
4. Click the `Network` menu then click the `default` network.
5. Click `Add Firewall rule` button in the `Firewalls rules` section.
6. Type `chatservice` in the `Name` field, `0.0.0.0/0` in the Source IP Ranges field
  and `tcp:65080` in the
`Allowed protocols or ports` field, then click `Create` button.

Now you're good to go!

To build:

Install the [Cloud SDK for Managed VMs](https://cloud.google.com/appengine/docs/managed-vms/)
To run the application, do the following:

1. Set the correct Cloud SDK project via `gcloud config set project YOUR_PROJECT`.
2. Run `mvn gcloud:deploy`
3. Visit `http://YOUR_PROJECT.appspot.com`.

For further information, consult the [Java App
Engine](https://cloud.google.com/appengine/docs/java/managed-vms)
documentation.

To see all the available goals for the Cloud SDK plugin, run

    mvn help:describe -Dplugin=gcloud

[1]: https://cloud.google.com/console
