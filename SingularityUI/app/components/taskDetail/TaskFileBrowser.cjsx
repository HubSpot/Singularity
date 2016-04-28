React = require 'react'
Table = require '../common/Table'
Link = require '../common/atomicDisplayItems/Link'
PlainText = require '../common/atomicDisplayItems/PlainText'
Glyphicon = require '../common/atomicDisplayItems/Glyphicon'
TimeStamp = require '../common/atomicDisplayItems/TimeStamp'
Utils = require '../../utils'

TaskFileBrowser = React.createClass

    sortBy: (field, sortDirectionAscending) ->
        @props.collection.sortBy field, sortDirectionAscending
        @forceUpdate()

    columns: ->
        sortBy = @sortBy # JS is annoying
        [
            {
                data: 'Name'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'name', sortDirectionAscending
            },
            {
                data: 'Size'
                className: 'hidden-xs'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'size', sortDirectionAscending
            },
            {
                data: 'Last Modified'
                className: 'hidden-xs'
                sortable: true
                doSort: (sortDirectionAscending) => sortBy 'mtime', sortDirectionAscending
            },
            {
                className: 'hidden-xs'
            }
        ]

    tableData: ->
        tableData = []
        @props.collection.map (file) =>
            FileNameComponent = if file.attributes.isDirectory or file.attributes.isTailable then Link else PlainText
            if file.attributes.isDirectory
                url = "#{ config.appRoot}/task/#{ file.attributes.taskId }/files/#{ file.attributes.uiPath }"
            else 
                url = "#{ config.appRoot }/task/#{ @props.task.taskId }/tail/#{ Utils.substituteTaskId file.attributes.uiPath, @props.task.taskId}"
            size = if file.attributes.isDirectory then '' else Utils.humanizeFileSize file.attributes.size
            row = {
                dataId: file.attributes.name
                dataCollection: 'files'
                data: [
                    {
                        component: FileNameComponent
                        prop: {
                            text: <span><Glyphicon
                                iconClass = {if file.attributes.isDirectory then 'folder-open' else 'file'}
                            /> {file.attributes.name}</span>
                            url: url
                        }
                    },
                    {
                        component: PlainText
                        className: 'hidden-xs'
                        prop: {
                            text: size
                        }
                    },
                    {
                        component: TimeStamp
                        className: 'hidden-xs'
                        prop: {
                            timestamp: file.attributes.mtime
                            display: 'absoluteTimestamp'
                        }
                    }
                ]
            }
            if file.attributes.isDirectory
                row.data.push {
                    component: PlainText
                    prop: {
                        text: ''
                    }
                }
            else
                row.data.push {
                    component: Link
                    className: 'hidden-xs actions-column'
                    prop: {
                        text: <Glyphicon
                            iconClass = 'download-alt'
                        />
                        url: file.attributes.downloadLink
                        title: 'Download'
                        altText: "Download #{file.attributes.name}"
                        overlayTrigger: true
                        overlayTriggerPlacement: 'top'
                        overlayToolTipContent: "Download #{file.attributes.name}"
                        overlayId: "downloadFile#{file.attributes.name}"
                    }
                }
            tableData.push row
        tableData

    emptyTableMessage: () ->
        {task, slaveOffline} = @props
        emptyTableMessage = 'No files exist in task directory.'

        if task.get('taskUpdates') and task.get('taskUpdates').length > 0
            switch _.last(task.get('taskUpdates')).taskState
                when 'TASK_LAUNCHED', 'TASK_STAGING', 'TASK_STARTING' then emptyTableMessage = 'Could not browse files. The task is still starting up.'
                when 'TASK_KILLED', 'TASK_FAILED', 'TASK_LOST', 'TASK_FINISHED' then emptyTableMessage = 'No files exist in task directory. It may have been cleaned up.'

        emptyTableMessage = "Task files are not availible because #{@props.task.attributes.task.taskId.sanitizedHost} is offline." if slaveOffline
        return emptyTableMessage

    navigate: (path, event) ->
        event.preventDefault()

        $table = $ 'table'
        # Get table height for later
        if $table.length
            tableHeight = $table.height()

        @props.collection.path = "#{ path }"

        app.router.navigate "#task/#{ @props.collection.taskId }/files/#{ @props.collection.path }"

        @props.collection.fetch
            reset: true
            done: @forceUpdate()

        @setState {scrollWhenReady: true}

        $loaderContainer = @$ '.page-loader-container'
        if tableHeight?
            $loaderContainer.css 'height', "#{ tableHeight }px"

    renderBreadcrumbs: ->
        breadcrumbs = []
        @props.breadcrumbs.map (breadcrumb, key) =>
            breadcrumbs.push <li key={key}>
                <a onClick={(event) => @navigate breadcrumb.path, event}>{ breadcrumb.name }</a>
            </li>
        breadcrumbs

    renderTable: ->
        <Table 
            noPages = {true}
            tableClassOpts = "table-striped files-table sortable-theme-bootstrap"
            columnHeads = {@columns()}
            tableRows = {@tableData()}
            emptyTableMessage = {@emptyTableMessage()}
            dataCollection = 'taskFiles'
        />

    renderBreadcrumbsAndTable: ->
        if @props.task.slaveMissing
            <div className="empty-table-message">
                Files can not be fetched because the slave is no longer available
            </div>
        else
            <div>
                <ul className="breadcrumb">
                    {@renderBreadcrumbs()}
                </ul>
                {@renderTable()}
            </div>

    render: ->
        <div>
            <div className="page-header file-browser-header">
                <h2>Files</h2>
            </div>
            {@renderBreadcrumbsAndTable()}
        </div>

module.exports = TaskFileBrowser
