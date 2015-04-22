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
