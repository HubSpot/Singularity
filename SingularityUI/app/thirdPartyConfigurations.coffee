vex = require 'vex'
moment = require 'moment'
Handlebars = require 'handlebars'
Messenger = require 'messenger'

# Set Vex default className
vex.defaultOptions.className = 'vex-theme-default'

# Time out requests within 10 seconds
$.ajaxSetup
    timeout: 10 * 1000

# Patch jQuery ajax to always use xhrFields.withCredentials true
_oldAjax = jQuery.ajax
jQuery.ajax = (opts) ->
    opts.xhrFields ?= {}
    opts.xhrFields.withCredentials = true

    _oldAjax.call jQuery, opts

# Eat M/D/Y & 24h-time, yanks! Mwahahahahaha!
moment.locale 'en',
    longDateFormat:
        LT : "HH:mm"
        L : "DD/MM/YYYY"
        LL : "D MMMM YYYY"
        LLL : "D MMMM YYYY LT"
        LLLL : "dddd, D MMMM YYYY LT"

# Messenger options
Messenger.options =
    extraClasses: 'messenger-fixed messenger-on-top'
    theme: 'air'
    hideOnNavigate: true
    maxMessages: 1
    messageDefaults:
        type: 'error'
        hideAfter: false
        showCloseButton: true

# Overwrite Handlebars logging
Handlebars.logger.log = (stuff...) =>
    for thing in stuff[1..]
        console.log thing
