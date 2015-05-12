Model = require './model'

class State extends Model

    url: -> "#{ config.apiRoot }/state"

    requestDetail: ->

        total = @get('activeRequests') + @get('pausedRequests') + @get('cooldownRequests') + @get('pendingRequests') + @get('cleaningRequests')

        requests = [
            {
                type: 'active'
                attribute: 'activeRequests'
                label: 'active'
                count: @get('activeRequests')
                percent: @get('activeRequests') / total * 100
                link: '/requests/active'
            }
            {
                type: 'paused'
                attribute: 'pausedRequests'
                label: 'paused'
                count: @get('pausedRequests')
                percent: @get('pausedRequests') / total * 100
                link: '/requests/paused'
            }
            {
                type: 'cooldown'
                attribute: 'cooldownRequests'
                label: 'cooling down'
                count: @get('cooldownRequests')
                percent: @get('cooldownRequests') / total * 100
                link: '/requests/cooldown'
            }
            {
                type: 'pending'
                attribute: 'pendingRequests'
                label: 'pending'
                count: @get('pendingRequests')
                percent: @attributes.pendingRequests / total * 100
                link: '/requests/pending'
            }
            {
                type: 'cleaning'
                attribute: 'cleaningRequests'
                label: 'cleaning'
                count: @get('cleaningRequests')
                percent: @get('cleaningRequests') / total * 100
                link: '/requests/cleaning'
            },
        ]

        return {
            requests: requests
            total: total
        }

    taskDetail: ->
        
        total = @get('activeTasks') + @get('lateTasks') + @get('scheduledTasks') + @get('cleaningTasks') + @get('lbCleanupTasks')
        tasks = [
            {
                type: 'active'
                attribute: 'activeTasks'
                label: 'active'
                count: @get('activeTasks')
                percent: @get('activeTasks') / total * 100
                link: '/tasks'
            }
            {
                type: 'scheduled'
                attribute: 'scheduledTasks'
                label: 'scheduled'
                count: @get('scheduledTasks')
                percent: @get('scheduledTasks') / total * 100
                link: '/tasks/scheduled'
            }
            {
                type: 'overdue'
                attribute: 'lateTasks'
                label: 'overdue'
                count: @get('lateTasks')
                percent: @get('lateTasks') / total * 100
                link: '/tasks/scheduled'
            }
            {
                type: 'cleaning'
                attribute: 'cleaningTasks'
                label: 'cleaning'
                count: @get('cleaningTasks')
                percent: @get('cleaningTasks') / total * 100
                link: '/tasks/cleaning'
            }
            {
                type: 'lbCleanup'
                attribute: 'lbCleanupTasks'
                label: 'load balancer cleanup'
                count: @get('lbCleanupTasks')
                percent: @get('lbCleanupTasks') / total * 100
                link: '/tasks/lbcleanup'
            }
        ]

        return {
            tasks: tasks
            total: total
        }


module.exports = State