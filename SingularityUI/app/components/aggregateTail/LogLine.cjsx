
LogLine = React.createClass

  render: ->
    <div className="line #{if @props.highlighted then 'highlightLine'  else '' }">
      <a href="##{@props.offset}" className="offset-link"><span className="glyphicon glyphicon-link"></span> </a>
      <span>
        {@props.content}
      </span>
    </div>

module.exports = LogLine
