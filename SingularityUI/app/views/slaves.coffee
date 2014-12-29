View = require './view'

Slave = require '../models/Slave'

class SlavesView extends View

    template: require '../templates/slaves/base'

    events: =>
        _.extend super,
            'click [data-action="remove"]': 'removeSlave'

    render: ->
        @$el.html @template()

        @$('#slaves').html           @subviews.slaves.$el

    removeSlave: (event) =>
        $target = $(event.currentTarget)
        state = $target.data 'state'
        slaveModel = new Slave
            id:    $target.data 'slave-id'
            state: state

        slaveModel.promptRemove => @trigger 'refreshrequest'


module.exports = SlavesView
