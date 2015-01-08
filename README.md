App Engine Java VM Runtime Websocket Chat
Copyright (C) 2010-2014 Google Inc.

## Sample websocket chat application for use with App Engine Java VM Runtime.

This application builds and deploys a chat application which uses web sockets to Google App Engine [Managed VMs][1], using Maven and the gcloud SDK.

## Project Setup

1. Create a billing enabled project and install the Google Cloud SDK as described [here][2].
2. Install Maven 3.1 or later as described [here](https://cloud.google.com/appengine/docs/java/managed-vms/maven#requirements).

### Create Firewall Exception

In order to run this application, you also need to configure the
Compute Engine firewall to allow incoming connections to the port 65080
by default.

Here is how to configure the Compute Engine firewall.

1. Go to the [cloud console][3].
2. Select your project
3. Select `Compute Engine`
4. Click the `Network` menu then click the `default` network.
5. Click `Create new` button in the `Firewalls` section.
6. Type `chatservice` in the `Name` field and `tcp:65080` in the
`Protocols & Ports` field, then click `Create` button.

Now you're good to go!

##Deploy

After setting up Maven, you can deploy your app locally, or to the Google Cloud Platform as described [here](https://cloud.google.com/appengine/docs/java/managed-vms/maven#run_and_deploy_your_app_with_the_cloud_sdk_development_server), then visit `http://chat.your-app-id.appspot.com/`.

[1]: https://cloud.google.com/appengine/docs/java/managed-vms/
[2]: https://cloud.google.com/appengine/docs/java/managed-vms/#install-sdk
[3]: https://cloud.google.com/console
