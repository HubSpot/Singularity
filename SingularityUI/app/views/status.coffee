View = require './view'

numberAttributes = [
    'activeTasks'
    'pausedRequests'
    'activeRequests'
    'cooldownRequests'
    'scheduledTasks'
    'pendingRequests'
    'cleaningRequests'
    'activeSlaves'
    'deadSlaves'
    'decomissioningSlaves'
    'activeRacks'
    'deadRacks'
    'decomissioningRacks'
    'numWebhooks'
    'cleaningTasks'
    'lateTasks'
    'futureTasks'
]

class StatusView extends View

    template: require './templates/status'

    initialize: ->
        @captureLastStateAttributes()

    captureLastStateAttributes: ->
        @lastStateAttributes = $.extend {}, app.state.attributes

    fetch: ->
        @captureLastStateAttributes()
        app.state.fetch()

    refresh: (fromRoute) ->
        @fetch(@lastTasksFilter).done =>
            @render(fromRoute)

        @

    render: (fromRoute) =>
        changedNumbers = {}

        if fromRoute
            @$el.html @template state: app.state.attributes

        else
            @$el.html @template state: @lastStateAttributes

            for numberAttribute in numberAttributes
                oldNumber = @lastStateAttributes[numberAttribute]
                newNumber = app.state.attributes[numberAttribute]
                if oldNumber isnt newNumber
                    changedNumbers[numberAttribute] =
                        direction: "#{ if newNumber > oldNumber then 'inc' else 'dec' }rease"
                        difference: "#{ if newNumber > oldNumber then '+' else '-' }#{ Math.abs(newNumber - oldNumber) }"

            for attributeName, changes of changedNumbers
                $number = @$el.find(""".number[data-state-attribute="#{ attributeName }"]""")
                $bigNumber = $number.parents('.big-number-link')
                changeClassName = "changed-direction-#{ changes.direction }"
                $bigNumber.addClass changeClassName
                $bigNumber.find('.well').attr('data-changed-difference', changes.difference)
                $number.html app.state.attributes[attributeName]
                do ($bigNumber, changeClassName) -> setTimeout (-> $bigNumber.removeClass changeClassName), 2000

        utils.setupSortableTables()

        @

module.exports = StatusView