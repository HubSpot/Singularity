Handlebars.registerHelper 'appRoot', ->
    config.appRoot

Handlebars.registerHelper 'apiDocs', ->
    config.apiDocs

Handlebars.registerHelper 'ifEqual', (v1, v2, options) ->
    if v1 is v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifNotEqual', (v1, v2, options) ->
    if v1 isnt v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifLT', (v1, v2, options) ->
    if v1 < v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifGT', (v1, v2, options) ->
    if v1 > v2 then options.fn @ else options.inverse @

Handlebars.registerHelper "ifAll", (conditions..., options)->
    for condition in conditions
        return options.inverse @ unless condition?
    options.fn @

Handlebars.registerHelper 'percentageOf', (v1, v2) ->
    (v1/v2) * 100

# Override decimal rounding: {{fixedDecimal data.cpuUsage place="4"}}
Handlebars.registerHelper 'fixedDecimal', (value, options) ->
    if options.hash.place then place = options.hash.place else place = 2
    +(value).toFixed(place)

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

Handlebars.registerHelper 'timestampFormattedWithSeconds', (timestamp) ->
    return '' if not timestamp
    timeObject = moment timestamp
    timeObject.format 'lll:ss'

# 'DRIVER_NOT_RUNNING' => 'Driver not running'
Handlebars.registerHelper 'humanizeText', (text) ->
    return '' if not text
    text = text.replace /_/g, ' '
    text = text.toLowerCase()
    text = text[0].toUpperCase() + text.substr 1
    text

# 2121 => '2 KB'
Handlebars.registerHelper 'humanizeFileSize', (bytes) ->
    k = 1024
    sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']

    return '0 B' if bytes is 0
    i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length-1)
    return +(bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]

# 'sbacanu@hubspot.com' => 'sbacanu'
# 'seb'                 => 'seb'
Handlebars.registerHelper 'usernameFromEmail', (email) ->
    return '' if not email
    email.split('@')[0]

Handlebars.registerHelper 'substituteTaskId', (value, taskId) ->
    value.replace('$TASK_ID', taskId)

Handlebars.registerHelper 'getLabelClass', (state) ->
    switch state
        when 'TASK_STARTING', 'TASK_CLEANING'
            'warning'
        when 'TASK_STAGING', 'TASK_LAUNCHED', 'TASK_RUNNING'
            'info'
        when 'TASK_FINISHED'
            'success'
        when 'TASK_KILLED', 'TASK_LOST', 'TASK_FAILED', 'TASK_LOST_WHILE_DOWN'
            'danger'
        else
            'default'
