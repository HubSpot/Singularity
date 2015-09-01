## Release process

Prereqs:
- sonatype server passwords in ~/.m2/settings.xml for mvn deploy (otherwise, 401)
- access to the sonatype user/password when browsing to the web ui
- gpg installed, key generated and shipped, and passphrase available 

Steps:
- `mvn release:clean`
- `mvn release:prepare`
- `mvn release:perform`
- [In staging repositories, close and release](https://oss.sonatype.org/)

If having issues releasing, run `mvn release:rollback` to undo changes.
