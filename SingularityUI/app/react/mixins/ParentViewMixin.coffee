ParentViewMixin =
  componentDidMount: ->
    console.log 'componentDidMount MIXIN'
    # So global refresh can re-fetch
    app.bootstrapReactView @
    # Kick off the first fetch
    @refresh()

module.exports = ParentViewMixin