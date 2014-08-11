View = require './view'

class NewDeployView extends View

    template: require '../templates/newDeploy'

    events:
        'change input':             'checkForm'
        'keyup input[type="text"]': 'checkForm'

    initialize: ({@requestId}) ->
        @checkForm()

    render: ->
        @$el.html @template {@requestId}

    checkForm: ->


module.exports = NewDeployView
