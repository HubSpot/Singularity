name             'singularity'
maintainer       'Tom Petr'
maintainer_email 'tpetr@hubspot.com'
license          'All rights reserved'
description      'Installs Singularity dependencies (mesos, zk, mysql)'
version          '0.6.0'
supports         'ubuntu'

depends 'apt'
depends 'mysql', '6.0.13'
depends 'mysql2_chef_gem'
depends 'database'
depends 'hostsfile'
depends 'java'
depends 'openssl', '~> 3.0'
