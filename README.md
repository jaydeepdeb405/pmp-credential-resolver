# MID Server External Credential Resolver for Password Manager Pro

This is the ServiceNow MID Server custom external credential resolver for the Password Manager Pro credential storage.

# Pre-requisites:

Password Manager Pro External Credential Resolver requires JDK 1.8 or newer
Eclipse or any equivalent IDE

# Steps to build
* Clone this repository.
* Import the project in Eclipse or any IDE.
* Update MID Server agent path in pom.xml to point to valid MID Server location.
* Update the code in CredentialResolver.java to customize anything.
* Use below maven command or IDE (Eclipse or Intellij) maven build option to build the jar.

	> mvn clean package

* pmp-credential-resolver-0.0.1-SNAPSHOT.jar will be generated under target folder.

# Steps to install and use Password Manager Pro as external credential resolver

* Make sure that “External Credential Storage” plugin (com.snc.discovery.external_credentials) is installed in your ServiceNow instance.
* Import the pmp-credential-resolver-0.0.1-SNAPSHOT.jar file from target folder in ServiceNow instance.
	- Navigate to MID Server – JAR Files
	- Create a New Record by clicking New
	- Name it “PMPCredentialResolver”, version 0.0.1 and attach pmp-credential-resolver-0.0.1-SNAPSHOT.jar from target folder.
	- Click Submit
* Update the config.xml in MID Server with below parameters (sample values provided) and restart the MID Server.

	`<parameter name="ext.cred.pmp.host" value="http://localhost:7272>"/>` 
	
	`<parameter name="ext.cred.pmp.authtoken" secure="true" value="<XXXXYYYY-AAAA-CCCC-BBB0-ABCD65YQAC>"/>`

* Create Credential in the instance with "External credential store" flag activated.
* Ensure that the "Credential ID" contains '@' delimited resouce (ID or Name) & account (Id or Name) in your Password Manager Pro instance.
* e.g. - ResouceId & AccountId combination - '123@223' (where '123' is the ResourceId & '223' is AccountId) 
*	     ResouceName & AccountName - 'UbuntuServer@user001' (where 'UbuntuServer' is the ResouceName & 'user001' is AccountName) 
  

	


