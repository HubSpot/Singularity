Utils = require '../../utils'
Enums = require './Enums'

FormField = require './FormField'
DropDown = require './DropDown'

TaskSearch = React.createClass

    headerText: 'Search for Tasks'

    attributeOrEmptyString: (attr) ->
        if attr
            return attr
        else
            return ''

    getInitialState: ->
        return {
        }

    handleSubmit: (event) ->
        @setState {
            blah: 'You hit the button!'
        }


    render: ->
        <div>
            <h2> {@headerText} </h2>
            <form onSubmit={@handleSubmit}>
                <table ><tbody>
                    <FormField 
                        title = 'Request ID' 
                        initialValue = @props.defaultRequestId 
                        inputType = 'requestId'
                        disabled = @props.requestLocked />
                    <FormField 
                        title = 'Deploy ID' 
                        initialValue = @props.defaultDeployId 
                        inputType = 'deployId' />
                    <FormField 
                        title = 'Host' 
                        initialValue = @props.defaultHost 
                        inputType = 'host' />
                    <DropDown
                        forceChooseValue = false
                        choices = Enums.extendedTaskState()
                        inputType = 'lastTaskStatus'
                        title = 'Last Task Status' />
                    <FormField 
                        title = 'Started Before' 
                        initialValue = @props.defaultStartedBefore 
                        inputType = 'startedBefore' />
                    <FormField 
                        title = 'Started After' 
                        initialValue = @props.defaultStartedAfter 
                        inputType = 'startedAfter' />
                    <DropDown
                        forceChooseValue = true
                        defaultValue = 'ASC'
                        choices = Enums.sortDirections()
                        inputType = 'sortDirection'
                        title = 'Sort Direction' />
                </tbody></table>
                <button>Search</button>
            </form>
        </div>

module.exports = TaskSearch