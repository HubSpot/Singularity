View = require './view'

class StatusView extends View

    template: require '../templates/status'

    initialize: ->
        @listenTo @model, 'sync', @render

    captureLastState: ->
        @lastState = _.clone @model.toJSON()

    render: =>
        @$el.html @template
            state:  @model.toJSON()
            synced: @model.synced
            tasks: @tasks(@model)
            requests: @requests(@model)
            totalRequests: @totalRequests
            totalTasks: @totalTasks

        @$('.chart .chart__data-point[title]').tooltip(placement: 'right')

        @captureLastState()

    requests: (model) =>
        total_requests = @model.get 'allRequests'

        requests = [
            {
                type: 'active'
                label: 'active'
                count: @model.get('activeRequests')
                percent: @model.get('activeRequests') / total_requests * 100
                link: '/requests/active'
            }
            {
                type: 'paused'
                label: 'paused'
                count: @model.get('pausedRequests')
                percent: @model.get('pausedRequests') / total_requests * 100
                link: '/requests/paused'
            }
            {
                type: 'cooldown'
                label: 'cooling down'
                count: @model.get('cooldownRequests')
                percent: @model.get('cooldownRequests') / total_requests * 100
                link: '/requests/cooldown'
            }
            {
                type: 'pending'
                label: 'pending'
                count: @model.get('pendingRequests')
                percent: @model.attributes.pendingRequests / total_requests * 100
                link: '/requests/pending'
            }
            {
                type: 'cleaning'
                label: 'cleaning'
                count: @model.get('cleaningRequests')
                percent: @model.get('cleaningRequests') / total_requests * 100
                link: '/requests/cleaning'
            },
        ]

        @totalRequests = @sumValues requests, 'count'

        requests


    tasks: (model) =>
        total_tasks = @model.get('activeTasks') + @model.get('lateTasks') + @model.get('scheduledTasks') + @model.get('lbCleanupTasks')

        tasks = [
            {
                type: 'active'
                label: 'active'
                count: @model.get('activeTasks')
                percent: @model.get('activeTasks') / total_tasks * 100
                link: '/tasks'
            }
            {
                type: 'scheduled'
                label: 'scheduled'
                count: @model.get('scheduledTasks')
                percent: @model.get('scheduledTasks') / total_tasks * 100
                link: '/tasks/scheduled'
            }
            {
                type: 'overdue'
                label: 'overdue'
                count: @model.get('lateTasks')
                percent: @model.get('lateTasks') / total_tasks * 100
            }
            {
                type: 'cleaning'
                label: 'cleaning'
                count: @model.get('cleaningTasks')
                percent: @model.get('cleaningTasks') / total_tasks * 100
                link: 'tasks/cleaning'
            }
            {
                type: 'lbCleanup'
                label: 'load balancer cleanup'
                count: @model.get('lbCleanupTasks')
                percent: @model.get('lbCleanupTasks') / total_tasks * 100
            }
        ]

        
        @totalTasks = @sumValues tasks, 'count'

        tasks

    sumValues: (obj, key) ->
        total = 0
        for item in obj
            total = total + item[key]
        total


module.exports = StatusView
