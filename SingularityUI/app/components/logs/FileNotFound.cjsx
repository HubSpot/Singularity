React = require 'react'

class FileNotFound extends React.Component
  render: ->
    <div className="lines-wrapper">
      <div className="empty-table-message">
        <p>{ @props.fileName } does not exist.</p>
      </div>
    </div>

module.exports = FileNotFound