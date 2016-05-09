React = require 'react'
Utils = require '../../utils'

FormField = require '../common/formItems/FormField'
DropDown = require '../common/formItems/DropDown'
DateEntry = require '../common/formItems/DateEntry'
LinkedFormItem = require '../common/formItems/LinkedFormItem'
Enums = require './Enums'

TaskSearchForm = React.createClass

    getRequestIdTitle: ->
        <div>
            Request ID
            {<span className='badge current-query-param'>{@props.requestIdCurrentSearch}</span> if @props.requestIdCurrentSearch}
        </div>

    getDeployIdTitle: ->
        <div>
            Deploy ID
            {<span className='badge current-query-param'>{@props.deployIdCurrentSearch}</span> if @props.deployIdCurrentSearch}
        </div>

    getHostTitle: ->
        <div>
            Host
            {<span className='badge current-query-param'>{@props.hostCurrentSearch}</span> if @props.hostCurrentSearch}
        </div>

    getStartedBetweenTitle: ->
        <div>
            Started Between
            {if @props.startedAfterCurrentSearch and @props.startedBeforeCurrentSearch
                <span className='badge current-query-param'>{@props.startedAfterCurrentSearch} - {@props.startedBeforeCurrentSearch}</span>
            else if @props.startedAfterCurrentSearch
                <span className='badge current-query-param'>After {@props.startedAfterCurrentSearch}</span>
            else if @props.startedBeforeCurrentSearch
                <span className='badge current-query-param'>Before {@props.startedBeforeCurrentSearch}</span>}
        </div>

    getLastTaskStatusTitle: ->
        <div>
            Last Task Status
            {<span className='badge current-query-param'>{@props.lastTaskStatusCurrentSearch}</span> if @props.lastTaskStatusCurrentSearch}
        </div>

    render: ->
        render: ->
        <div className='jumbotron col-md-12'>
            <form role='form' onSubmit={@props.handleSubmit} className='form-vertical'>
                <div className='row'>
                    <div className='col-md-4'>
                        <label htmlFor='requestId'>{@getRequestIdTitle()}</label>
                        <FormField
                            title = @getRequestIdTitle()
                            id = 'requestId'
                            prop = {{
                                value: @props.requestId
                                inputType: 'text'
                                disabled: not @props.global
                                updateFn: @props.updateReqeustId
                            }}
                        />
                    </div>
                    <div className='col-md-4'>
                        <label htmlFor='deployId'>{@getDeployIdTitle()}</label>
                        <FormField
                            title = @getDeployIdTitle()
                            id = 'deployId'
                            prop = {{
                                value: @props.deployId
                                inputType: 'text'
                                updateFn: @props.updateDeployId
                            }}
                        />
                    </div>
                    <div className='col-md-4'>
                        <label htmlFor='host'>{@getHostTitle()}</label>
                        <FormField
                            title = @getHostTitle()
                            id = 'host'
                            prop = {{
                                value: @props.host
                                inputType: 'text'
                                updateFn: @props.updateHost
                            }}
                        />
                    </div>
                </div>
                <div className='row'>
                    <div className='col-md-4'>
                        <label htmlFor='executionStartedBetween'>{@getStartedBetweenTitle()}</label>
                        <LinkedFormItem
                            id = 'executionStartedBetween'
                            title = @getStartedBetweenTitle()
                            prop = {{
                                customClass: 'form-inline'
                                formItem1:
                                    component: DateEntry
                                    title: 'Started After'
                                    id: 'startedAfter'
                                    prop:
                                        customClass: 'col-xs-5 pull-left'
                                        value: @props.startedAfter
                                        inputType: 'datetime'
                                        updateFn: @props.updateStartedAfter
                                separator: '-'
                                formItem2:
                                    component: DateEntry
                                    title: 'Started Before'
                                    id: 'startedBefore'
                                    prop:
                                        customClass: 'col-xs-5 pull-right'
                                        value: @props.startedBefore
                                        inputType: 'datetime'
                                        updateFn: @props.updateStartedBefore
                            }}
                        />
                    </div>
                    <div className='col-md-4'>
                        <label htmlFor='lastTaskStatus'>{@getLastTaskStatusTitle()}</label>
                        <DropDown
                            id = 'lastTaskStatus'
                            prop = {{
                                value: @props.lastTaskStatus
                                choices: Enums.extendedTaskState()
                                inputType: 'text'
                                updateFn: @props.updateLastTaskStatus
                            }}
                        />
                    </div>
                    <label htmlFor='buttons'>&nbsp;</label>
                    <div className='col-md-4' id='buttons'>
                        <div className='pull-right'>
                            <div className='col-md-3'>
                                <button type='submit' className='btn btn-primary'>Search</button>
                            </div>
                        </div>
                        <div className='pull-right'>
                            <div className='col-md-3'>
                                <button type='button' className='btn btn-danger' onClick=@props.resetForm>Clear</button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>

module.exports = TaskSearchForm
