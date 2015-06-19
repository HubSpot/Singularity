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

  titleCase: (str) ->
    str.replace /\w\S*/g, (txt) ->
      txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase()