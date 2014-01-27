# Make all string methods available on _
_.mixin _.string.exports()

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

# Configure moment().calender() to be used as an alternative to moment().calendar()
relative = -> "[#{ @from() }]"
relativePlus = -> "[#{ @from() }] ([#{ @format('l h:mma') }])"

moment.lang 'en',
    calendar:
        nextWeek: relativePlus
        nextDay: relativePlus
        sameDay: relative
        lastDay: relativePlus
        lastWeek: relativePlus
        sameElse: relativePlus