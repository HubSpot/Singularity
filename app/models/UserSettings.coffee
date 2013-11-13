Model = require './model'

class UserSettings extends Model

    url: => "https://#{env.INTERNAL_BASE}/#{constants.kumonga_api_base}/users/#{app.login.context.user.email}/settings"

    defaults:
        theme: 'light'
        #requests: [...]
        #projects: [...]

module.exports = UserSettings