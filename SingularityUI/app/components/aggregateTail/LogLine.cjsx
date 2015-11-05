
LogLine = React.createClass

  renderLineNumber: ->
    if @props.lineNumbers
      <span className="line-number">
        {@props.index}
      </span>
    else
      <span className="line-number"></span>

  render: ->
    <div className="line #{if @props.highlighted then 'highlightLine'  else '' } #{if @props.index is 0 then 'first-line'  else '' }">
      <div className="pre-line">
        <a href="##{@props.offset}" className="offset-link" data-offset="#{@props.offset}" onClick={@props.highlight}>
          <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </a>
        {@renderLineNumber()}
      </div>
      <span>
        {@props.content}
      </span>
    </div>

module.exports = LogLine
