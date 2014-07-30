Handlebars.registerHelper 'appRoot', ->
    config.appRoot

Handlebars.registerHelper 'ifEqual', (v1, v2, options) ->
    if v1 is v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifLT', (v1, v2, options) ->
    if v1 < v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifGT', (v1, v2, options) ->
    if v1 > v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifInSubFilter', (needle, haystack, options) ->
    return options.fn @ if haystack is 'all'
    if haystack.indexOf(needle) isnt -1
        options.fn @
    else
        options.inverse @

Handlebars.registerHelper 'unlessInSubFilter', (needle, haystack, options) ->
    return options.inverse @ if haystack is 'all'
    if haystack.indexOf(needle) is -1
        options.fn @
    else
        options.inverse @
