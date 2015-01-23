View = require './view'

class StatusView extends View

    template: require '../templates/status'

    initialize: ->
        @listenTo @model, 'sync', @render

    captureLastState: ->
        @lastState = _.clone @model.attributes

    render: =>
        @$el.html @template
            state:  @model.attributes
            synced: @model.synced
            tasks: @tasks(@model)
            requests: @requests(@model)

        @$('.chart-data div[title]').tooltip(placement: 'right')

        @captureLastState()

    requests: (model) =>
        total_requests = @model.attributes.allRequests
        requests = [
            {
                type: 'active_item',
                label: 'active',
                count: @model.attributes.activeRequests,
                height: @model.attributes.activeRequests / total_requests * 100,
                link: '/requests/active'
            },
            {
                type: 'paused_item',
                label: 'paused',
                count: @model.attributes.pausedRequests,
                height: @model.attributes.pausedRequests / total_requests * 100,
                link: '/requests/paused'
            },
            {
                type: 'cooldown_item',
                label: 'cooling down'
                count: @model.attributes.cooldownRequests,
                height: @model.attributes.cooldownRequests / total_requests * 100,
                link: '/requests/cooldown'
            },
            {
                type: 'pending_item',
                label: 'pending',
                count: @model.attributes.pendingRequests,
                height: @model.attributes.pendingRequests / total_requests * 100,
                link: '/requests/pending'
            },
            {
                type: 'cleaning_item',
                label: 'cleaning',
                count: @model.attributes.cleaningRequests,
                height: @model.attributes.cleaningRequests / total_requests * 100,
                link: '/requests/cleaning'
            },
        ]
        return requests

    tasks: (model) =>
        total_tasks = @model.attributes.activeTasks + @model.attributes.lateTasks + @model.attributes.scheduledTasks + @model.attributes.lbCleanupTasks
        tasks = [
            {
                type: 'active_item',
                label: 'active',
                count: @model.attributes.activeTasks,
                height: @model.attributes.activeTasks / total_tasks * 100,
                link: '/tasks'
            },
            {
                type: 'scheduled_item',
                label: 'scheduled'
                count: @model.attributes.scheduledTasks,
                height: @model.attributes.scheduledTasks / total_tasks * 100,
                link: '/tasks/scheduled'
            },
            {
                type: 'overdue_item',
                label: 'overdue',
                count: @model.attributes.lateTasks,
                height: @model.attributes.lateTasks / total_tasks * 100
            },
            {
                type: 'cleaning_item',
                label: 'cleaning',
                count: @model.attributes.cleaningTasks,
                height: @model.attributes.cleaningTasks / total_tasks * 100,
                link: 'tasks/cleaning'
            },
            {
                type: 'lbCleanup_item',
                label: 'load balancer cleanup',
                count: @model.attributes.lbCleanupTasks,
                height: @model.attributes.lbCleanupTasks / total_tasks * 100
            }
        ]
        return tasks

module.exports = StatusView
