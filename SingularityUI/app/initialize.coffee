window.env = require 'env'
window.utils = require 'utils'
window.constants = require 'constants'
window.app = require 'application'

# Make all string methods available on _
_.mixin _.string.exports()

# Set Vex default className
vex.defaultOptions.className = 'vex-theme-default'

# Patch jQuery ajax to always use xhrFields.withCredentials true
_oldAjax = jQuery.ajax
jQuery.ajax = (opts) ->
    opts.xhrFields ?= {}
    opts.xhrFields.withCredentials = true

    _oldAjax.call jQuery, opts

$ -> app.initialize()