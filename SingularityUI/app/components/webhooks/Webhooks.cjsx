React = require 'react'
ReactDOM = require 'react-dom'
Utils = require '../../utils'
Table = require '../common/Table'
PlainText = require '../common/atomicDisplayItems/PlainText'
TimeStamp = require '../common/atomicDisplayItems/TimeStamp'
Link = require '../common/atomicDisplayItems/Link'
Glyphicon = require '../common/atomicDisplayItems/Glyphicon'
NewWebhookForm = require './NewWebhookForm'
vex = require 'vex'

Webhooks = React.createClass

    defaultRowsPerPage: 10

    rowsPerPageChoices: [10, 20, 30, 40]

    webhookTypes: ['REQUEST', 'DEPLOY', 'TASK']

    sortBy: (field, sortDirectionAscending) ->
        @props.collections.webhooks.sortBy field, sortDirectionAscending
        @forceUpdate()

    webhookColumns: ->
        sortBy = @sortBy # JS is annoying
        [
            {
                data: 'URL'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'uri', sortDirectionAscending
            },
            {
                data: 'Type'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'type', sortDirectionAscending
            },
            {
                data: 'Timestamp'
                className: 'hidden-xs'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'timestamp', sortDirectionAscending
            },
            {
                data: 'User'
                className: 'hidden-xs'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'user', sortDirectionAscending
            },
            {
                data: 'Queue Size'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'queueSize', sortDirectionAscending
            },
            {
                className: 'hidden-xs'
            }
        ]

    deleteWebhook: (webhook) ->
        $.ajax
            url: "#{ config.apiRoot }/webhooks/?webhookId=#{webhook.attributes.webhook.id}"
            type: "DELETE"
            success: () => @props.collections.webhooks.fetch().done => @forceUpdate()

    promptDeleteWebhook: (webhook) ->
        deleteWebhook = (webhook) => @deleteWebhook webhook
        vex.dialog.confirm
            message: "<div class='delete-webhook' />" # This is not react
            afterOpen: =>
                ReactDOM.render(
                    <div>
                        <pre>({webhook.attributes.webhook.type}) {webhook.attributes.webhook.uri}</pre>
                        <p>Are you sure you want to delete this webhook?</p>
                    </div>,
                    $(".delete-webhook").get(0)
                )
            callback: (confirmed) =>
                return unless confirmed
                deleteWebhook webhook

    newWebhook: (uri, type) ->
        data = {
            uri: uri
            type: type
        }
        data.user = app.user.attributes.user.id if app.user.attributes.authenticated
        $.ajax
            url: "#{ config.apiRoot }/webhooks"
            type: "POST"
            contentType: 'application/json'
            data: JSON.stringify data
            success: () => @props.collections.webhooks.fetch().done => @forceUpdate()

    promptNewWebhook: ->
        newWebhook = (uri, type) => @newWebhook uri, type
        vex.dialog.open
            message: "<div class='new-webhook' />"
            afterOpen: =>
                @validateInput = (input) =>
                    try
                        new URL input
                        return true
                    catch err
                        return false
                @renderedForm = ReactDOM.render(
                    <NewWebhookForm
                        getErrors = {() => @errors}
                        webhookTypes = {@webhookTypes}
                        setType = {(selected) => @type = selected} 
                        setUri = {(uri) => @uri = uri} />,
                    $(".new-webhook").get(0)
                )
            beforeClose: =>
                return true unless @data
                @errors = []
                uriValidated = @validateInput @uri
                @errors.push 'Please select a type' unless @type
                @errors.push 'Invalid URL entered' unless uriValidated
                @renderedForm.forceUpdate() unless uriValidated and @type
                return false unless uriValidated
                return false unless @type
                @type = ''
                @uri = ''
                return true
            callback: (data) =>
                @data = data
                return unless @type and data and @validateInput @uri
                type = @type
                newWebhook @uri, type

    getWebhookTableData: ->
        data = []
        @props.collections.webhooks.map (webhook) => data.push ({
            dataId: webhook.attributes.webhook.id
            dataCollection: 'webhooks'
            data: [
                {
                    component: PlainText
                    prop: {
                        text: webhook.attributes.webhook.uri
                    }
                },
                {
                    component: PlainText
                    prop: {
                        text: Utils.humanizeText webhook.attributes.webhook.type
                    }
                },
                {
                    component: TimeStamp
                    className: 'hidden-xs'
                    prop: {
                        timestamp: webhook.attributes.webhook.timestamp
                        display: 'absoluteTimestamp'
                    }
                },
                {
                    component: PlainText
                    className: 'hidden-xs'
                    prop: {
                        text: webhook.attributes.webhook.user or 'N/A'
                    }
                },
                {
                    component: PlainText
                    prop: {
                        text: <b>{webhook.attributes.queueSize}</b>
                    }
                },
                {
                    component: Link
                    className: 'hidden-xs actions-column'
                    prop: {
                        text: <Glyphicon
                            iconClass = 'trash'
                        />
                        onClickFn: => @promptDeleteWebhook webhook
                        title: 'Delete'
                        altText: "Delete this webhook"
                        overlayTrigger: true
                        overlayTriggerPlacement: 'top'
                        overlayToolTipContent: 'Delete This Webhook'
                        overlayId: "deleteWebhook#{webhook.attributes.webhook.id}"
                    }
                }
            ]
        })
        data

    render: ->
        <div>
            <div className='row'>
                <div className='col-md-10'>
                    <span className='h1'>Webhooks</span>
                </div>
                <div className='col-md-2 button-container'>
                    <button
                        className = 'btn btn-success'
                        alt = "Create a new webhook"
                        title = "newWebhook"
                        onClick = {@promptNewWebhook}> New Webhook </button>
                </div>
            </div>
            <Table 
                defaultRowsPerPage = {@defaultRowsPerPage}
                rowsPerPageChoices = {@rowsPerPageChoices}
                tableClassOpts = "table-striped"
                columnHeads = {@webhookColumns()}
                tableRows = {@getWebhookTableData()}
                emptyTableMessage = 'No Webhooks'
                dataCollection = 'webhooks'
            />
        </div>

module.exports = Webhooks
