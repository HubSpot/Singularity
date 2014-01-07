window.env = require 'env'
window.utils = require 'utils'
window.constants = require 'constants'
window.app = require 'application'

# Make all string methods available on _
_.mixin _.string.exports()

# Set Vex default className
vex.defaultOptions.className = 'vex-theme-default'

$ -> app.initialize()