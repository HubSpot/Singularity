
Settings = React.createClass

  handleClick: (e) ->
    setting = $(e.target).attr 'data-setting'
    if setting is 'lineNumbers'
      @props.toggleLineNumbers()
    if setting is 'lineColors'
      @props.toggleLineColors()

  render: ->
    <div className="btn-group">
      <button type="button" className="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-cog"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu settings-dropdown">
        <li className="#{if @props.lineNumbers then 'checked'}" onClick={@handleClick}><a data-setting="lineNumbers">Show line numbers</a></li>
        <li className="#{if @props.lineColors then 'checked'}" onClick={@handleClick}><a data-setting="lineColors">Color code by task</a></li>
      </ul>
    </div>

module.exports = Settings
