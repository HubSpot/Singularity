React = require 'react'
Table = require './Table'
TimeStamp = require './atomicDisplayItems/TimeStamp'
TaskStateLabel = require './atomicDisplayItems/TaskStateLabel'
Link = require './atomicDisplayItems/Link'

TaskTable = React.createClass

    render: ->
        taskTableColumns = ["Name", "Last State", "Deploy", "Started", "Updated"]
        taskTableData = []
        for task in @props.models
            taskTableData.push([
                {
                    component: Link
                    prop: {
                        text: task.taskId.id
                        url: "#{window.config.appRoot}/task/#{task.taskId.id}"
                        altText: task.taskId.id
                    }
                },
                {
                    component: TaskStateLabel
                    prop: {
                        taskState: task.lastTaskState
                    }
                },
                {
                    component: Link
                    prop: {
                        text: task.taskId.deployId
                        url: "#{window.config.appRoot}/request/#{task.taskId.requestId}/deploy/#{task.taskId.deployId}"
                        altText: task.taskId.deployId
                    }
                },
                {
                    component: TimeStamp
                    prop: {
                        timestamp: task.taskId.startedAt
                        display: 'timeStampFromNow'
                    } 
                },
                {
                    component: TimeStamp
                    prop: {
                        timestamp: task.updatedAt
                        display: 'timeStampFromNow'
                    }
                }
            ])
        return <Table
                    tableClassOpts="table-striped"
                    columnNames={taskTableColumns}
                    tableRows={taskTableData}
                />

module.exports = TaskTable
