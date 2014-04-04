Handlebars.registerHelper 'appRoot', -> constants.appRoot

Handlebars.registerHelper 'ifEqual', (v1, v2, options) -> if v1 is v2 then options.fn @ else options.inverse @
Handlebars.registerHelper 'ifLT', (v1, v2, options) -> if v1 < v2 then options.fn @ else options.inverse @
Handlebars.registerHelper 'ifGT', (v1, v2, options) -> if v1 > v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'pluralize', (number, single, plural) -> if number is 1 then single else plural

Handlebars.registerHelper 'hardBreak', (string, options) -> string?.replace(/(:|-)/g, '$1<wbr/>')

Handlebars.registerHelper 'getShortTaskIDMiddleEllipsis', (taskId, options) -> (utils.getShortTaskIDMiddleEllipsis taskId)?.replace(/(:|-)/g, '$1<wbr/>')

Handlebars.registerHelper 'eachWithFn', (items, options) ->
    _(items).map((item, i, items) =>
        item._counter = i
        item._1counter = i + 1
        item._first = if i is 0 then true else false
        item._last = if i is (items.length - 1) then true else false
        item._even = if (i + 1) % 2 is 0 then true else false
        item._thirded = if (i + 1) % 3 is 0 then true else false
        item._sixthed = if (i + 1) % 6 is 0 then true else false
        _.isFunction(options.hash.fn) and options.hash.fn.apply options, [item, i, items]
        options.fn(item)
    ).join('')

Handlebars.registerHelper 'ifFilteredRequest', (request, searchFilter, options) ->
    return options.inverse(@) unless request and searchFilter

    rowText = request.id
    user = request.deployUser
    rowText = "#{ rowText } #{ user }" if user?

    if utils.matchWordsInWords searchFilter, rowText
        options.inverse @
    else
        options.fn @