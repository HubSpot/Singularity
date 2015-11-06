
LogLine = React.createClass

  render: ->
    <div className="line #{if @props.highlighted then 'highlightLine'  else '' } #{if @props.index is 0 then 'first-line'  else '' }">
      <div className="pre-line">
        <a href="##{@props.offset}" className="offset-link" data-offset="#{@props.offset}" onClick={@props.highlight}>
          <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </a>
      </div>
      <span>
        {@props.content}
      </span>
    </div>

module.exports = LogLine
