React = require 'react'
Utils = require '../../../utils'

TaskStateLabel = React.createClass

    getLabelClass: ->
        switch @props.prop.taskState
            when 'TASK_STARTING', 'TASK_CLEANING'
                'warning'
            when 'TASK_STAGING', 'TASK_LAUNCHED', 'TASK_RUNNING'
                'info'
            when 'TASK_FINISHED'
                'success'
            when 'TASK_KILLED', 'TASK_LOST', 'TASK_FAILED', 'TASK_LOST_WHILE_DOWN'
                'danger'
            else
                'default'

    getClass: ->
        return 'label label-' + @getLabelClass()

    render: ->
        <span className={@getClass()}>
            {Utils.humanizeText @props.prop.taskState}
        </span>

module.exports = TaskStateLabel
