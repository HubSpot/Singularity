InfiniteScroll =

  getInitialState: ->
    {
      renderProgress: 0
      lastRender: 0
    }

  getDefaultProps: ->
    {
      renderAtOnce: 50
      offset: 250
    }

  componentWillMount: ->
    @tasksRowsCache = []

  componentDidMount: ->
    @attachScrollListener()
  
  scrollListener: ->      
    if (window.innerHeight + window.scrollY + @props.offset) >= document.body.offsetHeight
      @renderTableChunk()

  attachScrollListener: ->
    window.addEventListener 'scroll', @scrollListener
    window.addEventListener 'resize', @scrollListener
    @scrollListener()

  detachScrollListener: ->
    window.removeEventListener 'scroll', @scrollListener
    window.removeEventListener 'resize', @scrollListener

  renderTableChunk: ->
    newProgress = @state.renderProgress + @props.renderAtOnce
    @setState { 
      renderProgress: @state.renderProgress + @props.renderAtOnce
      lastRender: @state.renderProgress
    }


module.exports = InfiniteScroll