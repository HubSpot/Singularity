Model = require './model'

MOCK_JSON = {"master":true,"uptime":147486266,"activeTasks":9,"requests":9,"scheduledTasks":1,"pendingRequests":0,"cleaningRequests":0,"driverStatus":"DRIVER_RUNNING"}

class State extends Model

    #url: -> "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/state"
    url: => "https://#{env.INTERNAL_BASE}/#{constants.kumonga_api_base}/users/#{app.login.context.user.email}/settings"

    parse: (state) =>
        state = MOCK_JSON
        state.uptimeHuman = moment.duration(state.uptime).humanize()
        state

module.exports = State