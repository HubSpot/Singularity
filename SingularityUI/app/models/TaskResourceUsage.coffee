Model = require './model'

class TaskResourceUsage extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @taskId }/statistics"

    initialize: ({ @taskId }) =>

    # Calculate CPU Usage by comparing previous usage to current usage
    setCpuUsage: ->
        previous = @get('previousUsage')
        currentTime = @get('cpusSystemTimeSecs') + @get('cpusUserTimeSecs')
        previousTime = previous.cpusSystemTimeSecs + previous.cpusUserTimeSecs
        timestampDiff = @get('timestamp') - previous.timestamp
        cpus_used = (currentTime - previousTime) / timestampDiff
        cpuUsageExceeding = (cpus_used / @get('cpusLimit')) > 1.10
        
        @set 'cpuUsageExceeding', cpuUsageExceeding
        if cpuUsageExceeding
            @set 'cpuUsageClassStatus', 'danger'
        else
            @set 'cpuUsageClassStatus', 'success'

        @set 'cpuUsage', cpus_used

module.exports = TaskResourceUsage
