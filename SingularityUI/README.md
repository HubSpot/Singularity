# SingularityUI

The Singularity UI is a brunch app. To develop locally, first install brunch.

```shell
npm install -g brunch
```

Then npm install `/SingularityUI/package.json`:

```shell
npm install
```

Run Chrome or Chrome Canary with web security disabled (to allow cross-domain requests):

```shell
open -a Google\ Chrome\ Canary --args --disable-web-security
```

Lastly, run brunch on your preferred port:

```shell
brunch watch --server -p PORT
```