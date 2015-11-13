
LogLine = React.createClass

  getClassNames: ->
    clazz = 'line'
    clazz += if @props.highlighted then ' highlightLine' else ''
    clazz += if @props.index is 0 then ' first-line' else ''
    clazz += if @props.index >= @props.totalLines - 1 then ' last-line' else ''
    clazz

  render: ->
    <div className="#{@getClassNames()}">
      <div className="pre-line">
        <a href="##{@props.offset}" className="offset-link" data-offset="#{@props.offset}" onClick={@props.highlight}>
          <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </a>
      </div>
      <span>
        {@props.index} | {@props.content}
      </span>
    </div>

module.exports = LogLine
