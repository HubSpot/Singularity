View = require './view'

class FormBaseView extends View

    events: ->
        _.extend super,
            'change input':             'checkForm'
            'keyup input[type="text"]': 'checkForm'

            'submit form':              'submit'

            'click #reset-button':      'render'

    initialize: ->
        @checkForm = _.debounce @checkForm, 100

    render: ->
        @$el.html @template()
        @checkForm()

        # @$('#help-column').css 'height', "#{ @$('form').height() }px"

    checkForm: ->
        return if @lockdown
        @checkMultiInputs()

        # Make sure all the visible required fields are filled in
        requiredFieldsOkay = true
        for $group in @$ '.required'
            $field = $($group).children 'input'
            if $field.is(':visible') and not $field.val()
                requiredFieldsOkay = false

        $type = @$ '#type'
        if $type.length and not $type.find('.active').length
            requiredFieldsOkay = false

        if requiredFieldsOkay
            @$('button[type="submit"]').removeAttr 'disabled'
        else
            @$('button[type="submit"]').attr 'disabled', 'disabled'

    alert: (message, success = true) ->
        @$('.alert').remove()
        alertClass = if success then 'alert-success' else 'alert-danger'
        @$('form').append "<p class='alert #{ alertClass }'>#{ message }<p>"

    checkMultiInputs: ->
        for $container in @$el.find '.multi-input'
            $elements = $($container).children()

            $firstElement = $(_.first($elements))
            $lastElement  = $(_.last($elements))

            for $element in $elements
                $element = $ $element

                isntLast   = $element[0] isnt $lastElement[0]
                isntFirst  = $element[0] isnt $firstElement[0]
                notFocused = not $element.is ':focus'

                if not $element.val() and isntLast and notFocused
                    $element.remove()

            if $lastElement.val()
                $newElement = $ '<input type="text">'
                $newElement.attr 'class', $lastElement.attr 'class'
                $newElement.attr 'placeholder', $lastElement.attr 'placeholder'

                $elements.parent().append $newElement

module.exports = FormBaseView
