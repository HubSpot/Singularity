Utils = require '../../utils'

Form = require '../common/Form'
FormField = require '../common/formItems/FormField'
DropDown = require '../common/formItems/DropDown'
DateEntry = require '../common/formItems/DateEntry'
Enums = require './Enums'

TaskSearchForm = React.createClass

    getFormGroups: ->
        [
            {
                component: FormField
                title: 'Request ID'
                id: 'requestId'
                prop: {
                    value: @props.requestId
                    inputType: 'text'
                    disabled: not @props.global
                    updateFn: @props.updateReqeustId
                }
            },
            {
                component: FormField
                title: 'Deploy ID'
                id: 'deployId'
                prop: {
                    value: @props.deployId
                    inputType: 'text'
                    updateFn: @props.updateDeployId
                }
            },
            {
                component: FormField
                title: 'Host'
                id: 'host'
                prop: {
                    value: @props.host
                    inputType: 'text'
                    updateFn: @props.updateHost
                }
            },
            {
                component: DropDown
                title: 'Last Task Status'
                id: 'lastTaskStatus'
                prop: {
                    value: @props.lastTaskStatus
                    forceChooseValue: false
                    choices: Enums.extendedTaskState()
                    inputType: 'text'
                    updateFn: @props.updateLastTaskStatus
                }
            },
            {
                component: DateEntry
                title: 'Started After'
                id: 'startedAfter'
                prop: {
                    value: @props.startedAfter
                    inputType: 'datetime'
                    updateFn: @props.updateStartedAfter
                }
            },
            {
                component: DateEntry
                title: 'Started Before'
                id: 'startedBefore'
                prop: {
                    value: @props.startedBefore
                    inputType: 'datetime'
                    updateFn: @props.updateStartedBefore
                }
            }
        ]

    render: ->
        <div className='col-xs-5'>
            <Form
                formGroups = {@getFormGroups()}
                submitButtonText = "Search"
                resetForm = {@props.resetForm}
                additionalButton = {{
                    functionality: @props.resetFormToCurrentParams
                    buttonStyle: 'danger'
                    text: 'Revert To Query Params'
                }}
                handleSubmit = {@props.handleSubmit}
            />
        </div>

module.exports = TaskSearchForm
