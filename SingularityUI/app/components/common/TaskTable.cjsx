React = require 'react'
Table = require './Table'
TimeStamp = require './atomicDisplayItems/TimeStamp'
TaskStateLabel = require './atomicDisplayItems/TaskStateLabel'
Link = require './atomicDisplayItems/Link'
IconButton = require './atomicDisplayItems/IconButton'
Glyphicon = require './atomicDisplayItems/Glyphicon'
PlainText = require './atomicDisplayItems/PlainText'
Utils = require '../../utils'

TaskTable = React.createClass

    ### 
    NOTE: @props.sortByX, if provided, should do at least three things:
        - explicitly set @props.sortDirection
        - explicitly set @props.sortBy
        - sort @props.models
    ###
    render: ->
        taskTableColumns = [
            {}, 
            {
                data: 'Request ID'
                className: 'hidden-sm hidden-xs'
                sortable: @props.sortableByRequestId
                doSort: @props.sortByRequestId
                sortAttr: 'requestId'
            }, 
            {
                data: 'Deploy ID'
                className: 'hidden-sm hidden-xs'
                sortable: @props.sortableByDeployId
                doSort: @props.sortByDeployId
                sortAttr: 'deployId'
            }, 
            {
                data: 'Host'
                className: 'hidden-sm hidden-xs'
                sortable: @props.sortableByHost
                doSort: @props.sortByHost
                sortAttr: 'host'
            },
            {
                data: 'Last Status'
                className: 'hidden-sm hidden-xs'
                sortable: @props.sortableByLastStatus
                doSort: @props.sortByLastStatus
                sortAttr: 'lastTaskState'
            },
            {
                data: 'Started'
                className: 'hidden-sm hidden-xs'
                sortable: @props.sortableByStarted
                doSort: @props.sortByStarted
                sortAttr: 'startedAt'
            },
            {
                data: 'Updated'
                className: 'hidden-xs'
                sortable: @props.sortableByUpdated
                doSort: @props.sortByUpdated
                sortAttr: 'updatedAt'
            },
            {
                className: 'hidden-xs'
            },
            {
                className: 'hidden-xs'
            }
        ]
        taskTableData = []
        @props.models.map (task) ->
            viewJsonFn = (event) -> Utils.viewJSON task
            taskTableData.push({
                dataId: task.taskId.id
                dataCollection: 'taskHistory'
                data: [
                    {
                        component: Link
                        className: 'actions-column'
                        prop: {
                            text: <Glyphicon
                                iconClass = 'link'
                            />
                            url: "#{window.config.appRoot}/task/#{task.taskId.id}"
                            altText: "Task #{task.taskId.id}"
                            title: "Go To Task"
                        }
                    },
                    {
                        component: Link
                        className: 'hidden-sm hidden-xs'
                        prop: {
                            text: task.taskId.requestId
                            url: "#{window.config.appRoot}/request/#{task.taskId.requestId}/"
                            altText: "Request #{task.taskId.requestId}"
                        }
                    },
                    {
                        component: Link
                        className: 'hidden-sm hidden-xs'
                        prop: {
                            text: task.taskId.deployId
                            url: "#{window.config.appRoot}/request/#{task.taskId.requestId}/deploy/#{task.taskId.deployId}"
                            altText: "Deploy #{task.taskId.deployId}"
                        }
                    },
                    {
                        component: PlainText
                        className: 'hidden-sm hidden-xs'
                        prop: {
                            text: task.taskId.host
                        }
                    },
                    {
                        component: TaskStateLabel
                        className: 'hidden-sm hidden-xs'
                        prop: {
                            taskState: task.lastTaskState
                        }
                    },
                    {
                        component: TimeStamp
                        className: 'hidden-sm hidden-xs'
                        prop: {
                            timestamp: task.taskId.startedAt
                            display: 'timeStampFromNow'
                        } 
                    },
                    {
                        component: TimeStamp
                        className: 'hidden-xs'
                        prop: {
                            timestamp: task.updatedAt
                            display: 'timeStampFromNow'
                        }
                    },
                    {
                        component: Link
                        className: 'hidden-xs actions-column'
                        prop: {
                            text: <Glyphicon
                                iconClass = 'option-horizontal'
                            />
                            url: "#{window.config.appRoot}/request/#{task.taskId.requestId}/tail/stdout/?taskIds=#{task.taskId.id}"
                            title: 'Log'
                            altText: "Logs for task #{task.taskId.id}"
                        }
                    },
                    {
                        component: Link
                        className: 'hidden-xs actions-column'
                        prop: {
                            text: '{ }'
                            url: '#'
                            title: 'JSON'
                            onClickFn: viewJsonFn
                            altText: "View JSON for task #{task.taskId.id}"
                        }
                    }
            ]})
        return <Table
                    tableClassOpts = 'table-striped'
                    columnHeads = {taskTableColumns}
                    tableRows = {taskTableData}
                    sortDirection = @props.sortDirection
                    sortDirectionAscending = @props.sortDirectionAscending
                    sortBy = @props.sortBy
                    emptyTableMessage = {@props.emptyTableMessage or 'No Tasks'}
                    customSorting = true
                    customPaging = true
                    rowsPerPageChoices = @props.rowsPerPageChoices
                    setRowsPerPage = @props.setRowsPerPage
                    pageNumber = @props.pageNumber
                    pageDown = @props.pageDown
                    pageUp = @props.pageUp
                />

module.exports = TaskTable
