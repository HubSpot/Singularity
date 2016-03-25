React = require 'react'
classNames = require 'classnames'
Utils = require '../../../utils'

TaskStateLabel = React.createClass

    getLabelClass: ->
        Utils.getLabelClassFromTaskState @props.prop.taskState

    getClass: ->
        return classNames 'label', "label-#{@getLabelClass()}", @props.prop.className

    render: ->
        <span className={@getClass()} label="Task State: #{Utils.humanizeText @props.prop.taskState}">
            {Utils.humanizeText @props.prop.taskState}
        </span>

module.exports = TaskStateLabel
