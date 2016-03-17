View = require './view'
select2 = require 'select2'

class FormBaseView extends View

    events: ->
        _.extend super,
            'change input':             'checkForm'
            'keyup input[type="text"]': 'checkForm'
            'blur input[type="text"]':  'checkForm'

            'submit form':              'submit'

            'click #reset-button':      'render'

    initialize: ->
        @checkForm = _.debounce @checkForm, 100

    render: (event) ->
        event.preventDefault() if event
        
        @$el.html @template
            config: config
            model: @model?.toJSON()
        
        @checkForm()
        @afterRender()

        # @$('#help-column').css 'height', "#{ @$('form').height() }px"

    afterRender: ->
        return
        
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

        $button = @$ 'button[type="submit"]'
        if requiredFieldsOkay
            $button.removeAttr 'disabled'
            $button.parent().attr 'title', undefined
            $button.parent().tooltip 'destroy'
        else
            $button.attr 'disabled', 'disabled'
            $button.parent().attr 'title', 'Please fill in all the required fields'
            $button.parent().tooltip
                placement: 'right'
            
    alert: (message, success = true) ->
        @$("[data-alert-location='form']").remove()
        alertClass = if success then 'alert-success' else 'alert-danger'
        @$('form').append "<p data-alert-location='form' class='alert #{ alertClass }'>#{ message }<p>"

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

    valOrNothing: (selector, $parentElement) =>
        # Returns value if element is visible and, if it's a string-based
        # input, not blank
        if $parentElement?
            $element = $parentElement.find selector
        else
            $element = @$ selector
        return undefined unless $element.is ':visible'

        if $element.attr('type') is 'checkbox'
            return $element.is ':checked'
        else
            val = $element.val()
            return val if val
            return if $element.parents('.required').length then "" else undefined
    
    base64Encode: (content) -> 
        return if not content?
        btoa content

    multiMap: (selector) =>
        $elements = @$ selector
        return undefined unless $elements.is ':visible'

        output = {}

        for $element in $elements
            $element = $ $element

            pair = $element.val().split '='
            # Not a valid pair
            continue if pair.length < 2
            # In case there were multiple `=`s
            pair[1] = pair.slice(1, pair.length) if pair.length > 2
            # Slap it onto our map
            output[pair[0]] = pair[1]

        return if _.isEmpty output then undefined else output

    multiList: (selector) =>
        $elements = @$ selector
        return undefined unless $elements.is ':visible'

        output = []

        for $element in $elements
            $element = $ $element

            val = $element.val()
            output.push val if val

        return if _.isEmpty output then undefined else output

    generateSelectBox: (selector, options={}) ->
        @$(selector).select2(options)

    setSelect2Val: (selector, value) =>
        @$(selector).select2("val", value)

    getSelect2Val: (selector) =>
        @$(selector).select2("val")

    # renders select2 taggable list
    renderTaggable: (selector, data) ->
        data = _.map data, (val) -> id: val, text: val
        selector.select2 'data', data

    postSave: ->
        @lockdown = false
        @checkForm()
        @$('#reset-button').removeClass 'hide'

module.exports = FormBaseView
