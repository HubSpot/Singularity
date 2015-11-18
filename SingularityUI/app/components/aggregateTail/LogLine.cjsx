
LogLine = React.createClass

  getClassNames: ->
    clazz = 'line'
    clazz += if @props.highlighted then ' highlightLine' else ''
    clazz += if @props.index is 0 then ' first-line' else ''
    clazz += if @props.index >= @props.totalLines - 1 then ' last-line' else ''
    clazz

  render: ->
    <div className="#{@getClassNames()}" style={backgroundColor: @props.color}>
      <a target="blank" href="#{@props.offsetLink}" className="offset-link" data-offset="#{@props.offset}">
        <div className="pre-line">
            <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </div>
      </a>
      <span>
        {@props.content}
      </span>
    </div>

module.exports = LogLine
