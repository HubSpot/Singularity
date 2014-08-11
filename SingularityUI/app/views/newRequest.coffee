View = require './view'

class NewRequest extends View

    template: require '../templates/newRequest'

    events:
        'change input':             'checkForm'
        'keyup input[type="text"]': 'checkForm'

    initialize: ->
        @checkForm()

    render: ->
        @$el.html @template()

        @$('#help-column').css 'height', "#{ @$('form').height() }px"

    checkForm: ->
        id   = @$('#id').val()
        type = @$('#type .active input').val()

module.exports = NewRequest
