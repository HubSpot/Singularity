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
            tasks: @model.taskDetail().tasks
            requests: @model.requestDetail().requests
            totalRequests: @model.requestDetail().total
            totalTasks: @model.taskDetail().total

        if @lastState?
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
                changeClassName = "changed-direction-#{ changes.direction }"
                $attribute = @$el.find("""[data-state-attribute="#{ attributeName }"]""").not('[data-type="column"]')
                $bigNumber = $attribute.closest('.list-group-item')
                $bigNumber.find('a').addClass(changeClassName).append("<span class='changeDifference'>#{changes.difference}</span>")
                $attribute.html @model.attributes[attributeName]

                do ($bigNumber, changeClassName) ->
                    setTimeout (->
                        $bigNumber.find('a').removeClass(changeClassName)
                                  .find('changeDifference').remove().end()
                                  .find('.changeDifference').fadeOut(1500)
                    ), 2500

        @$('.chart .chart__data-point[title]').tooltip(placement: 'right')
        @captureLastState()

module.exports = StatusView
