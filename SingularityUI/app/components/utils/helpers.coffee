module.exports =

  # 1429107530864 => 7 days ago (15 Apr 2015 10:18)
  timestampFromNow: (timestamp) ->
    return '' if not timestamp
    timeObject = moment timestamp
    "#{timeObject.fromNow()} (#{ timeObject.format 'lll'})"

  # 'blodge@hubspot.com' => 'blodge'
  # 'blodge'             => 'blodge'
  usernameFromEmail: (email) ->
    return '' if not email
    email.split('@')[0]

  timestampFormatted: (timestamp) ->
    return '' if not timestamp
    timeObject = moment timestamp
    timeObject.format 'lll'

  timestampDuration: (timestamp) ->
    return '' if not timestamp
    moment.duration(timestamp).humanize()
    
  isTimestampInPast: (timestamp, options) ->
    return true if not timestamp
    timeObject = moment timestamp
    now = moment()
    if timeObject.isBefore(now)
        return true
    false

  # 'DRIVER_NOT_RUNNING' => 'Driver not running'
  humanizeText: (text) ->
    return '' if not text
    text = text.replace /_/g, ' '
    text = text.toLowerCase()
    text = text[0].toUpperCase() + text.substr 1

  titleCase: (str) ->
    str.replace /\w\S*/g, (txt) ->
      txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase()

  isInSubFilter: (needle, haystack, options) ->
    return true if haystack is 'all'
    if haystack.indexOf(needle) isnt -1
      return true
    false

  routeLink: (e) =>
    $link = $(e.target)
    $parentLink = $link.parents('a[href]')
    $link = $parentLink if $parentLink.length
    url = $link.attr('href')

    return true if $link.attr('target') is '_blank' or url is 'javascript:;' or typeof url is 'undefined' or url.indexOf(config.appRoot) != 0

    if e.metaKey or e.ctrlKey or e.shiftKey
        return

  routeComponentLink: (e, route, trigger=false) =>
    e.preventDefault() if e
    url = route.replace(config.appRoot, '')
    app.router.navigate url, trigger: trigger