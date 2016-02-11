Utils = require '../../utils'

Form = require '../common/Form'
FormField = require '../common/atomicFormItems/FormField'
DropDown = require '../common/atomicFormItems/DropDown'
DateEntry = require '../common/atomicFormItems/DateEntry'
Enums = require './Enums'
Header = require './Header'

TaskSearchForm = React.createClass

    updateCount: (event) ->
        @props.updateCount(event.target.value)

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
                title: 'Started Before'
                id: 'startedBefore'
                prop: {
                    value: @props.startedBefore
                    inputType: 'date'
                    updateFn: @props.updateStartedBefore
                }
            },
            {
                component: DateEntry
                title: 'Started After'
                id: 'startedAfter'
                prop: {
                    value: @props.startedAfter
                    inputType: 'date'
                    updateFn: @props.updateStartedAfter
                }
            },
            {
                component: DropDown
                title: 'Sort Direction'
                id: 'sortDirection'
                prop: {
                    value: @props.sortDirection
                    inputType: 'text'
                    choices: Enums.sortDirections()
                    forceChooseValue: true
                    updateFn: @props.updateSortDirection
                }
            },
            {
                component: DropDown
                title: 'Tasks Per Page'
                id: 'count'
                prop: {
                    value: @props.count
                    choices: @props.countChoices
                    inputType: 'number'
                    forceChooseValue: true
                    updateFn: @updateCount
                }
            }
        ]

    render: ->
        <div className='col-xs-5'>
        <Header
            global = @props.global
            requestId = @props.requestId
        />
        <Form
            formGroups = {@getFormGroups()}
            submitButtonText = "Search"
            resetForm = {@props.resetForm}
            handleSubmit = {@props.handleSubmit}
        />
        </div>

module.exports = TaskSearchForm
