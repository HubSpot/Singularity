EmptyTableMsg = React.createClass
  
  displayName: 'EmptyTableMsg'

  propTypes:
    msg: React.PropTypes.string.isRequired

  render: ->
    return (
      <div className="empty-table-message">
        <p>{@props.msg}</p>
      </div>
    )

module.exports = EmptyTableMsg