React = require 'react'
FormField = require '../common/formItems/FormField'

NewWebhookForm = React.createClass

    getInitialState: ->
        selected: @props.defaultSelectedType
        uri: ''

    select: (selected) ->
        @setState {selected: selected}
        @props.selectVex selected

    updateUri: (event) ->
        @setState
            uri: event.target.value
        @props.setUri event.target.value

    render: ->
        <div>
            <div className='form-group'>
                <label>Type</label>
                <br />
                <button
                    data-type = "REQUEST" 
                    className = "btn btn-default #{if @state.selected is 'REQUEST' then 'active' else ''}"
                    onClick = {(event) =>
                        event.preventDefault()
                        @select 'REQUEST'
                    }
                >
                    Request
                </button>
                <button 
                    data-type = "DEPLOY" 
                    className = "btn btn-default #{if @state.selected is 'DEPLOY' then 'active' else ''}"
                    onClick = {(event) =>
                        event.preventDefault()
                        @select 'DEPLOY'
                    }
                >
                    Deploy
                </button>
                <button 
                    data-type = "TASK" 
                    className = "btn btn-default #{if @state.selected is 'TASK' then 'active' else ''}"
                    onClick = {(event) =>
                        event.preventDefault()
                        @select 'TASK'
                    }
                >
                    Task
                </button>
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
