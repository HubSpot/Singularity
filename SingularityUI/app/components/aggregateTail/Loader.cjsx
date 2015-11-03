
Loader = React.createClass

  render: ->
    if @props.isVisable
      <div className="loading-spinner">
        <div className="loader">
          <div className="box"></div>
          <div className="box"></div>
          <div className="box"></div>
          <div className="box"></div>
        </div>
        <div id="loadingText" className="text">
          {@props.text}
        </div>
      </div>
    else
      <div></div>

module.exports = Loader
