React = require 'react'
ReactDOM = require 'react-dom'
Utils = require '../../utils'
Table = require '../common/Table'
PlainText = require '../common/atomicDisplayItems/PlainText'
TimeStamp = require '../common/atomicDisplayItems/TimeStamp'
Link = require '../common/atomicDisplayItems/Link'
Glyphicon = require '../common/atomicDisplayItems/Glyphicon'
vex = require 'vex'

Webhooks = React.createClass

    defaultRowsPerPage: 10

    rowsPerPageChoices: [10, 20, 30, 40]

    webhookColumns: [
        {
            data: 'URI'
        },
        {
            data: 'Type'
        },
        {
            data: 'Timestamp'
            className: 'hidden-xs'
        },
        {
            data: 'User'
            className: 'hidden-xs'
        },
        {
            data: 'Queue Size'
        },
        {
            className: 'hidden-xs'
        },
        {
            className: 'hidden-xs'
        }
    ]

    newWebhook: () =>
        #c#onsole.log "new webhook"

    editWebhook: (webhook) =>
        #c#onsole.log "edit #{webhook.attributes.webhook.id}"

    deleteWebhook: (webhook) =>
        #c#onsole.log "Delete Webhook"

    promptDeleteWebhook: (webhook) =>
        deleteWebhook = @deleteWebhook
        newWebhook = @newWebhook
        vex.dialog.confirm
            message: "<div class='delete-webhook' />" # This is not react
            afterOpen: =>
                ReactDOM.render(
                    <button
                        className = 'btn btn-success'
                        alt = "Create a new webhook"
                        title = "newWebhook"
                        onClick = { (event) =>
                            event.preventDefault()
                            newWebhook()
                        }> New Webhook </button>,
                    $(".delete-webhook").get(0)
                )
            callback: (confirmed) =>
                return unless confirmed
                deleteWebhook webhook

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
                        text: webhook.attributes.webhook.user
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
                            iconClass = 'edit'
                        />
                        onClickFn: => @editWebhook(webhook)
                        title: 'Edit'
                        altText: "Edit this webhook"
                        overlayTrigger: true
                        overlayTriggerPlacement: 'top'
                        overlayToolTipContent: 'Edit This Webhook'
                        overlayId: "editWebhook#{webhook.attributes.webhook.id}"
                    }
                },
                {
                    component: Link
                    className: 'hidden-xs actions-column'
                    prop: {
                        text: <Glyphicon
                            iconClass = 'trash'
                        />
                        onClickFn: => @promptDeleteWebhook(webhook)
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
                        onClick = {@newWebhook}> New Webhook </button>
                </div>
            </div>
            <Table 
                defaultRowsPerPage = {@defaultRowsPerPage}
                rowsPerPageChoices = {@rowsPerPageChoices}
                tableClassOpts = "table-striped"
                columnHeads = {@webhookColumns}
                tableRows = {@getWebhookTableData()}
                emptyTableMessage = 'No Webhooks'
                dataCollection = 'webhooks'
            />
        </div>

module.exports = Webhooks
