React = require 'react'
Utils = require '../../utils'
FormField = require '../common/formItems/FormField'

NewWebhookForm = React.createClass

    getInitialState: ->
        selected: ''
        uri: ''

    select: (selected) ->
        @setState {selected: selected}
        @props.setType selected

    updateUri: (event) ->
        @setState
            uri: event.target.value
        @props.setUri event.target.value

    buttons: ->
        buttons = []
        @props.webhookTypes.map (type, key) =>
            buttons.push (
                <button
                    key = {key}
                    data-type = {type}
                    className = "btn btn-default #{if @state.selected is type then 'active' else ''}"
                    onClick = {(event) =>
                        event.preventDefault()
                        @select type
                    }
                >
                    {Utils.humanizeText type}
                </button>
            )
        buttons

    alert: ->
        errors = @props.getErrors()
        return null unless errors and errors.length > 0
        formattedErrors = []
        if errors.length > 1
            errors.map (error, key) ->
                formattedErrors.push <li key={key}>{error}</li>
        <div className='alert alert-danger'>
            {if errors.length is 1 then errors[0] else <ul>{formattedErrors}</ul>}
        </div>

    render: ->
        <div>
            {@alert()}
            <h3> New Webhook </h3>
            <div className='form-group'>
                <label>Type</label>
                <br />
                {@buttons()}
            </div>
            <div className='form-group'>
                <label>URI</label>
                <FormField
                    id = 'uri'
                    prop = {{
                        placeholder: 'https://www.example.com/path/to/webhook'
                        inputType: 'text'
                        updateFn: @updateUri
                        value: @state.uri
                        required: true
                    }} />
            </div>
        </div>

module.exports = NewWebhookForm
