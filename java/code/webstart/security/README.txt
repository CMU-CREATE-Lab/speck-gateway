Creating a Keystore and Self-Signed Certificate
------------------------------------------------
Here's what I did to create the keystore so we can sign the jars with a self-signed certificate.

First, create the keystore by doing the following in the Terminal, with the current directory set to this directory.
When prompted, I used the password "password".

   $ keytool -genkey -keystore CREATELabKeystore -alias createlab
     Enter keystore password:
     Re-enter new password:
     What is your first and last name?
       [Unknown]:  CREATE Lab
     What is the name of your organizational unit?
       [Unknown]:  Robotics Institute
     What is the name of your organization?
       [Unknown]:  Carnegie Mellon University
     What is the name of your City or Locality?
       [Unknown]:  Pittsburgh
     What is the name of your State or Province?
       [Unknown]:  Pennsylvania
     What is the two-letter country code for this unit?
       [Unknown]:  US
     Is CN=CREATE Lab, OU=Robotics Institute, O=Carnegie Mellon University, L=Pittsburgh, ST=Pennsylvania, C=US correct?
       [no]:  yes

     Enter key password for <createlab>
      (RETURN if same as keystore password):

Next, create a self-signed certificate.  The "validity" flag specifies for how many days the certificate is valid.  I
chose 369 here so that it would expire on 11-12-13.

   $ keytool -selfcert -validity 369 -alias createlab -keystore CREATELabKeystore
     Enter keystore password:

This last step is optional, and is just used to verify that everything worked OK.

   $ keytool -list -keystore CREATELabKeystore
     Enter keystore password:

     Keystore type: JKS
     Keystore provider: SUN

     Your keystore contains 1 entry

     createlab, Nov 8, 2012, PrivateKeyEntry,
     Certificate fingerprint (MD5): 37:47:76:EF:39:D0:E7:1C:FB:C3:1E:78:F1:FD:71:0E
