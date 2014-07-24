View = require './view'

State = require '../models/State'

class StatusView extends View

    template: require '../templates/status'

    initialize: ->
        @model = new State
        @model.fetch().done @render

    captureLastState: ->
        @lastState = _.clone @model.attributes

    refresh: ->
        @captureLastState()
        @model.fetch().done @render

    render: (fromRoute) =>
        # When refreshing we want to display a nice pretty animation
        # showing which number boxes have changed.
        if not @lastState?
            # Render template from fresh data
            @$el.html @template
                state:  @model.attributes
                synced: @model.synced
        else
            # Render template with old data and animate the new stuff in
            @$el.html @template
                state: @lastState
                synced: @model.synced

            changedNumbers = {}

            numberAttributes = []
            # Go through each key. If the value is a number, we'll (try to)
            # perform a change animation on that key's box
            _.each _.keys(@model.attributes), (attribute) =>
                if typeof @model.attributes[attribute] is 'number'
                    numberAttributes.push attribute

            for numberAttribute in numberAttributes
                oldNumber = @lastState[numberAttribute]
                newNumber = @model.attributes[numberAttribute]
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
                $number.html @model.attributes[attributeName]
                do ($bigNumber, changeClassName) -> setTimeout (-> $bigNumber.removeClass changeClassName), 2000

module.exports = StatusView