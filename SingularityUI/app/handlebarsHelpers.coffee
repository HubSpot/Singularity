Handlebars.registerHelper 'appRoot', ->
    config.appRoot

Handlebars.registerHelper 'apiDocs', ->
    config.apiDocs

Handlebars.registerHelper 'ifEqual', (v1, v2, options) ->
    if v1 is v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifLT', (v1, v2, options) ->
    if v1 < v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifGT', (v1, v2, options) ->
    if v1 > v2 then options.fn @ else options.inverse @

Handlebars.registerHelper "ifAll", (conditions..., options)->
    for condition in conditions
        return options.inverse @ unless condition?
    options.fn @

Handlebars.registerHelper 'percentageOf', (v1, v2) ->
    Math.round(v1/v2)

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

# {{#withLast [1, 2, 3]}}
#     {{! this = 3 }}
# {{/withLast}}
Handlebars.registerHelper 'withLast', (list, options) ->
    options.fn _.last list

# 1234567890 => 20 minutes ago
Handlebars.registerHelper 'timestampFromNow', (timestamp) ->
    return '' if not timestamp
    timeObject = moment timestamp
    "#{timeObject.fromNow()} (#{ timeObject.format 'lll'})"

Handlebars.registerHelper 'ifTimestampInPast', (timestamp, options) ->
    return options.inverse @ if not timestamp
    timeObject = moment timestamp
    now = moment()
    if timeObject.isBefore(now)
        options.fn @
    else
        options.inverse @

# 12345 => 12 seconds
Handlebars.registerHelper 'timestampDuration', (timestamp) ->
    return '' if not timestamp
    moment.duration(timestamp).humanize()

# 1234567890 => 1 Aug 1991 15:00
Handlebars.registerHelper 'timestampFormatted', (timestamp) ->
    return '' if not timestamp
    timeObject = moment timestamp
    timeObject.format 'lll'

# 'DRIVER_NOT_RUNNING' => 'Driver not running'
Handlebars.registerHelper 'humanizeText', (text) ->
    return '' if not text
    text = text.replace /_/g, ' '
    text = text.toLowerCase()
    text = text[0].toUpperCase() + text.substr 1
    text

# 2121 => '2 KB'
Handlebars.registerHelper 'humanizeFileSize', (fileSize) ->
    kilo = 1024
    mega = 1024 * 1024
    giga = 1024 * 1024 * 1024

    shorten = (which) -> Math.round fileSize / which

    if fileSize > giga
        return "#{ shorten giga } GB"
    else if fileSize > mega
        return "#{ shorten mega } MB"
    else if fileSize > kilo
        return "#{ shorten kilo } KB"
    else
        return "#{ fileSize } B"

# 'sbacanu@hubspot.com' => 'sbacanu'
# 'seb'                 => 'seb'
Handlebars.registerHelper 'usernameFromEmail', (email) ->
    return '' if not email
    email.split('@')[0]
