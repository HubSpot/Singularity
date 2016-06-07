React = require 'react'

class FileNotFound extends React.Component
  render: ->
    <div className="lines-wrapper">
      <div className="empty-table-message">
        <p>{ _.last @props.fileName.split('/') } does not exist
        {if @props.fileName and @props.fileName.indexOf('$TASK_ID') isnt -1 then " in this task's directory" else ' for this task'}.</p>
      </div>
    </div>

module.exports = FileNotFound
