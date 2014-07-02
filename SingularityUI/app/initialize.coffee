# Set up the only app globals
window.utils = require 'utils'
window.constants = require 'constants'
window.app = require 'application'

# Set up third party configurations
require 'thirdPartyConfigurations'

# Initialize the app on DOMContentReady
$ -> app.initialize()